package com.hexvane.aetherhaven.pathtool;

import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** In-memory path style list selection and chest edit session. */
public final class PathToolStyleSessions {
    public static final class Session {
        public final SimpleItemContainer container = PathToolStyleEditorHelper.createContainer();
        public int listSelectedIndex;
        /** Index in config style list when editing; -1 when creating new. */
        public int editIndex = -1;
        public boolean creating;
        public boolean editingActive;
        @Nonnull
        public String styleName = "New path";
    }

    private static final ConcurrentHashMap<UUID, Session> BY_PLAYER = new ConcurrentHashMap<>();

    private PathToolStyleSessions() {}

    @Nonnull
    public static Session getOrCreate(@Nonnull UUID playerId) {
        return BY_PLAYER.computeIfAbsent(playerId, id -> new Session());
    }

    @Nullable
    public static Session get(@Nonnull UUID playerId) {
        return BY_PLAYER.get(playerId);
    }

    public static void clear(@Nonnull UUID playerId) {
        BY_PLAYER.remove(playerId);
    }
}
