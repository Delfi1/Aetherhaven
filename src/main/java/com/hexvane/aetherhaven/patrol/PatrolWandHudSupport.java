package com.hexvane.aetherhaven.patrol;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class PatrolWandHudSupport {
    private PatrolWandHudSupport() {}

    @Nonnull
    public static PatrolWandStatusHud obtainPatrolWandHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.PATROL_WAND_HUD_KEY);
        if (existing instanceof PatrolWandStatusHud h) {
            return h;
        }
        PatrolWandStatusHud created = new PatrolWandStatusHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static boolean isPatrolWandHudActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(AetherhavenConstants.PATROL_WAND_HUD_KEY) instanceof PatrolWandStatusHud;
    }

    public static void removePatrolWandHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.PATROL_WAND_HUD_KEY);
    }
}
