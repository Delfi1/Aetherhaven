package com.hexvane.aetherhaven.villager.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Inn morning visitor pool row from {@link VillagerDefinitionCatalog#innPoolEntriesSorted()}. */
public record InnPoolEntry(
    @Nonnull String npcRoleId,
    @Nonnull String visitorBindingKind,
    int order,
    @Nonnull InnPoolRequires requires,
    @Nullable int[] spawnLocal
) {
    public InnPoolEntry(@Nonnull String npcRoleId, @Nonnull String visitorBindingKind, int order) {
        this(npcRoleId, visitorBindingKind, order, InnPoolRequires.EMPTY, null);
    }
}
