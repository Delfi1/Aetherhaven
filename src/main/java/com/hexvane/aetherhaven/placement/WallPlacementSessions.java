package com.hexvane.aetherhaven.placement;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WallPlacementSessions {
    private static final Map<UUID, WallPlacementSession> SESSIONS = new ConcurrentHashMap<>();

    private WallPlacementSessions() {}

    @Nullable
    public static WallPlacementSession get(@Nonnull UUID playerUuid) {
        return SESSIONS.get(playerUuid);
    }

    public static void put(@Nonnull UUID playerUuid, @Nonnull WallPlacementSession session) {
        SESSIONS.put(playerUuid, session);
    }

    @Nullable
    public static WallPlacementSession removeAndGet(@Nonnull UUID playerUuid) {
        return SESSIONS.remove(playerUuid);
    }

    public static void remove(@Nonnull UUID playerUuid) {
        SESSIONS.remove(playerUuid);
    }
}
