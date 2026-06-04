package com.hexvane.aetherhaven.shopspot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hexvane.aetherhaven.AetherhavenConstants;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShopPriceCatalog {
    private final int catalogRevision;
    private final long defaultGoldPrice;
    private final int defaultBatchSize;
    @Nonnull
    private final Map<String, ShopPriceEntry> prices;

    private ShopPriceCatalog(
        int catalogRevision,
        long defaultGoldPrice,
        int defaultBatchSize,
        @Nonnull Map<String, ShopPriceEntry> prices
    ) {
        this.catalogRevision = Math.max(0, catalogRevision);
        this.defaultGoldPrice = Math.max(0L, defaultGoldPrice);
        this.defaultBatchSize = Math.max(1, defaultBatchSize);
        this.prices = prices;
    }

    @Nonnull
    public static ShopPriceCatalog empty() {
        return new ShopPriceCatalog(0, AetherhavenConstants.SHOP_SPOT_DEFAULT_GOLD_PRICE, 1, Map.of());
    }

    @Nonnull
    public static ShopPriceCatalog parseJson(@Nonnull String json) {
        int catalogRevision = 0;
        long defaultPrice = AetherhavenConstants.SHOP_SPOT_DEFAULT_GOLD_PRICE;
        int defaultBatch = 1;
        Map<String, ShopPriceEntry> map = new HashMap<>();
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return new ShopPriceCatalog(catalogRevision, defaultPrice, defaultBatch, map);
        }
        JsonObject obj = root.getAsJsonObject();
        if (obj.has("catalogRevision")) {
            catalogRevision = Math.max(0, obj.get("catalogRevision").getAsInt());
        }
        if (obj.has("defaultGoldPrice")) {
            defaultPrice = Math.max(0L, obj.get("defaultGoldPrice").getAsLong());
        }
        if (obj.has("defaultBatchSize")) {
            defaultBatch = Math.max(1, obj.get("defaultBatchSize").getAsInt());
        }
        if (obj.has("prices") && obj.get("prices").isJsonObject()) {
            JsonObject pricesObj = obj.getAsJsonObject("prices");
            for (var e : pricesObj.entrySet()) {
                ShopPriceEntry entry = parsePriceEntry(e.getValue(), defaultPrice, defaultBatch);
                if (entry != null) {
                    map.put(e.getKey(), entry);
                }
            }
        }
        return new ShopPriceCatalog(catalogRevision, defaultPrice, defaultBatch, map);
    }

    @Nullable
    private static ShopPriceEntry parsePriceEntry(
        @Nonnull JsonElement value,
        long defaultGold,
        int defaultBatch
    ) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new ShopPriceEntry(value.getAsLong(), 1);
        }
        if (!value.isJsonObject()) {
            return null;
        }
        JsonObject o = value.getAsJsonObject();
        long gold = defaultGold;
        if (o.has("gold")) {
            gold = Math.max(0L, o.get("gold").getAsLong());
        } else if (o.has("goldPrice")) {
            gold = Math.max(0L, o.get("goldPrice").getAsLong());
        }
        int batch = defaultBatch;
        if (o.has("batchSize")) {
            batch = Math.max(1, o.get("batchSize").getAsInt());
        } else if (o.has("batch")) {
            batch = Math.max(1, o.get("batch").getAsInt());
        }
        return new ShopPriceEntry(gold, batch);
    }

    @Nonnull
    public static ShopPriceCatalog loadFromFile(@Nonnull Path path, @Nonnull String fallbackJson) throws IOException {
        if (!Files.isRegularFile(path)) {
            return parseJson(fallbackJson);
        }
        return parseJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    @Nonnull
    public ShopPriceEntry getEntry(@Nonnull String itemId) {
        ShopPriceEntry explicit = prices.get(itemId);
        if (explicit != null) {
            return explicit;
        }
        return new ShopPriceEntry(defaultGoldPrice, defaultBatchSize);
    }

    /** Gold per purchase lot (batch of {@link ShopPriceEntry#getBatchSize()} items when batched). */
    public long getGoldPrice(@Nonnull String itemId) {
        return getEntry(itemId).getGoldPerBatch();
    }

    public int getBatchSize(@Nonnull String itemId) {
        return getEntry(itemId).getBatchSize();
    }

    public boolean hasExplicitPrice(@Nonnull String itemId) {
        return prices.containsKey(itemId);
    }

    public int getCatalogRevision() {
        return catalogRevision;
    }

    public int getExplicitPriceCount() {
        return prices.size();
    }

    public long getDefaultGoldPrice() {
        return defaultGoldPrice;
    }

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    /** Bundled catalog plus per-item overrides from the data folder (override keys win). */
    @Nonnull
    public ShopPriceCatalog withOverrides(@Nonnull ShopPriceCatalog overrides) {
        Map<String, ShopPriceEntry> merged = new HashMap<>(this.prices);
        merged.putAll(overrides.prices);
        return new ShopPriceCatalog(
            Math.max(this.catalogRevision, overrides.catalogRevision),
            this.defaultGoldPrice,
            this.defaultBatchSize,
            merged
        );
    }
}
