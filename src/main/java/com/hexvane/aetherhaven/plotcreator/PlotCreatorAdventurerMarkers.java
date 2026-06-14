package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.construction.PrefabYaw;
import com.hexvane.aetherhaven.guild.marker.AdventurerSpawnMarkerEntity;
import com.hexvane.aetherhaven.guild.marker.AdventurerSpawnMarkerSpawner;
import com.hexvane.aetherhaven.marker.MarkerEntityProximity;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Spawns {@link AdventurerSpawnMarkerEntity} in the world so plot creator prefab export includes all adventurer spots. */
public final class PlotCreatorAdventurerMarkers {
    private static final double MATCH_DIST = 0.6;
    private static final double MATCH_DIST_SQ = MATCH_DIST * MATCH_DIST;
    private static final double REMOVE_NEAR_BLOCK_DIST = 2.0;

    private PlotCreatorAdventurerMarkers() {}

    /** Safe during interaction ticks (uses {@link CommandBuffer#addEntity}). */
    public static void spawnForEntry(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull PlotCreatorDraft draft,
        @Nonnull PlotCreatorAdventurerSpawnEntry entry
    ) {
        Store<EntityStore> store = commandBuffer.getStore();
        Holder<EntityStore> holder = createMarkerHolder(world, store, draft, entry);
        if (holder != null) {
            commandBuffer.addEntity(holder, AddReason.SPAWN);
        }
    }

    /** Safe outside entity system ticks (prefab export sync). */
    public static void spawnForEntry(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotCreatorDraft draft,
        @Nonnull PlotCreatorAdventurerSpawnEntry entry
    ) {
        Holder<EntityStore> holder = createMarkerHolder(world, store, draft, entry);
        if (holder != null) {
            store.addEntity(holder, AddReason.SPAWN);
        }
    }

    @Nullable
    private static Holder<EntityStore> createMarkerHolder(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotCreatorDraft draft,
        @Nonnull PlotCreatorAdventurerSpawnEntry entry
    ) {
        Vector3d pos = PlotCreatorSpawnLocations.standCenterWorld(draft, entry.localArray());
        if (MarkerEntityProximity.isDuplicatePosition(store, AdventurerSpawnMarkerEntity.getComponentType(), pos)) {
            return null;
        }
        float worldYaw =
            PrefabYaw.worldFromPrefabLocal(PlotCreatorPrefabCoords.placementYaw(draft), entry.getYawRadians());
        return AdventurerSpawnMarkerSpawner.createHolder(world, pos, worldYaw);
    }

    /**
     * Ensures one marker entity exists per draft entry inside the plot bounds, and removes stray markers that no longer
     * match a saved spot (e.g. after reload or before prefab export).
     */
    public static void syncAll(
        @Nonnull World world,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull PlotCreatorDraft draft
    ) {
        if (draft.getPlotAnchor() == null) {
            return;
        }
        Store<EntityStore> store = commandBuffer.getStore();
        List<MarkerRow> markers = collectMarkersInBounds(store, draft);
        Set<Integer> matched = new HashSet<>();
        for (PlotCreatorAdventurerSpawnEntry entry : draft.getAdventurerSpawns()) {
            Vector3d expected = PlotCreatorSpawnLocations.standCenterWorld(draft, entry.localArray());
            int idx = indexNearestUnmatched(markers, matched, expected);
            if (idx < 0) {
                spawnForEntry(world, commandBuffer, draft, entry);
            } else {
                matched.add(idx);
            }
        }
        for (int i = 0; i < markers.size(); i++) {
            if (!matched.contains(i)) {
                Ref<EntityStore> ref = markers.get(i).ref;
                if (ref.isValid()) {
                    commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                }
            }
        }
    }

    /**
     * Ensures one marker entity exists per draft entry inside the plot bounds, and removes stray markers that no longer
     * match a saved spot. Use only outside entity system / interaction ticks.
     */
    public static void syncAll(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull PlotCreatorDraft draft) {
        if (draft.getPlotAnchor() == null) {
            return;
        }
        List<MarkerRow> markers = collectMarkersInBounds(store, draft);
        Set<Integer> matched = new HashSet<>();
        for (PlotCreatorAdventurerSpawnEntry entry : draft.getAdventurerSpawns()) {
            Vector3d expected = PlotCreatorSpawnLocations.standCenterWorld(draft, entry.localArray());
            int idx = indexNearestUnmatched(markers, matched, expected);
            if (idx < 0) {
                spawnForEntry(world, store, draft, entry);
            } else {
                matched.add(idx);
            }
        }
        for (int i = 0; i < markers.size(); i++) {
            if (!matched.contains(i)) {
                Ref<EntityStore> ref = markers.get(i).ref;
                if (ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                }
            }
        }
    }

    /** Removes the nearest adventurer marker near {@code clickedBlock} when within range. */
    public static boolean removeMarkerNear(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3i clickedBlock
    ) {
        Store<EntityStore> store = commandBuffer.getStore();
        Ref<EntityStore> ref =
            MarkerEntityProximity.findNearestNearBlock(
                store,
                AdventurerSpawnMarkerEntity.getComponentType(),
                clickedBlock,
                REMOVE_NEAR_BLOCK_DIST
            );
        if (ref != null && ref.isValid()) {
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            return true;
        }
        return false;
    }

    @Nonnull
    private static List<MarkerRow> collectMarkersInBounds(@Nonnull Store<EntityStore> store, @Nonnull PlotCreatorDraft draft) {
        List<MarkerRow> rows = new ArrayList<>();
        store.forEachChunk(
            Query.and(AdventurerSpawnMarkerEntity.getComponentType(), TransformComponent.getComponentType()),
            (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null) {
                        continue;
                    }
                    Vector3d p = tc.getPosition();
                    Vector3i block = new Vector3i((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
                    if (!draft.isInsideBounds(block)) {
                        continue;
                    }
                    rows.add(new MarkerRow(chunk.getReferenceTo(i), new Vector3d(p)));
                }
            }
        );
        return rows;
    }

    private static int indexNearestUnmatched(
        @Nonnull List<MarkerRow> markers,
        @Nonnull Set<Integer> matched,
        @Nonnull Vector3d expected
    ) {
        int best = -1;
        double bestDistSq = MATCH_DIST_SQ;
        for (int i = 0; i < markers.size(); i++) {
            if (matched.contains(i)) {
                continue;
            }
            Vector3d p = markers.get(i).position;
            double dx = p.x - expected.x;
            double dy = p.y - expected.y;
            double dz = p.z - expected.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= bestDistSq) {
                bestDistSq = d2;
                best = i;
            }
        }
        return best;
    }

    private record MarkerRow(@Nonnull Ref<EntityStore> ref, @Nonnull Vector3d position) {}
}
