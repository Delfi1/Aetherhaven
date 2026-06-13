package com.hexvane.aetherhaven.bard;

import com.hypixel.hytale.builtin.audio.components.ForcedMusicTracker;
import com.hypixel.hytale.builtin.audio.systems.ForcedMusicSystems;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Applies per-player bard {@link com.hypixel.hytale.protocol.packets.world.UpdateForcedMusic}
 * after vanilla {@link ForcedMusicSystems.Tick} so only nearby players hear the performance.
 */
public final class BardMusicProximitySystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Query<EntityStore> query =
        Archetype.of(
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            TransformComponent.getComponentType(),
            UUIDComponent.getComponentType(),
            ForcedMusicTracker.getComponentType()
        );

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, ForcedMusicSystems.Tick.class));
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        BardActivePerformancesResource performances =
            store.getResource(BardActivePerformancesResource.getResourceType());
        long worldTick = store.getExternalData().getWorld().getTick();
        performances.rebuildForTick(store, worldTick);

        Ref<EntityStore> playerEntityRef = archetypeChunk.getReferenceTo(index);
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        UUIDComponent uuidComponent = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        ForcedMusicTracker tracker = archetypeChunk.getComponent(index, ForcedMusicTracker.getComponentType());
        if (playerEntityRef == null
            || !playerEntityRef.isValid()
            || transform == null
            || playerRef == null
            || uuidComponent == null
            || tracker == null) {
            return;
        }

        UUID playerId = uuidComponent.getUuid();
        BardMusicProximityState proximityState = store.getResource(BardMusicProximityState.getResourceType());

        var pos = transform.getPosition();
        int desiredContainer = performances.nearestMusic(pos.x, pos.y, pos.z).musicContainerIndex();
        int have = tracker.getCurrentContainerIndex();
        if (have == desiredContainer) {
            if (desiredContainer == 0 && proximityState.isListening(playerId)) {
                proximityState.clear(playerId);
            }
            return;
        }

        BardEnvironmentMusic.setForcedMusic(
            playerEntityRef,
            commandBuffer,
            store,
            playerRef,
            tracker,
            desiredContainer
        );
        proximityState.setActive(playerId, desiredContainer);
    }
}
