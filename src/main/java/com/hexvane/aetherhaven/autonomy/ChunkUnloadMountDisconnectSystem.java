package com.hexvane.aetherhaven.autonomy;

import com.hypixel.hytale.builtin.mounts.BlockMountComponent;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.NonTicking;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.EntityChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * When a chunk stops ticking, vanilla {@link EntityChunk.EntityChunkLoadingSystem} unloads live entities while the
 * chunk store is still processing. If those entities still have {@link com.hypixel.hytale.builtin.mounts.MountedComponent},
 * {@code MountSystems.RemoveMounted} calls {@code ChunkStore.removeComponent} and throws.
 *
 * <p>Runs before entity unload: disconnect mounts via {@link BlockMountRelease#disconnectForUnload} and queue empty seat
 * cleanup on the chunk command buffer.
 */
public final class ChunkUnloadMountDisconnectSystem extends RefChangeSystem<ChunkStore, NonTicking<ChunkStore>> {
    @Nonnull
    private final Archetype<ChunkStore> archetype =
        Archetype.of(WorldChunk.getComponentType(), EntityChunk.getComponentType());

    @Nonnull
    private final Set<Dependency<ChunkStore>> dependencies =
        Set.of(new SystemDependency<>(Order.BEFORE, EntityChunk.EntityChunkLoadingSystem.class));

    @Nonnull
    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return archetype;
    }

    @Nonnull
    @Override
    public ComponentType<ChunkStore, NonTicking<ChunkStore>> componentType() {
        return ChunkStore.REGISTRY.getNonTickingComponentType();
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<ChunkStore> ref,
        @Nonnull NonTicking<ChunkStore> component,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {
        EntityChunk entityChunk = store.getComponent(ref, EntityChunk.getComponentType());
        if (entityChunk == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        for (Ref<EntityStore> entityRef : entityChunk.getEntityReferences()) {
            if (!entityRef.isValid()) {
                continue;
            }
            Ref<ChunkStore> deadSeatRef = BlockMountRelease.disconnectForUnload(entityRef, entityStore);
            if (deadSeatRef != null) {
                commandBuffer.tryRemoveComponent(deadSeatRef, BlockMountComponent.getComponentType());
            }
        }
    }

    @Override
    public void onComponentSet(
        @Nonnull Ref<ChunkStore> ref,
        NonTicking<ChunkStore> oldComponent,
        @Nonnull NonTicking<ChunkStore> newComponent,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {}

    @Override
    public void onComponentRemoved(
        @Nonnull Ref<ChunkStore> ref,
        @Nonnull NonTicking<ChunkStore> component,
        @Nonnull Store<ChunkStore> store,
        @Nonnull CommandBuffer<ChunkStore> commandBuffer
    ) {}
}
