package com.hexvane.aetherhaven.marker;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Resolves debug marker entities by client entity id or proximity to a clicked block. */
public final class MarkerEntityProximity {
    private static final double DEFAULT_MAX_DIST = 2.0;
    private static final double DEFAULT_MAX_DIST_SQ = DEFAULT_MAX_DIST * DEFAULT_MAX_DIST;
    private static final double DEDUP_DIST_SQ = 0.5 * 0.5;

    private MarkerEntityProximity() {}

    @Nullable
    public static Ref<EntityStore> resolveTarget(
        @Nonnull Store<EntityStore> store,
        @Nonnull InteractionContext context,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType
    ) {
        return resolveTarget(store, context, markerComponentType, null, DEFAULT_MAX_DIST);
    }

    @Nullable
    public static Ref<EntityStore> resolveTarget(
        @Nonnull Store<EntityStore> store,
        @Nonnull InteractionContext context,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType,
        @Nullable Vector3i blockPos,
        double maxDist
    ) {
        @Nullable
        Ref<EntityStore> byEntityId = resolveByEntityId(store, context, markerComponentType);
        if (byEntityId != null) {
            return byEntityId;
        }
        if (blockPos == null) {
            return null;
        }
        return findNearestNearBlock(store, markerComponentType, blockPos, maxDist);
    }

    public static boolean hasNearby(
        @Nonnull Store<EntityStore> store,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType,
        @Nonnull Vector3d position,
        double maxDist
    ) {
        return findNearestNearPosition(store, markerComponentType, position, maxDist) != null;
    }

    @Nullable
    private static Ref<EntityStore> resolveByEntityId(
        @Nonnull Store<EntityStore> store,
        @Nonnull InteractionContext context,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType
    ) {
        @Nullable
        InteractionSyncData sync = context.getClientState();
        if (sync == null || sync.entityId <= 0) {
            return null;
        }
        @Nullable
        Ref<EntityStore> targetedRef = store.getExternalData().getRefFromNetworkId(sync.entityId);
        if (targetedRef != null
            && targetedRef.isValid()
            && store.getComponent(targetedRef, markerComponentType) != null) {
            return targetedRef;
        }
        return null;
    }

    @Nullable
    public static Ref<EntityStore> findNearestNearBlock(
        @Nonnull Store<EntityStore> store,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType,
        @Nonnull Vector3i blockPos,
        double maxDist
    ) {
        Vector3d center = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
        return findNearestNearPosition(store, markerComponentType, center, maxDist);
    }

    @Nullable
    public static Ref<EntityStore> findNearestNearPosition(
        @Nonnull Store<EntityStore> store,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType,
        @Nonnull Vector3d center,
        double maxDist
    ) {
        double maxDistSq = maxDist * maxDist;
        NearestMarker nearest = new NearestMarker();
        store.forEachChunk(
            Query.and(markerComponentType, TransformComponent.getComponentType()),
            (ArchetypeChunk<EntityStore> chunk, com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null) {
                        continue;
                    }
                    Vector3d p = tc.getPosition();
                    double dx = p.x - center.x;
                    double dy = p.y - center.y;
                    double dz = p.z - center.z;
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= maxDistSq && d2 < nearest.distSq) {
                        nearest.distSq = d2;
                        nearest.ref = chunk.getReferenceTo(i);
                    }
                }
            }
        );
        return nearest.ref;
    }

    private static final class NearestMarker {
        double distSq = Double.MAX_VALUE;
        @Nullable
        Ref<EntityStore> ref;
    }

    public static boolean isDuplicatePosition(
        @Nonnull Store<EntityStore> store,
        @Nonnull ComponentType<EntityStore, ?> markerComponentType,
        @Nonnull Vector3d position
    ) {
        return hasNearby(store, markerComponentType, position, Math.sqrt(DEDUP_DIST_SQ));
    }
}
