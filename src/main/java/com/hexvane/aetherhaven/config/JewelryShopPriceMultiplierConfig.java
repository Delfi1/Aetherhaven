package com.hexvane.aetherhaven.config;

import com.hexvane.aetherhaven.jewelry.JewelryRarity;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Catalog price multipliers for appraised jewelry sold at shop spots. JSON: {@code Jewelry.ShopPriceMultipliers.*} */
public final class JewelryShopPriceMultiplierConfig {
    public static final BuilderCodec<JewelryShopPriceMultiplierConfig> CODEC =
        BuilderCodec.builder(JewelryShopPriceMultiplierConfig.class, JewelryShopPriceMultiplierConfig::new)
            .append(
                new KeyedCodec<>("Note", Codec.STRING),
                (o, v) -> o.note = v != null ? v : defaultNote(),
                o -> o.note
            )
            .documentation("Shop spot catalog gold is multiplied by these values from rolled rarity.")
            .add()
            .append(new KeyedCodec<>("Common", Codec.DOUBLE), (o, v) -> o.common = v != null ? v : 1.0, o -> o.common)
            .add()
            .append(
                new KeyedCodec<>("Uncommon", Codec.DOUBLE),
                (o, v) -> o.uncommon = v != null ? v : 1.25,
                o -> o.uncommon
            )
            .add()
            .append(new KeyedCodec<>("Rare", Codec.DOUBLE), (o, v) -> o.rare = v != null ? v : 1.65, o -> o.rare)
            .add()
            .append(new KeyedCodec<>("Mythic", Codec.DOUBLE), (o, v) -> o.mythic = v != null ? v : 2.1, o -> o.mythic)
            .add()
            .append(
                new KeyedCodec<>("Legendary", Codec.DOUBLE),
                (o, v) -> o.legendary = v != null ? v : 2.75,
                o -> o.legendary
            )
            .add()
            .build();

    @Nullable
    private String note;
    private double common = 1.0;
    private double uncommon = 1.25;
    private double rare = 1.65;
    private double mythic = 2.1;
    private double legendary = 2.75;

    public JewelryShopPriceMultiplierConfig() {}

    @Nonnull
    private static String defaultNote() {
        return
            "Shop spot catalog prices for jewelry assume a common-tier base from shop_prices.json."
                + " The buyer pays base * multiplier for the rolled rarity on the listing.";
    }

    public double forRarity(@Nonnull JewelryRarity rarity) {
        double v =
            switch (rarity) {
                case COMMON -> common;
                case UNCOMMON -> uncommon;
                case RARE -> rare;
                case MYTHIC -> mythic;
                case LEGENDARY -> legendary;
            };
        return Math.max(0.0, v);
    }
}
