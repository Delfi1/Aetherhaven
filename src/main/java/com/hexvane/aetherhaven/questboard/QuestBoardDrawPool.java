package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.town.TownRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shuffle bag for quest board offers: pick without replacement until empty, then refill. */
public final class QuestBoardDrawPool {
    public static final String TYPE_FETCH = "fetch";
    public static final String TYPE_HUNT = "hunt";
    public static final String TYPE_RAID = "raid";

    private QuestBoardDrawPool() {}

    @Nonnull
    public static String entryKey(@Nonnull String roleId, @Nonnull String questType, @Nonnull String entryId) {
        return roleId.trim() + ":" + questType.trim() + ":" + entryId.trim();
    }

    @Nullable
    public static ParsedEntryKey parseEntryKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        int first = trimmed.indexOf(':');
        if (first <= 0 || first >= trimmed.length() - 1) {
            return null;
        }
        int second = trimmed.indexOf(':', first + 1);
        if (second > 0 && second < trimmed.length() - 1) {
            return new ParsedEntryKey(
                trimmed.substring(0, first).trim(),
                trimmed.substring(first + 1, second).trim(),
                trimmed.substring(second + 1).trim()
            );
        }
        return new ParsedEntryKey(trimmed.substring(0, first).trim(), TYPE_FETCH, trimmed.substring(first + 1).trim());
    }

    @Nonnull
    public static Set<String> occupiedBoardEntryKeys(@Nonnull TownRecord town) {
        Set<String> out = new HashSet<>();
        for (QuestBoardSlotRecord slot : town.getQuestBoardSlots()) {
            if (!slot.occupiesBoardSlot()) {
                continue;
            }
            String role = slot.getGiverRoleId();
            String entry = slot.getConfigEntryId();
            String type = slot.getQuestType();
            if (role != null && !role.isBlank() && entry != null && !entry.isBlank()) {
                String questType = type != null && !type.isBlank() ? type.trim() : TYPE_FETCH;
                out.add(entryKey(role, questType, entry));
            }
        }
        return out;
    }

    /**
     * Picks a quest entry key from the town draw pool. Removes the chosen key from the pool. Refills and shuffles when
     * no eligible keys remain in the bag.
     */
    @Nullable
    public static String pickEntryKey(
        @Nonnull TownRecord town,
        @Nonnull List<String> eligibleKeys,
        @Nonnull Set<String> excludeKeys,
        @Nonnull Random rng
    ) {
        if (eligibleKeys.isEmpty()) {
            return null;
        }
        List<String> eligible = eligibleKeys.stream().filter(k -> !excludeKeys.contains(k)).distinct().toList();
        if (eligible.isEmpty()) {
            return null;
        }

        List<String> pool = town.getQuestBoardDrawPool();
        List<String> available = filterPool(pool, eligible, excludeKeys);
        if (available.isEmpty()) {
            refillPool(town, eligible, rng);
            pool = town.getQuestBoardDrawPool();
            available = filterPool(pool, eligible, excludeKeys);
        }
        if (available.isEmpty()) {
            return null;
        }

        String pick = available.get(rng.nextInt(available.size()));
        pool.remove(pick);
        return pick;
    }

    @Nonnull
    private static List<String> filterPool(@Nonnull List<String> pool, @Nonnull List<String> eligible, @Nonnull Set<String> excludeKeys) {
        Set<String> eligibleSet = new HashSet<>(eligible);
        List<String> out = new ArrayList<>();
        for (String key : pool) {
            String normalized = normalizePoolKey(key, eligibleSet);
            if (normalized != null && eligibleSet.contains(normalized) && !excludeKeys.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out;
    }

    @Nullable
    private static String normalizePoolKey(@Nonnull String key, @Nonnull Set<String> eligibleSet) {
        ParsedEntryKey parsed = parseEntryKey(key);
        if (parsed == null) {
            return null;
        }
        String normalized = entryKey(parsed.roleId(), parsed.questType(), parsed.entryId());
        if (eligibleSet.contains(normalized)) {
            return normalized;
        }
        if (eligibleSet.contains(key)) {
            return key;
        }
        return normalized;
    }

    private static void refillPool(@Nonnull TownRecord town, @Nonnull List<String> eligibleKeys, @Nonnull Random rng) {
        List<String> next = new ArrayList<>(new HashSet<>(eligibleKeys));
        Collections.shuffle(next, rng);
        town.setQuestBoardDrawPool(next);
    }

    public record ParsedEntryKey(@Nonnull String roleId, @Nonnull String questType, @Nonnull String entryId) {}
}
