package com.hexvane.aetherhaven.map;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thread-safe raid mob positions for compass markers. Written on the world thread ({@code RaidQuestMarchSystem}),
 * read on the world-map thread ({@link RaidQuestMarkerProvider}).
 */
public final class RaidQuestCompassCache {
    public record Entry(
        @Nonnull UUID townId,
        @Nonnull UUID mobUuid,
        @Nonnull String instanceId,
        @Nullable String targetLabelLangKey,
        double x,
        double y,
        double z
    ) {}

    private static final ConcurrentHashMap<String, ConcurrentHashMap<UUID, Entry>> BY_WORLD = new ConcurrentHashMap<>();

    private RaidQuestCompassCache() {}

    public static void upsert(@Nonnull String worldName, @Nonnull Entry entry) {
        BY_WORLD.computeIfAbsent(worldName, ignored -> new ConcurrentHashMap<>()).put(entry.mobUuid(), entry);
    }

    public static void removeMob(@Nonnull String worldName, @Nonnull UUID mobUuid) {
        ConcurrentHashMap<UUID, Entry> world = BY_WORLD.get(worldName);
        if (world != null) {
            world.remove(mobUuid);
        }
    }

    public static void removeMobs(@Nonnull String worldName, @Nonnull Iterable<String> mobUuidStrings) {
        for (String uuidStr : mobUuidStrings) {
            if (uuidStr == null || uuidStr.isBlank()) {
                continue;
            }
            try {
                removeMob(worldName, UUID.fromString(uuidStr.trim()));
            } catch (IllegalArgumentException ignored) {
                // invalid uuid
            }
        }
    }

    public static void removeForTown(@Nonnull String worldName, @Nonnull UUID townId) {
        ConcurrentHashMap<UUID, Entry> world = BY_WORLD.get(worldName);
        if (world == null || world.isEmpty()) {
            return;
        }
        world.values().removeIf(entry -> townId.equals(entry.townId()));
    }

    @Nonnull
    public static List<Entry> entriesForTown(@Nonnull String worldName, @Nonnull UUID townId) {
        ConcurrentHashMap<UUID, Entry> world = BY_WORLD.get(worldName);
        if (world == null || world.isEmpty()) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>();
        for (Entry entry : world.values()) {
            if (townId.equals(entry.townId())) {
                out.add(entry);
            }
        }
        return out;
    }
}
