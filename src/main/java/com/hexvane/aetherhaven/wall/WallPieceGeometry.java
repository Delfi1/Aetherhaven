package com.hexvane.aetherhaven.wall;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;

/**
 * Wall placement math: inputs and outputs are in <em>world</em> space ({@link WallCardinal} = world N/S/E/W,
 * sign positions = world block coords). Kit offsets in {@link WallKitCatalog} are prefab-local; each piece's
 * {@link Rotation} rotates them to world before adding to the logical anchor.
 */
public final class WallPieceGeometry {
    /** Sign / logical anchor offset for segments and towers (center of footprint on ground). */
    public static final int[] PLOT_ANCHOR_OFFSET = new int[] {0, 0, 0};

    private static final WallKitCatalog KIT = WallKitCatalog.get();

    private WallPieceGeometry() {}

    public static int segmentChainSpan() {
        return KIT.chainSpan();
    }

    /** @deprecated use {@link #segmentChainSpan()} */
    @Deprecated
    public static final int SEGMENT_CHAIN_SPAN = 16;

    public static int towerConnectionHalf(@Nonnull String constructionId) {
        return KIT.piece(constructionId).towerConnectionHalf();
    }

    /** @deprecated use {@link #towerConnectionHalf(String)} */
    @Deprecated
    public static final int TOWER_CONNECTION_HALF = 4;

    /**
     * World sign for the next piece. {@code worldExpandDir} is the pad the player chose (already mapped from UI to
     * world); piece yaws come from the chain planner from that direction.
     */
    @Nonnull
    public static Vector3i nextSignPosition(
        @Nonnull String fromConstructionId,
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull String toConstructionId,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal worldExpandDir,
        boolean fromIsTower,
        boolean newPieceIsTower
    ) {
        boolean mixedJoint = fromIsTower != newPieceIsTower;
        boolean segmentRun = !fromIsTower && !newPieceIsTower;
        boolean segmentToTower = mixedJoint && !fromIsTower && newPieceIsTower;
        boolean towerToSegmentRun = mixedJoint && fromIsTower && !newPieceIsTower;

        WallCardinal worldExit = worldExpandDir;
        WallCardinal worldEnter = worldExpandDir.opposite();
        if (segmentRun || towerToSegmentRun) {
            worldExit = worldExpandDir;
            worldEnter = worldExpandDir.opposite();
        }

        boolean jointOffsets = mixedJoint && !towerToSegmentRun;
        Vector3i fromExit =
            segmentToTower
                ? worldAttachPoint(fromConstructionId, fromSign, fromYaw, worldExpandDir, WallKitCatalog.OffsetKind.EXTERIOR)
                : worldAttachPoint(
                    fromConstructionId,
                    fromSign,
                    fromYaw,
                    worldExit,
                    jointOffsets && !fromIsTower
                        ? WallKitCatalog.OffsetKind.TOWER_JOINT
                        : fromIsTower
                            ? WallKitCatalog.OffsetKind.TOWER_CONNECTION
                            : WallKitCatalog.OffsetKind.CHAIN
                );
        int signY = fromSign.y;
        Vector3i probeSign = new Vector3i(0, signY, 0);
        Vector3i towerOriginProbe = logicalAnchor(probeSign);
        WallKitCatalog.OffsetKind enterKind =
            jointOffsets && newPieceIsTower
                ? WallKitCatalog.OffsetKind.TOWER_CONNECTION
                : jointOffsets && !newPieceIsTower
                    ? WallKitCatalog.OffsetKind.TOWER_JOINT
                    : newPieceIsTower
                        ? WallKitCatalog.OffsetKind.TOWER_CONNECTION
                        : WallKitCatalog.OffsetKind.CHAIN;
        Vector3i enterAtProbe = worldAttachPoint(toConstructionId, probeSign, newYaw, worldEnter, enterKind);
        int ax = fromExit.x - (enterAtProbe.x - towerOriginProbe.x);
        int az = fromExit.z - (enterAtProbe.z - towerOriginProbe.z);
        if (segmentToTower) {
            Vector3i locked =
                lockTowerBesideWall(fromConstructionId, fromSign, fromYaw, worldExpandDir, ax, az);
            ax = locked.x;
            az = locked.z;
        } else if (towerToSegmentRun) {
            Vector3i locked =
                lockTowerBesideWall(toConstructionId, fromSign, newYaw, worldExpandDir.opposite(), ax, az);
            ax = locked.x;
            az = locked.z;
        }
        return new Vector3i(ax, fromSign.y, az);
    }

    /** Legacy entry without construction ids (segment + default tower). */
    @Nonnull
    public static Vector3i nextSignPosition(
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal worldExpandDir,
        boolean fromIsTower,
        boolean newPieceIsTower
    ) {
        String fromId =
            fromIsTower
                ? AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S
                : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
        String toId =
            newPieceIsTower
                ? AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S
                : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
        return nextSignPosition(fromId, fromSign, fromYaw, toId, newYaw, worldExpandDir, fromIsTower, newPieceIsTower);
    }

    @Nonnull
    public static Vector3i worldAttachPoint(
        @Nonnull String constructionId,
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal worldDir,
        @Nonnull WallKitCatalog.OffsetKind kind
    ) {
        Vector3i logical = logicalAnchor(signPos);
        Vector3i off = KIT.worldOffsetFromAnchor(constructionId, yaw, worldDir, kind);
        return new Vector3i(logical.x + off.x, signPos.y, logical.z + off.z);
    }

    @Nonnull
    public static Vector3i segmentExitOffsetWorld(
        @Nonnull String segmentConstructionId,
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal worldAlongRun
    ) {
        return worldAttachPoint(segmentConstructionId, signPos, yaw, worldAlongRun, WallKitCatalog.OffsetKind.CHAIN);
    }

    @Nonnull
    public static Vector3i segmentTowerExitWorld(
        @Nonnull String segmentConstructionId,
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal worldAlongRun
    ) {
        return worldAttachPoint(segmentConstructionId, signPos, yaw, worldAlongRun, WallKitCatalog.OffsetKind.TOWER_JOINT);
    }

    @Nonnull
    public static Vector3i connectionPointWorld(
        @Nonnull String constructionId,
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal worldDir,
        boolean pieceIsTower,
        boolean segmentTowerJoint,
        boolean enterOnNewPiece
    ) {
        WallKitCatalog.OffsetKind kind;
        if (segmentTowerJoint) {
            kind =
                pieceIsTower
                    ? WallKitCatalog.OffsetKind.TOWER_CONNECTION
                    : WallKitCatalog.OffsetKind.TOWER_JOINT;
        } else if (pieceIsTower) {
            kind = WallKitCatalog.OffsetKind.TOWER_CONNECTION;
        } else {
            kind = WallKitCatalog.OffsetKind.CHAIN;
        }
        return worldAttachPoint(constructionId, signPos, yaw, worldDir, kind);
    }

    @Nonnull
    public static Vector3i connectionPointWorld(
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal worldDir,
        boolean pieceIsTower,
        boolean segmentTowerJoint,
        boolean enterOnNewPiece
    ) {
        String id =
            pieceIsTower
                ? AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S
                : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
        return connectionPointWorld(id, signPos, yaw, worldDir, pieceIsTower, segmentTowerJoint, enterOnNewPiece);
    }

    /**
     * @deprecated Chain math uses {@code worldAlongRun} directly; kept for tests migrating off local-face labels.
     */
    @Nonnull
    @Deprecated
    public static WallCardinal segmentChainExitFace(@Nonnull WallCardinal worldAlongRun, @Nonnull Rotation yaw) {
        return worldAlongRun;
    }

    @Nonnull
    public static Vector3i segmentExteriorAttachWorld(
        @Nonnull String segmentConstructionId,
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull WallCardinal worldOutward
    ) {
        return worldAttachPoint(segmentConstructionId, segmentSign, segmentYaw, worldOutward, WallKitCatalog.OffsetKind.EXTERIOR);
    }

    @Nonnull
    public static Vector3i segmentExteriorAttachWorld(
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull WallCardinal worldOutward
    ) {
        return segmentExteriorAttachWorld(
            AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT, segmentSign, segmentYaw, worldOutward
        );
    }

    @Nonnull
    public static Vector3i seatTowerSignAtSegmentRunEnd(
        @Nonnull String segmentConstructionId,
        @Nonnull String towerConstructionId,
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull Rotation towerYaw,
        @Nonnull WallCardinal worldOutward,
        @Nonnull Vector3i rowColumnLockSign
    ) {
        Vector3i attachWorld =
            segmentExteriorAttachWorld(segmentConstructionId, segmentSign, segmentYaw, worldOutward);
        int signY = segmentSign.y;
        Vector3i probeSign = new Vector3i(0, signY, 0);
        Vector3i towerOriginProbe = logicalAnchor(probeSign);
        Vector3i enterAtProbe =
            worldAttachPoint(
                towerConstructionId,
                probeSign,
                towerYaw,
                worldOutward.opposite(),
                WallKitCatalog.OffsetKind.TOWER_CONNECTION
            );
        Vector3i enterOffset =
            new Vector3i(
                enterAtProbe.x - towerOriginProbe.x,
                enterAtProbe.y - towerOriginProbe.y,
                enterAtProbe.z - towerOriginProbe.z
            );
        int ax = attachWorld.x - enterOffset.x;
        int az = attachWorld.z - enterOffset.z;
        Vector3i locked =
            lockTowerBesideWall(segmentConstructionId, rowColumnLockSign, segmentYaw, worldOutward, ax, az);
        return new Vector3i(locked.x, signY, locked.z);
    }

    @Nonnull
    public static Vector3i seatTowerSignAtSegmentRunEnd(
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull Rotation towerYaw,
        @Nonnull WallCardinal worldOutward,
        @Nonnull Vector3i rowColumnLockSign
    ) {
        return seatTowerSignAtSegmentRunEnd(
            AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT,
            AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S,
            segmentSign,
            segmentYaw,
            towerYaw,
            worldOutward,
            rowColumnLockSign
        );
    }

    @Nonnull
    public static Vector3i seatTowerSignAtSegmentRunEnd(
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull Rotation towerYaw,
        @Nonnull WallCardinal worldOutward
    ) {
        return seatTowerSignAtSegmentRunEnd(segmentSign, segmentYaw, towerYaw, worldOutward, segmentSign);
    }

    /**
     * After seating a tower beside a segment, pin the sign to the wall's run row or long-side column in
     * <em>world</em> space. {@code worldAttachDir} is the pad direction (world N/S/E/W).
     */
    @Nonnull
    private static Vector3i lockTowerBesideWall(
        @Nonnull String wallConstructionId,
        @Nonnull Vector3i wallSign,
        @Nonnull Rotation wallYaw,
        @Nonnull WallCardinal worldAttachDir,
        int ax,
        int az
    ) {
        boolean runAlongWorldZ = KIT.runAlongWorldZ(wallConstructionId, wallYaw);
        boolean attachOnRunEnd =
            runAlongWorldZ
                ? worldAttachDir == WallCardinal.NORTH || worldAttachDir == WallCardinal.SOUTH
                : worldAttachDir == WallCardinal.EAST || worldAttachDir == WallCardinal.WEST;
        if (runAlongWorldZ) {
            return attachOnRunEnd
                ? new Vector3i(wallSign.x, wallSign.y, az)
                : new Vector3i(ax, wallSign.y, wallSign.z);
        }
        return attachOnRunEnd
            ? new Vector3i(ax, wallSign.y, wallSign.z)
            : new Vector3i(wallSign.x, wallSign.y, az);
    }

    @Nonnull
    private static Vector3i logicalAnchor(@Nonnull Vector3i signPos) {
        return new Vector3i(
            signPos.x,
            signPos.y - AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR,
            signPos.z
        );
    }

    public static boolean isTowerConstructionId(@Nonnull String constructionId) {
        return constructionId.startsWith("plot_wall_tower_");
    }
}
