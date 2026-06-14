package com.hexvane.aetherhaven.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.spawning.world.component.WorldSpawnData;
import com.hypixel.hytale.server.spawning.world.system.WorldSpawningSystem;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Vanilla {@link WorldSpawningSystem} can call {@code Store.getComponent} on {@link Ref}&lt;ChunkStore&gt;
 * entries in {@link WorldSpawnData} after {@code ChunkUnloadingSystem} invalidates them in the same world tick.
 * Drop stale refs before spawning runs.
 */
public final class WorldSpawnStaleChunkRefCleanupSystem extends TickingSystem<ChunkStore> {
    @Nonnull
    private final Set<Dependency<ChunkStore>> dependencies =
        Set.of(new SystemDependency<>(Order.BEFORE, WorldSpawningSystem.class));

    @Nonnull
    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<ChunkStore> store) {
        World world = store.getExternalData().getWorld();
        if (!world.isAlive()) {
            return;
        }
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (entityStore == null) {
            return;
        }
        WorldSpawnData worldSpawnData = entityStore.getResource(WorldSpawnData.getResourceType());
        if (worldSpawnData == null) {
            return;
        }
        worldSpawnData.forEachEnvironmentSpawnData(
            env -> env.getChunkRefList().removeIf(ref -> ref == null || !ref.isValid())
        );
    }
}
