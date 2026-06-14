package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Keeps the commander at the saved exit body until client movement settles after RTS exit. */
public final class RtsExitMovementGuardSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        RtsExitMovementGuard.Guard guard = RtsExitMovementGuard.peek(pr.getUuid());
        if (guard == null) {
            return;
        }
        TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        org.joml.Vector3d pos = tc.getPosition();
        if (!RtsExitMovementGuard.needsCorrection(pos.x, pos.y, pos.z)) {
            return;
        }
        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        org.joml.Vector3d dest = new org.joml.Vector3d(guard.x(), guard.y(), guard.z());
        tc.getPosition().set(dest);
        tc.getRotation().set(guard.pitch(), guard.yaw(), guard.roll());
        commandBuffer.putComponent(playerRef, TransformComponent.getComponentType(), tc);
        commandBuffer.addComponent(
            playerRef,
            Teleport.getComponentType(),
            Teleport.createForPlayer(dest, tc.getRotation())
        );
        Velocity vel = chunk.getComponent(index, Velocity.getComponentType());
        if (vel != null) {
            vel.setZero();
            commandBuffer.putComponent(playerRef, Velocity.getComponentType(), vel);
        }
    }
}
