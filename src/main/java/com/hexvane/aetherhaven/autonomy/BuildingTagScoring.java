package com.hexvane.aetherhaven.autonomy;

import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.townsfolk.data.TownsfolkPersonalityCatalog;
import com.hexvane.aetherhaven.townsfolk.data.TownsfolkPersonalityDefinition;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BuildingTagScoring {
    private BuildingTagScoring() {}

    public static float multiplier(
        @Nonnull ConstructionDefinition def,
        @Nullable TownsfolkPersonalityCatalog catalog,
        @Nullable List<String> personalityIds
    ) {
        if (catalog == null || personalityIds == null || personalityIds.isEmpty()) {
            return 1f;
        }
        Set<String> buildingTags = def.getBuildingTags();
        if (buildingTags.isEmpty()) {
            return 1f;
        }
        float sum = 0f;
        int count = 0;
        for (String pid : personalityIds) {
            TownsfolkPersonalityDefinition p = catalog.byId(pid);
            if (p == null) {
                continue;
            }
            float m = 1f;
            for (var e : p.getLeisurePoiTagWeights().entrySet()) {
                if (buildingTags.contains(e.getKey())) {
                    m = Math.max(m, e.getValue().floatValue());
                }
            }
            sum += m;
            count++;
        }
        return count > 0 ? sum / count : 1f;
    }
}
