package com.hexvane.aetherhaven.rts;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** In-memory exit transform snapshot; survives component copies and bad codec round-trips. */
final class RtsExitPositionCache {
    record ExitSnapshot(double x, double y, double z, float yaw, float pitch, float roll) {}

    private static final Map<UUID, ExitSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private RtsExitPositionCache() {}

    static void save(
        @Nonnull UUID playerUuid,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll
    ) {
        SNAPSHOTS.put(playerUuid, new ExitSnapshot(x, y, z, yaw, pitch, roll));
    }

    @Nullable
    static ExitSnapshot peek(@Nonnull UUID playerUuid) {
        return SNAPSHOTS.get(playerUuid);
    }

    static void clear(@Nonnull UUID playerUuid) {
        SNAPSHOTS.remove(playerUuid);
    }
}
