package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.questboard.data.QuestBoardRankTierJson;
import javax.annotation.Nonnull;

/** Current tier XP progress toward the next town rank. */
public record QuestBoardRankXpDisplay(int currentInTier, int neededInTier, float progress, boolean atMaxRank) {
    @Nonnull
    public static QuestBoardRankXpDisplay forTownXp(int xp, @Nonnull QuestBoardCatalog catalog) {
        String currentId = TownQuestBoardRank.tierIdForXp(xp, catalog);
        QuestBoardRankTierJson current = catalog.rankTier(currentId);
        QuestBoardRankTierJson next = TownQuestBoardRank.nextTier(currentId, catalog);
        if (current == null) {
            return new QuestBoardRankXpDisplay(0, 1, 0f, false);
        }
        int base = current.xpRequired();
        if (next == null) {
            return new QuestBoardRankXpDisplay(Math.max(0, xp - base), 0, 1f, true);
        }
        int target = next.xpRequired();
        int span = Math.max(1, target - base);
        int inTier = Math.max(0, xp - base);
        float progress = Math.min(1f, Math.max(0f, (float) inTier / (float) span));
        return new QuestBoardRankXpDisplay(inTier, span, progress, false);
    }
}
