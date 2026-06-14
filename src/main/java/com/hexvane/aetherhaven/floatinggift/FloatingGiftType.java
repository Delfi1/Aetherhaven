package com.hexvane.aetherhaven.floatinggift;

import javax.annotation.Nonnull;

public enum FloatingGiftType {
    REGULAR,
    GREEN,
    RED;

    @Nonnull
    public static FloatingGiftType fromString(@Nonnull String raw) {
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return REGULAR;
        }
    }

    @Nonnull
    public String modelAssetId() {
        return switch (this) {
            case REGULAR -> "Floating_Gift";
            case GREEN -> "Floating_Gift_Green";
            case RED -> "Floating_Gift_Red";
        };
    }

    @Nonnull
    public String chestBlockId() {
        return switch (this) {
            case REGULAR -> "Furniture_Christmas_Chest_Small_White";
            case GREEN -> "Furniture_Christmas_Chest_Small_Green";
            case RED -> "Furniture_Christmas_Chest_Small_Red";
        };
    }
}
