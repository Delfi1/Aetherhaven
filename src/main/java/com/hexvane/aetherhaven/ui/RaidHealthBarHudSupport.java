package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class RaidHealthBarHudSupport {
    private RaidHealthBarHudSupport() {}

    @Nonnull
    public static RaidHealthBarHud obtainHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.RAID_HEALTH_BAR_HUD_KEY);
        if (existing instanceof RaidHealthBarHud h) {
            return h;
        }
        RaidHealthBarHud created = new RaidHealthBarHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static boolean isActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.RAID_HEALTH_BAR_HUD_KEY) instanceof RaidHealthBarHud;
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.RAID_HEALTH_BAR_HUD_KEY);
    }
}
