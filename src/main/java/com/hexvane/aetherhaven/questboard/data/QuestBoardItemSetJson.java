package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import com.hexvane.aetherhaven.questboard.QuestBoardItemRequirement;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardItemSetJson {
    @SerializedName("weight")
    private int weight;

    @SerializedName("items")
    @Nullable
    private List<QuestBoardItemRequirement> items;

    public int weight() {
        return Math.max(1, weight);
    }

    @Nonnull
    public List<QuestBoardItemRequirement> itemsOrEmpty() {
        return items != null ? items : List.of();
    }
}
