package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * RTS commanders are hidden from NPC sensors via {@link RtsNpcPlayerDetectionPatch} while the session is active.
 * This helper clears any targets mobs already held when command mode starts.
 */
public final class RtsCommanderNpcVisibility {
    private RtsCommanderNpcVisibility() {}

    public static void apply(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommanderNpcStealthSystem.clearMarkedTargetsForPlayer(playerRef, store);
    }

    public static void restore(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommanderNpcStealthSystem.clearMarkedTargetsForPlayer(playerRef, store);
    }
}
