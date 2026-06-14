package com.hexvane.aetherhaven.poi.tool;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Registers the POI tool legend overlay via vanilla keyed {@link com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager}. */
public final class PoiToolHudSupport {
    private PoiToolHudSupport() {}

    @Nonnull
    public static PoiToolLegendHud obtainPoiToolHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.POI_TOOL_HUD_KEY);
        if (existing instanceof PoiToolLegendHud h) {
            return h;
        }
        PoiToolLegendHud created = new PoiToolLegendHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static boolean isPoiToolHudActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.POI_TOOL_HUD_KEY) instanceof PoiToolLegendHud;
    }

    public static void removePoiToolHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.POI_TOOL_HUD_KEY);
    }
}
