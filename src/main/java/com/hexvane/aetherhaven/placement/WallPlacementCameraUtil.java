package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.wall.WallCardinal;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import org.joml.Vector3f;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/**
 * Wall wand placement camera: tilted birds-eye (not straight down) with a fixed view side instead of free pan.
 */
public final class WallPlacementCameraUtil {
    public static final float DEFAULT_DISTANCE = 22f;
    public static final float MIN_DISTANCE = 12f;
    public static final float MAX_DISTANCE = 48f;

    /** Pitch above straight down ({@code -π/2}); ~32° from horizontal so wall faces stay visible. */
    private static final float TILT_PITCH = (float) (-Math.PI / 2 + 0.58);

    private WallPlacementCameraUtil() {}

    @Nonnull
    public static ServerCameraSettings settings(
        float distance, @Nonnull Position offsetFromPlayer, @Nonnull WallCardinal viewFromSide
    ) {
        float d = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
        float yaw =
            switch (viewFromSide) {
                case NORTH -> (float) Math.PI;
                case EAST -> (float) (Math.PI * 0.5);
                case SOUTH -> 0.0F;
                case WEST -> (float) (-Math.PI * 0.5);
            };
        ServerCameraSettings cameraSettings = new ServerCameraSettings();
        cameraSettings.positionLerpSpeed = 0.1F;
        cameraSettings.rotationLerpSpeed = 0.12F;
        cameraSettings.distance = d;
        cameraSettings.displayCursor = true;
        cameraSettings.sendMouseMotion = true;
        cameraSettings.isFirstPerson = false;
        cameraSettings.movementForceRotationType = MovementForceRotationType.Custom;
        cameraSettings.eyeOffset = true;
        cameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        cameraSettings.positionOffset = offsetFromPlayer;
        cameraSettings.rotationType = RotationType.Custom;
        cameraSettings.rotation = new Direction(yaw, TILT_PITCH, 0.0F);
        cameraSettings.mouseInputType = MouseInputType.LookAtPlane;
        cameraSettings.planeNormal = new Vector3f(0.0F, 1.0F, 0.0F);
        return cameraSettings;
    }

    public static void apply(
        @Nonnull PlayerRef playerRef,
        float distance,
        @Nonnull WallCardinal viewFromSide,
        double playerX,
        double playerY,
        double playerZ,
        double focusX,
        double focusY,
        double focusZ
    ) {
        Position offset = new Position(focusX - playerX, focusY - playerY, focusZ - playerZ);
        playerRef
            .getPacketHandler()
            .writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, settings(distance, offset, viewFromSide)));
    }
}
