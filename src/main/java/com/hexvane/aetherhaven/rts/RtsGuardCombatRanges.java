package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;

public final class RtsGuardCombatRanges {
    private RtsGuardCombatRanges() {}

    public static double attackEngageRange(@Nonnull NPCEntity npc) {
        String role = npc.getRoleName();
        if (role != null) {
            if (AetherhavenConstants.NPC_GUARD_ARCHER.equals(role)
                || AetherhavenConstants.NPC_GUARD_MAGE.equals(role)) {
                return AetherhavenConstants.RTS_RANGED_ENGAGE_RANGE;
            }
        }
        return AetherhavenConstants.RTS_MELEE_ENGAGE_RANGE;
    }
}
