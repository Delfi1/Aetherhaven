package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public final class ShopLootFiles {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LOOT_DIR = "shop_loot";

    private ShopLootFiles() {}

    @Nonnull
    public static Path lootDir(@Nonnull AetherhavenPlugin plugin) {
        return plugin.getDataDirectory().resolve(LOOT_DIR);
    }

    @Nonnull
    public static Path lootTablePath(@Nonnull AetherhavenPlugin plugin, @Nonnull String tableId) {
        String safe = tableId.replaceAll("[^a-zA-Z0-9_\\-]", "");
        if (safe.isBlank()) {
            safe = "default";
        }
        return lootDir(plugin).resolve(safe + ".json");
    }

    @Nonnull
    public static String readEmbeddedLootJson(@Nonnull String tableId) throws IOException {
        String safe = tableId.replaceAll("[^a-zA-Z0-9_\\-]", "");
        String resource = "/defaults/shop_loot/" + safe + ".json";
        try (InputStream in = ShopLootFiles.class.getResourceAsStream(resource)) {
            if (in == null) {
                return "{\"entries\":[]}";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void ensureDefaultLootTables(@Nonnull AetherhavenPlugin plugin) {
        Path dir = lootDir(plugin);
        try {
            Files.createDirectories(dir);
            copyEmbeddedIfMissing(plugin, "gifts");
            copyEmbeddedIfMissing(plugin, "merchant");
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to ensure shop loot tables");
        }
    }

    private static void copyEmbeddedIfMissing(@Nonnull AetherhavenPlugin plugin, @Nonnull String tableId) throws IOException {
        Path path = lootTablePath(plugin, tableId);
        if (Files.isRegularFile(path)) {
            return;
        }
        Files.writeString(path, readEmbeddedLootJson(tableId), StandardCharsets.UTF_8);
    }

    @Nonnull
    public static ShopLootTable loadTable(@Nonnull AetherhavenPlugin plugin, @Nonnull String tableId) {
        try {
            String fallback = readEmbeddedLootJson(tableId);
            return ShopLootTable.loadFromFile(lootTablePath(plugin, tableId), fallback);
        } catch (IOException e) {
            try {
                return ShopLootTable.parseJson(readEmbeddedLootJson(tableId));
            } catch (IOException e2) {
                return ShopLootTable.empty();
            }
        }
    }

    @Nonnull
    public static String[] knownTableIds() {
        return new String[] { "gifts", "merchant" };
    }

    /** All loot table ids from packaged defaults plus {@code shop_loot/*.json} in plugin data. */
    @Nonnull
    public static List<String> listLootTableIds(@Nonnull AetherhavenPlugin plugin) {
        Set<String> ids = new LinkedHashSet<>();
        Collections.addAll(ids, knownTableIds());
        Path dir = lootDir(plugin);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5))
                    .filter(id -> !id.isBlank())
                    .forEach(ids::add);
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Failed to list shop loot tables in %s", dir);
            }
        }
        return new ArrayList<>(ids);
    }
}
