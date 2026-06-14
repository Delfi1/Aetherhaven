package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.villager.AetherhavenRoleLabels;
import javax.annotation.Nonnull;

/** Shared guard role type labels for patrol and house assignment UI. */
public final class GuardRoleLabels {
    private GuardRoleLabels() {}

    @Nonnull
    public static String guardTypeLangKey(@Nonnull String roleId) {
        return AetherhavenRoleLabels.guardTypeTranslationKey(roleId);
    }
}
