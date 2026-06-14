package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.construction.PrefabLocalOffset;
import com.hexvane.aetherhaven.construction.PrefabYaw;
import com.hexvane.aetherhaven.guild.marker.GuildHallAdventurerSpawnPositions;
import com.hexvane.aetherhaven.marker.MarkerFacingYaw;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Prefab-local vs world conversions for plot creator (matches placed-plot {@link Rotation} from building JSON). */
public final class PlotCreatorPrefabCoords {
    private PlotCreatorPrefabCoords() {}

    @Nonnull
    public static Rotation placementYaw(@Nonnull PlotCreatorDraft draft) {
        return PlotCreatorJsonWriter.parseRotationYaw(draft.getRotationYaw());
    }

    @Nonnull
    public static Vector3i prefabAnchorWorld(@Nonnull PlotCreatorDraft draft) {
        Vector3i sign = draft.getPlotAnchor();
        if (sign == null) {
            throw new IllegalStateException("plot anchor not set");
        }
        return resolvePrefabAnchorWorld(sign, draft.getPlotAnchorOffset(), placementYaw(draft));
    }

    @Nonnull
    public static Vector3i standWorldBlock(@Nonnull PlotCreatorDraft draft, @Nonnull Vector3i clickedBlock) {
        return new Vector3i(
            clickedBlock.x,
            clickedBlock.y + PlotCreatorSpawnLocations.STAND_BLOCK_ABOVE_CLICK,
            clickedBlock.z
        );
    }

    @Nonnull
    public static Vector3i standWorldBlockFromPrefabLocal(
        @Nonnull PlotCreatorDraft draft,
        int prefabLocalX,
        int prefabLocalY,
        int prefabLocalZ
    ) {
        Vector3i anchor = prefabAnchorWorld(draft);
        Vector3i d = PrefabLocalOffset.rotate(placementYaw(draft), prefabLocalX, prefabLocalY, prefabLocalZ);
        return new Vector3i(anchor.x + d.x, anchor.y + d.y, anchor.z + d.z);
    }

    @Nonnull
    public static int[] standPrefabLocal(@Nonnull PlotCreatorDraft draft, @Nonnull Vector3i standWorldBlock) {
        Vector3i anchor = prefabAnchorWorld(draft);
        Rotation yaw = placementYaw(draft);
        int dx = standWorldBlock.x - anchor.x;
        int dy = standWorldBlock.y - anchor.y;
        int dz = standWorldBlock.z - anchor.z;
        Vector3i local = PrefabLocalOffset.inverseRotateWorldDelta(yaw, dx, dy, dz);
        return new int[] {local.x, local.y, local.z};
    }

    public static float standPrefabYawFacingPlayer(
        @Nonnull PlotCreatorDraft draft,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        int prefabLocalX,
        int prefabLocalY,
        int prefabLocalZ
    ) {
        Vector3i anchor = prefabAnchorWorld(draft);
        Rotation yaw = placementYaw(draft);
        Vector3d standPos =
            GuildHallAdventurerSpawnPositions.fromPrefabLocalStandCell(
                anchor,
                yaw,
                prefabLocalX,
                prefabLocalY,
                prefabLocalZ
            );
        TransformComponent playerTc = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTc == null) {
            return 0f;
        }
        float worldYaw = MarkerFacingYaw.yawFacingToward(standPos, playerTc.getPosition());
        return PrefabYaw.prefabFromWorld(yaw, worldYaw);
    }

    @Nonnull
    private static Vector3i resolvePrefabAnchorWorld(
        @Nonnull Vector3i plotSignBlockWorldPos,
        @Nonnull int[] plotAnchorOffset,
        @Nonnull Rotation placementYaw
    ) {
        Vector3i logical =
            new Vector3i(
                plotSignBlockWorldPos.x,
                plotSignBlockWorldPos.y - AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR,
                plotSignBlockWorldPos.z
            );
        Vector3i off = PrefabLocalOffset.rotate(placementYaw, plotAnchorOffset[0], plotAnchorOffset[1], plotAnchorOffset[2]);
        return new Vector3i(logical.x + off.x, logical.y + off.y, logical.z + off.z);
    }
}
