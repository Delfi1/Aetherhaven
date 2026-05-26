package com.hexvane.aetherhaven.jewelry;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import javax.annotation.Nonnull;

/** Resolves item-quality index (tooltip border, slot art) from rolled {@link JewelryRarity}. */
public final class JewelryItemQualityIndex {

    private JewelryItemQualityIndex() {}

    public static int forRarity(@Nonnull JewelryRarity rarity, @Nonnull String baseItemId) {
        Item item = Item.getAssetMap().getAsset(baseItemId);
        int fallback = item != null ? item.getQualityIndex() : 0;
        return ItemQuality.getAssetMap().getIndexOrDefault(rarity.itemQualityId(), fallback);
    }

    /** Resolve the item-quality index for a jewelry stack. */
    public static int forStack(@Nonnull ItemStack stack) {
        if (ItemStack.isEmpty(stack) || !JewelryItemIds.isJewelry(stack.getItemId())) {
            return stack.getItem().getQualityIndex();
        }
        JewelryRarity r = JewelryMetadata.readRarity(stack);
        if (r == null) {
            return stack.getItem().getQualityIndex();
        }
        return forRarity(r, stack.getItemId());
    }
}
