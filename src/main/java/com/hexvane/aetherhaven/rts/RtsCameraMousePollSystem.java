package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3i;

/**
 * Polls {@link CameraManager} mouse state when {@link MouseInteraction} packets arrive.
 * Enables box drag when the client sends mouse packets through the custom camera.
 */
public final class RtsCameraMousePollSystem extends EntityTickingSystem<EntityStore> {
    private static final class Track {
        MouseButtonState left = MouseButtonState.Released;
        MouseButtonState right = MouseButtonState.Released;
    }

    private final Map<UUID, Track> tracks = new HashMap<>();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            RtsCommandPlayerComponent.getComponentType(),
            Player.getComponentType(),
            CameraManager.getComponentType()
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
        RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        CameraManager camera = chunk.getComponent(index, CameraManager.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
        if (camera == null || pr == null) {
            return;
        }

        Track track = tracks.computeIfAbsent(pr.getUuid(), id -> new Track());
        MouseButtonState left = camera.getMouseButtonState(MouseButtonType.Left);
        MouseButtonState right = camera.getMouseButtonState(MouseButtonType.Right);
        Vector3i targetBlock = camera.getLastTargetBlock();
        Vector2dc screenRaw = camera.getLastScreenPoint();
        Vector2fc screen = screenRaw != null ? new Vector2f((float) screenRaw.x(), (float) screenRaw.y()) : null;

        RtsDiagnostics.cameraPollState(pr, left.name(), screenRaw != null, session.isBoxSelectActive());

        if (session.isBoxSelectActive()) {
            RtsPrimaryDragTracker.tickPendingReleases(playerRef, store, commandBuffer, session, pr);
        }

        if (session.isBoxSelectActive() && screen != null) {
            RtsPrimaryDragTracker.noteScreenMotion(pr.getUuid(), screen.x(), screen.y());
            RtsMouseInputListener.updateBoxDrag(playerRef, store, session, targetBlock, screen);
            commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
            RtsMouseInputListener.refreshBoxHud(playerRef, store, session);
        }

        if (left == MouseButtonState.Pressed && track.left != MouseButtonState.Pressed) {
            RtsDiagnostics.mouseClick(pr, "camera-press", targetBlock, screen);
            RtsMouseInputListener.processLeftButton(
                playerRef, store, commandBuffer, session, MouseButtonState.Pressed, targetBlock, screen
            );
        } else if (left == MouseButtonState.Pressed) {
            RtsMouseInputListener.handleMotion(playerRef, store, commandBuffer, session, targetBlock, screen);
        } else if (left == MouseButtonState.Released && track.left == MouseButtonState.Pressed) {
            RtsDiagnostics.mouseClick(pr, "camera-release", targetBlock, screen);
            RtsMouseInputListener.processLeftButton(
                playerRef, store, commandBuffer, session, MouseButtonState.Released, targetBlock, screen
            );
        }
        track.left = left;

        if (right == MouseButtonState.Pressed && track.right != MouseButtonState.Pressed) {
            RtsDiagnostics.mouseClick(pr, "camera-right-press", targetBlock, screen);
            RtsClickService.handleSecondaryClick(
                playerRef, store, commandBuffer, session, targetBlock, screen
            );
        }
        track.right = right;
    }
}
