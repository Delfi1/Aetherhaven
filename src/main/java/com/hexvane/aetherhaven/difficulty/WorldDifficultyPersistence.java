package com.hexvane.aetherhaven.difficulty;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.TownManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/** Loads and saves {@link WorldDifficultyState} per world under plugin data. */
public final class WorldDifficultyPersistence {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ConcurrentHashMap<String, WorldDifficultyState> CACHE = new ConcurrentHashMap<>();

    private WorldDifficultyPersistence() {}

    @Nonnull
    public static Path difficultyFile(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        String name = sanitizeWorldDirName(world.getName());
        return TownManager.pluginData(plugin).resolve("worlds").resolve(name).resolve("difficulty.json");
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
    public static WorldDifficultyState getOrLoad(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        return CACHE.computeIfAbsent(world.getName(), n -> loadFromDisk(world, plugin));
    }

    @Nonnull
    public static WorldDifficultyState loadFromDisk(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        Path path = difficultyFile(world, plugin);
        try {
            WorldDifficultyFile file = WorldDifficultyFile.readOrEmpty(path);
            WorldDifficultyState state = file.getState();
            if (state == null) {
                state = WorldDifficultyState.normalUntilChosen();
            }
            CACHE.put(world.getName(), state);
            return state;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load difficulty for world %s", world.getName());
            WorldDifficultyState fallback = WorldDifficultyState.normalUntilChosen();
            CACHE.put(world.getName(), fallback);
            return fallback;
        }
    }

    public static void save(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull WorldDifficultyState state) {
        CACHE.put(world.getName(), state);
        Path path = difficultyFile(world, plugin);
        try {
            WorldDifficultyFile file = new WorldDifficultyFile();
            file.setState(state);
            file.writeAtomic(path);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save difficulty for world %s", world.getName());
        }
    }

    public static void unloadWorld(@Nonnull World world) {
        WorldDifficultyState state = CACHE.remove(world.getName());
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
            // World name only in cache; persist without requiring live World handle
            String name = e.getKey();
            Path path =
                TownManager.pluginData(plugin)
                    .resolve("worlds")
                    .resolve(sanitizeWorldDirName(name))
                    .resolve("difficulty.json");
            try {
                WorldDifficultyFile file = new WorldDifficultyFile();
                file.setState(e.getValue());
                file.writeAtomic(path);
            } catch (IOException ex) {
                LOGGER.atSevere().withCause(ex).log("Failed to save difficulty for world %s", name);
            }
        }
    }
}
