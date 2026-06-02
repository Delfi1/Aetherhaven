package com.hexvane.aetherhaven.growthserum;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Maps baby livestock NPC role names to their adult counterparts. */
public final class GrowthSerumService {
    private static final String[] BABY_SUFFIXES = {"_Calf", "_Chick", "_Lamb", "_Piglet", "_Foal", "_Kid"};

    private GrowthSerumService() {}

    /**
     * @return adult role name, or null if the role is not a recognized baby livestock type
     */
    @Nullable
    public static String resolveAdultRole(@Nullable String babyRole) {
        if (babyRole == null || babyRole.isBlank()) {
            return null;
        }
        String role = babyRole.trim();
        for (String suffix : BABY_SUFFIXES) {
            if (role.endsWith(suffix)) {
                return role.substring(0, role.length() - suffix.length());
            }
        }
        return null;
    }

    public static boolean isBabyLivestockRole(@Nonnull String roleName) {
        return resolveAdultRole(roleName) != null;
    }
}
