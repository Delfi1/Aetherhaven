package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Keeps gameplay HUD visible while RTS command mode is active (no full-screen custom page). */
public final class RtsHudVisibility {
    private static final HudComponent[] GAMEPLAY_HUD = {
        HudComponent.Hotbar,
        HudComponent.Reticle,
        HudComponent.InputBindings,
        HudComponent.Chat,
        HudComponent.Health,
        HudComponent.Stamina,
        HudComponent.Notifications
    };

    private RtsHudVisibility() {}

    public static void showGameplayHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().showHudComponents(playerRef, GAMEPLAY_HUD);
    }
}
