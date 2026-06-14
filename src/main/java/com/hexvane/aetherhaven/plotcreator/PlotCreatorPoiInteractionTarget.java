package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.plot.PlotBlockRotationUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Prefab-local stand cell one block in front of a placed POI block (from block yaw). */
public final class PlotCreatorPoiInteractionTarget {
    private PlotCreatorPoiInteractionTarget() {}

    public static void applyFromBlockFacing(
        @Nonnull World world,
        @Nonnull Vector3i blockWorldPos,
        @Nonnull int[] poiLocal,
        @Nonnull PlotCreatorPoiDraft poi
    ) {
        Rotation yaw = PlotBlockRotationUtil.readBlockYaw(world, blockWorldPos);
        int[] forward = horizontalForwardLocal(yaw);
        poi.setInteractionTargetLocal(poiLocal[0] + forward[0], poiLocal[1], poiLocal[2] + forward[2]);
    }

    /**
     * Prefab-local horizontal step from POI cell toward where an NPC should stand (matches bundled buildings such as
     * {@code plot_blacksmith_shop} with {@code interactionTargetLocalZ = localZ - 1} when yaw is {@link Rotation#None}).
     */
    @Nonnull
    private static int[] horizontalForwardLocal(@Nonnull Rotation yaw) {
        return switch (yaw) {
            case None -> new int[] {0, 0, -1};
            case Ninety -> new int[] {1, 0, 0};
            case OneEighty -> new int[] {0, 0, 1};
            case TwoSeventy -> new int[] {-1, 0, 0};
            default -> new int[] {0, 0, -1};
        };
    }
}
