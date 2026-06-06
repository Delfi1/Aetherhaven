package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardVillagerJson {
    @SerializedName("fetchEntries")
    @Nullable
    private List<QuestBoardFetchEntryJson> fetchEntries;

    @Nonnull
    public List<QuestBoardFetchEntryJson> fetchEntriesOrEmpty() {
        return fetchEntries != null ? fetchEntries : List.of();
    }
}
