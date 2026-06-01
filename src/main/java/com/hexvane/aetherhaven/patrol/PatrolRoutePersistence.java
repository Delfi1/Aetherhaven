package com.hexvane.aetherhaven.patrol;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.TownManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;

public final class PatrolRoutePersistence {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private PatrolRoutePersistence() {}

    @Nonnull
    private static Path pathFile(@Nonnull AetherhavenPlugin plugin, @Nonnull String worldName) {
        return TownManager.pluginData(plugin)
            .resolve("worlds")
            .resolve(sanitizeWorldDirName(worldName))
            .resolve("patrol_routes.json");
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

    public static void load(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull PatrolRouteRegistry registry
    ) {
        Path p = pathFile(plugin, world.getName());
        try {
            PatrolRoutesWorldFile f = PatrolRoutesWorldFile.readOrEmpty(p);
            registry.replaceAll(f.getRoutes());
            LOGGER.atInfo().log(
                "Aetherhaven loaded %s patrol routes for world %s from %s",
                registry.all().size(),
                world.getName(),
                p
            );
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load patrol routes for world %s", world.getName());
        }
    }

    public static void save(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull PatrolRouteRegistry registry
    ) {
        Path p = pathFile(plugin, world.getName());
        try {
            PatrolRoutesWorldFile f = new PatrolRoutesWorldFile();
            f.getRoutes().addAll(registry.all());
            f.writeAtomic(p);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save patrol routes for world %s", world.getName());
        }
    }
}
