package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class PlotCreatorHudSupport {
    private PlotCreatorHudSupport() {}

    @Nonnull
    public static PlotCreatorStatusHud obtainHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.PLOT_CREATOR_HUD_KEY);
        if (existing instanceof PlotCreatorStatusHud h) {
            return h;
        }
        PlotCreatorStatusHud created = new PlotCreatorStatusHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static boolean isActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.PLOT_CREATOR_HUD_KEY) instanceof PlotCreatorStatusHud;
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.PLOT_CREATOR_HUD_KEY);
    }
}
