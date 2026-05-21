package com.hexvane.aetherhaven.difficulty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** World difficulty preset; {@link #CUSTOM} when sliders diverge from a named preset. */
public enum DifficultyPreset {
    EASY,
    NORMAL,
    HARD,
    CUSTOM;

    @Nonnull
    public static DifficultyPreset fromPersisted(@Nullable String s) {
        if (s == null || s.isBlank()) {
            return NORMAL;
        }
        return switch (s.trim().toUpperCase()) {
            case "EASY" -> EASY;
            case "HARD" -> HARD;
            case "CUSTOM" -> CUSTOM;
            default -> NORMAL;
        };
    }

    @Nonnull
    public String persisted() {
        return name();
    }
}
