package com.hexvane.aetherhaven.questboard;

import com.hypixel.hytale.server.core.Message;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Cardinal approach direction for a raid wave, chosen when the board offer is generated. */
public enum RaidApproachDirection {
    NORTH("north", 0, -1),
    SOUTH("south", 0, 1),
    EAST("east", 1, 0),
    WEST("west", -1, 0);

    private static final RaidApproachDirection[] VALUES = values();

    private final String id;
    private final int axisX;
    private final int axisZ;

    RaidApproachDirection(@Nonnull String id, int axisX, int axisZ) {
        this.id = id;
        this.axisX = axisX;
        this.axisZ = axisZ;
    }

    @Nonnull
    public String id() {
        return id;
    }

    public int axisX() {
        return axisX;
    }

    public int axisZ() {
        return axisZ;
    }

    @Nonnull
    public static RaidApproachDirection random(@Nonnull Random rng) {
        return VALUES[rng.nextInt(VALUES.length)];
    }

    @Nullable
    public static RaidApproachDirection fromId(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase();
        for (RaidApproachDirection d : VALUES) {
            if (d.id.equals(trimmed)) {
                return d;
            }
        }
        return null;
    }

    @Nonnull
    public Message displayLabel() {
        return Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.raidDirection." + id);
    }
}
