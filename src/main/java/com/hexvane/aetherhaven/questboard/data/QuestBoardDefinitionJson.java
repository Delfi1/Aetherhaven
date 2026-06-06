package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardDefinitionJson {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    @SerializedName("schemaVersion")
    private int schemaVersion;

    @SerializedName("slotCount")
    private int slotCount;

    @SerializedName("ranks")
    @Nullable
    private List<QuestBoardRankTierJson> ranks;

    @SerializedName("villagers")
    @Nullable
    private Map<String, QuestBoardVillagerJson> villagers;

    public int schemaVersion() {
        return schemaVersion;
    }

    public int slotCount() {
        return slotCount <= 0 ? 3 : slotCount;
    }

    @Nonnull
    public List<QuestBoardRankTierJson> ranksOrEmpty() {
        return ranks != null ? ranks : List.of();
    }

    @Nonnull
    public Map<String, QuestBoardVillagerJson> villagersOrEmpty() {
        return villagers != null ? villagers : Map.of();
    }
}
