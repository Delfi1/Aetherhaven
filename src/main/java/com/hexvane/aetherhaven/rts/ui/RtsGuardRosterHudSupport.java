package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RtsGuardRosterHudSupport {
    private RtsGuardRosterHudSupport() {}

    @Nonnull
    public static RtsGuardRosterHud obtainHud(
        @Nonnull Player player,
        @Nonnull PlayerRef playerRef,
        @Nonnull UUID townId
    ) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.RTS_GUARD_ROSTER_HUD_KEY);
        if (existing instanceof RtsGuardRosterHud h && h.getTownId().equals(townId)) {
            return h;
        }
        removeHud(player, playerRef);
        RtsGuardRosterHud created = new RtsGuardRosterHud(playerRef, townId);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static void show(
        @Nonnull Player player,
        @Nonnull PlayerRef playerRef,
        @Nonnull UUID townId,
        @Nonnull java.util.List<RtsGuardRosterSupport.GuardRow> rows
    ) {
        RtsGuardRosterHud hud = obtainHud(player, playerRef, townId);
        hud.show();
        hud.rebuild(rows);
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, AetherhavenConstants.RTS_GUARD_ROSTER_HUD_KEY);
    }

    @Nullable
    public static RtsGuardRosterHud activeHud(@Nonnull Player player) {
        CustomUIHud existing = player.getHudManager().getCustomHud(AetherhavenConstants.RTS_GUARD_ROSTER_HUD_KEY);
        return existing instanceof RtsGuardRosterHud h ? h : null;
    }
}
