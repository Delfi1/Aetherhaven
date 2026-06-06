package com.hexvane.aetherhaven.questboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum QuestBoardSlotState {
    EMPTY,
    OFFER,
    ACCEPTED,
    /** Turned in; slot stays visible until the next online dawn refresh. */
    COMPLETED;

    @Nonnull
    public static QuestBoardSlotState fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return EMPTY;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EMPTY;
        }
    }
}
