package com.hexvane.aetherhaven.rts;

public enum RtsOrderMode {
    ATTACK_MOVE,
    MOVE;

    public RtsOrderMode next() {
        return this == ATTACK_MOVE ? MOVE : ATTACK_MOVE;
    }
}
