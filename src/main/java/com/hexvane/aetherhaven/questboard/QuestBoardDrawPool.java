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
    private QuestBoardDrawPool() {}

    @Nonnull
    public static String entryKey(@Nonnull String roleId, @Nonnull String entryId) {
        return roleId.trim() + ":" + entryId.trim();
    }

    @Nullable
    public static ParsedEntryKey parseEntryKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        int sep = key.indexOf(':');
        if (sep <= 0 || sep >= key.length() - 1) {
            return null;
        }
        return new ParsedEntryKey(key.substring(0, sep).trim(), key.substring(sep + 1).trim());
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
            if (role != null && !role.isBlank() && entry != null && !entry.isBlank()) {
                out.add(entryKey(role, entry));
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
            if (eligibleSet.contains(key) && !excludeKeys.contains(key)) {
                out.add(key);
            }
        }
        return out;
    }

    private static void refillPool(@Nonnull TownRecord town, @Nonnull List<String> eligibleKeys, @Nonnull Random rng) {
        List<String> next = new ArrayList<>(new HashSet<>(eligibleKeys));
        Collections.shuffle(next, rng);
        town.setQuestBoardDrawPool(next);
    }

    public record ParsedEntryKey(@Nonnull String roleId, @Nonnull String entryId) {}
}
