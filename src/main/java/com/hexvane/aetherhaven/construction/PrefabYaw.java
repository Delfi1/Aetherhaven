package com.hexvane.aetherhaven.construction;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;

/** Converts body yaw between prefab-local axes and world axes for a placed plot rotation. */
public final class PrefabYaw {
    private PrefabYaw() {}

    public static float worldFromPrefabLocal(@Nonnull Rotation placementYaw, float prefabYawRadians) {
        return prefabYawRadians + placementYawRadians(placementYaw);
    }

    public static float prefabFromWorld(@Nonnull Rotation placementYaw, float worldYawRadians) {
        return worldYawRadians - placementYawRadians(placementYaw);
    }

    private static float placementYawRadians(@Nonnull Rotation placementYaw) {
        return switch (placementYaw) {
            case Ninety -> (float) (Math.PI / 2.0);
            case OneEighty -> (float) Math.PI;
            case TwoSeventy -> (float) (3.0 * Math.PI / 2.0);
            default -> 0f;
        };
    }
}
