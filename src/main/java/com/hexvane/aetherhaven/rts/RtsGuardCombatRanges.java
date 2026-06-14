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

    /** Wider than {@link #attackEngageRange} so guards do not flip combat/travel every tick at the edge. */
    public static double disengageRange(@Nonnull NPCEntity npc) {
        double attack = attackEngageRange(npc);
        return Math.max(attack * 1.35, attack + 1.5);
    }
}
