package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.questboard.data.QuestBoardRankTierJson;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Town rank tier and quest rank tier helpers (E through SSS). */
public final class TownQuestBoardRank {
    private static final List<String> ORDER = List.of("E", "D", "C", "B", "A", "S", "SS", "SSS");

    private TownQuestBoardRank() {}

    @Nonnull
    public static List<String> rankOrder() {
        return ORDER;
    }

    public static int rankIndex(@Nullable String rankId) {
        if (rankId == null || rankId.isBlank()) {
            return 0;
        }
        String id = rankId.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < ORDER.size(); i++) {
            if (ORDER.get(i).equals(id)) {
                return i;
            }
        }
        return 0;
    }

    public static boolean townRankWithinWindow(int townRankIndex, @Nullable String minRank, @Nullable String maxRank) {
        int min = minRank != null && !minRank.isBlank() ? rankIndex(minRank) : 0;
        int max = maxRank != null && !maxRank.isBlank() ? rankIndex(maxRank) : ORDER.size() - 1;
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        return townRankIndex >= min && townRankIndex <= max;
    }

    @Nonnull
    public static String tierIdForXp(int xp, @Nonnull QuestBoardCatalog catalog) {
        String current = "E";
        for (QuestBoardRankTierJson tier : catalog.ranks()) {
            if (xp >= tier.xpRequired()) {
                current = tier.idOrEmpty();
            }
        }
        return current.isBlank() ? "E" : current;
    }

    @Nullable
    public static QuestBoardRankTierJson nextTier(@Nonnull String currentRankId, @Nonnull QuestBoardCatalog catalog) {
        int idx = rankIndex(currentRankId);
        List<QuestBoardRankTierJson> tiers = catalog.ranks();
        for (QuestBoardRankTierJson tier : tiers) {
            if (rankIndex(tier.idOrEmpty()) == idx + 1) {
                return tier;
            }
        }
        return null;
    }

    /** Progress toward next town tier, 0.0 to 1.0. Returns 1.0 at max rank. */
    public static float progressToNextTier(int xp, @Nonnull QuestBoardCatalog catalog) {
        String currentId = tierIdForXp(xp, catalog);
        QuestBoardRankTierJson current = catalog.rankTier(currentId);
        QuestBoardRankTierJson next = nextTier(currentId, catalog);
        if (current == null) {
            return 0f;
        }
        if (next == null) {
            return 1f;
        }
        int base = current.xpRequired();
        int target = next.xpRequired();
        if (target <= base) {
            return 1f;
        }
        return Math.min(1f, Math.max(0f, (float) (xp - base) / (float) (target - base)));
    }

    @Nonnull
    public static String iconPathForRank(@Nonnull String rankId, @Nonnull QuestBoardCatalog catalog) {
        String icon = catalog.iconForRank(rankId);
        if (icon != null && !icon.isBlank()) {
            return icon.trim();
        }
        return "UI/Custom/" + rankId.trim().toLowerCase(Locale.ROOT) + ".png";
    }
}
