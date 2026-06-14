package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.entity.EntityChunkUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Removes transient plot preview block ghosts when their column unloads so vanilla
 * {@code UpdateLocationSystems} does not warn every tick for thousands of entities.
 */
public final class PlotBlockPreviewCleanupSystem extends TickingSystem<EntityStore> {
    private static final float INTERVAL_SEC = 2.0f;

    private float timer;

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        timer += dt;
        if (timer < INTERVAL_SEC) {
            return;
        }
        timer = 0.0f;
        World world = store.getExternalData().getWorld();
        if (!world.isAlive()) {
            return;
        }
        store.forEachChunk(
            Query.and(
                BlockEntity.getComponentType(),
                EntityStore.REGISTRY.getNonSerializedComponentType(),
                TransformComponent.getComponentType()
            ),
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null) {
                        continue;
                    }
                    if (EntityChunkUtil.isPositionChunkInMemory(world, tc.getPosition())) {
                        continue;
                    }
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    if (ref.isValid()) {
                        commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                    }
                }
            }
        );
    }
}
