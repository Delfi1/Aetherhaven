package com.hexvane.aetherhaven.bard.data;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;

public final class BardSongDefinition {
    @SerializedName("id")
    private String id = "";

    @SerializedName("displayLangKey")
    private String displayLangKey = "";

    @SerializedName("musicTrack")
    private String musicTrack = "";

    @SerializedName("ambienceFxId")
    private String ambienceFxId = "";

    /** Legacy catalog field; same id as {@link #ambienceFxId}. */
    @SerializedName("soundEventId")
    private String legacySoundEventId = "";

    @SerializedName("durationSeconds")
    private int durationSeconds = 120;

    @Nonnull
    public String getId() {
        return id != null ? id.trim() : "";
    }

    @Nonnull
    public String getDisplayLangKey() {
        return displayLangKey != null ? displayLangKey.trim() : "";
    }

    @Nonnull
    public String getMusicTrack() {
        return musicTrack != null ? musicTrack.trim() : "";
    }

    @Nonnull
    public String getAmbienceFxId() {
        if (ambienceFxId != null && !ambienceFxId.isBlank()) {
            return ambienceFxId.trim();
        }
        return legacySoundEventId != null ? legacySoundEventId.trim() : "";
    }

    @Nonnull
    public String getSoundEventId() {
        if (legacySoundEventId != null && !legacySoundEventId.isBlank()) {
            return legacySoundEventId.trim();
        }
        return getAmbienceFxId();
    }

    public int getDurationSeconds() {
        return Math.max(30, durationSeconds);
    }
}
