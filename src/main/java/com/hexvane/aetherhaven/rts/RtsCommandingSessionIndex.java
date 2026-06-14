package com.hexvane.aetherhaven.rts;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Thread-safe RTS command-mode flags for packet adapters (avoid Store access off the world thread).
 * Also tracks one active commander per town so two players cannot control the same guard roster.
 */
public final class RtsCommandingSessionIndex {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, UUID> TOWN_COMMANDER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> PLAYER_TOWN = new ConcurrentHashMap<>();

    private RtsCommandingSessionIndex() {}

    /**
     * Claim exclusive command rights for a town.
     *
     * @return false if another player is already commanding this town
     */
    public static boolean tryClaimTown(@Nonnull UUID playerUuid, @Nonnull UUID townId) {
        ACTIVE.add(playerUuid);
        UUID prior = TOWN_COMMANDER.putIfAbsent(townId, playerUuid);
        if (prior == null || prior.equals(playerUuid)) {
            PLAYER_TOWN.put(playerUuid, townId);
            return true;
        }
        ACTIVE.remove(playerUuid);
        return false;
    }

    /** Re-sync player flag when re-entering an already-active session. */
    public static void markActive(@Nonnull UUID playerUuid) {
        ACTIVE.add(playerUuid);
    }

    public static void unmarkActive(@Nonnull UUID playerUuid) {
        ACTIVE.remove(playerUuid);
        UUID townId = PLAYER_TOWN.remove(playerUuid);
        if (townId != null) {
            TOWN_COMMANDER.remove(townId, playerUuid);
        }
    }

    public static boolean isCommanding(@Nonnull UUID playerUuid) {
        return ACTIVE.contains(playerUuid);
    }
}
