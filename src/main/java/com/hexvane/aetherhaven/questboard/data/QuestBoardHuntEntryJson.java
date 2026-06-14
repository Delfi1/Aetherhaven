package com.hexvane.aetherhaven.questboard.data;

import com.google.gson.annotations.SerializedName;
import com.hexvane.aetherhaven.quest.data.QuestReward;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardHuntEntryJson {
    @SerializedName("id")
    @Nullable
    private String id;

    @SerializedName("rank")
    @Nullable
    private String rank;

    @SerializedName("minRank")
    @Nullable
    private String minRank;

    @SerializedName("maxRank")
    @Nullable
    private String maxRank;

    @SerializedName("weight")
    private int weight;

    @SerializedName("daysLimit")
    private int daysLimit;

    @SerializedName("rankXpReward")
    private int rankXpReward;

    @SerializedName("titleLangKey")
    @Nullable
    private String titleLangKey;

    @SerializedName("descriptionLangKey")
    @Nullable
    private String descriptionLangKey;

    @SerializedName("targetLabelLangKey")
    @Nullable
    private String targetLabelLangKey;

    @SerializedName("killSets")
    @Nullable
    private List<QuestBoardKillSetJson> killSets;

    @SerializedName("rewards")
    @Nullable
    private List<QuestReward> rewards;

    @Nullable
    public String id() {
        return id;
    }

    @Nullable
    public String rank() {
        return rank;
    }

    @Nullable
    public String minRank() {
        return minRank;
    }

    @Nullable
    public String maxRank() {
        return maxRank;
    }

    public int weight() {
        return Math.max(1, weight);
    }

    public int daysLimit() {
        return Math.max(1, daysLimit);
    }

    public int rankXpReward() {
        return rankXpReward;
    }

    @Nullable
    public String titleLangKey() {
        return titleLangKey;
    }

    @Nullable
    public String descriptionLangKey() {
        return descriptionLangKey;
    }

    @Nullable
    public String targetLabelLangKey() {
        return targetLabelLangKey;
    }

    @Nonnull
    public List<QuestBoardKillSetJson> killSetsOrEmpty() {
        return killSets != null ? killSets : List.of();
    }

    @Nonnull
    public List<QuestReward> rewardsOrEmpty() {
        return rewards != null ? rewards : List.of();
    }
}
