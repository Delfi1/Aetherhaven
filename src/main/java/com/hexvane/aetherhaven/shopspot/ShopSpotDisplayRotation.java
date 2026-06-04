package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.plot.PlotBlockRotationUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Display item yaw from placement facing (block NESW rotation + mesh forward offset). */
public final class ShopSpotDisplayRotation {
    private static final float MESH_FORWARD_OFFSET = (float) Math.PI;

    private ShopSpotDisplayRotation() {}

    @Nonnull
    public static Rotation3f forRecord(@Nonnull World world, @Nonnull ShopSpotRecord record) {
        float yaw = record.getDisplayYawRadians();
        if (Float.isNaN(yaw)) {
            Rotation blockYaw = PlotBlockRotationUtil.readBlockYaw(world, record.getBlockPosition());
            yaw = (float) blockYaw.getRadians() + MESH_FORWARD_OFFSET;
        }
        return new Rotation3f(0.0F, yaw, 0.0F);
    }

    public static float yawFromPlacementRotation(@Nonnull com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple rotation) {
        return (float) rotation.yaw().getRadians() + MESH_FORWARD_OFFSET;
    }

    public static float yawFromBlockAt(@Nonnull World world, @Nonnull Vector3i blockPos) {
        Rotation blockYaw = PlotBlockRotationUtil.readBlockYaw(world, blockPos);
        return (float) blockYaw.getRadians() + MESH_FORWARD_OFFSET;
    }
}
