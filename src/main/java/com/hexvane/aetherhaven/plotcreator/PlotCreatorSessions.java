package com.hexvane.aetherhaven.plotcreator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlotCreatorSessions {
    private static final Map<UUID, PlotCreatorSession> BY_PLAYER = new ConcurrentHashMap<>();

    private PlotCreatorSessions() {}

    @Nullable
    public static PlotCreatorSession get(@Nonnull UUID playerUuid) {
        return BY_PLAYER.get(playerUuid);
    }

    public static void put(@Nonnull PlotCreatorSession session) {
        BY_PLAYER.put(session.getPlayerUuid(), session);
    }

    @Nullable
    public static PlotCreatorSession remove(@Nonnull UUID playerUuid) {
        return BY_PLAYER.remove(playerUuid);
    }

    public static boolean has(@Nonnull UUID playerUuid) {
        return BY_PLAYER.containsKey(playerUuid);
    }
}
