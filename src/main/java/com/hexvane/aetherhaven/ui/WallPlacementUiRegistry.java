package com.hexvane.aetherhaven.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tracks the latest {@link WallPlacementPage} per player so UI events reach the active page instance. */
public final class WallPlacementUiRegistry {
    private static final Map<UUID, WallPlacementPage> PAGES = new ConcurrentHashMap<>();

    private WallPlacementUiRegistry() {}

    public static void register(@Nonnull UUID playerUuid, @Nonnull WallPlacementPage page) {
        PAGES.put(playerUuid, page);
    }

    public static void unregister(@Nonnull UUID playerUuid, @Nonnull WallPlacementPage page) {
        PAGES.remove(playerUuid, page);
    }

    @Nullable
    public static WallPlacementPage get(@Nonnull UUID playerUuid) {
        return PAGES.get(playerUuid);
    }
}
