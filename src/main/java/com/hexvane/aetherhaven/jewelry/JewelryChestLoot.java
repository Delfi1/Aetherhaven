package com.hexvane.aetherhaven.jewelry;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.config.AetherhavenPluginConfig;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

/** Chest jewelry rolls: optional artifact prefix, then unidentified enchanted jewelry. */
public final class JewelryChestLoot {
    private JewelryChestLoot() {}

    @Nonnull
    public static ItemStack rollForChest(@Nonnull ThreadLocalRandom rnd, @Nonnull AetherhavenPluginConfig cfg) {
        double c = cfg.getJewelryRarityWeightCommon();
        double u = cfg.getJewelryRarityWeightUncommon();
        double r = cfg.getJewelryRarityWeightRare();
        double m = cfg.getJewelryRarityWeightMythic();
        double l = cfg.getJewelryRarityWeightLegendary();
        double total = c + u + r + m + l;
        if (total <= 0.0) {
            return rollArtifactFallback(rnd);
        }
        double p = rnd.nextDouble() * total;
        if (p < l) {
            return artifactStack(AetherhavenConstants.ITEM_RING_LARGE_GLOW);
        }
        p -= l;
        if (p < m) {
            return artifactStack(AetherhavenConstants.ITEM_RING_GLOW);
        }
        return UnidentifiedJewelry.rollEnchantedStack(rnd);
    }

    @Nonnull
    private static ItemStack rollArtifactFallback(@Nonnull ThreadLocalRandom rnd) {
        int t = rnd.nextInt(100);
        if (t < 1) {
            return artifactStack(AetherhavenConstants.ITEM_RING_LARGE_GLOW);
        }
        if (t < 5) {
            return artifactStack(AetherhavenConstants.ITEM_RING_GLOW);
        }
        return UnidentifiedJewelry.rollEnchantedStack(rnd);
    }

    @Nonnull
    private static ItemStack artifactStack(@Nonnull String itemId) {
        return new ItemStack(itemId, 1);
    }
}
