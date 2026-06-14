package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.ui.PlayerTownJournalState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Effective RTS orthographic pick tuning for one player (journal overrides or mod defaults). */
public record RtsPickTuning(float verticalFovDeg, float aspectRatio, float cameraEyeOffsetY) {
    @Nonnull
    public static RtsPickTuning defaults() {
        return new RtsPickTuning(
            AetherhavenConstants.RTS_COMMAND_PICK_VERTICAL_FOV_DEG,
            AetherhavenConstants.RTS_COMMAND_PICK_ASPECT_RATIO,
            AetherhavenConstants.RTS_COMMAND_PICK_CAMERA_EYE_OFFSET_Y
        );
    }

    @Nonnull
    public static RtsPickTuning fromJournal(@Nullable PlayerTownJournalState state) {
        if (state == null) {
            return defaults();
        }
        return new RtsPickTuning(
            state.effectiveRtsPickVerticalFovDeg(),
            state.effectiveRtsPickAspectRatio(),
            AetherhavenConstants.RTS_COMMAND_PICK_CAMERA_EYE_OFFSET_Y
        );
    }
}
