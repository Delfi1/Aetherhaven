package com.hexvane.aetherhaven.bard.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hexvane.aetherhaven.asset.AetherhavenAssetPaths;
import com.hexvane.aetherhaven.asset.AetherhavenPackAssetScanner;
import com.hexvane.aetherhaven.asset.AetherhavenPackAssetScanner.PackJsonFile;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BardSongCatalog {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String BARD_SONGS_FILE = "bard_songs.json";

    private final Map<String, BardSongDefinition> byId;
    private final List<BardSongDefinition> songsOrdered;

    private BardSongCatalog(@Nonnull Map<String, BardSongDefinition> byId, @Nonnull List<BardSongDefinition> songsOrdered) {
        this.byId = byId;
        this.songsOrdered = songsOrdered;
    }

    @Nonnull
    public static BardSongCatalog empty() {
        return new BardSongCatalog(Map.of(), List.of());
    }

    @Nonnull
    public static BardSongCatalog loadFromAssetPacksOrClasspath(@Nonnull ClassLoader classLoader) {
        Gson gson = new GsonBuilder().create();
        Map<String, BardSongDefinition> map = new LinkedHashMap<>();
        List<PackJsonFile> packFiles = AetherhavenPackAssetScanner.listJsonFilesUnderAllPacks(AetherhavenAssetPaths.BARD);
        if (!packFiles.isEmpty()) {
            for (PackJsonFile f : packFiles) {
                if (!f.absolutePath().getFileName().toString().equalsIgnoreCase(BARD_SONGS_FILE)) {
                    continue;
                }
                try (InputStream in = Files.newInputStream(f.absolutePath())) {
                    loadFromStream(gson, in, f.packName() + ":" + f.absolutePath(), map);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load bard songs %s", f.absolutePath());
                }
            }
        }
        if (map.isEmpty()) {
            String path = AetherhavenAssetPaths.BARD + "/" + BARD_SONGS_FILE;
            try (InputStream in = classLoader.getResourceAsStream(path)) {
                if (in != null) {
                    loadFromStream(gson, in, "classpath:" + path, map);
                } else {
                    LOGGER.atWarning().log("Bard songs file not found: %s", path);
                }
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to load bard songs from classpath %s", path);
            }
        }
        List<BardSongDefinition> ordered = new ArrayList<>(map.values());
        return new BardSongCatalog(Collections.unmodifiableMap(map), Collections.unmodifiableList(ordered));
    }

    private static void loadFromStream(
        @Nonnull Gson gson,
        @Nonnull InputStream in,
        @Nonnull String source,
        @Nonnull Map<String, BardSongDefinition> map
    ) {
        JsonObject root = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        if (root == null || !root.has("songs") || !root.get("songs").isJsonArray()) {
            LOGGER.atWarning().log("Bard songs file %s missing songs array", source);
            return;
        }
        root.getAsJsonArray("songs").forEach(el -> {
            if (!el.isJsonObject()) {
                return;
            }
            BardSongDefinition def = gson.fromJson(el.getAsJsonObject(), BardSongDefinition.class);
            if (def == null || def.getId().isEmpty()) {
                return;
            }
            map.put(def.getId(), def);
        });
    }

    @Nullable
    public BardSongDefinition byId(@Nonnull String id) {
        return byId.get(id.trim());
    }

    @Nonnull
    public List<BardSongDefinition> songsOrdered() {
        return songsOrdered;
    }
}
