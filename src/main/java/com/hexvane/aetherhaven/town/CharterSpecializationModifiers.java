package com.hexvane.aetherhaven.town;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import javax.annotation.Nonnull;

/** Central place for charter specialization bonuses on production workplaces. */
public final class CharterSpecializationModifiers {
    private CharterSpecializationModifiers() {}

    /**
     * @param constructionId {@link AetherhavenConstants#CONSTRUCTION_PLOT_FARM} etc.
     * @return 2.0 when the town specialization matches this workplace, else 1.0
     */
    public static double productionMultiplier(
        @Nonnull TownRecord town,
        @Nonnull ConstructionCatalog constructionCatalog,
        @Nonnull String plotOrGameplayConstructionId
    ) {
        CharterSpecialization s = town.getCharterSpecializationEnum();
        if (s == null) {
            return 1.0;
        }
        String g = constructionCatalog.resolveGameplayConstructionId(plotOrGameplayConstructionId);
        return switch (s) {
            case MINING -> AetherhavenConstants.CONSTRUCTION_PLOT_MINERS_HUT.equals(g) ? 2.0 : 1.0;
            case LOGGING -> AetherhavenConstants.CONSTRUCTION_PLOT_LUMBERMILL.equals(g) ? 2.0 : 1.0;
            case FARMING -> AetherhavenConstants.CONSTRUCTION_PLOT_FARM.equals(g) ? 2.0 : 1.0;
            case RANCHING -> AetherhavenConstants.CONSTRUCTION_PLOT_BARN.equals(g) ? 2.0 : 1.0;
        };
    }

    /** Items granted per completed production cycle (1 normally, 2 when specialization matches). */
    public static long productionAmountPerCycle(
        @Nonnull TownRecord town,
        @Nonnull ConstructionCatalog constructionCatalog,
        @Nonnull String plotOrGameplayConstructionId
    ) {
        return (long) productionMultiplier(town, constructionCatalog, plotOrGameplayConstructionId);
    }
}
