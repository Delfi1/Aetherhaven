package com.hexvane.aetherhaven.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import java.util.EnumSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Unit tests for wall wand directional chaining ({@link WallPlacementChainPlanner}). */
@Tag("wall-placement")
class WallPlacementChainPlannerTest {

    @Test
    void nsSegmentChainNorth_movesNorthOnZ() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        Vector3i before = sim.anchor();
        sim.expandPlace(WallCardinal.NORTH);
        assertDominantAxis(before, sim.anchor(), WallCardinal.NORTH);
        assertEquals(0, sim.rotationSteps());
    }

    @Test
    void nsSegmentThenCornerTowerWest_usesSouthWestConnections() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.WEST);
        assertEquals(WallCardinal.WEST, plan.positionDir());
        assertTrue(plan.towerConnections().contains(WallCardinal.WEST));
        assertEquals(2, plan.towerConnections().size());
    }

    @Test
    void nsSegmentThenCornerTowerWest_commitStoresWestOpening() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        WallPlacementChainPlanner.ChainCommittedPiece tower = sim.lastCommitted();
        assertTrue(tower.towerConnectionDirs().contains(WallCardinal.WEST));
        assertEquals(2, tower.towerConnectionDirs().size());
    }

    @Test
    void cornerTowerScenario_allowedPadsOnlyWestAfterTower() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        WallPlacementChainPlanner.ExpandPreviewPlan preview =
            WallPlacementChainPlanner.planExpandPreview(
                sim.anchor(),
                sim.rotationSteps(),
                WallPlacementChainPlanner.PieceKind.SEGMENT,
                sim.committed(),
                WallCardinal.WEST
            );
        assertEquals(EnumSet.of(WallCardinal.WEST), preview.allowedExpandDirections());
    }

    @Test
    void afterCornerTower_firstWestSegment_isEastWestRotation() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        assertEquals(3, sim.rotationSteps());
    }

    @Test
    void ewSegmentAfterCornerTower_sharesTowerSignZ_notFootprintCentroidDrift() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        WallPlacementChainPlanner.ChainCommittedPiece tower = sim.committed().get(1);
        Vector3i towerSign = tower.signAnchor();
        sim.expandPlace(WallCardinal.WEST);
        WallPlacementChainPlanner.ChainCommittedPiece ewSegment = sim.lastCommitted();
        assertTrue(
            Math.abs(ewSegment.signAnchor().z - towerSign.z) <= WallPieceGeometry.TOWER_CONNECTION_HALF,
            "E/W run must stay on tower Z row"
        );
        Vector3i beforeSecondWest = ewSegment.signAnchor().clone();
        sim.expandPlace(WallCardinal.WEST);
        assertTrue(
            Math.abs(sim.lastCommitted().signAnchor().z - towerSign.z) <= WallPieceGeometry.TOWER_CONNECTION_HALF
        );
        assertTrue(sim.lastCommitted().signAnchor().x < beforeSecondWest.x, "second west segment steps west on X");
    }

    @Test
    void ewRunChainWest_movesWestOnX() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        Vector3i before = sim.anchor();
        sim.expandPlace(WallCardinal.WEST);
        assertDominantAxis(before, sim.anchor(), WallCardinal.WEST);
        assertEquals(3, sim.rotationSteps());
    }

    @Test
    void nsSegment_allowedExcludesBackAfterFirstPlace() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        WallPlacementChainPlanner.ExpandPreviewPlan preview =
            WallPlacementChainPlanner.planExpandPreview(
                sim.anchor(),
                sim.rotationSteps(),
                WallPlacementChainPlanner.PieceKind.SEGMENT,
                sim.committed(),
                WallCardinal.NORTH
            );
        assertTrue(preview.allowedExpandDirections().contains(WallCardinal.NORTH));
        assertFalse(preview.allowedExpandDirections().contains(WallCardinal.SOUTH));
    }

    @Test
    void towerPlacement_allowedIncludesChainForward_notPreviewAhead() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan preview =
            WallPlacementChainPlanner.planExpandPreview(
                sim.anchor(),
                sim.rotationSteps(),
                WallPlacementChainPlanner.PieceKind.TOWER,
                sim.committed(),
                WallCardinal.NORTH
            );
        assertTrue(preview.allowedExpandDirections().contains(WallCardinal.NORTH));
        assertFalse(preview.allowedExpandDirections().contains(WallCardinal.SOUTH));
    }

    @Test
    void towerJointExpandDir_nsRunCorner_usesLongSidePad() {
        var last =
            new WallPlacementChainPlanner.ChainCommittedPiece(
                AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT,
                new Vector3i(0, 0, 0),
                0,
                null,
                WallCardinal.NORTH
            );
        assertEquals(WallCardinal.WEST, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.WEST));
        assertEquals(WallCardinal.SOUTH, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.SOUTH));
        assertEquals(WallCardinal.NORTH, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.NORTH));
    }

    @Test
    void towerJointExpandDir_ewRunSide_usesOutgoingPadNotChainEnd() {
        var last =
            new WallPlacementChainPlanner.ChainCommittedPiece(
                AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT,
                new Vector3i(-1714, 121, 47),
                1,
                null,
                WallCardinal.EAST
            );
        assertEquals(WallCardinal.NORTH, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.NORTH));
        assertEquals(WallCardinal.EAST, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.EAST));
    }

    @Test
    void southSegmentRot2_thenTowerButton_flushAtWallSouthEnd() {
        var sim = WallPlacementChainSimulation.start(-1686, 123, 37);
        sim.expandPlace(WallCardinal.SOUTH);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        Rotation wallYaw = sim.committed().get(0).prefabYaw();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.SOUTH);
        WallPlacementJointAssert.assertFlushStraightTower(
            wallSign,
            wallYaw,
            WallCardinal.SOUTH,
            plan.anchor(),
            WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps()),
            "south tab"
        );
        assertTrue(plan.anchor().z > wallSign.z);
        assertNotEquals(wallSign, plan.anchor());
    }

    /** User log: north wall then corner tower east — tower south face meets segment north face, not chain-tip slot. */
    @Test
    void westSegment_towerTab_flushAtWallWestEnd_notAlongLongSide() {
        var sim = WallPlacementChainSimulation.start(-1696, 124, 121);
        sim.expandPlace(WallCardinal.WEST);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        Rotation wallYaw = sim.committed().get(0).prefabYaw();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.WEST);
        WallPlacementJointAssert.assertFlushStraightTower(
            wallSign,
            wallYaw,
            WallCardinal.WEST,
            plan.anchor(),
            WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps()),
            "west tab"
        );
        assertTrue(plan.anchor().x < wallSign.x - 2, "must not sit on long-side west face near sign x=" + wallSign.x);
        WallPlacementJointAssert.assertSameRow(wallSign.z, plan.anchor(), "west tab row");
    }

    @Test
    void westSegmentRot3_towerSouthPad_cornerOnLongSide() {
        var sim = WallPlacementChainSimulation.start(-1698, 122, 118);
        sim.expandPlace(WallCardinal.WEST);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.SOUTH);
        assertEquals(WallCardinal.SOUTH, plan.positionDir());
        assertTrue(plan.towerConnections().contains(WallCardinal.SOUTH));
        assertEquals(2, plan.towerConnections().size());
        WallPlacementJointAssert.assertWallColumnAligned(wallSign.x, plan.anchor(), "corner on west run");
    }

    @Test
    void northSegment_cornerTowerEast_meetsSegmentEastLongFace() {
        var sim = WallPlacementChainSimulation.start(-1729, 121, 61);
        sim.expandPlace(WallCardinal.NORTH);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        var yaw = sim.committed().get(0).prefabYaw();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.EAST);
        Vector3i towerSign = sim.lastCommitted().signAnchor();
        WallPlacementJointAssert.assertFlushMixedJoint(
            wallSign,
            yaw,
            WallCardinal.EAST,
            false,
            towerSign,
            sim.lastCommitted().prefabYaw(),
            WallCardinal.WEST,
            true,
            "east corner"
        );
    }

    @Test
    void cornerTower_thenEastSegment_staysNearTowerZ_notWorldOrigin() {
        var sim = WallPlacementChainSimulation.start(-1729, 121, 61);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.EAST);
        Vector3i towerSign = sim.committed().get(1).signAnchor();
        sim.expandPlace(WallCardinal.EAST);
        Vector3i segSign = sim.lastCommitted().signAnchor();
        assertTrue(
            Math.abs(segSign.z - towerSign.z) <= WallPieceGeometry.TOWER_CONNECTION_HALF,
            "E/W segment must stay on tower row"
        );
        assertTrue(Math.abs(segSign.z) > 32, "must not collapse to z~0, got " + segSign.z);
        assertTrue(
            segSign.x >= towerSign.x - WallPieceGeometry.TOWER_CONNECTION_HALF,
            "east segment must not sit west of tower (tower x=" + towerSign.x + ", seg x=" + segSign.x + ")"
        );
    }

    @Test
    void ewSegmentThenTowerNorth_positionDirIsNorthNotEast() {
        var sim = WallPlacementChainSimulation.start(-1714, 123, 47);
        sim.expandPlace(WallCardinal.EAST);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.NORTH);
        assertEquals(WallCardinal.NORTH, plan.positionDir());
        assertEquals(WallCardinal.NORTH, plan.outgoingExpandDir());
    }

    private static void assertDominantAxis(Vector3i from, Vector3i to, WallCardinal expected) {
        int dx = to.x - from.x;
        int dz = to.z - from.z;
        switch (expected) {
            case NORTH -> {
                assertTrue(dz < 0, "expected north (dz<0) got dx=" + dx + " dz=" + dz);
                assertTrue(Math.abs(dz) >= Math.abs(dx), "north should dominate dx=" + dx + " dz=" + dz);
            }
            case SOUTH -> {
                assertTrue(dz > 0, "expected south (dz>0) got dx=" + dx + " dz=" + dz);
                assertTrue(Math.abs(dz) >= Math.abs(dx));
            }
            case WEST -> {
                assertTrue(dx < 0, "expected west (dx<0) got dx=" + dx + " dz=" + dz);
                assertTrue(Math.abs(dx) >= Math.abs(dz));
            }
            case EAST -> {
                assertTrue(dx > 0, "expected east (dx>0) got dx=" + dx + " dz=" + dz);
                assertTrue(Math.abs(dx) >= Math.abs(dz));
            }
        }
    }
}
