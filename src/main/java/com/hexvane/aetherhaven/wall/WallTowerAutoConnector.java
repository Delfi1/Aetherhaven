package com.hexvane.aetherhaven.wall;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Infers tower connection faces from placement-pad chaining (no manual toggles). */
public final class WallTowerAutoConnector {
    private WallTowerAutoConnector() {}

    /**
     * Single connection on the face that meets the previous wall piece when chaining along {@code chainExpandDir}
     * (the direction you moved to place this piece).
     */
    @Nonnull
    public static EnumSet<WallCardinal> connectionsTowardPreviousPiece(@Nonnull WallCardinal chainExpandDir) {
        return EnumSet.of(chainExpandDir.opposite());
    }

    /** Corner pair once the player picks the next run direction before placing the following piece. */
    @Nonnull
    public static EnumSet<WallCardinal> connectionsForCorner(
        @Nonnull EnumSet<WallCardinal> existing, @Nonnull WallCardinal outgoingExpandDir
    ) {
        EnumSet<WallCardinal> dirs = EnumSet.copyOf(existing);
        dirs.add(outgoingExpandDir);
        return dirs;
    }

    @Nullable
    public static WallTowerPrefabResolver.ResolvedTower resolve(@Nonnull EnumSet<WallCardinal> connections) {
        return WallTowerPrefabResolver.resolve(connections);
    }
}
