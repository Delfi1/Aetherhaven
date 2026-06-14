package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.questboard.QuestBoardCatalog;
import com.hexvane.aetherhaven.questboard.QuestBoardRankXpDisplay;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Layered rank XP bar: green fill, tick separators, border frame, and center text. */
public final class QuestBoardRankBarUi {
    private static final String STACK = "#RankXpTrack #RankXpBarStack";
    private static final String SEPARATORS = STACK + " #RankXpSeparators";
    private static final int FLEX_SCALE = 1000;

    private QuestBoardRankBarUi() {}

    public static void apply(@Nonnull UICommandBuilder cmd, int xp, @Nonnull QuestBoardCatalog catalog) {
        QuestBoardRankXpDisplay display = QuestBoardRankXpDisplay.forTownXp(xp, catalog);
        cmd.set(
            "#RankXpTitle.TextSpans",
            Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rankXpTitle")
        );
        applyFill(cmd, display);
        if (display.atMaxRank()) {
            cmd.set(
                STACK + " #RankXpText.TextSpans",
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rankXpMax")
            );
        } else {
            cmd.set(
                STACK + " #RankXpText.TextSpans",
                Message.raw(display.currentInTier() + "/" + display.neededInTier())
            );
        }
        applySeparators(cmd, display);
    }

    private static void applyFill(@Nonnull UICommandBuilder cmd, @Nonnull QuestBoardRankXpDisplay display) {
        String track = STACK + " #RankXpFillTrack";
        int fillWeight = Math.max(0, Math.round(display.progress() * FLEX_SCALE));
        int remainWeight = Math.max(1, FLEX_SCALE - fillWeight);
        if (fillWeight == 0) {
            remainWeight = FLEX_SCALE;
        }
        cmd.set(track + " #RankXpFill.FlexWeight", fillWeight);
        cmd.set(track + " #RankXpFillRemainder.FlexWeight", remainWeight);
        cmd.set(track + " #RankXpFill.Visible", fillWeight > 0);
    }

    private static void applySeparators(@Nonnull UICommandBuilder cmd, @Nonnull QuestBoardRankXpDisplay display) {
        cmd.clear(SEPARATORS);
        if (display.atMaxRank() || display.neededInTier() <= 1) {
            return;
        }
        List<Float> ticks = separatorFractions(display.neededInTier());
        if (ticks.isEmpty()) {
            return;
        }
        List<Integer> weights = flexWeightsForTicks(ticks);
        int child = 0;
        for (int i = 0; i < ticks.size(); i++) {
            appendFlexSpacer(cmd, child++, weights.get(i));
            cmd.append(SEPARATORS, "Aetherhaven/QuestBoardXpBarSeparator.ui");
            child++;
        }
        appendFlexSpacer(cmd, child, weights.get(weights.size() - 1));
    }

    private static void appendFlexSpacer(@Nonnull UICommandBuilder cmd, int index, int weight) {
        cmd.append(SEPARATORS, "Aetherhaven/QuestBoardXpBarFlex.ui");
        cmd.set(SEPARATORS + "[" + index + "].FlexWeight", Math.max(1, weight));
    }

    @Nonnull
    private static List<Integer> flexWeightsForTicks(@Nonnull List<Float> ticks) {
        List<Integer> weights = new ArrayList<>();
        float prev = 0f;
        for (float tick : ticks) {
            weights.add(Math.max(1, Math.round((tick - prev) * FLEX_SCALE)));
            prev = tick;
        }
        weights.add(Math.max(1, Math.round((1f - prev) * FLEX_SCALE)));
        return weights;
    }

    @Nonnull
    private static List<Float> separatorFractions(int spanXp) {
        int step = niceXpStep(spanXp);
        List<Float> out = new ArrayList<>();
        for (int xp = step; xp < spanXp; xp += step) {
            out.add((float) xp / (float) spanXp);
            if (out.size() >= 6) {
                break;
            }
        }
        return out;
    }

    private static int niceXpStep(int spanXp) {
        if (spanXp <= 12) {
            return 2;
        }
        if (spanXp <= 35) {
            return 5;
        }
        if (spanXp <= 90) {
            return 10;
        }
        if (spanXp <= 200) {
            return 20;
        }
        return 50;
    }
}
