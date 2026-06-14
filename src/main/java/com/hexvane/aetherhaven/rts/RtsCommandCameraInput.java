package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.WorldInteraction;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3i;

/** Applies {@code LookAtPlane} mouse packets to {@link CameraManager} without vanilla interaction handling. */
public final class RtsCommandCameraInput {
    private RtsCommandCameraInput() {}

    public static void queueFeed(@Nonnull PlayerRef playerRef, @Nonnull MouseInteraction packet) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> feed(ref, store, packet));
    }

    private static void feed(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull MouseInteraction packet
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        CameraManager camera = store.getComponent(playerRef, CameraManager.getComponentType());
        if (camera == null) {
            return;
        }

        Vector3i targetBlock = targetBlock(packet.worldInteraction);
        if (packet.mouseButton != null) {
            camera.handleMouseButtonState(packet.mouseButton.mouseButtonType, packet.mouseButton.state, targetBlock);
        }
        if (packet.screenPoint != null) {
            camera.setLastScreenPoint(new Vector2d(packet.screenPoint.x(), packet.screenPoint.y()));
        }
        camera.setLastBlockPosition(targetBlock);
    }

    @Nullable
    private static Vector3i targetBlock(@Nullable WorldInteraction worldInteraction) {
        if (worldInteraction == null) {
            return null;
        }
        BlockPosition blockPosition = worldInteraction.blockPosition;
        if (blockPosition == null) {
            return null;
        }
        return new Vector3i(blockPosition.x, blockPosition.y, blockPosition.z);
    }
}
