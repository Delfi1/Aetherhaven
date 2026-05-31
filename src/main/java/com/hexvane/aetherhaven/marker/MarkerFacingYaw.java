package com.hexvane.aetherhaven.marker;

import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Horizontal body yaw for debug markers and NPCs facing a world point (matches {@code PoiAutonomyVisuals}). */
public final class MarkerFacingYaw {
    private MarkerFacingYaw() {}

    /** Yaw (radians) for an entity at {@code from} to face toward {@code toward} on the XZ plane. */
    public static float yawFacingToward(@Nonnull Vector3d from, @Nonnull Vector3d toward) {
        double dx = toward.x - from.x;
        double dz = toward.z - from.z;
        if (dx * dx + dz * dz < 1.0e-8) {
            return 0f;
        }
        return (float) (Math.atan2(dx, dz) + Math.PI);
    }
}
