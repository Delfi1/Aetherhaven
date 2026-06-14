package com.hexvane.aetherhaven.villager.data;

import com.google.gson.annotations.SerializedName;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import com.hexvane.aetherhaven.town.TownRecord;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Optional gating for inn pool eligibility on a {@link VillagerDefinition}. */
public final class InnPoolRequires {
    static final InnPoolRequires EMPTY = new InnPoolRequires();

    @SerializedName("completedQuestIds")
    @Nullable
    private List<String> completedQuestIds;

    @SerializedName("completeConstructionIds")
    @Nullable
    private List<String> completeConstructionIds;

    @Nonnull
    public List<String> completedQuestIdsOrEmpty() {
        return completedQuestIds != null ? completedQuestIds : Collections.emptyList();
    }

    @Nonnull
    public List<String> completeConstructionIdsOrEmpty() {
        return completeConstructionIds != null ? completeConstructionIds : Collections.emptyList();
    }

    public boolean isEmpty() {
        return completedQuestIdsOrEmpty().isEmpty() && completeConstructionIdsOrEmpty().isEmpty();
    }

    /** True when every listed quest is completed and every listed construction plot is built. */
    public boolean satisfiedBy(@Nonnull TownRecord town, @Nonnull ConstructionCatalog constructionCatalog) {
        for (String id : completedQuestIdsOrEmpty()) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (!town.hasQuestCompleted(id.trim())) {
                return false;
            }
        }
        for (String id : completeConstructionIdsOrEmpty()) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (!town.hasCompletePlotWithConstruction(constructionCatalog, id.trim())) {
                return false;
            }
        }
        return true;
    }

    public boolean satisfiedBy(@Nonnull TownRecord town, @Nonnull AetherhavenPlugin plugin) {
        return satisfiedBy(town, plugin.getConstructionCatalog());
    }
}
