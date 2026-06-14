package com.hexvane.aetherhaven.jewelry;

import javax.annotation.Nonnull;

/** Hand mirror loadout slots (extend when adding trinket slots). */
public enum JewelrySlot {
    RING_1(0),
    RING_2(1),
    NECKLACE(2);

    private final int index;

    JewelrySlot(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public static final int COUNT = 3;

    @Nonnull
    public static JewelrySlot fromIndex(int index) {
        return switch (index) {
            case 0 -> RING_1;
            case 1 -> RING_2;
            case 2 -> NECKLACE;
            default -> throw new IllegalArgumentException("Invalid jewelry slot: " + index);
        };
    }
}
