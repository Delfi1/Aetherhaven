package com.hexvane.aetherhaven.poi.marker;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.prefab.PrefabCopyableComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Ensures POI markers can be saved into prefabs. */
public final class PoiMarkerSystems {
    private PoiMarkerSystems() {}

    public static final class EnsurePrefabCopyable extends RefSystem<EntityStore> {
        @Nonnull
        private static final Query<EntityStore> QUERY = Query.and(PoiMarkerEntity.getComponentType());

        @Override
        public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            commandBuffer.ensureComponent(ref, PrefabCopyableComponent.getComponentType());
        }

        @Override
        public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {}

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return QUERY;
        }
    }
}
