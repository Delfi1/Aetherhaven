package com.hexvane.aetherhaven.pathtool;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Registers the path tool status overlay via vanilla keyed {@link com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager}. */
public final class PathToolHudSupport {

    private PathToolHudSupport() {}

    /**
     * Returns an existing path-tool HUD widget or creates and registers one under {@link
     * AetherhavenConstants#PATH_TOOL_HUD_KEY}.
     */
    @Nonnull
    public static PathToolStatusHud obtainPathToolHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.PATH_TOOL_HUD_KEY);
        if (existing instanceof PathToolStatusHud h) {
            return h;
        }
        PathToolStatusHud created = new PathToolStatusHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    /**
     * True when this mod's path-tool status layer is currently registered.
     */
    public static boolean isPathToolHudActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.PATH_TOOL_HUD_KEY) instanceof PathToolStatusHud;
    }

    /** Removes the path-tool overlay without affecting other keyed custom HUDs. */
    public static void removePathToolHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.PATH_TOOL_HUD_KEY);
    }
}
