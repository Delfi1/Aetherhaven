package com.hexvane.aetherhaven.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import java.util.EnumSet;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Coordinate, flush, and row/column alignment for wall↔tower placement (geometry path). */
@Tag("wall-placement")
class WallPlacementJointScenariosTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("straightRunTowerCases")
    void straightRunTowerTab_alignedAndFlush(
        String name, int x, int y, int z, WallCardinal chain, boolean runAlongZ
    ) {
        var sim = WallPlacementChainSimulation.start(x, y, z);
        sim.expandPlace(chain);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(chain);
        Vector3i tower = plan.anchor();
        Rotation towerYaw = WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps());

        if (runAlongZ) {
            WallPlacementJointAssert.assertWallColumnAligned(wallSign.x, tower, name);
            switch (chain) {
                case NORTH -> WallPlacementJointAssert.assertNearNorthOf(wallSign.z, tower, name);
                case SOUTH -> WallPlacementJointAssert.assertNearSouthOf(wallSign.z, tower, name);
                default -> throw new IllegalStateException(chain.name());
            }
        } else {
            WallPlacementJointAssert.assertSameRow(wallSign.z, tower, name);
            switch (chain) {
                case WEST -> WallPlacementJointAssert.assertNearWestOf(wallSign.x, tower, name);
                case EAST -> WallPlacementJointAssert.assertNearEastOf(wallSign.x, tower, name);
                default -> throw new IllegalStateException(chain.name());
            }
        }
        Rotation wallYaw = sim.committed().get(0).prefabYaw();
        WallPlacementJointAssert.assertFlushStraightTower(wallSign, wallYaw, chain, tower, towerYaw, name);
        assertNotEquals(wallSign, tower, name + " tower must not sit on wall sign");
    }

    static Stream<Arguments> straightRunTowerCases() {
        return Stream.of(
            Arguments.of("NS north tab", 100, 120, 80, WallCardinal.NORTH, true),
            Arguments.of("NS south tab", 100, 120, 80, WallCardinal.SOUTH, true),
            Arguments.of("EW west tab", -1693, 121, 123, WallCardinal.WEST, false),
            Arguments.of("EW east tab", -1693, 121, 123, WallCardinal.EAST, false)
        );
    }

    @org.junit.jupiter.api.Test
    void userLog_westWall_towerWestPad_sameZ_flushWestEnd() {
        var sim = WallPlacementChainSimulation.start(-1693, 122, 123);
        sim.expandPlace(WallCardinal.WEST);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        int westChainTipX = sim.anchor().x;
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.WEST);
        Vector3i tower = plan.anchor();
        Rotation towerYaw = WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps());

        WallPlacementJointAssert.assertSameRow(wallSign.z, tower, "user west run");
        assertTrue(tower.x <= westChainTipX + 12, "tower near west chain tip x=" + westChainTipX + " got " + tower.x);
        assertTrue(tower.x < wallSign.x, "not on long-side west face at sign");
        Rotation wallYaw = sim.committed().get(0).prefabYaw();
        WallPlacementJointAssert.assertFlushStraightTower(
            wallSign, wallYaw, WallCardinal.WEST, tower, towerYaw, "user west run"
        );
        assertEquals(EnumSet.of(WallCardinal.EAST), plan.towerConnections(), "end cap, not E+W corner");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cornerTowerCases")
    void cornerTower_flushAndStaysOnWallRowOrColumn(
        String name,
        int x,
        int y,
        int z,
        WallCardinal chain,
        WallCardinal cornerPad,
        boolean runAlongZ
    ) {
        var sim = WallPlacementChainSimulation.start(x, y, z);
        sim.expandPlace(chain);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(cornerPad);
        Vector3i tower = plan.anchor();
        Rotation wallYaw = sim.committed().get(0).prefabYaw();
        Rotation towerYaw = WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps());

        WallCardinal seatFace = WallPlacementChainPlanner.towerSeatDirection(sim.committed().get(0), cornerPad);
        WallCardinal towerEnter = seatFace.opposite();
        if (runAlongZ) {
            WallPlacementJointAssert.assertWallColumnAligned(wallSign.x, tower, name);
            assertTrue(tower.z < wallSign.z == (chain == WallCardinal.NORTH), name + " tower at chain run tip");
        } else {
            WallPlacementJointAssert.assertWallRowAligned(wallSign.z, tower, name);
            assertTrue(tower.x < wallSign.x == (chain == WallCardinal.WEST), name + " tower at chain run tip");
        }
        WallPlacementJointAssert.assertFlushMixedJoint(
            wallSign,
            wallYaw,
            seatFace,
            false,
            tower,
            towerYaw,
            towerEnter,
            true,
            name
        );
        assertNotEquals(wallSign, tower);
        assertTrue(plan.towerConnections().size() == 2);
    }

    static Stream<Arguments> cornerTowerCases() {
        return Stream.of(
            Arguments.of("NS chain north + west corner", 362, 121, -171, WallCardinal.NORTH, WallCardinal.WEST, true),
            Arguments.of("NS chain north + east corner", 362, 121, -171, WallCardinal.NORTH, WallCardinal.EAST, true),
            Arguments.of("EW chain west + south corner", -1698, 122, 118, WallCardinal.WEST, WallCardinal.SOUTH, false),
            Arguments.of("EW chain west + north corner", -1698, 122, 118, WallCardinal.WEST, WallCardinal.NORTH, false)
        );
    }

    @org.junit.jupiter.api.Test
    void segmentChain_secondSegmentFlushWithFirst() {
        var sim = WallPlacementChainSimulation.start(-1693, 122, 123);
        sim.expandPlace(WallCardinal.WEST);
        Vector3i firstSign = sim.committed().get(0).signAnchor();
        Rotation firstYaw = sim.committed().get(0).prefabYaw();
        Vector3i secondSign = sim.anchor();
        WallPlacementJointAssert.assertFlushSegmentChain(
            firstSign, firstYaw, WallCardinal.WEST, secondSign, firstYaw, WallCardinal.EAST, "segment chain"
        );
        assertTrue(secondSign.x < firstSign.x);
        WallPlacementJointAssert.assertSameRow(firstSign.z, secondSign, "segment chain Z");
    }

    /** User log: N/S wall — tower sign must sit outside the segment on every pad (not on wall center). */
    @org.junit.jupiter.api.Test
    void nsWall_allTowerPads_signOutsideWallVolume() {
        var sim = WallPlacementChainSimulation.start(-1584, 122, 169);
        sim.expandPlace(WallCardinal.NORTH);
        Vector3i wall = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);

        Vector3i north = sim.previewOnly(WallCardinal.NORTH).anchor();
        assertTrue(north.z <= wall.z - 10, "north tower outside north end, got z=" + north.z + " wall=" + wall.z);

        Vector3i south = sim.previewOnly(WallCardinal.SOUTH).anchor();
        assertTrue(south.z >= wall.z + 11, "south tower outside south end, got z=" + south.z + " wall=" + wall.z);

        Vector3i east = sim.previewOnly(WallCardinal.EAST).anchor();
        assertTrue(east.z < wall.z - 10, "east pad corner sits at north run tip, got z=" + east.z + " wall=" + wall.z);
        assertTrue(planHasConnections(sim.previewOnly(WallCardinal.EAST), WallCardinal.SOUTH, WallCardinal.EAST));

        Vector3i west = sim.previewOnly(WallCardinal.WEST).anchor();
        assertTrue(west.z < wall.z - 10, "west pad corner sits at north run tip, got z=" + west.z);
        assertTrue(planHasConnections(sim.previewOnly(WallCardinal.WEST), WallCardinal.SOUTH, WallCardinal.WEST));
    }

    /** Tower → north wall → north tower: end cap must meet the wall segment, not the next-chain ghost slot. */
    @org.junit.jupiter.api.Test
    void towerThenNorthWall_thenNorthTower_flushAndCentered() {
        var sim = WallPlacementChainSimulation.start(-1700, 122, 50);
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.NORTH);
        sim.expandPlace(WallCardinal.NORTH);
        Vector3i wallSign = sim.committed().get(1).signAnchor();
        Rotation wallYaw = sim.committed().get(1).prefabYaw();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.NORTH);
        Vector3i tower1 = plan.anchor();
        Rotation tower1Yaw = WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps());
        WallPlacementJointAssert.assertWallColumnAligned(wallSign.x, tower1, "second tower");
        WallPlacementJointAssert.assertNearNorthOf(wallSign.z, tower1, "second tower");
        WallPlacementJointAssert.assertFlushStraightTower(
            wallSign, wallYaw, WallCardinal.NORTH, tower1, tower1Yaw, "wall north end cap"
        );
    }

    @org.junit.jupiter.api.Test
    void towerThenWestSegment_alignedRowAndFlush() {
        var sim = WallPlacementChainSimulation.start(-1693, 122, 123);
        sim.expandPlace(WallCardinal.WEST);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER).expandPlace(WallCardinal.WEST);
        Vector3i towerSign = sim.committed().get(1).signAnchor();
        Rotation towerYaw = sim.committed().get(1).prefabYaw();
        sim.expandPlace(WallCardinal.WEST);
        var seg2 = sim.committed().get(2);
        WallPlacementJointAssert.assertSameRow(towerSign.z, seg2.signAnchor(), "after tower");
        assertTrue(seg2.signAnchor().x < towerSign.x, "west chain continues west");
        WallPlacementJointAssert.assertFlushTowerToSegment(
            towerSign,
            towerYaw,
            WallCardinal.WEST,
            seg2.signAnchor(),
            seg2.prefabYaw(),
            WallCardinal.EAST,
            "tower → west run"
        );
    }

    @org.junit.jupiter.api.Test
    void nsChain_thenCornerTowerEast_connectionsAndFlush() {
        var sim = WallPlacementChainSimulation.start(-1729, 121, 61);
        sim.expandPlace(WallCardinal.NORTH);
        Vector3i wallSign = sim.committed().get(0).signAnchor();
        sim.pieceKind(WallPlacementChainPlanner.PieceKind.TOWER);
        WallPlacementChainPlanner.ExpandPreviewPlan plan = sim.previewOnly(WallCardinal.EAST);
        assertEquals(WallCardinal.EAST, plan.positionDir());
        assertTrue(plan.towerConnections().contains(WallCardinal.EAST));
        assertEquals(2, plan.towerConnections().size());
        assertTrue(plan.anchor().z < wallSign.z, "corner at north run tip");
        WallPlacementJointAssert.assertWallColumnAligned(wallSign.x, plan.anchor(), "corner");
        WallPlacementJointAssert.assertFlushMixedJoint(
            wallSign,
            sim.committed().get(0).prefabYaw(),
            WallCardinal.NORTH,
            false,
            plan.anchor(),
            WallPlacementChainPlanner.rotationStepsFrom(plan.rotationSteps()),
            WallCardinal.SOUTH,
            true,
            "corner"
        );
    }

    private static boolean planHasConnections(
        @Nonnull WallPlacementChainPlanner.ExpandPreviewPlan plan,
        @Nonnull WallCardinal a,
        @Nonnull WallCardinal b
    ) {
        return plan.towerConnections() != null
            && plan.towerConnections().contains(a)
            && plan.towerConnections().contains(b);
    }
}
