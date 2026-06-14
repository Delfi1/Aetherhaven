package com.hexvane.aetherhaven.jewelry;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Applies all loadout side effects when jewelry is equipped or unequipped. */
public final class JewelryLoadoutEffects {
    private JewelryLoadoutEffects() {}

    public static void apply(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull ComponentAccessor<EntityStore> componentCommands,
        @Nonnull PlayerJewelryLoadout loadout
    ) {
        JewelryStatSync.apply(playerRef, store, loadout);
        JewelryLightEffect.apply(playerRef, componentCommands, loadout);
    }
}
