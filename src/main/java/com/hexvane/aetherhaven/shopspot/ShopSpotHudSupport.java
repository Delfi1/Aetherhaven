package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class ShopSpotHudSupport {
    private ShopSpotHudSupport() {}

    @Nonnull
    public static ShopSpotStatusHud obtainHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.SHOP_SPOT_HUD_KEY);
        if (existing instanceof ShopSpotStatusHud h) {
            return h;
        }
        ShopSpotStatusHud created = new ShopSpotStatusHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.SHOP_SPOT_HUD_KEY);
    }

    public static boolean isActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.SHOP_SPOT_HUD_KEY) instanceof ShopSpotStatusHud;
    }
}
