package com.hexvane.aetherhaven.town;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

/** Whether an owner or member of a town is currently online in a world. */
public final class TownOnlinePresence {
    private TownOnlinePresence() {}

    @Nonnull
    public static Set<UUID> collectOnlinePlayerUuids(@Nonnull World world) {
        Set<UUID> out = new HashSet<>();
        for (PlayerRef pr : world.getPlayerRefs()) {
            if (pr != null) {
                out.add(pr.getUuid());
            }
        }
        return out;
    }

    public static boolean hasAffiliatedPlayerOnline(@Nonnull TownRecord town, @Nonnull Set<UUID> onlinePlayers) {
        for (UUID playerUuid : onlinePlayers) {
            if (town.hasMemberOrOwner(playerUuid)) {
                return true;
            }
        }
        return false;
    }
}
