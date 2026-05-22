package com.hexvane.aetherhaven.wall;

import com.hexvane.aetherhaven.AetherhavenConstants;
import java.util.EnumSet;
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
            WallCardinal only = connections.iterator().next();
            int steps = only.opposite().rotationStepsForLocalNorthAlongAxis();
            return new ResolvedTower(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S, steps);
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
}
