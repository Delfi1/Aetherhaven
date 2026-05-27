package com.hexvane.aetherhaven.townsfolk;

import javax.annotation.Nonnull;

/** Assignment roles for townsfolk checked out of the world pool. */
public final class TownsfolkAssignmentKinds {
    /** Stand and wander locally; placeholder until tourist/guard behaviors exist. */
    public static final String IDLE = "idle";
    public static final String TOURIST = "tourist";
    public static final String GUARD = "guard";

    private TownsfolkAssignmentKinds() {}

    /** True when POI autonomy should not run (all phase 1 kinds). */
    public static boolean usesIdleStandAround(@Nonnull String assignmentKind) {
        String k = assignmentKind.trim().toLowerCase();
        return IDLE.equals(k) || TOURIST.equals(k) || GUARD.equals(k);
    }
}
