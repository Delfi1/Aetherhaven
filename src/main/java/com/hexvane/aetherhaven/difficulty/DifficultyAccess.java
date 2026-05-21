package com.hexvane.aetherhaven.difficulty;

import com.hexvane.aetherhaven.ui.JournalSettingsAccess;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class DifficultyAccess {
    private DifficultyAccess() {}

    public static boolean canChangeDifficulty(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull WorldDifficultyState worldState
    ) {
        if (!worldState.isDifficultyChosen()) {
            return true;
        }
        return JournalSettingsAccess.canOpen(store, ref);
    }
}
