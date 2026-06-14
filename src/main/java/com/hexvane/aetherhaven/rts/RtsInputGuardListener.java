package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Blocks vanilla combat/block interactions while commanding troops. */
public final class RtsInputGuardListener {
    private RtsInputGuardListener() {}

    public static void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerMouseButtonEvent.class, RtsInputGuardListener::onMouseButton);
    }

    private static void onMouseButton(@Nonnull PlayerMouseButtonEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session != null && session.isActive()) {
            event.setCancelled(true);
        }
    }
}
