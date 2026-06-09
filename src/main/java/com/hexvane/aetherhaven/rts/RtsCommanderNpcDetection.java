package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** True while the player is commanding in RTS and should be ignored by NPC sensors (creative-style stealth). */
public final class RtsCommanderNpcDetection {
    private RtsCommanderNpcDetection() {}

    public static boolean isHiddenFromNpcs(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        if (!playerRef.isValid()) {
            return false;
        }
        RtsCommandPlayerComponent session = accessor.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        return session != null && session.isActive();
    }
}
