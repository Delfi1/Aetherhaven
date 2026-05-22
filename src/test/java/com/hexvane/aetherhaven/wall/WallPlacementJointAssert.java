package com.hexvane.aetherhaven.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;

/** Shared flush / row-column alignment checks for wall wand placement math. */
public final class WallPlacementJointAssert {
    /** Segment block faces use ±2 on E/W; towers use ±4 — allow that joint span in flush checks. */
    private static final int FLUSH_EPSILON = 4;

    private WallPlacementJointAssert() {}

    public static void assertSameRow(int wallZ, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertWallRowAligned(wallZ, anchor, label);
    }

    /** Tower sign may sit ±{@link WallPieceGeometry#TOWER_CONNECTION_HALF} from wall sign while faces stay flush. */
    public static void assertWallRowAligned(int wallZ, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(
            Math.abs(anchor.z - wallZ) <= WallPieceGeometry.TOWER_CONNECTION_HALF,
            label + " must align with wall Z row (wall z=" + wallZ + ", got z=" + anchor.z + ")"
        );
    }

    public static void assertWallColumnAligned(int wallX, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(
            Math.abs(anchor.x - wallX) <= WallPieceGeometry.TOWER_CONNECTION_HALF,
            label + " must align with wall X column (wall x=" + wallX + ", got x=" + anchor.x + ")"
        );
    }

    public static void assertSameColumn(int wallX, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertEquals(
            wallX,
            anchor.x,
            label + " must stay on wall X column (wall x=" + wallX + ", got x=" + anchor.x + ")"
        );
    }

    /** Straight-run tower: committed segment run-end face meets tower (not the next segment chain slot). */
    public static void assertFlushStraightTower(
        @Nonnull Vector3i wallSign,
        @Nonnull Rotation wallYaw,
        @Nonnull WallCardinal alongRun,
        @Nonnull Vector3i towerSign,
        @Nonnull Rotation towerYaw,
        @Nonnull String label
    ) {
        assertFlushRunEnd(wallSign, wallYaw, alongRun, towerSign, towerYaw, label);
    }

    /** Segment run-end face meets tower face opposite the run. */
    public static void assertFlushRunEnd(
        @Nonnull Vector3i wallSign,
        @Nonnull Rotation wallYaw,
        @Nonnull WallCardinal alongRun,
        @Nonnull Vector3i towerSign,
        @Nonnull Rotation towerYaw,
        @Nonnull String label
    ) {
        Vector3i segmentFace =
            WallPieceGeometry.segmentExteriorAttachWorld(wallSign, wallYaw, alongRun);
        Vector3i towerFace =
            WallPieceGeometry.connectionPointWorld(
                towerSign, towerYaw, alongRun.opposite(), true, true, true
            );
        assertNear(segmentFace.x, towerFace.x, label + " flush X");
        assertNear(segmentFace.z, towerFace.z, label + " flush Z");
    }

    /** Segment-to-segment chain: chain face centers meet (not tower joint offsets). */
    public static void assertFlushSegmentChain(
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull WallCardinal exitFace,
        @Nonnull Vector3i toSign,
        @Nonnull Rotation toYaw,
        @Nonnull WallCardinal enterFace,
        @Nonnull String label
    ) {
        Vector3i exitWorld =
            WallPieceGeometry.connectionPointWorld(fromSign, fromYaw, exitFace, false, false, false);
        Vector3i enterWorld =
            WallPieceGeometry.connectionPointWorld(toSign, toYaw, enterFace, false, false, true);
        assertNear(exitWorld.x, enterWorld.x, label + " flush X");
        assertNear(exitWorld.z, enterWorld.z, label + " flush Z");
    }

    /** Tower run exit meets the exterior face of the next segment (outside wall volume). */
    public static void assertFlushTowerToSegment(
        @Nonnull Vector3i towerSign,
        @Nonnull Rotation towerYaw,
        @Nonnull WallCardinal exitFace,
        @Nonnull Vector3i segmentSign,
        @Nonnull Rotation segmentYaw,
        @Nonnull WallCardinal enterFace,
        @Nonnull String label
    ) {
        Vector3i exitWorld =
            WallPieceGeometry.connectionPointWorld(towerSign, towerYaw, exitFace, true, true, false);
        Vector3i enterWorld =
            WallPieceGeometry.segmentExteriorAttachWorld(segmentSign, segmentYaw, enterFace);
        assertNear(exitWorld.x, enterWorld.x, label + " flush X");
        assertNear(exitWorld.z, enterWorld.z, label + " flush Z");
    }

    /** Mixed segment↔tower joint: exit point on A equals enter point on B. */
    public static void assertFlushMixedJoint(
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull WallCardinal exitFace,
        boolean fromIsTower,
        @Nonnull Vector3i toSign,
        @Nonnull Rotation toYaw,
        @Nonnull WallCardinal enterFace,
        boolean toIsTower,
        @Nonnull String label
    ) {
        Vector3i exitWorld =
            !fromIsTower && toIsTower
                ? WallPieceGeometry.segmentExteriorAttachWorld(fromSign, fromYaw, exitFace)
                : WallPieceGeometry.connectionPointWorld(fromSign, fromYaw, exitFace, fromIsTower, true, false);
        Vector3i enterWorld =
            WallPieceGeometry.connectionPointWorld(toSign, toYaw, enterFace, toIsTower, true, true);
        assertNear(exitWorld.x, enterWorld.x, label + " flush X");
        assertNear(exitWorld.z, enterWorld.z, label + " flush Z");
    }

    public static void assertNearWestOf(int wallSignX, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(anchor.x < wallSignX, label + " must be west of wall sign x=" + wallSignX + ", got x=" + anchor.x);
    }

    public static void assertNearNorthOf(int wallSignZ, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(anchor.z < wallSignZ, label + " must be north of wall sign z=" + wallSignZ + ", got z=" + anchor.z);
    }

    public static void assertNearSouthOf(int wallSignZ, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(anchor.z > wallSignZ, label + " must be south of wall sign z=" + wallSignZ + ", got z=" + anchor.z);
    }

    public static void assertNearEastOf(int wallSignX, @Nonnull Vector3i anchor, @Nonnull String label) {
        assertTrue(anchor.x > wallSignX, label + " must be east of wall sign x=" + wallSignX + ", got x=" + anchor.x);
    }

    private static void assertNear(int expected, int actual, @Nonnull String label) {
        assertTrue(
            Math.abs(expected - actual) <= FLUSH_EPSILON,
            label + ": expected " + expected + " got " + actual
        );
    }
}
