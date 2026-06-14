package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardKillSetJson {
    @SerializedName("weight")
    private int weight;

    @SerializedName("killCount")
    private int killCount;

    @SerializedName("entityTagsAny")
    @Nullable
    private List<String> entityTagsAny;

    public int weight() {
        return Math.max(1, weight);
    }

    public int killCount() {
        return Math.max(1, killCount);
    }

    @Nonnull
    public List<String> entityTagsAnyOrEmpty() {
        return entityTagsAny != null ? entityTagsAny : List.of();
    }
}
