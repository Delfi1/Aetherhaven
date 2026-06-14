package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardRankTierJson {
    @SerializedName("id")
    @Nullable
    private String id;

    @SerializedName("xpRequired")
    private int xpRequired;

    @SerializedName("xpReward")
    private int xpReward;

    @SerializedName("icon")
    @Nullable
    private String icon;

    @Nullable
    public String id() {
        return id;
    }

    public int xpRequired() {
        return Math.max(0, xpRequired);
    }

    public int xpReward() {
        return Math.max(0, xpReward);
    }

    @Nullable
    public String icon() {
        return icon;
    }

    @Nonnull
    public String idOrEmpty() {
        return id != null ? id.trim() : "";
    }
}
