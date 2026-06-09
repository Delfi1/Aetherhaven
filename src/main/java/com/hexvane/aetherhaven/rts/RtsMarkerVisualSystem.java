package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class RtsMarkerVisualSystem extends EntityTickingSystem<EntityStore> {
    /** Re-start the active emitter before its short system lifespan expires. */
    private static final int MARKER_REFRESH_TICKS = 4;
    private static final double MIN_VIEW_RADIUS = 64.0;

    private enum MarkerKind {
        NONE,
        GREY,
        BLUE,
        RED
    }

    private static final class ActiveMarker {
        MarkerKind kind = MarkerKind.NONE;
        int lastSpawnTick;
    }

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();
    @SuppressWarnings("unused")
    private final AetherhavenPlugin plugin;
    private final Map<UUID, Map<UUID, ActiveMarker>> markersByCommander = new HashMap<>();
    private final Map<UUID, Set<UUID>> lastSelectedByCommander = new HashMap<>();
    private int tickCounter;

    public RtsMarkerVisualSystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(RtsCommandPlayerComponent.getComponentType(), Player.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        tickCounter++;
        RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
        if (session == null) {
            return;
        }
        UUIDComponent commanderUuid = chunk.getComponent(index, UUIDComponent.getComponentType());
        UUID commanderId = commanderUuid != null ? commanderUuid.getUuid() : null;
        if (commanderId == null) {
            return;
        }
        Ref<EntityStore> commanderRef = chunk.getReferenceTo(index);
        List<Ref<EntityStore>> audience = List.of(commanderRef);

        if (!session.isActive()) {
            clearTrackedMarkers(store, commanderId, audience);
            return;
        }
        TownRecord town = RtsSelectionService.townForSession(store, store.getExternalData().getWorld(), plugin, session);
        if (town == null) {
            return;
        }

        double cx = session.getFocusX();
        double cz = session.getFocusZ();
        double viewRadius = viewRadiusBlocks(session);
        Set<UUID> selected = Set.copyOf(session.getSelectedGuardUuids());
        Set<UUID> visibleThisTick = new HashSet<>();
        Map<UUID, ActiveMarker> guardMarkers = markersByCommander.computeIfAbsent(commanderId, id -> new HashMap<>());

        if (selectionChanged(commanderId, selected)) {
            invalidateAllMarkerTracks(guardMarkers);
        }

        for (Ref<EntityStore> guardRef : RtsGuardDirectory.livingGuardRefs(town, store)) {
            TransformComponent tc = store.getComponent(guardRef, TransformComponent.getComponentType());
            if (tc == null || !nearFocus(tc.getPosition(), cx, cz, viewRadius)) {
                continue;
            }
            UUIDComponent uc = store.getComponent(guardRef, UUIDComponent.getComponentType());
            if (uc == null) {
                continue;
            }
            UUID guardId = uc.getUuid();
            visibleThisTick.add(guardId);
            MarkerKind kind = selected.contains(guardId) ? MarkerKind.BLUE : MarkerKind.GREY;
            ensureMarker(guardRef, kind, audience, store, guardMarkers.computeIfAbsent(guardId, id -> new ActiveMarker()));
        }

        List<Ref<EntityStore>> hostiles = new ArrayList<>();
        RtsHostileQuery.collectHostilesInBox(
            store,
            cx - viewRadius,
            cx + viewRadius,
            cz - viewRadius,
            cz + viewRadius,
            hostiles
        );
        for (Ref<EntityStore> hostile : hostiles) {
            TransformComponent tc = store.getComponent(hostile, TransformComponent.getComponentType());
            if (tc == null) {
                continue;
            }
            UUIDComponent uc = store.getComponent(hostile, UUIDComponent.getComponentType());
            if (uc == null) {
                continue;
            }
            UUID hostileId = uc.getUuid();
            visibleThisTick.add(hostileId);
            ensureMarker(hostile, MarkerKind.RED, audience, store, guardMarkers.computeIfAbsent(hostileId, id -> new ActiveMarker()));
        }

        pruneMarkers(guardMarkers, visibleThisTick, audience, store);
    }

    private boolean selectionChanged(@Nonnull UUID commanderId, @Nonnull Set<UUID> selected) {
        Set<UUID> previous = lastSelectedByCommander.get(commanderId);
        if (previous != null && previous.equals(selected)) {
            return false;
        }
        lastSelectedByCommander.put(commanderId, new HashSet<>(selected));
        return true;
    }

    private static void invalidateAllMarkerTracks(@Nonnull Map<UUID, ActiveMarker> guardMarkers) {
        for (ActiveMarker track : guardMarkers.values()) {
            track.kind = MarkerKind.NONE;
            track.lastSpawnTick = 0;
        }
    }

    private void ensureMarker(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull MarkerKind desired,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull Store<EntityStore> store,
        @Nonnull ActiveMarker track
    ) {
        boolean kindChanged = track.kind != desired;
        boolean needsRefresh = !kindChanged
            && tickCounter - track.lastSpawnTick >= MARKER_REFRESH_TICKS;
        if (!kindChanged && !needsRefresh) {
            return;
        }
        String particle = particleFor(desired);
        String nodeName = nodeNameFor(desired);
        if (particle == null || nodeName == null) {
            return;
        }
        if (kindChanged) {
            RtsModelMarkerUtil.clearAttachedMarker(entityRef, audience, store);
        }
        if (RtsModelMarkerUtil.spawnAttachedMarker(entityRef, particle, nodeName, audience, store)) {
            track.kind = desired;
            track.lastSpawnTick = tickCounter;
        }
    }

    private static void pruneMarkers(
        @Nonnull Map<UUID, ActiveMarker> guardMarkers,
        @Nonnull Set<UUID> visibleThisTick,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull Store<EntityStore> store
    ) {
        Iterator<Map.Entry<UUID, ActiveMarker>> it = guardMarkers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveMarker> entry = it.next();
            if (visibleThisTick.contains(entry.getKey())) {
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(entry.getKey());
            if (ref != null && ref.isValid()) {
                RtsModelMarkerUtil.clearAttachedMarker(ref, audience, store);
            }
            it.remove();
        }
    }

    private void clearTrackedMarkers(
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID commanderId,
        @Nonnull List<Ref<EntityStore>> audience
    ) {
        Map<UUID, ActiveMarker> guardMarkers = markersByCommander.remove(commanderId);
        lastSelectedByCommander.remove(commanderId);
        if (guardMarkers == null || guardMarkers.isEmpty()) {
            return;
        }
        for (UUID entityId : guardMarkers.keySet()) {
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(entityId);
            if (ref != null && ref.isValid()) {
                RtsModelMarkerUtil.clearAttachedMarker(ref, audience, store);
            }
        }
    }

    @Nullable
    private static String particleFor(@Nonnull MarkerKind kind) {
        return switch (kind) {
            case BLUE -> AetherhavenConstants.RTS_MARKER_BLUE_PARTICLE;
            case GREY -> AetherhavenConstants.RTS_MARKER_GREY_PARTICLE;
            case RED -> AetherhavenConstants.RTS_MARKER_RED_PARTICLE;
            case NONE -> null;
        };
    }

    @Nullable
    private static String nodeNameFor(@Nonnull MarkerKind kind) {
        return switch (kind) {
            case GREY -> "AetherhavenRtsMarker_grey";
            case BLUE -> "AetherhavenRtsMarker_blue";
            case RED -> "AetherhavenRtsMarker_red";
            case NONE -> null;
        };
    }

    private static double viewRadiusBlocks(@Nonnull RtsCommandPlayerComponent session) {
        return Math.max(MIN_VIEW_RADIUS, session.getSavedViewRadiusBlocks() + session.getDistance() + 16.0);
    }

    private static boolean nearFocus(@Nonnull Vector3d pos, double cx, double cz, double radius) {
        double dx = pos.x - cx;
        double dz = pos.z - cz;
        return dx * dx + dz * dz <= radius * radius;
    }
}
