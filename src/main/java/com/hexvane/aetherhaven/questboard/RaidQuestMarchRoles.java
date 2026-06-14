package com.hexvane.aetherhaven.questboard;

import javax.annotation.Nonnull;

/** Maps quest-board raid roster roles to {@code Aetherhaven_Raid_<RoleId>} spawn variants. */
public final class RaidQuestMarchRoles {
    private static final String RAID_PREFIX = "Aetherhaven_Raid_";

    private RaidQuestMarchRoles() {}

    @Nonnull
    public static String spawnRoleFor(@Nonnull String rosterRoleId) {
        return RAID_PREFIX + rosterRoleId;
    }

    public static boolean isRaidSpawnRole(@Nonnull String roleId) {
        return roleId.startsWith(RAID_PREFIX);
    }
}
