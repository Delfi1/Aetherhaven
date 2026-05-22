package com.hexvane.aetherhaven.wall;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;

/**
 * Prefab-local connection geometry for wall kit pieces (anchor at 0,0,0). Segments run along local Z with north at
 * negative Z and south at positive Z.
 */
public final class WallPieceGeometry {
    /** Prefab-local offset from anchor to north connection face center (segment). */
    private static final Vector3i SEGMENT_LOCAL_NORTH = new Vector3i(0, 0, -7);

    /** Prefab-local offset from anchor to south connection face center (segment). */
    private static final Vector3i SEGMENT_LOCAL_SOUTH = new Vector3i(0, 0, 9);

    /** |north| + |south| segment connection span along local Z. */
    public static final int SEGMENT_CHAIN_SPAN = 16;

    /** Tower footprint half-size for wall connection faces (9×9 tower, z/x ∈ [-4, 4]). */
    private static final int TOWER_CONNECTION_HALF = 4;

    /** Sign / logical anchor offset for segments and towers (center of footprint on ground). */
    public static final int[] PLOT_ANCHOR_OFFSET = new int[] {0, 0, 0};

    private WallPieceGeometry() {}

    /**
     * World sign position for a new piece whose {@code enterFace} connects to {@code fromSign} on the previous piece.
     */
    @Nonnull
    public static Vector3i nextSignPosition(
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal expandDir,
        boolean fromIsTower,
        boolean newPieceIsTower
    ) {
        boolean mixedJoint = fromIsTower != newPieceIsTower;
        boolean segmentRun = !fromIsTower && !newPieceIsTower;
        WallCardinal exitFace = segmentRun ? segmentRunEndFace(expandDir, fromYaw) : expandDir;
        WallCardinal enterFace =
            segmentRun ? segmentRunEndFace(expandDir.opposite(), newYaw) : expandDir.opposite();
        Vector3i fromExit = connectionOffsetWorld(fromSign, fromYaw, exitFace, fromIsTower, mixedJoint, false);
        int signY = fromSign.y;
        Vector3i probeSign = new Vector3i(0, signY, 0);
        Vector3i enterAtProbe =
            connectionOffsetWorld(probeSign, newYaw, enterFace, newPieceIsTower, mixedJoint, true);
        int ax = fromExit.x - (enterAtProbe.x - probeSign.x);
        int az = fromExit.z - (enterAtProbe.z - probeSign.z);
        // Geometry fallback for tower→E/W segment (tests / no prefab buffers): keep tower row Z.
        if (mixedJoint && (expandDir == WallCardinal.EAST || expandDir == WallCardinal.WEST)) {
            az = fromSign.z;
        }
        return new Vector3i(ax, fromSign.y, az);
    }

    /**
     * Segment runs along prefab local Z; pick the local north/south connection face whose rotated offset points along
     * {@code worldAlongRun} (avoids relying on rotation-step labels matching prefab rotation handedness).
     */
    @Nonnull
    private static WallCardinal segmentRunEndFace(@Nonnull WallCardinal worldAlongRun, @Nonnull Rotation yaw) {
        Vector3i northWorld = rotateLocal(SEGMENT_LOCAL_NORTH.clone(), yaw);
        WallCardinal northPoints = WallCardinal.fromVector(new Vector3i(0, 0, 0), northWorld);
        if (northPoints == worldAlongRun) {
            return WallCardinal.NORTH;
        }
        Vector3i southWorld = rotateLocal(SEGMENT_LOCAL_SOUTH.clone(), yaw);
        WallCardinal southPoints = WallCardinal.fromVector(new Vector3i(0, 0, 0), southWorld);
        if (southPoints == worldAlongRun) {
            return WallCardinal.SOUTH;
        }
        return worldAlongRun;
    }

    private static int rotationStepsFrom(@Nonnull Rotation yaw) {
        return switch (yaw) {
            case Ninety -> 1;
            case OneEighty -> 2;
            case TwoSeventy -> 3;
            default -> 0;
        };
    }

    @Nonnull
    public static Vector3i segmentExitOffsetWorld(@Nonnull Vector3i signPos, @Nonnull Rotation yaw, @Nonnull WallCardinal exitFace) {
        return connectionOffsetWorld(signPos, yaw, exitFace, false, false, false);
    }

    @Nonnull
    private static Vector3i connectionOffsetWorld(
        @Nonnull Vector3i signPos,
        @Nonnull Rotation yaw,
        @Nonnull WallCardinal face,
        boolean pieceIsTower,
        boolean segmentTowerJoint,
        boolean enterOnNewPiece
    ) {
        Vector3i logical = logicalAnchor(signPos);
        Vector3i local = connectionLocalOffsetForFace(face, pieceIsTower, segmentTowerJoint, enterOnNewPiece);
        Vector3i world = rotateLocal(local, yaw);
        return new Vector3i(logical.x + world.x, signPos.y, logical.z + world.z);
    }

    /**
     * Segment-to-segment uses chain offsets (z −7 / +9). Segment↔tower uses segment block faces (z −7 / +8) on the
     * segment side and tower mesh extents (±4) on the tower side so prefabs touch.
     */
    @Nonnull
    private static Vector3i connectionLocalOffsetForFace(
        @Nonnull WallCardinal face, boolean pieceIsTower, boolean segmentTowerJoint, boolean enterOnNewPiece
    ) {
        if (segmentTowerJoint) {
            if (pieceIsTower) {
                return towerConnectionOffset(face);
            }
            return segmentTowerJointOffset(face);
        }
        if (pieceIsTower) {
            return towerConnectionOffset(face);
        }
        return switch (face) {
            case NORTH -> SEGMENT_LOCAL_NORTH.clone();
            case SOUTH -> SEGMENT_LOCAL_SOUTH.clone();
            case EAST -> new Vector3i(2, 0, 0);
            case WEST -> new Vector3i(-2, 0, 0);
        };
    }

    @Nonnull
    private static Vector3i towerConnectionOffset(@Nonnull WallCardinal face) {
        return switch (face) {
            case NORTH -> new Vector3i(0, 0, -TOWER_CONNECTION_HALF);
            case SOUTH -> new Vector3i(0, 0, TOWER_CONNECTION_HALF);
            case EAST -> new Vector3i(TOWER_CONNECTION_HALF, 0, 0);
            case WEST -> new Vector3i(-TOWER_CONNECTION_HALF, 0, 0);
        };
    }

    /** Block-face offsets where a 9×9 tower meets a segment (±8 on Z, not chain ±7/9, so mesh faces touch without overlap). */
    @Nonnull
    private static Vector3i segmentTowerJointOffset(@Nonnull WallCardinal face) {
        return switch (face) {
            case NORTH -> new Vector3i(0, 0, -8);
            case SOUTH -> new Vector3i(0, 0, 8);
            case EAST -> new Vector3i(2, 0, 0);
            case WEST -> new Vector3i(-2, 0, 0);
        };
    }

    @Nonnull
    private static Vector3i logicalAnchor(@Nonnull Vector3i signPos) {
        return new Vector3i(
            signPos.x,
            signPos.y - AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR,
            signPos.z
        );
    }

    @Nonnull
    private static Vector3i rotateLocal(@Nonnull Vector3i local, @Nonnull Rotation yaw) {
        Vector3i copy = local.clone();
        com.hypixel.hytale.server.core.prefab.PrefabRotation.fromRotation(yaw).rotate(copy);
        return copy;
    }

    public static boolean isTowerConstructionId(@Nonnull String constructionId) {
        return constructionId.startsWith("plot_wall_tower_");
    }
}
