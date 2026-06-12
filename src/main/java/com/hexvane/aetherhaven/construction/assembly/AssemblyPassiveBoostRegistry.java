package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/** Per world passive assembly speed multiplier from the builder villager (and similar future buffs). */
public final class AssemblyPassiveBoostRegistry {
    private static final ConcurrentHashMap<String, ConcurrentHashMap<UUID, Integer>> WORLD_PLOT_BOOST = new ConcurrentHashMap<>();

    private AssemblyPassiveBoostRegistry() {}

    public static void clearForWorld(@Nonnull World world) {
        WORLD_PLOT_BOOST.remove(world.getName());
    }

    /** {@code multiplier <= 0} removes any entry for the plot. */
    public static void setBoost(@Nonnull World world, @Nonnull UUID plotId, int multiplier) {
        if (multiplier <= 0) {
            ConcurrentHashMap<UUID, Integer> m = WORLD_PLOT_BOOST.get(world.getName());
            if (m != null) {
                m.remove(plotId);
            }
            return;
        }
        WORLD_PLOT_BOOST.computeIfAbsent(world.getName(), k -> new ConcurrentHashMap<>()).merge(plotId, multiplier, Math::max);
    }

    public static int boostFor(@Nonnull World world, @Nonnull UUID plotId) {
        ConcurrentHashMap<UUID, Integer> m = WORLD_PLOT_BOOST.get(world.getName());
        if (m == null) {
            return 1;
        }
        return Math.max(1, m.getOrDefault(plotId, 1));
    }

    @Nonnull
    public static Map<UUID, Integer> snapshot(@Nonnull World world) {
        ConcurrentHashMap<UUID, Integer> m = WORLD_PLOT_BOOST.get(world.getName());
        return m != null ? Map.copyOf(m) : Map.of();
    }
}
