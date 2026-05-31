package com.hexvane.aetherhaven.equipment.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hexvane.aetherhaven.asset.AetherhavenPackAssetScanner;
import com.hexvane.aetherhaven.asset.ClasspathResourceScanner;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EquipmentProfileCatalog {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String EQUIPMENT_PREFIX = "Server/Aetherhaven/Equipment/";

    private final Map<String, EquipmentProfileDefinition> byId;

    private EquipmentProfileCatalog(@Nonnull Map<String, EquipmentProfileDefinition> byId) {
        this.byId = byId;
    }

    @Nonnull
    public static EquipmentProfileCatalog empty() {
        return new EquipmentProfileCatalog(Map.of());
    }

    @Nonnull
    public static EquipmentProfileCatalog loadFromAssetPacksOrClasspath(@Nonnull ClassLoader classLoader) {
        Gson gson = new GsonBuilder().create();
        Map<String, EquipmentProfileDefinition> byId = new LinkedHashMap<>();
        var packFiles = AetherhavenPackAssetScanner.listJsonFilesUnderAllPacks(EQUIPMENT_PREFIX);
        if (!packFiles.isEmpty()) {
            for (var f : packFiles) {
                try (InputStream in = Files.newInputStream(f.absolutePath())) {
                    loadStream(gson, in, f.packName() + ":" + f.absolutePath(), byId);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load equipment profile %s", f.absolutePath());
                }
            }
        } else {
            for (String path : ClasspathResourceScanner.listJsonFiles(classLoader, EQUIPMENT_PREFIX)) {
                try (InputStream in = classLoader.getResourceAsStream(path)) {
                    if (in != null) {
                        loadStream(gson, in, path, byId);
                    }
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load equipment profile %s", path);
                }
            }
        }
        LOGGER.atInfo().log("Loaded %s equipment profile(s)", byId.size());
        return new EquipmentProfileCatalog(Collections.unmodifiableMap(byId));
    }

    private static void loadStream(
        @Nonnull Gson gson,
        @Nonnull InputStream in,
        @Nonnull String label,
        @Nonnull Map<String, EquipmentProfileDefinition> byId
    ) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return;
            }
            EquipmentProfileDefinition def = gson.fromJson(root.getAsJsonObject(), EquipmentProfileDefinition.class);
            if (def == null || def.getId().isEmpty()) {
                LOGGER.atWarning().log("Skipping equipment profile with empty id (%s)", label);
                return;
            }
            byId.put(def.getId(), def);
        }
    }

    @Nullable
    public EquipmentProfileDefinition byId(@Nonnull String id) {
        return byId.get(id.trim());
    }

    @Nonnull
    public Map<String, EquipmentProfileDefinition> allById() {
        return byId;
    }
}
