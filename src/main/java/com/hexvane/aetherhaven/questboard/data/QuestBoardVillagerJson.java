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

    @SerializedName("huntEntries")
    @Nullable
    private List<QuestBoardHuntEntryJson> huntEntries;

    @SerializedName("raidEntries")
    @Nullable
    private List<QuestBoardRaidEntryJson> raidEntries;

    @Nonnull
    public List<QuestBoardFetchEntryJson> fetchEntriesOrEmpty() {
        return fetchEntries != null ? fetchEntries : List.of();
    }

    @Nonnull
    public List<QuestBoardHuntEntryJson> huntEntriesOrEmpty() {
        return huntEntries != null ? huntEntries : List.of();
    }

    @Nonnull
    public List<QuestBoardRaidEntryJson> raidEntriesOrEmpty() {
        return raidEntries != null ? raidEntries : List.of();
    }
}
