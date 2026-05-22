package com.hexvane.aetherhaven.wall;

import com.hexvane.aetherhaven.AetherhavenConstants;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Maps up to two tower connection toggles to a tower construction id and yaw steps.
 *
 * <p>Corner prefab {@code Wall_Tower_OuterCorner_SE} opens toward <b>south</b> and <b>east</b> at rotation 0.
 */
public final class WallTowerPrefabResolver {
    private static final EnumSet<WallCardinal> CORNER_BASE_OPENINGS = EnumSet.of(WallCardinal.SOUTH, WallCardinal.EAST);

    private static final List<EnumSet<WallCardinal>> TOWER_CONNECTION_PAIRS =
            List.of(
                EnumSet.of(WallCardinal.NORTH, WallCardinal.SOUTH),
                EnumSet.of(WallCardinal.EAST, WallCardinal.WEST),
                EnumSet.of(WallCardinal.NORTH, WallCardinal.EAST),
                EnumSet.of(WallCardinal.EAST, WallCardinal.SOUTH),
                EnumSet.of(WallCardinal.SOUTH, WallCardinal.WEST),
                EnumSet.of(WallCardinal.NORTH, WallCardinal.WEST)
            );

    public record ResolvedTower(@Nonnull String constructionId, int rotationSteps) {}

    private WallTowerPrefabResolver() {}

    /** World faces opened by the SE corner tower prefab at {@code rotationSteps} (0..3). */
    @Nonnull
    public static EnumSet<WallCardinal> worldOpeningsForCornerRotation(int rotationSteps) {
        EnumSet<WallCardinal> faces = EnumSet.noneOf(WallCardinal.class);
        int steps = ((rotationSteps % 4) + 4) % 4;
        for (WallCardinal base : CORNER_BASE_OPENINGS) {
            WallCardinal world = base;
            for (int i = 0; i < steps; i++) {
                world = world.rotateCcw90();
            }
            faces.add(world);
        }
        return faces;
    }

    @Nullable
    public static ResolvedTower resolve(@Nonnull Set<WallCardinal> connections) {
        if (connections.isEmpty() || connections.size() > 2) {
            return null;
        }
        if (connections.size() == 1) {
            WallCardinal opening = connections.iterator().next();
            return new ResolvedTower(
                AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S,
                endcapRotationStepsForWorldOpening(opening)
            );
        }
        EnumSet<WallCardinal> pair = EnumSet.copyOf(connections);
        if (pair.contains(WallCardinal.NORTH) && pair.contains(WallCardinal.SOUTH)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_EASTDOOR_NS, 0);
        }
        if (pair.contains(WallCardinal.EAST) && pair.contains(WallCardinal.WEST)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_EASTDOOR_NS, 1);
        }
        if (pair.contains(WallCardinal.NORTH) && pair.contains(WallCardinal.EAST)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_OUTERCORNER_SE, 1);
        }
        if (pair.contains(WallCardinal.EAST) && pair.contains(WallCardinal.SOUTH)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_OUTERCORNER_SE, 0);
        }
        if (pair.contains(WallCardinal.SOUTH) && pair.contains(WallCardinal.WEST)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_OUTERCORNER_SE, 3);
        }
        if (pair.contains(WallCardinal.NORTH) && pair.contains(WallCardinal.WEST)) {
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_OUTERCORNER_SE, 2);
        }
        return null;
    }

    /**
     * {@code EndCap_S} opens toward local south at rotation 0. N/S end caps use the opposite world axis; E/W use the
     * opening direction directly (in-game E/W faces were flipped when using {@code opposite()} for all axes).
     */
    public static int endcapRotationStepsForWorldOpening(@Nonnull WallCardinal openingToward) {
        return switch (openingToward) {
            case EAST, WEST -> openingToward.rotationStepsForLocalNorthAlongAxis();
            case NORTH, SOUTH -> openingToward.opposite().rotationStepsForLocalNorthAlongAxis();
        };
    }

    /** Infers connection faces from a placed tower plot (for continuing a chain from an existing piece). */
    @Nullable
    public static EnumSet<WallCardinal> connectionsForPlacedTower(
        @Nonnull String constructionId, int rotationSteps
    ) {
        if (!WallPieceGeometry.isTowerConstructionId(constructionId)) {
            return null;
        }
        int steps = ((rotationSteps % 4) + 4) % 4;
        for (WallCardinal opening : WallCardinal.values()) {
            ResolvedTower single = resolve(EnumSet.of(opening));
            if (single != null
                && single.constructionId().equals(constructionId)
                && single.rotationSteps() == steps) {
                return EnumSet.of(opening);
            }
        }
        for (EnumSet<WallCardinal> pair : TOWER_CONNECTION_PAIRS) {
            ResolvedTower two = resolve(pair);
            if (two != null
                && two.constructionId().equals(constructionId)
                && two.rotationSteps() == steps) {
                return EnumSet.copyOf(pair);
            }
        }
        return null;
    }
}
