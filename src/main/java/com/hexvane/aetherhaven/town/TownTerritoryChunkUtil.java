package com.hexvane.aetherhaven.town;

import com.hexvane.aetherhaven.entity.EntityChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;

/** Chunk load checks for town-scoped simulation (charter anchor, plot footprints). */
public final class TownTerritoryChunkUtil {
    private TownTerritoryChunkUtil() {}

    /**
     * True when the chunk column containing the town charter block is in memory — the town prefab is present in the
     * simulation, not only in {@link TownRecord} data.
     */
    public static boolean isCharterChunkLoaded(@Nonnull World world, @Nonnull TownRecord town) {
        return EntityChunkUtil.isBlockChunkInMemory(world, town.getCharterX(), town.getCharterZ());
    }
}
