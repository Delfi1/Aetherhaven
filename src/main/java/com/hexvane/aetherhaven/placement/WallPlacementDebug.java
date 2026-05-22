package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.wall.WallCardinal;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Chat + server log lines for wall wand placement (toggle per player with /ah walldebug). */
public final class WallPlacementDebug {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private WallPlacementDebug() {}

    public static void log(
        @Nullable PlayerRef playerRef, @Nonnull WallPlacementSession session, @Nonnull String event, @Nonnull String detail
    ) {
        if (!session.isDebugLogging()) {
            return;
        }
        String line = "[WallWand] " + event + " | " + detail;
        LOGGER.atInfo().log(line);
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(line));
        }
    }

    public static void logState(@Nullable PlayerRef playerRef, @Nonnull WallPlacementSession session, @Nonnull String trigger) {
        if (!session.isDebugLogging()) {
            return;
        }
        log(playerRef, session, trigger, session.describeState());
    }

    @Nonnull
    public static String formatDirs(@Nullable EnumSet<WallCardinal> dirs) {
        if (dirs == null || dirs.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (WallCardinal d : dirs) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append(d.name().charAt(0));
        }
        return sb.toString();
    }

    @Nonnull
    public static String formatAllowed(@Nonnull EnumSet<WallCardinal> allowed) {
        StringBuilder sb = new StringBuilder();
        for (WallCardinal d : WallCardinal.values()) {
            if (allowed.contains(d)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(d.name().charAt(0));
            }
        }
        return sb.isEmpty() ? "none" : sb.toString();
    }

    /** Toggle debug for the player's active wall session, or global default for new sessions. */
    public static boolean toggleForPlayer(@Nonnull UUID playerUuid, boolean enabled) {
        WallPlacementSession session = WallPlacementSessions.get(playerUuid);
        if (session != null) {
            session.setDebugLogging(enabled);
        }
        WallPlacementSession.setDefaultDebugLogging(enabled);
        return enabled;
    }

    public static boolean isEnabledForPlayer(@Nonnull UUID playerUuid) {
        WallPlacementSession session = WallPlacementSessions.get(playerUuid);
        return session != null ? session.isDebugLogging() : WallPlacementSession.isDefaultDebugLogging();
    }
}
