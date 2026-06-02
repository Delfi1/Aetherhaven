package com.hexvane.aetherhaven.jewelry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** How a jewelry item id behaves when equipped (enchanted gem pieces vs static artifacts). */
public enum JewelryPieceKind {
    ENCHANTED,
    ARTIFACT;

    @Nullable
    public static JewelryPieceKind forItemId(@Nullable String itemId) {
        if (itemId == null || !JewelryItemIds.isJewelry(itemId)) {
            return null;
        }
        if (JewelryGem.fromItemId(itemId) != null) {
            return ENCHANTED;
        }
        return ARTIFACT;
    }

    public static boolean isEnchanted(@Nullable String itemId) {
        return forItemId(itemId) == ENCHANTED;
    }

    public static boolean isArtifact(@Nullable String itemId) {
        return forItemId(itemId) == ARTIFACT;
    }
}
