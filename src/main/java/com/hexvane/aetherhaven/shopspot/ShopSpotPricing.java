package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.config.AetherhavenPluginConfig;
import javax.annotation.Nonnull;

public final class ShopSpotPricing {
    private ShopSpotPricing() {}

    @Nonnull
    public static ShopPriceEntry catalogEntry(@Nonnull AetherhavenPlugin plugin, @Nonnull String itemId) {
        return plugin.getShopPriceCatalog().getEntry(itemId);
    }

    /** Gold per purchase lot after player spot discount (NPC spots use full catalog gold). */
    public static long goldPerBatch(
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRecord record,
        @Nonnull String itemId
    ) {
        ShopPriceEntry entry = catalogEntry(plugin, itemId);
        long catalogGold = entry.getGoldPerBatch();
        catalogGold = ShopSpotJewelrySupport.scaleCatalogGold(plugin, itemId, record, catalogGold);
        if (!record.isPlayerControlled()) {
            return catalogGold;
        }
        return playerListingGoldPerBatch(catalogGold, plugin.getConfig().get());
    }

    public static long playerListingGoldPerBatch(long catalogGoldPerBatch, @Nonnull AetherhavenPluginConfig cfg) {
        if (catalogGoldPerBatch <= 0L) {
            return 0L;
        }
        int percent = cfg.getShopSpotPlayerListingPricePercent();
        long scaled = (catalogGoldPerBatch * (long) percent) / 100L;
        return Math.max(1L, scaled);
    }

    public static long totalCost(long goldPerBatch, int batchCount) {
        return goldPerBatch * Math.max(0, batchCount);
    }
}
