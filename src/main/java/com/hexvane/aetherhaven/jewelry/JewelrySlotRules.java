package com.hexvane.aetherhaven.jewelry;

import javax.annotation.Nonnull;

public final class JewelrySlotRules {
    private JewelrySlotRules() {}

    public static boolean canAccept(@Nonnull JewelrySlot slot, @Nonnull String itemId) {
        return switch (slot) {
            case RING_1, RING_2 -> JewelryItemIds.isRing(itemId);
            case NECKLACE -> JewelryItemIds.isNecklace(itemId);
        };
    }

    public static boolean canAccept(int slotIndex0To2, @Nonnull String itemId) {
        if (slotIndex0To2 < 0 || slotIndex0To2 >= JewelrySlot.COUNT) {
            return false;
        }
        return canAccept(JewelrySlot.fromIndex(slotIndex0To2), itemId);
    }
}
