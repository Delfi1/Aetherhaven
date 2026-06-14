package com.hexvane.aetherhaven.construction.assembly;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.pathtool.PathDebugPreviewUtil;
import com.hexvane.aetherhaven.placement.PlotFootprintOverlayRefresh;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-player {@link PathDebugPreviewUtil} ghost cubes for assembly frontier cells.
 *
 * <p>{@link com.hypixel.hytale.protocol.packets.player.ClearDebugShapes} only on starting preview, swapping away from staff,
 * or after a {@linkplain #markStaffAssemblyBlockPlaced staff-driven} placement — otherwise autobuild would blank every
 * debug overlay and flash in sync.</p>
 *
 * <p>Every remaining tick redraws capped frontier cells without clearing so cube lifetimes refresh and brush “grow”
 * animation advances.</p>
 */
public final class PlotAssemblyPreviewSystem extends EntityTickingSystem<EntityStore> {
    /**
     * Quantize feet position before sphere tests so frontier cells sitting near the range limit do not pop in/out from
     * sub-voxel movement between ticks.
     */
    private static final double PREVIEW_OBSERVER_SNAP_GRID = 0.25;

    /** {@code true} while this player is actively showing staff + non-empty frontier preview (for enter/exit clear). */
    private static final ConcurrentHashMap<UUID, Boolean> ASSEMBLY_FRONTIER_PREVIEW_ACTIVE = new ConcurrentHashMap<>();

    /** Players who need {@link PathDebugPreviewUtil#clear} + redraw after committing a staff assembly block this tick. */
    private static final Set<UUID> STAFF_ASSEMBLY_FRONTIER_REFRESH = ConcurrentHashMap.newKeySet();

    /** Throttle idle clearing-marker redraws (shape lifetime is long; grow animation only needs ticks while brushing). */
    private static final long CLEARING_PREVIEW_IDLE_REDRAW_INTERVAL_NS = 250_000_000L;
    private static final ConcurrentHashMap<UUID, Long> LAST_CLEARING_PREVIEW_REDRAW_NS = new ConcurrentHashMap<>();
    /** Detects when visible clearing markers change (manual breaks, frontier shift) so we clear stale debug shapes. */
    private static final ConcurrentHashMap<UUID, Long> LAST_CLEARING_VISIBLE_FINGERPRINT = new ConcurrentHashMap<>();

    /** Same family as path-tool “replaceable” tint, shifted green. */
    private static final Vector3f NEXT_CELL_COLOR = new Vector3f(0.22f, 0.92f, 0.48f);

    private static final Vector3f OBSTRUCTION_CELL_COLOR = PathDebugPreviewUtil.COLOR_OBSTRUCTION_MARKER;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();
    @SuppressWarnings("unused")
    private final AetherhavenPlugin plugin;

    public PlotAssemblyPreviewSystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    /** Called when the building staff commits one assembly block for this player. */
    public static void markStaffAssemblyBlockPlaced(@Nullable UUID staffActor) {
        if (staffActor != null) {
            STAFF_ASSEMBLY_FRONTIER_REFRESH.add(staffActor);
        }
    }

    /**
     * After {@link com.hexvane.aetherhaven.placement.PlotPlacementWireframeOverlay#send} issues {@link
     * com.hypixel.hytale.protocol.packets.player.ClearDebugShapes}, re-paint assembly frontier cubes in the same task so
     * they are not missing until the next entity tick (plot/charter UI refreshes run on the world queue).
     */
    public static void repaintFrontierAfterExternalDebugClear(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        if (!ref.isValid()) {
            return;
        }
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return;
        }
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        UUID id = pr.getUuid();
        if (id == null) {
            return;
        }
        ItemStack hand = InventoryComponent.getItemInHand(store, ref);
        if (hand != null
            && !hand.isEmpty()
            && AetherhavenConstants.PATH_TOOL_ITEM_ID.equals(hand.getItemId())) {
            return;
        }
        boolean staffInHand = hand != null && !hand.isEmpty() && BuildingStaffTiers.isBuildingStaff(hand.getItemId());
        if (!staffInHand) {
            return;
        }
        World world = store.getExternalData().getWorld();
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3d obsForRange = snapObserverForAssemblyPreview(tc.getPosition());
        boolean hasClearing = AssemblyWorldRegistry.anyJobInPhase(world, PlotAssemblyPhase.CLEARING);
        boolean hasPlacing = AssemblyWorldRegistry.anyJobInPhase(world, PlotAssemblyPhase.PLACING);
        List<Vector3i> obstructionCells = hasClearing ? new ArrayList<>() : List.of();
        if (hasClearing) {
            AssemblyObstructionWorldCells.collectWithinDefaultRange(world, p, obsForRange, obstructionCells);
        }
        List<Vector3i> frontierCells = hasPlacing ? new ArrayList<>() : List.of();
        if (hasPlacing) {
            AssemblyFrontierWorldCells.collectWithinDefaultRange(world, p, obsForRange, frontierCells);
        }
        if (obstructionCells.isEmpty() && frontierCells.isEmpty()) {
            return;
        }
        long nowNs = System.nanoTime();
        BuildingStaffAssemblyChannelComponent channel =
            store.getComponent(ref, BuildingStaffAssemblyChannelComponent.getComponentType());
        if (channel != null) {
            channel.setBrushChebyshevRadius(BuildingStaffTiers.assemblyBrushChebyshevRadius(hand.getItemId()));
        }
        int clearingMaxDraw = AetherhavenConstants.BUILDING_STAFF_CLEARING_PREVIEW_MAX_GHOST_CELLS;
        int frontierMaxDraw = AetherhavenConstants.BUILDING_STAFF_ASSEMBLY_PREVIEW_MAX_GHOST_CELLS;
        List<Vector3i> drawObstruction =
            obstructionCells.isEmpty()
                ? List.of()
                : obstructionCells.size() <= clearingMaxDraw
                    ? obstructionCells
                    : cappedObstructionPreviewCells(obstructionCells, clearingMaxDraw, channel, nowNs, obsForRange);
        List<Vector3i> drawFrontier =
            frontierCells.isEmpty()
                ? List.of()
                : frontierCells.size() <= frontierMaxDraw
                    ? frontierCells
                    : cappedPreviewCells(frontierCells, frontierMaxDraw, channel, nowNs, obsForRange);
        redrawObstructionCells(pr, world, drawObstruction, true, channel, nowNs);
        redrawAssemblyFrontierCells(pr, world, drawFrontier, true, channel, nowNs);
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        World world = store.getExternalData().getWorld();
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return;
        }
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        UUID previewCacheKey = pr.getUuid();
        if (previewCacheKey == null) {
            return;
        }
        ItemStack hand = InventoryComponent.getItemInHand(commandBuffer, ref);
        if (hand != null
            && !hand.isEmpty()
            && AetherhavenConstants.PATH_TOOL_ITEM_ID.equals(hand.getItemId())) {
            if (ASSEMBLY_FRONTIER_PREVIEW_ACTIVE.remove(previewCacheKey) != null) {
                STAFF_ASSEMBLY_FRONTIER_REFRESH.remove(previewCacheKey);
                LAST_CLEARING_PREVIEW_REDRAW_NS.remove(previewCacheKey);
                LAST_CLEARING_VISIBLE_FINGERPRINT.remove(previewCacheKey);
                clearDebugShapesThenRestoreFootprintUi(pr, ref, store);
            }
            return;
        }
        boolean staffInHand = hand != null && !hand.isEmpty() && BuildingStaffTiers.isBuildingStaff(hand.getItemId());
        if (!staffInHand) {
            if (ASSEMBLY_FRONTIER_PREVIEW_ACTIVE.remove(previewCacheKey) != null) {
                STAFF_ASSEMBLY_FRONTIER_REFRESH.remove(previewCacheKey);
                LAST_CLEARING_PREVIEW_REDRAW_NS.remove(previewCacheKey);
                LAST_CLEARING_VISIBLE_FINGERPRINT.remove(previewCacheKey);
                clearDebugShapesThenRestoreFootprintUi(pr, ref, store);
            }
            return;
        }
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3d obsForRange = snapObserverForAssemblyPreview(tc.getPosition());
        boolean hasClearing = AssemblyWorldRegistry.anyJobInPhase(world, PlotAssemblyPhase.CLEARING);
        boolean hasPlacing = AssemblyWorldRegistry.anyJobInPhase(world, PlotAssemblyPhase.PLACING);
        List<Vector3i> obstructionCells = hasClearing ? new ArrayList<>() : List.of();
        if (hasClearing) {
            AssemblyObstructionWorldCells.collectWithinDefaultRange(world, p, obsForRange, obstructionCells);
        }
        List<Vector3i> frontierCells = hasPlacing ? new ArrayList<>() : List.of();
        if (hasPlacing) {
            AssemblyFrontierWorldCells.collectWithinDefaultRange(world, p, obsForRange, frontierCells);
        }
        if (obstructionCells.isEmpty() && frontierCells.isEmpty()) {
            if (ASSEMBLY_FRONTIER_PREVIEW_ACTIVE.remove(previewCacheKey) != null) {
                STAFF_ASSEMBLY_FRONTIER_REFRESH.remove(previewCacheKey);
                LAST_CLEARING_PREVIEW_REDRAW_NS.remove(previewCacheKey);
                LAST_CLEARING_VISIBLE_FINGERPRINT.remove(previewCacheKey);
                clearDebugShapesThenRestoreFootprintUi(pr, ref, store);
            }
            return;
        }
        if (hasPlacing) {
            frontierCells.sort(
                Comparator
                    .comparingInt((Vector3i v) -> v.x)
                    .thenComparingInt(v -> v.y)
                    .thenComparingInt(v -> v.z)
            );
        }
        long nowNs = System.nanoTime();
        BuildingStaffAssemblyChannelComponent channel =
            store.getComponent(ref, BuildingStaffAssemblyChannelComponent.getComponentType());
        BuildingStaffAssemblyChannelComponent channelForDraw =
            commandBuffer.getComponent(ref, BuildingStaffAssemblyChannelComponent.getComponentType());
        if (channelForDraw == null) {
            channelForDraw = channel;
        }
        int brushR = BuildingStaffTiers.assemblyBrushChebyshevRadius(hand.getItemId());
        if (channel != null) {
            channel.setBrushChebyshevRadius(brushR);
        }
        if (channelForDraw != null) {
            channelForDraw.setBrushChebyshevRadius(brushR);
        }
        int clearingMaxDraw = AetherhavenConstants.BUILDING_STAFF_CLEARING_PREVIEW_MAX_GHOST_CELLS;
        int frontierMaxDraw = AetherhavenConstants.BUILDING_STAFF_ASSEMBLY_PREVIEW_MAX_GHOST_CELLS;
        List<Vector3i> drawObstruction =
            obstructionCells.isEmpty()
                ? List.of()
                : obstructionCells.size() <= clearingMaxDraw
                    ? obstructionCells
                    : cappedObstructionPreviewCells(obstructionCells, clearingMaxDraw, channelForDraw, nowNs, obsForRange);
        List<Vector3i> drawFrontier =
            frontierCells.isEmpty()
                ? List.of()
                : frontierCells.size() <= frontierMaxDraw
                    ? frontierCells
                    : cappedPreviewCells(frontierCells, frontierMaxDraw, channelForDraw, nowNs, obsForRange);

        boolean entering = ASSEMBLY_FRONTIER_PREVIEW_ACTIVE.put(previewCacheKey, Boolean.TRUE) == null;
        boolean staffRefresh = STAFF_ASSEMBLY_FRONTIER_REFRESH.remove(previewCacheKey);
        long clearingFingerprint = clearingVisibleFingerprint(drawObstruction);
        Long prevClearingFingerprint = LAST_CLEARING_VISIBLE_FINGERPRINT.get(previewCacheKey);
        boolean clearingSetChanged =
            !drawObstruction.isEmpty()
                && (prevClearingFingerprint == null || prevClearingFingerprint.longValue() != clearingFingerprint);
        if (clearingSetChanged) {
            LAST_CLEARING_VISIBLE_FINGERPRINT.put(previewCacheKey, clearingFingerprint);
        }

        if (entering || staffRefresh || clearingSetChanged) {
            clearDebugShapesThenRestoreFootprintUi(pr, ref, store);
        }
        if (shouldRedrawClearingPreview(previewCacheKey, nowNs, entering, staffRefresh, channelForDraw, !drawObstruction.isEmpty())) {
            redrawObstructionCells(pr, world, drawObstruction, staffInHand, channelForDraw, nowNs);
        }
        if (!drawFrontier.isEmpty()) {
            redrawAssemblyFrontierCells(pr, world, drawFrontier, staffInHand, channelForDraw, nowNs);
        }
    }

    private static long clearingVisibleFingerprint(@Nonnull List<Vector3i> cells) {
        long fp = cells.size();
        for (int i = 0; i < cells.size(); i++) {
            Vector3i c = cells.get(i);
            fp = fp * 31L + c.x;
            fp = fp * 31L + c.y;
            fp = fp * 31L + c.z;
        }
        return fp;
    }

    private static boolean shouldRedrawClearingPreview(
        @Nonnull UUID playerId,
        long nowNs,
        boolean entering,
        boolean staffRefresh,
        @Nullable BuildingStaffAssemblyChannelComponent channel,
        boolean hasObstructionMarkers
    ) {
        if (!hasObstructionMarkers) {
            return false;
        }
        if (entering || staffRefresh) {
            LAST_CLEARING_PREVIEW_REDRAW_NS.put(playerId, nowNs);
            return true;
        }
        if (channel != null && channel.hasActiveTarget() && channel.isFresh(nowNs)) {
            LAST_CLEARING_PREVIEW_REDRAW_NS.put(playerId, nowNs);
            return true;
        }
        Long last = LAST_CLEARING_PREVIEW_REDRAW_NS.get(playerId);
        if (last != null && nowNs - last < CLEARING_PREVIEW_IDLE_REDRAW_INTERVAL_NS) {
            return false;
        }
        LAST_CLEARING_PREVIEW_REDRAW_NS.put(playerId, nowNs);
        return true;
    }

    private static void redrawObstructionCells(
        @Nonnull PlayerRef pr,
        @Nonnull World world,
        @Nonnull List<Vector3i> cellsInRange,
        boolean staffInHand,
        @Nullable BuildingStaffAssemblyChannelComponent channel,
        long nowNs
    ) {
        for (Vector3i cell : cellsInRange) {
            double grow01 =
                staffInHand
                    && channel != null
                    && channel.cellMatchesBrush(cell.x, cell.y, cell.z)
                    && channel.isFresh(nowNs)
                    ? channel.channelGrow01(nowNs)
                    : 0.0;
            PathDebugPreviewUtil.drawObstructionCellMarkers(
                pr, cell.x, cell.y, cell.z, OBSTRUCTION_CELL_COLOR, world, grow01
            );
        }
    }

    /**
     * Like {@link #cappedPreviewCells} but fills overflow with evenly spaced cells so large footprints still show
     * distant obstructions during clearing (not only the nearest subset to the player).
     */
    @Nonnull
    private static List<Vector3i> cappedObstructionPreviewCells(
        @Nonnull List<Vector3i> sortedFull,
        int maxDraw,
        @Nullable BuildingStaffAssemblyChannelComponent channel,
        long nowNs,
        @Nonnull Vector3d ppos
    ) {
        if (sortedFull.size() <= maxDraw) {
            return sortedFull;
        }
        ArrayList<Vector3i> priority = new ArrayList<>();
        if (channel != null && channel.hasActiveTarget() && channel.isFresh(nowNs)) {
            for (int i = 0; i < sortedFull.size(); i++) {
                Vector3i c = sortedFull.get(i);
                if (channel.cellMatchesBrush(c.x, c.y, c.z)) {
                    priority.add(c);
                }
            }
        }
        if (priority.size() >= maxDraw) {
            priority.sort(
                Comparator
                    .comparingInt((Vector3i v) -> v.x)
                    .thenComparingInt(v -> v.y)
                    .thenComparingInt(v -> v.z)
            );
            return new ArrayList<>(priority.subList(0, maxDraw));
        }
        ArrayList<Vector3i> out = new ArrayList<>(maxDraw);
        out.addAll(priority);
        int slots = maxDraw - out.size();
        int n = sortedFull.size();
        for (int s = 0; s < slots; s++) {
            int idx = (s * n) / slots;
            Vector3i c = sortedFull.get(Math.min(idx, n - 1));
            if (!cellOccursIn(c, out)) {
                out.add(c);
            }
        }
        if (out.size() >= maxDraw) {
            return out;
        }
        ArrayList<Vector3i> rest = new ArrayList<>(sortedFull.size());
        for (int i = 0; i < sortedFull.size(); i++) {
            Vector3i c = sortedFull.get(i);
            if (!cellOccursIn(c, out)) {
                rest.add(c);
            }
        }
        rest.sort(
            Comparator.comparingDouble((Vector3i c) -> {
                double dx = c.x + 0.5 - ppos.x;
                double dy = c.y + 0.5 - ppos.y;
                double dz = c.z + 0.5 - ppos.z;
                return dx * dx + dy * dy + dz * dz;
            })
        );
        for (int i = 0; i < rest.size() && out.size() < maxDraw; i++) {
            out.add(rest.get(i));
        }
        return out;
    }

    /**
     * Keeps every cell in the active brush volume (for growth tint) plus nearest other frontier cells to the player up
     * to {@code maxDraw}. Without this, a pure “nearest N” cap can omit the aimed brush region entirely.
     */
    @Nonnull
    private static List<Vector3i> cappedPreviewCells(
        @Nonnull List<Vector3i> sortedFull,
        int maxDraw,
        @Nullable BuildingStaffAssemblyChannelComponent channel,
        long nowNs,
        @Nonnull Vector3d ppos
    ) {
        ArrayList<Vector3i> priority = new ArrayList<>();
        if (channel != null && channel.hasActiveTarget() && channel.isFresh(nowNs)) {
            for (int i = 0; i < sortedFull.size(); i++) {
                Vector3i c = sortedFull.get(i);
                if (channel.cellMatchesBrush(c.x, c.y, c.z)) {
                    priority.add(c);
                }
            }
        }
        if (priority.size() >= maxDraw) {
            priority.sort(
                Comparator
                    .comparingInt((Vector3i v) -> v.x)
                    .thenComparingInt(v -> v.y)
                    .thenComparingInt(v -> v.z)
            );
            return new ArrayList<>(priority.subList(0, maxDraw));
        }
        ArrayList<Vector3i> rest = new ArrayList<>(sortedFull.size());
        for (int i = 0; i < sortedFull.size(); i++) {
            Vector3i c = sortedFull.get(i);
            if (!cellOccursIn(c, priority)) {
                rest.add(c);
            }
        }
        rest.sort(
            Comparator.comparingDouble((Vector3i c) -> {
                double dx = c.x + 0.5 - ppos.x;
                double dy = c.y + 0.5 - ppos.y;
                double dz = c.z + 0.5 - ppos.z;
                return dx * dx + dy * dy + dz * dz;
            })
        );
        ArrayList<Vector3i> out = new ArrayList<>(maxDraw);
        out.addAll(priority);
        for (int i = 0; i < rest.size() && out.size() < maxDraw; i++) {
            out.add(rest.get(i));
        }
        return out;
    }

    private static boolean cellOccursIn(@Nonnull Vector3i cell, @Nonnull ArrayList<Vector3i> list) {
        for (int i = 0; i < list.size(); i++) {
            Vector3i o = list.get(i);
            if (o.x == cell.x && o.y == cell.y && o.z == cell.z) {
                return true;
            }
        }
        return false;
    }

    private static void clearDebugShapesThenRestoreFootprintUi(
        @Nonnull PlayerRef pr,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        PathDebugPreviewUtil.clear(pr);
        PlotFootprintOverlayRefresh.afterClearDebugShapes(ref, store);
    }

    private static void redrawAssemblyFrontierCells(
        @Nonnull PlayerRef pr,
        @Nonnull World world,
        @Nonnull List<Vector3i> cellsInRange,
        boolean staffInHand,
        @Nullable BuildingStaffAssemblyChannelComponent channel,
        long nowNs
    ) {
        for (Vector3i cell : cellsInRange) {
            double grow01 =
                staffInHand
                    && channel != null
                    && channel.cellMatchesBrush(cell.x, cell.y, cell.z)
                    && channel.isFresh(nowNs)
                    ? channel.channelGrow01(nowNs)
                    : 0.0;
            PathDebugPreviewUtil.drawAssemblyFrontierCellCube(pr, cell.x, cell.y, cell.z, NEXT_CELL_COLOR, world, grow01);
        }
    }

    @Nonnull
    private static Vector3d snapObserverForAssemblyPreview(@Nonnull Vector3d feetWorld) {
        double g = PREVIEW_OBSERVER_SNAP_GRID;
        return new Vector3d(
            Math.round(feetWorld.x / g) * g,
            Math.round(feetWorld.y / g) * g,
            Math.round(feetWorld.z / g) * g
        );
    }
}
