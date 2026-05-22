package com.hexvane.aetherhaven.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.EnumSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Verifies corner tower yaw matches world connection faces (SE base prefab opens S+E at rot 0). */
@Tag("wall-placement")
class WallTowerPrefabResolverTest {

    @Test
    void cornerRotation_zeroOpensSouthEast() {
        assertEquals(EnumSet.of(WallCardinal.SOUTH, WallCardinal.EAST), WallTowerPrefabResolver.worldOpeningsForCornerRotation(0));
    }

    @Test
    void cornerRotation_oneOpensNorthEast() {
        assertEquals(EnumSet.of(WallCardinal.NORTH, WallCardinal.EAST), WallTowerPrefabResolver.worldOpeningsForCornerRotation(1));
    }

    @Test
    void cornerRotation_twoOpensNorthWest() {
        assertEquals(EnumSet.of(WallCardinal.NORTH, WallCardinal.WEST), WallTowerPrefabResolver.worldOpeningsForCornerRotation(2));
    }

    @Test
    void cornerRotation_threeOpensSouthWest() {
        assertEquals(EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST), WallTowerPrefabResolver.worldOpeningsForCornerRotation(3));
    }

    @Test
    void resolve_southWestCorner_usesRotationThreeNotOne() {
        EnumSet<WallCardinal> requested = EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST);
        WallTowerPrefabResolver.ResolvedTower resolved = WallTowerPrefabResolver.resolve(requested);
        assertNotNull(resolved);
        assertEquals(3, resolved.rotationSteps());
        assertEquals(requested, WallTowerPrefabResolver.worldOpeningsForCornerRotation(resolved.rotationSteps()));
    }

    @Test
    void resolve_southWest_doesNotUseRotationOne() {
        EnumSet<WallCardinal> requested = EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST);
        WallTowerPrefabResolver.ResolvedTower wrong = WallTowerPrefabResolver.resolve(requested);
        assertNotNull(wrong);
        assertEquals(
            EnumSet.of(WallCardinal.NORTH, WallCardinal.EAST),
            WallTowerPrefabResolver.worldOpeningsForCornerRotation(1),
            "rotation 1 is N+E; that was the in-game bug for S+W towers"
        );
    }

    @ParameterizedTest
    @EnumSource(CornerPair.class)
    void resolve_cornerPairs_yawMatchesWorldConnections(CornerPair pair) {
        WallTowerPrefabResolver.ResolvedTower resolved = WallTowerPrefabResolver.resolve(pair.connections());
        assertNotNull(resolved, () -> "connections " + pair.connections());
        assertEquals(pair.expectedSteps(), resolved.rotationSteps());
        assertEquals(
            pair.connections(),
            WallTowerPrefabResolver.worldOpeningsForCornerRotation(resolved.rotationSteps()),
            () -> "wrong world openings for " + pair.connections()
        );
    }

    private enum CornerPair {
        NE(EnumSet.of(WallCardinal.NORTH, WallCardinal.EAST), 1),
        ES(EnumSet.of(WallCardinal.EAST, WallCardinal.SOUTH), 0),
        SW(EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST), 3),
        NW(EnumSet.of(WallCardinal.NORTH, WallCardinal.WEST), 2);

        private final EnumSet<WallCardinal> connections;
        private final int expectedSteps;

        CornerPair(EnumSet<WallCardinal> connections, int expectedSteps) {
            this.connections = connections;
            this.expectedSteps = expectedSteps;
        }

        EnumSet<WallCardinal> connections() {
            return EnumSet.copyOf(connections);
        }

        int expectedSteps() {
            return expectedSteps;
        }
    }
}
