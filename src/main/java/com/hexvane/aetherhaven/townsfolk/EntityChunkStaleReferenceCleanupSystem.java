package com.hexvane.aetherhaven.townsfolk;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import java.util.Set;
import javax.annotation.Nonnull;

/** Prunes dead entity refs from {@code EntityChunk} before chunk save runs in the same world tick. */
public final class EntityChunkStaleReferenceCleanupSystem extends TickingSystem<ChunkStore> {
    @Nonnull
    private final Set<Dependency<ChunkStore>> dependencies = Set.of(
        new SystemDependency<>(Order.BEFORE, ChunkSavingSystems.Ticking.class)
    );

    @Nonnull
    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<ChunkStore> store) {
        PendingEntityRemovalService.pruneInvalidEntityReferences(store);
    }
}
