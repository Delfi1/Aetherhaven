package com.hexvane.aetherhaven.rts.ui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;

/** Mini green health bar fill for RTS guard portrait cells. */
public final class RtsGuardPortraitBarUi {
    private static final int FLEX_SCALE = 1000;

    private RtsGuardPortraitBarUi() {}

    public static void apply(@Nonnull UICommandBuilder cmd, @Nonnull String cellSelector, float healthFraction) {
        String track = cellSelector + " #PortraitButton #HealthBarStack #HealthFillTrack";
        int fillWeight = Math.max(0, Math.round(Math.max(0f, Math.min(1f, healthFraction)) * FLEX_SCALE));
        int remainWeight = Math.max(1, FLEX_SCALE - fillWeight);
        if (fillWeight == 0) {
            remainWeight = FLEX_SCALE;
        }
        cmd.set(track + " #HealthFill.FlexWeight", fillWeight);
        cmd.set(track + " #HealthFillRemainder.FlexWeight", remainWeight);
        cmd.set(track + " #HealthFill.Visible", fillWeight > 0);
    }
}
