package com.hexvane.aetherhaven.plotcreator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/** Canonical construction ids that variant buildings may {@code countsAsConstructionId} point to. */
public final class PlotCreatorMainConstructions {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String RESOURCE = "Server/Aetherhaven/plot_creator_main_constructions.json";

    public record Entry(@Nonnull String id, @Nonnull String labelLang) {}

    private static volatile List<Entry> cachedEntries = List.of();

    private PlotCreatorMainConstructions() {}

    @Nonnull
    public static List<Entry> entries(@Nonnull ClassLoader classLoader) {
        if (!cachedEntries.isEmpty()) {
            return cachedEntries;
        }
        synchronized (PlotCreatorMainConstructions.class) {
            if (!cachedEntries.isEmpty()) {
                return cachedEntries;
            }
            cachedEntries = load(classLoader);
            return cachedEntries;
        }
    }

    @Nonnull
    public static List<String> mainConstructionIds(@Nonnull ClassLoader classLoader) {
        ObjectArrayList<String> ids = new ObjectArrayList<>();
        for (Entry entry : entries(classLoader)) {
            ids.add(entry.id());
        }
        return ids;
    }

    public static boolean isKnownMainConstruction(@Nonnull String constructionId) {
        return mainConstructionIds(PlotCreatorMainConstructions.class.getClassLoader()).contains(constructionId);
    }

    @Nonnull
    public static ObjectArrayList<DropdownEntryInfo> dropdownEntries(@Nonnull ClassLoader classLoader) {
        ObjectArrayList<DropdownEntryInfo> out = new ObjectArrayList<>();
        for (Entry entry : entries(classLoader)) {
            out.add(new DropdownEntryInfo(LocalizableString.fromMessageId(entry.labelLang()), entry.id()));
        }
        return out;
    }

    @Nonnull
    private static List<Entry> load(@Nonnull ClassLoader classLoader) {
        try (InputStream in = classLoader.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOGGER.atWarning().log("Missing %s", RESOURCE);
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("mainConstructions");
            if (arr == null) {
                LOGGER.atWarning().log("%s missing mainConstructions array", RESOURCE);
                return List.of();
            }
            ObjectArrayList<Entry> loaded = new ObjectArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject obj = el.getAsJsonObject();
                String id = stringField(obj, "id");
                String labelLang = stringField(obj, "labelLang");
                if (id == null || labelLang == null) {
                    LOGGER.atWarning().log("%s entry missing id or labelLang: %s", RESOURCE, obj);
                    continue;
                }
                loaded.add(new Entry(id, labelLang));
            }
            return Collections.unmodifiableList(loaded);
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to load %s", RESOURCE);
            return List.of();
        }
    }

    private static String stringField(@Nonnull JsonObject obj, @Nonnull String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) {
            return null;
        }
        String value = el.getAsString().trim();
        return value.isEmpty() ? null : value;
    }
}
