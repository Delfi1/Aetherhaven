package com.hexvane.aetherhaven.ui;

import java.util.UUID;
import javax.annotation.Nonnull;

/** One town resident row for needs, gift, and journal UIs. */
public record TownVillagerRow(
    @Nonnull String label,
    @Nonnull UUID entityUuid,
    @Nonnull String roleId,
    @Nonnull String bindingKind,
    int kindOrder,
    @Nonnull String portraitPath,
    boolean usesNeeds
) {}
