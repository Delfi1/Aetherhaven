package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.production.ProductionCatalog;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlotBuildingKindRequirements {
    public record SubstepRequirement(@Nonnull PlotCreatorSubstepType type, int minCount) {}

    private PlotBuildingKindRequirements() {}

    @Nonnull
    public static List<SubstepRequirement> forDraft(@Nonnull PlotCreatorDraft draft, @Nullable AetherhavenPlugin plugin) {
        PlotBuildingKind kind = draft.getKind();
        if (kind == PlotBuildingKind.VARIANT) {
            kind = resolveVariantBaseKind(draft, plugin);
        }
        if (kind == null) {
            return List.of();
        }
        return switch (kind) {
            case DECORATION -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 0)
            );
            case HOME -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.SLEEP_POI, 1)
            );
            case WORK -> workSubsteps(draft, plugin);
            case AMENITY -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.FUN_POI, 1)
            );
            case SHOP -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.SHOP_POI, 1)
            );
            case INN -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.WORK_POI, 1),
                new SubstepRequirement(PlotCreatorSubstepType.SLEEP_POI, 2),
                new SubstepRequirement(PlotCreatorSubstepType.EAT_POI, 1),
                new SubstepRequirement(PlotCreatorSubstepType.INNKEEPER_SPAWN, 1),
                new SubstepRequirement(PlotCreatorSubstepType.VISITOR_SPAWN, 1),
                new SubstepRequirement(PlotCreatorSubstepType.GUILD_MASTER_SPAWN, 0)
            );
            case TOWN_HALL -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.TREASURY_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.PLANNING_DESK_POI, 1)
            );
            case GUILD_HALL -> List.of(
                new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1),
                new SubstepRequirement(PlotCreatorSubstepType.WORK_POI, 1),
                new SubstepRequirement(PlotCreatorSubstepType.ADVENTURER_SPAWN, 1)
            );
            case VARIANT -> List.of();
        };
    }

    @Nonnull
    private static List<SubstepRequirement> workSubsteps(@Nonnull PlotCreatorDraft draft, @Nullable AetherhavenPlugin plugin) {
        List<SubstepRequirement> out = new ArrayList<>();
        out.add(new SubstepRequirement(PlotCreatorSubstepType.MANAGEMENT_BLOCK, 1));
        if (requiresProductionStorage(draft, plugin)) {
            out.add(new SubstepRequirement(PlotCreatorSubstepType.PRODUCTION_STORAGE, 1));
        }
        out.add(new SubstepRequirement(PlotCreatorSubstepType.WORK_POI, 1));
        return out;
    }

    private static boolean requiresProductionStorage(@Nonnull PlotCreatorDraft draft, @Nullable AetherhavenPlugin plugin) {
        String id = draft.getCountsAsConstructionId();
        if (id == null || id.isBlank()) {
            id = draft.getConstructionId();
        }
        if (id == null || id.isBlank() || plugin == null) {
            return false;
        }
        String gameplayId = plugin.getConstructionCatalog().resolveGameplayConstructionId(id.trim());
        return ProductionCatalog.isProductionWorkplaceConstruction(gameplayId);
    }

    @Nullable
    private static PlotBuildingKind resolveVariantBaseKind(@Nonnull PlotCreatorDraft draft, @Nullable AetherhavenPlugin plugin) {
        String baseId = draft.getCountsAsConstructionId();
        if (baseId == null || baseId.isBlank() || plugin == null) {
            return PlotBuildingKind.HOME;
        }
        ConstructionDefinition base = plugin.getConstructionCatalog().get(baseId.trim());
        if (base == null) {
            return PlotBuildingKind.HOME;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_INN.equals(baseId)) {
            return PlotBuildingKind.INN;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_TOWN_HALL.equals(baseId)) {
            return PlotBuildingKind.TOWN_HALL;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL.equals(baseId)) {
            return PlotBuildingKind.GUILD_HALL;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_PARK.equals(baseId)
            || AetherhavenConstants.CONSTRUCTION_PLOT_GAIA_ALTAR.equals(baseId)) {
            return PlotBuildingKind.AMENITY;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_MARKET_STALL.equals(baseId)) {
            return PlotBuildingKind.SHOP;
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_HOUSE.equals(base.getGameplayConstructionId())) {
            return PlotBuildingKind.HOME;
        }
        if (base.getPois().stream().anyMatch(p -> p.getTags().contains("FUN") || p.getTags().contains("SIT"))) {
            return PlotBuildingKind.AMENITY;
        }
        if (base.getPois().stream().anyMatch(p -> p.getTags().contains("SHOP"))) {
            return PlotBuildingKind.SHOP;
        }
        return PlotBuildingKind.WORK;
    }

    public static boolean isSpecialBlockType(@Nonnull String blockTypeId) {
        return AetherhavenConstants.MANAGEMENT_BLOCK_TYPE_ID.equals(blockTypeId)
            || AetherhavenConstants.BLOCK_PRODUCTION_STORAGE.equals(blockTypeId)
            || AetherhavenConstants.TREASURY_BLOCK_TYPE_ID.equals(blockTypeId)
            || "Aetherhaven_Town_Planning_Desk".equals(blockTypeId);
    }
}
