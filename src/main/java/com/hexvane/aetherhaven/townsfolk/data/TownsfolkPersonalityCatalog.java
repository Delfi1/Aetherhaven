package com.hexvane.aetherhaven.townsfolk.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hexvane.aetherhaven.asset.AetherhavenAssetPaths;
import com.hexvane.aetherhaven.asset.AetherhavenPackAssetScanner;
import com.hexvane.aetherhaven.asset.AetherhavenPackAssetScanner.PackJsonFile;
import com.hexvane.aetherhaven.asset.ClasspathResourceScanner;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TownsfolkPersonalityCatalog {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, TownsfolkPersonalityDefinition> byId;

    private TownsfolkPersonalityCatalog(@Nonnull Map<String, TownsfolkPersonalityDefinition> byId) {
        this.byId = byId;
    }

    @Nonnull
    public static TownsfolkPersonalityCatalog empty() {
        return new TownsfolkPersonalityCatalog(Map.of());
    }

    @Nonnull
    public static TownsfolkPersonalityCatalog loadFromAssetPacksOrClasspath(@Nonnull ClassLoader classLoader) {
        Gson gson = new GsonBuilder().create();
        Map<String, TownsfolkPersonalityDefinition> byId = new LinkedHashMap<>();
        List<PackJsonFile> packFiles = AetherhavenPackAssetScanner.listJsonFilesUnderAllPacks(AetherhavenAssetPaths.PERSONALITIES);
        if (!packFiles.isEmpty()) {
            for (PackJsonFile f : packFiles) {
                try (InputStream in = Files.newInputStream(f.absolutePath())) {
                    loadFromStream(gson, in, f.packName() + ":" + f.absolutePath(), byId);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load personality %s", f.absolutePath());
                }
            }
        } else {
            for (String path : ClasspathResourceScanner.listJsonFiles(classLoader, AetherhavenAssetPaths.personalitiesPrefix())) {
                try (InputStream in = classLoader.getResourceAsStream(path)) {
                    if (in != null) {
                        loadFromStream(gson, in, path, byId);
                    }
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load personality %s", path);
                }
            }
        }
        LOGGER.atInfo().log("Loaded %s townsfolk personality definition(s)", byId.size());
        return new TownsfolkPersonalityCatalog(Collections.unmodifiableMap(byId));
    }

    private static void loadFromStream(
        @Nonnull Gson gson,
        @Nonnull InputStream in,
        @Nonnull String label,
        @Nonnull Map<String, TownsfolkPersonalityDefinition> byId
    ) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || root.isJsonNull() || !root.isJsonObject()) {
                return;
            }
            TownsfolkPersonalityDefinition def = gson.fromJson(root.getAsJsonObject(), TownsfolkPersonalityDefinition.class);
            putDefinition(def, label, byId);
        }
    }

    private static void putDefinition(
        @Nullable TownsfolkPersonalityDefinition def,
        @Nonnull String label,
        @Nonnull Map<String, TownsfolkPersonalityDefinition> byId
    ) {
        if (def == null) {
            return;
        }
        String id = def.getId();
        if (id.isEmpty()) {
            LOGGER.atWarning().log("Skipping personality with empty id (%s)", label);
            return;
        }
        byId.put(id, def);
    }

    @Nullable
    public TownsfolkPersonalityDefinition byId(@Nonnull String personalityId) {
        return byId.get(personalityId.trim());
    }

    @Nonnull
    public Map<String, TownsfolkPersonalityDefinition> allById() {
        return byId;
    }
}
