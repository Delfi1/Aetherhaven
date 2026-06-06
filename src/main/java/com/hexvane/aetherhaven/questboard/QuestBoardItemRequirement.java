package com.hexvane.aetherhaven.questboard;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardItemRequirement {
    @SerializedName("itemId")
    @Nullable
    private String itemId;

    @SerializedName("count")
    private int count;

    public QuestBoardItemRequirement() {}

    public QuestBoardItemRequirement(@Nonnull String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    @Nullable
    public String itemId() {
        return itemId;
    }

    public int count() {
        return Math.max(1, count);
    }

    @Nonnull
    public String itemIdOrEmpty() {
        return itemId != null ? itemId.trim() : "";
    }
}
