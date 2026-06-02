package com.hexvane.aetherhaven.huntingknife;

import javax.annotation.Nonnull;

/** Item ids that count as bonus butcher loot from the hunting knife. */
public final class HuntingKnifeButcherLoot {
    private HuntingKnifeButcherLoot() {}

    public static boolean isButcherLoot(@Nonnull String itemId) {
        return isHide(itemId) || isRawMeat(itemId) || isFeathers(itemId);
    }

    public static boolean isHide(@Nonnull String itemId) {
        return itemId.startsWith("Ingredient_Hide_");
    }

    public static boolean isRawMeat(@Nonnull String itemId) {
        return itemId.startsWith("Food_") && itemId.contains("_Raw");
    }

    public static boolean isFeathers(@Nonnull String itemId) {
        return itemId.startsWith("Ingredient_Feathers_");
    }
}
