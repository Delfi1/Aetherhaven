package com.hexvane.aetherhaven.rts;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/** Thread-safe RTS command-mode flag for packet adapters (avoid Store access off the world thread). */
public final class RtsCommandingSessionIndex {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    private RtsCommandingSessionIndex() {}

    public static void markActive(@Nonnull UUID playerUuid) {
        ACTIVE.add(playerUuid);
    }

    public static void unmarkActive(@Nonnull UUID playerUuid) {
        ACTIVE.remove(playerUuid);
    }

    public static boolean isCommanding(@Nonnull UUID playerUuid) {
        return ACTIVE.contains(playerUuid);
    }
}
