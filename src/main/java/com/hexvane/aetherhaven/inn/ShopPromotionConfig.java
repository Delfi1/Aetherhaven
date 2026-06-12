package com.hexvane.aetherhaven.inn;

import javax.annotation.Nonnull;

/** Parameters for promoting an inn visitor to a shop resident when their workplace plot completes. */
public record ShopPromotionConfig(
    @Nonnull String shopQuestId,
    @Nonnull String npcRoleId,
    @Nonnull String residentKind,
    @Nonnull String logLabel
) {}
