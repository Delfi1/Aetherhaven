package com.hexvane.aetherhaven.town;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Level-2 town specialization; doubles output at the matching production workplace. */
public enum CharterSpecialization {
    MINING("mining"),
    LOGGING("logging"),
    FARMING("farming"),
    RANCHING("ranching");

    private final String id;

    CharterSpecialization(@Nonnull String id) {
        this.id = id;
    }

    @Nonnull
    public String id() {
        return id;
    }

    @Nullable
    public static CharterSpecialization fromId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        for (CharterSpecialization v : values()) {
            if (v.id.equalsIgnoreCase(s)) {
                return v;
            }
        }
        return null;
    }
}
