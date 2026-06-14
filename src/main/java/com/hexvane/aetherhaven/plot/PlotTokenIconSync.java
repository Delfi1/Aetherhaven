package com.hexvane.aetherhaven.plot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Hooks plot-creator icon registration and token grants into {@link PlotTokenIconPacketAdapter}. */
public final class PlotTokenIconSync {
    private static final int POST_GIVE_REFRESH_DELAY_MS = 250;

    private PlotTokenIconSync() {}

    public static void afterIconRegistered(@Nonnull AetherhavenPlugin plugin, @Nonnull String constructionId) {
        PlotTokenIconPacketAdapter adapter = plugin.getPlotTokenIconPacketAdapter();
        if (adapter != null) {
            adapter.onConstructionIconRegistered(constructionId.trim());
        }
    }

    /** Delayed refresh so {@link PlotTokenIconPacketAdapter} sees the new stack in {@code lastRawInventory}. */
    public static void afterTokenGranted(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> {
                AetherhavenPlugin plugin = AetherhavenPlugin.get();
                if (plugin == null) {
                    return;
                }
                PlotTokenIconPacketAdapter adapter = plugin.getPlotTokenIconPacketAdapter();
                if (adapter != null) {
                    adapter.refreshPlayer(uuid);
                }
            },
            POST_GIVE_REFRESH_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }
}
