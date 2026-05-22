package com.hexvane.aetherhaven.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
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
        assertEquals(WallCardinal.NORTH, plan.positionDir());
        assertEquals(EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST), plan.towerConnections());
        assertEquals(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_OUTERCORNER_SE, plan.resolvedConstructionId());
        assertEquals(3, plan.rotationSteps());
        assertEquals(
            EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST),
            WallTowerPrefabResolver.worldOpeningsForCornerRotation(plan.rotationSteps())
        );
    }

    @Test
    void nsSegmentThenCornerTowerWest_commitStoresRotationMatchingConnections() {
        var sim = WallPlacementChainSimulation.start(362, 121, -171);
        sim.expandPlace(WallCardinal.NORTH);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        WallPlacementChainPlanner.ChainCommittedPiece tower = sim.lastCommitted();
        assertEquals(EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST), tower.towerConnectionDirs());
        assertEquals(3, tower.rotationSteps());
        assertEquals(
            tower.towerConnectionDirs(),
            WallTowerPrefabResolver.worldOpeningsForCornerRotation(tower.rotationSteps())
        );
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
        assertEquals(towerSign.z, ewSegment.signAnchor().z, "E/W run must stay on tower sign Z row");
        Vector3i beforeSecondWest = ewSegment.signAnchor().clone();
        sim.expandPlace(WallCardinal.WEST);
        assertEquals(towerSign.z, sim.lastCommitted().signAnchor().z);
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
    void towerJointExpandDir_nsRunCorner_usesChainEndNotOutgoingPad() {
        var last =
            new WallPlacementChainPlanner.ChainCommittedPiece(
                AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT,
                new Vector3i(0, 0, 0),
                0,
                null,
                WallCardinal.NORTH
            );
        assertEquals(WallCardinal.NORTH, WallPlacementChainPlanner.towerJointExpandDir(last, WallCardinal.WEST));
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
    void southSegmentRot2_thenTowerButton_movesSouthOfCommittedSign() {
        var sim = WallPlacementChainSimulation.start(-1686, 123, 37);
        sim.expandPlace(WallCardinal.SOUTH);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        assertTrue(sim.anchor().z > wallSign.z, "tower must sit south of wall sign, got " + sim.anchor());
        assertNotEquals(wallSign, sim.anchor());
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
