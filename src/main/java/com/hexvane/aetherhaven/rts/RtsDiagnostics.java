package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Targeted RTS commander diagnostics for falling, WASD pan, and hotbar slot desync. */
public final class RtsDiagnostics {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PERIODIC_TICKS = 40;

    private static final Map<UUID, Integer> wishTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> panTick = new ConcurrentHashMap<>();

    private RtsDiagnostics() {}

    public static void enter(@Nonnull PlayerRef playerRef) {
        clearPlayer(playerRef.getUuid());
    }

    public static void wishCapture(
        @Nonnull PlayerRef playerRef,
        int queueSize,
        int wishCount,
        int relCount,
        int absCount,
        int statesCount,
        double wishX,
        double wishZ,
        double forwardAxis,
        boolean sprintHeld,
        boolean zoomHeld,
        boolean hadInput
    ) {
        int tick = wishTick.merge(playerRef.getUuid(), 1, Integer::sum);
        if (hadInput || tick % PERIODIC_TICKS == 0) {
            LOGGER.atInfo().log(
                "[RTS-WASD] player=%s queue=%d wish=%d rel=%d abs=%d states=%d wish=(%.2f,%.2f) fwd=%.3f sprint=%s zoom=%s hadInput=%s",
                playerRef.getUsername(),
                queueSize,
                wishCount,
                relCount,
                absCount,
                statesCount,
                wishX,
                wishZ,
                forwardAxis,
                sprintHeld,
                zoomHeld,
                hadInput
            );
        }
    }

    public static void zoomApplied(
        @Nonnull PlayerRef playerRef,
        float before,
        float after,
        double wishZ
    ) {
        LOGGER.atInfo().log(
            "[RTS-ZOOM] player=%s dist %.1f -> %.1f wishZ=%.2f",
            playerRef.getUsername(),
            before,
            after,
            wishZ
        );
    }

    public static void panApplied(
        @Nonnull PlayerRef playerRef,
        double dx,
        double dz,
        double focusX,
        double focusZ,
        @Nonnull String source
    ) {
        if (Math.abs(dx) < 0.0005 && Math.abs(dz) < 0.0005) {
            return;
        }
        LOGGER.atInfo().log(
            "[RTS-PAN] player=%s source=%s delta=(%.3f,%.3f) focus=(%.2f,%.2f)",
            playerRef.getUsername(),
            source,
            dx,
            dz,
            focusX,
            focusZ
        );
    }

    public static void panSkipped(@Nonnull PlayerRef playerRef, @Nonnull String reason) {
        int tick = panTick.merge(playerRef.getUuid(), 1, Integer::sum);
        if (tick % PERIODIC_TICKS == 0) {
            LOGGER.atInfo().log("[RTS-PAN] player=%s skipped: %s", playerRef.getUsername(), reason);
        }
    }

    public static void hotbarSync(@Nonnull PlayerRef playerRef, @Nonnull String reason, byte serverSlot) {
        LOGGER.atInfo().log(
            "[RTS-HOTBAR] player=%s reason=%s serverSlot=%d",
            playerRef.getUsername(),
            reason,
            serverSlot
        );
    }

    public static void hotbarClick(@Nonnull PlayerRef playerRef, byte serverSlot, @Nonnull String action) {
        LOGGER.atInfo().log(
            "[RTS-HOTBAR] player=%s click action=%s serverSlot=%d (compare to failedGetActiveSlot packet= in chat)",
            playerRef.getUsername(),
            action,
            serverSlot
        );
    }

    public static void mousePacketReceived(@Nonnull PlayerRef playerRef, int clientSlot) {
        LOGGER.atInfo().log(
            "[RTS-MOUSE-PKT] player=%s slot=%d",
            playerRef.getUsername(),
            clientSlot
        );
    }

    public static void mouseClick(
        @Nonnull PlayerRef playerRef,
        @Nonnull String state,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable org.joml.Vector2fc screen
    ) {
        LOGGER.atInfo().log(
            "[RTS-CLICK] player=%s state=%s block=%s screen=%s",
            playerRef.getUsername(),
            state,
            targetBlock != null ? targetBlock.x() + "," + targetBlock.y() + "," + targetBlock.z() : "null",
            screen != null ? String.format("(%.3f,%.3f)", screen.x(), screen.y()) : "null"
        );
    }

    public static void primaryInteraction(
        @Nonnull PlayerRef playerRef,
        @Nonnull String itemId,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable Ref<EntityStore> targetEntity
    ) {
        LOGGER.atInfo().log(
            "[RTS-PRIMARY] player=%s item=%s block=%s entity=%s",
            playerRef.getUsername(),
            itemId,
            targetBlock != null ? targetBlock.x() + "," + targetBlock.y() + "," + targetBlock.z() : "null",
            targetEntity != null && targetEntity.isValid() ? "yes" : "no"
        );
    }

    public static void primaryInteractionFailed(@Nonnull PlayerRef playerRef, @Nonnull String reason) {
        LOGGER.atInfo().log("[RTS-PRIMARY] player=%s failed: %s", playerRef.getUsername(), reason);
    }

    public static void guardClickResult(
        @Nonnull PlayerRef playerRef,
        boolean handled,
        @Nullable RtsScreenPickUtil.GroundPick pick
    ) {
        LOGGER.atInfo().log(
            "[RTS-CLICK] player=%s guardHandled=%s pick=%s",
            playerRef.getUsername(),
            handled,
            pick != null ? String.format("(%.1f,%.1f,%.1f)", pick.x(), pick.y(), pick.z()) : "null"
        );
    }

    public static void moveOrderPick(
        @Nonnull PlayerRef playerRef,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull RtsScreenPickUtil.GroundPick pick,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable org.joml.Vector2fc screen,
        @Nonnull String source
    ) {
        LOGGER.atInfo().log(
            "[RTS-MOVE-PICK] player=%s source=%s pick=(%.1f,%.1f,%.1f) block=%s screen=%s ortho=(%.2f,%.2f)",
            playerRef.getUsername(),
            source,
            pick.x(),
            pick.y(),
            pick.z(),
            targetBlock != null ? targetBlock.x() + "," + targetBlock.y() + "," + targetBlock.z() : "null",
            screen != null ? String.format("(%.3f,%.3f)", screen.x(), screen.y()) : "null",
            RtsScreenPickUtil.orthoHalfWidth(session),
            RtsScreenPickUtil.orthoHalfHeight(session)
        );
    }

    public static void movementProfile(@Nonnull PlayerRef playerRef, boolean enabling, boolean canFly) {
        LOGGER.atInfo().log(
            "[RTS-MOVE] player=%s %s canFly=%s",
            playerRef.getUsername(),
            enabling ? "enable-command-profile" : "restore-default-profile",
            canFly
        );
    }

    public static void boxDragPulse(
        @Nonnull PlayerRef playerRef,
        @Nonnull String phase,
        int pulseCount,
        @Nullable org.joml.Vector3i targetBlock,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable org.joml.Vector2fc cameraScreen
    ) {
        // Box-select debug logging disabled by default; use /aetherhaven rtsboxdebug on for overlay.
    }

    public static void boxDragRelease(
        @Nonnull PlayerRef playerRef,
        @Nonnull String reason,
        long idleMs,
        int pulseCount,
        @Nonnull RtsCommandPlayerComponent session
    ) {
    }

    public static void boxSelectionComplete(
        @Nonnull PlayerRef playerRef,
        @Nonnull String method,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable RtsScreenPickUtil.WorldAabb column,
        int selectedCount
    ) {
    }

    public static void boxHudLifecycle(@Nonnull PlayerRef playerRef, @Nonnull String event) {
    }

    public static void boxHudSkipped(@Nonnull String reason) {
    }

    public static void boxHudRefresh(
        @Nonnull PlayerRef playerRef,
        boolean visible,
        int left,
        int top,
        int width,
        int height,
        @Nonnull String reason,
        boolean repeatWhileVisible
    ) {
    }

    public static void cameraPollState(
        @Nonnull PlayerRef playerRef,
        @Nonnull String leftState,
        boolean hasScreen,
        boolean boxActive
    ) {
    }

    private static void clearPlayer(@Nonnull UUID uuid) {
        wishTick.remove(uuid);
        panTick.remove(uuid);
    }
}
