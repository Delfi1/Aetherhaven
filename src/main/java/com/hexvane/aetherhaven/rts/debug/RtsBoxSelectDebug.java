package com.hexvane.aetherhaven.rts.debug;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/** Per-player preference for RTS box-select world debug overlay. */
public final class RtsBoxSelectDebug {
    private static final Map<UUID, Boolean> ENABLED = new ConcurrentHashMap<>();

    private RtsBoxSelectDebug() {}

    public static boolean isEnabled(@Nonnull UUID playerId) {
        return ENABLED.getOrDefault(playerId, false);
    }

    public static void setEnabled(@Nonnull UUID playerId, boolean enabled) {
        if (enabled) {
            ENABLED.put(playerId, true);
        } else {
            ENABLED.remove(playerId);
        }
    }
}
