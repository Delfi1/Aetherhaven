package com.hexvane.aetherhaven.rts;

public enum RtsCombatStance {
    DEFENSIVE,
    AGGRESSIVE,
    STAND_GROUND,
    HOLD_FIRE;

    public RtsCombatStance next() {
        return switch (this) {
            case DEFENSIVE -> AGGRESSIVE;
            case AGGRESSIVE -> STAND_GROUND;
            case STAND_GROUND -> HOLD_FIRE;
            case HOLD_FIRE -> DEFENSIVE;
        };
    }
}
