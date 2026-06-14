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
        boolean hadInput
    ) {
    }

    public static void panApplied(
        @Nonnull PlayerRef playerRef,
        double dx,
        double dz,
        double focusX,
        double focusZ,
        @Nonnull String source
    ) {
    }

    public static void panSkipped(@Nonnull PlayerRef playerRef, @Nonnull String reason) {
    }

    public static void hotbarSync(@Nonnull PlayerRef playerRef, @Nonnull String reason, byte serverSlot) {
    }

    public static void hotbarClick(@Nonnull PlayerRef playerRef, byte serverSlot, @Nonnull String action) {
    }

    public static void mousePacketReceived(@Nonnull PlayerRef playerRef, int clientSlot) {
    }

    public static void mouseClick(
        @Nonnull PlayerRef playerRef,
        @Nonnull String state,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable org.joml.Vector2fc screen
    ) {
    }

    public static void primaryInteraction(
        @Nonnull PlayerRef playerRef,
        @Nonnull String itemId,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable Ref<EntityStore> targetEntity
    ) {
    }

    public static void primaryInteractionFailed(@Nonnull PlayerRef playerRef, @Nonnull String reason) {
        LOGGER.atInfo().log("[RTS-PRIMARY] player=%s failed: %s", playerRef.getUsername(), reason);
    }

    public static void guardClickResult(
        @Nonnull PlayerRef playerRef,
        boolean handled,
        @Nullable RtsScreenPickUtil.GroundPick pick
    ) {
    }

    public static void moveOrderPick(
        @Nonnull PlayerRef playerRef,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull RtsScreenPickUtil.GroundPick pick,
        @Nullable org.joml.Vector3i targetBlock,
        @Nullable org.joml.Vector2fc screen,
        @Nonnull String source
    ) {
    }

    public static void movementProfile(@Nonnull PlayerRef playerRef, boolean enabling, boolean canFly) {
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
