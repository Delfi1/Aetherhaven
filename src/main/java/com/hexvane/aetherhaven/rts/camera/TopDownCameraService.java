package com.hexvane.aetherhaven.rts.camera;

import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.MouseInputType;
import com.hypixel.hytale.protocol.MovementForceRotationType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.PositionDistanceOffsetType;
import com.hypixel.hytale.protocol.RotationType;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

/**
 * Top-down RTS camera: vanilla {@code /camera topdown} rig with raycast ground picking.
 * Hytale's {@link ServerCameraSettings} has no orthographic projection flag — selection
 * uses orthographic ground math in {@link com.hexvane.aetherhaven.rts.RtsScreenPickUtil}.
 */
public final class TopDownCameraService {
    /** Fixed pull-back sent in the top-down camera packet; altitude zoom is flight height only. */
    public static final float DEFAULT_DISTANCE = 20f;

    private TopDownCameraService() {}

    public static void apply(@Nonnull PlayerRef playerRef, float distance) {
        ServerCameraSettings cameraSettings = new ServerCameraSettings();
        cameraSettings.positionLerpSpeed = 0.05F;
        cameraSettings.rotationLerpSpeed = 0.08F;
        cameraSettings.distance = distance;
        cameraSettings.allowPitchControls = false;
        cameraSettings.displayCursor = true;
        cameraSettings.displayReticle = false;
        cameraSettings.sendMouseMotion = true;
        cameraSettings.isFirstPerson = false;
        cameraSettings.movementForceRotationType = MovementForceRotationType.Custom;
        cameraSettings.movementForceRotation = new Direction(0.0F, (float) (-Math.PI / 2), 0.0F);
        cameraSettings.eyeOffset = true;
        cameraSettings.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
        cameraSettings.positionOffset = new Position(0.0, 3.0, 0.0);
        cameraSettings.rotationType = RotationType.Custom;
        cameraSettings.rotation = new Direction(0.0F, (float) (-Math.PI / 2), 0.0F);
        cameraSettings.mouseInputType = MouseInputType.LookAtPlane;
        cameraSettings.planeNormal = new Vector3f(0.0F, 1.0F, 0.0F);
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, cameraSettings));
    }

    public static void reset(@Nonnull PlayerRef playerRef) {
        playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    /** Initial commander altitude above ground focus when entering command mode. */
    public static double commanderBodyY(double groundY, float distance) {
        return groundY + distance;
    }
}
