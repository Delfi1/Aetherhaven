package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

public final class QuestBoardRaidMobPoolEntryJson {
    @SerializedName("roleId")
    @Nullable
    private String roleId;

    @SerializedName("weight")
    private int weight;

    @Nullable
    public String roleId() {
        return roleId;
    }

    public int weight() {
        return Math.max(1, weight);
    }
}
