package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public final class ShopLootFiles {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LOOT_DIR = "shop_loot";
    private static final String EMBEDDED_LOOT_PREFIX = "defaults/shop_loot/";
    private static final String EMBEDDED_LOOT_ANCHOR = EMBEDDED_LOOT_PREFIX + "gifts.json";

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
            for (String tableId : listEmbeddedDefaultTableIds()) {
                ensureEmbeddedTable(plugin, tableId);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to ensure shop loot tables");
        }
    }

    private static void ensureEmbeddedTable(@Nonnull AetherhavenPlugin plugin, @Nonnull String tableId) throws IOException {
        Path path = lootTablePath(plugin, tableId);
        String embedded = readEmbeddedLootJson(tableId);
        if (!Files.isRegularFile(path)) {
            Files.writeString(path, embedded, StandardCharsets.UTF_8);
            return;
        }
        String onDisk = Files.readString(path, StandardCharsets.UTF_8);
        if (isValidLootJson(onDisk)) {
            return;
        }
        LOGGER.atWarning().log("Repairing invalid bundled shop loot table %s at %s", tableId, path);
        Files.writeString(path, embedded, StandardCharsets.UTF_8);
    }

    @Nonnull
    public static ShopLootTable loadTable(@Nonnull AetherhavenPlugin plugin, @Nonnull String tableId) {
        Path path = lootTablePath(plugin, tableId);
        try {
            String fallback = readEmbeddedLootJson(tableId);
            if (!Files.isRegularFile(path)) {
                return ShopLootTable.parseJson(fallback);
            }
            String onDisk = Files.readString(path, StandardCharsets.UTF_8);
            try {
                return ShopLootTable.parseJson(onDisk);
            } catch (RuntimeException parseError) {
                if (isValidLootJson(fallback)) {
                    LOGGER
                        .atWarning()
                        .withCause(parseError)
                        .log("Invalid shop loot table %s at %s; using bundled default", tableId, path);
                    try {
                        Files.writeString(path, fallback, StandardCharsets.UTF_8);
                    } catch (IOException writeError) {
                        LOGGER.atWarning().withCause(writeError).log("Failed to repair shop loot table %s", tableId);
                    }
                    return ShopLootTable.parseJson(fallback);
                }
                throw parseError;
            }
        } catch (IOException e) {
            try {
                return ShopLootTable.parseJson(readEmbeddedLootJson(tableId));
            } catch (IOException e2) {
                return ShopLootTable.empty();
            }
        }
    }

    private static boolean isValidLootJson(@Nonnull String json) {
        try {
            ShopLootTable.parseJson(json);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Nonnull
    public static String[] knownTableIds() {
        List<String> ids = listEmbeddedDefaultTableIds();
        return ids.toArray(String[]::new);
    }

    /** All loot table ids from packaged defaults plus {@code shop_loot/*.json} in plugin data. */
    @Nonnull
    public static List<String> listLootTableIds(@Nonnull AetherhavenPlugin plugin) {
        Set<String> ids = new LinkedHashSet<>(listEmbeddedDefaultTableIds());
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

    @Nonnull
    private static List<String> listEmbeddedDefaultTableIds() {
        Set<String> ids = new LinkedHashSet<>();
        ClassLoader classLoader = ShopLootFiles.class.getClassLoader();
        URL anchor = classLoader.getResource(EMBEDDED_LOOT_ANCHOR);
        if (anchor == null) {
            ids.add("gifts");
            ids.add("merchant");
            return new ArrayList<>(ids);
        }
        try {
            if ("jar".equalsIgnoreCase(anchor.getProtocol())) {
                scanEmbeddedJar(anchor, ids);
            } else {
                scanEmbeddedDirectory(anchor, ids);
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to list embedded shop loot tables");
            ids.add("gifts");
            ids.add("merchant");
        }
        return new ArrayList<>(ids);
    }

    private static void scanEmbeddedJar(@Nonnull URL anchorJarEntry, @Nonnull Set<String> ids) throws IOException {
        JarURLConnection conn = (JarURLConnection) anchorJarEntry.openConnection();
        try (JarFile jar = conn.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.startsWith(EMBEDDED_LOOT_PREFIX) && name.endsWith(".json")) {
                    addTableIdFromResourcePath(name, ids);
                }
            }
        }
    }

    private static void scanEmbeddedDirectory(@Nonnull URL anchorResource, @Nonnull Set<String> ids) throws Exception {
        Path dir = Path.of(anchorResource.toURI()).getParent();
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(name -> name.endsWith(".json"))
                .map(name -> EMBEDDED_LOOT_PREFIX + name)
                .forEach(path -> addTableIdFromResourcePath(path, ids));
        }
    }

    private static void addTableIdFromResourcePath(@Nonnull String resourcePath, @Nonnull Set<String> ids) {
        if (!resourcePath.startsWith(EMBEDDED_LOOT_PREFIX) || !resourcePath.endsWith(".json")) {
            return;
        }
        String id = resourcePath.substring(EMBEDDED_LOOT_PREFIX.length(), resourcePath.length() - 5);
        if (!id.isBlank()) {
            ids.add(id);
        }
    }
}
