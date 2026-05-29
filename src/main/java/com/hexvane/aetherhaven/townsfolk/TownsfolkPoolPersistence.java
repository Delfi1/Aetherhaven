package com.hexvane.aetherhaven.townsfolk;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.TownManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public final class TownsfolkPoolPersistence {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ConcurrentHashMap<String, TownsfolkPoolState> CACHE = new ConcurrentHashMap<>();

    private TownsfolkPoolPersistence() {}

    @Nonnull
    public static Path poolFile(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        String name = sanitizeWorldDirName(world.getName());
        return TownManager.pluginData(plugin).resolve("worlds").resolve(name).resolve("townsfolk_pool.json");
    }

    @Nonnull
    private static String sanitizeWorldDirName(@Nonnull String worldName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < worldName.length(); i++) {
            char c = worldName.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.isEmpty() ? "world" : sb.toString();
    }

    @Nonnull
    public static TownsfolkPoolState getOrLoad(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        return CACHE.computeIfAbsent(world.getName(), n -> loadFromDisk(world, plugin));
    }

    /** Loads pool state from disk only; {@link #getOrLoad} installs the result in {@link #CACHE}. */
    @Nonnull
    private static TownsfolkPoolState loadFromDisk(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        Path path = poolFile(world, plugin);
        try {
            TownsfolkPoolFile file = TownsfolkPoolFile.readOrEmpty(path);
            TownsfolkPoolState state = new TownsfolkPoolState();
            state.loadFromFile(file);
            return state;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load townsfolk pool for world %s", world.getName());
            return new TownsfolkPoolState();
        }
    }

    public static void save(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull TownsfolkPoolState state) {
        CACHE.put(world.getName(), state);
        Path path = poolFile(world, plugin);
        try {
            state.toFile().writeAtomic(path);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save townsfolk pool for world %s", world.getName());
        }
    }

    public static void unloadWorld(@Nonnull World world) {
        TownsfolkPoolState state = CACHE.remove(world.getName());
        if (state == null) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin != null) {
            save(world, plugin, state);
        }
    }

    public static void saveAll() {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        for (var e : CACHE.entrySet()) {
            // World name only in cache key; save uses path from world name
            Path path = TownManager.pluginData(plugin).resolve("worlds").resolve(sanitizeWorldDirName(e.getKey())).resolve("townsfolk_pool.json");
            try {
                e.getValue().toFile().writeAtomic(path);
            } catch (IOException ex) {
                LOGGER.atSevere().withCause(ex).log("Failed to save townsfolk pool for world %s", e.getKey());
            }
        }
    }
}
