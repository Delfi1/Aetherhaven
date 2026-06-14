package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class RtsBoxSelectHudSupport {
    private RtsBoxSelectHudSupport() {}

    @Nonnull
    public static RtsBoxSelectHud obtainHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.RTS_BOX_SELECT_HUD_KEY);
        if (existing instanceof RtsBoxSelectHud h) {
            return h;
        }
        RtsBoxSelectHud created = new RtsBoxSelectHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    @Nonnull
    public static RtsBoxSelectHud showForDrag(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        RtsBoxSelectHud hud = obtainHud(player, playerRef);
        hud.ensureShown();
        return hud;
    }

    public static void hideSelection(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.RTS_BOX_SELECT_HUD_KEY);
        if (existing instanceof RtsBoxSelectHud h) {
            h.hideSelection();
        }
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.RTS_BOX_SELECT_HUD_KEY);
    }
}
