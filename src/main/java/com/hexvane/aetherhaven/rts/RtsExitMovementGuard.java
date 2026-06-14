package com.hexvane.aetherhaven.rts;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Rejects client absolutePosition snaps to origin for a short window after RTS exit. */
final class RtsExitMovementGuard {
    private static final long GUARD_NANOS = 1_500_000_000L;

    private static final Map<UUID, Guard> ACTIVE = new ConcurrentHashMap<>();

    private RtsExitMovementGuard() {}

    record Guard(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll,
        long expiresAtNanos
    ) {
        boolean expired(long nowNanos) {
            return nowNanos >= expiresAtNanos;
        }
    }

    static void arm(
        @Nonnull UUID playerUuid,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll
    ) {
        ACTIVE.put(
            playerUuid,
            new Guard(x, y, z, yaw, pitch, roll, System.nanoTime() + GUARD_NANOS)
        );
    }

    static void clear(@Nonnull UUID playerUuid) {
        ACTIVE.remove(playerUuid);
    }

    @Nullable
    static Guard peek(@Nonnull UUID playerUuid) {
        Guard guard = ACTIVE.get(playerUuid);
        if (guard == null) {
            return null;
        }
        if (guard.expired(System.nanoTime())) {
            ACTIVE.remove(playerUuid);
            return null;
        }
        return guard;
    }

    static boolean needsCorrection(double x, double y, double z) {
        return !isPlausibleBody(x, y, z);
    }

    private static boolean isPlausibleBody(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }
        if (Math.abs(x) < 0.5 && Math.abs(z) < 0.5 && y < 2.0) {
            return false;
        }
        return y > -64 && y < 320;
    }
}
