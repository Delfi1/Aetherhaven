package com.hexvane.aetherhaven.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Layered raid health bar: red fill, per-mob tick separators, border frame, and center text. */
public final class RaidHealthBarUi {
    private static final String STACK = "#RaidHudPanel #RaidBarStack";
    private static final String SEPARATORS = STACK + " #RaidBarSeparators";
    private static final int FLEX_SCALE = 1000;

    private RaidHealthBarUi() {}

    public static void apply(@Nonnull UICommandBuilder cmd, int kills, int total) {
        int need = Math.max(0, total);
        int killed = Math.max(0, Math.min(kills, need));
        int remaining = need - killed;
        applyFill(cmd, remaining, need);
        if (need > 0) {
            cmd.set(STACK + " #RaidBarText.TextSpans", Message.raw(remaining + "/" + need));
        } else {
            cmd.set(STACK + " #RaidBarText.TextSpans", Message.raw(""));
        }
        applySeparators(cmd, need);
    }

    private static void applyFill(@Nonnull UICommandBuilder cmd, int remaining, int total) {
        String track = STACK + " #RaidBarFillTrack";
        float progress = total > 0 ? (float) remaining / (float) total : 0f;
        int fillWeight = Math.max(0, Math.round(progress * FLEX_SCALE));
        int remainWeight = Math.max(1, FLEX_SCALE - fillWeight);
        if (fillWeight == 0) {
            remainWeight = FLEX_SCALE;
        }
        cmd.set(track + " #RaidBarFill.FlexWeight", fillWeight);
        cmd.set(track + " #RaidBarFillRemainder.FlexWeight", remainWeight);
        cmd.set(track + " #RaidBarFill.Visible", fillWeight > 0);
    }

    private static void applySeparators(@Nonnull UICommandBuilder cmd, int mobCount) {
        cmd.clear(SEPARATORS);
        if (mobCount <= 1) {
            return;
        }
        List<Float> ticks = mobSeparatorFractions(mobCount);
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
    private static List<Float> mobSeparatorFractions(int mobCount) {
        List<Float> out = new ArrayList<>();
        for (int i = 1; i < mobCount; i++) {
            out.add((float) i / (float) mobCount);
        }
        return out;
    }
}
