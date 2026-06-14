package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hexvane.aetherhaven.plot.PlotBlockRotationUtil;
import com.hexvane.aetherhaven.shopspot.ShopSpotRecord;
import com.hexvane.aetherhaven.shopspot.ShopSpotRegistry;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Tourist browsing: stand on the customer side of shop spot blocks and face the listing. */
public final class TouristShopSpotBrowse {
    private TouristShopSpotBrowse() {}

    @Nonnull
    public static List<ShopSpotRecord> listOnPlot(@Nonnull ShopSpotRegistry registry, @Nonnull UUID plotId) {
        List<ShopSpotRecord> out = new ArrayList<>();
        for (ShopSpotRecord record : registry.allRecords()) {
            if (plotId.equals(record.getPlotId())) {
                out.add(record);
            }
        }
        return out;
    }

    public static boolean plotHasShopSpots(@Nonnull ShopSpotRegistry registry, @Nonnull UUID plotId) {
        return !listOnPlot(registry, plotId).isEmpty();
    }

    @Nullable
    public static ShopSpotRecord pickNext(
        @Nonnull List<ShopSpotRecord> spots,
        @Nullable UUID excludeSpotId,
        @Nonnull Random random
    ) {
        if (spots.isEmpty()) {
            return null;
        }
        List<ShopSpotRecord> pool = new ArrayList<>();
        for (ShopSpotRecord spot : spots) {
            if (excludeSpotId == null || !excludeSpotId.equals(spot.getSpotId())) {
                pool.add(spot);
            }
        }
        if (pool.isEmpty()) {
            pool = spots;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * World feet position on the block in front of the shop spot (customer side from block yaw).
     *
     * @return {@code {x, y, z}} or null when no stand cell is found
     */
    @Nullable
    public static double[] customerStandWorld(@Nonnull World world, @Nonnull ShopSpotRecord spot) {
        Vector3i block = spot.getBlockPosition();
        int[] forward = horizontalForwardWorld(world, block);
        int cx = block.x + forward[0];
        int cz = block.z + forward[2];
        int standY = VillagerBlockUtil.findStandY(world, cx, cz, block.y + 3);
        if (standY == Integer.MIN_VALUE) {
            return null;
        }
        return new double[] {cx + 0.5, standY + 0.02, cz + 0.5};
    }

    public static void faceTowardShopSpot(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull ShopSpotRecord spot
    ) {
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3i block = spot.getBlockPosition();
        double dx = (block.x + 0.5) - tc.getPosition().x;
        double dz = (block.z + 0.5) - tc.getPosition().z;
        if (dx * dx + dz * dz < 1.0e-6) {
            return;
        }
        tc.getRotation().setYaw((float) (Math.atan2(dx, dz) + Math.PI));
        commandBuffer.putComponent(npcRef, TransformComponent.getComponentType(), tc);
    }

    @Nonnull
    private static int[] horizontalForwardWorld(@Nonnull World world, @Nonnull Vector3i blockWorldPos) {
        Rotation yaw = PlotBlockRotationUtil.readBlockYaw(world, blockWorldPos);
        return switch (yaw) {
            case None -> new int[] {0, 0, -1};
            case Ninety -> new int[] {1, 0, 0};
            case OneEighty -> new int[] {0, 0, 1};
            case TwoSeventy -> new int[] {-1, 0, 0};
            default -> new int[] {0, 0, -1};
        };
    }
}
