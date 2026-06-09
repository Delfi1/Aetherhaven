package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class PlotCreatorValidator {
    private PlotCreatorValidator() {}

    @Nullable
    public static String validateId(@Nonnull String raw, @Nonnull ConstructionCatalog catalog, @Nullable String editingId) {
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            return "id_empty";
        }
        if (!id.startsWith("plot_")) {
            return "id_prefix";
        }
        if (id.startsWith("plot_wall")) {
            return "id_wall";
        }
        if (!id.matches("plot_[a-z0-9_]+")) {
            return "id_chars";
        }
        if (!id.equals(editingId) && catalog.get(id) != null) {
            return "id_taken";
        }
        return null;
    }

    @Nullable
    public static String validateBeforeSave(@Nonnull PlotCreatorDraft draft, @Nonnull AetherhavenPlugin plugin) {
        if (draft.getConstructionId() == null || draft.getDisplayName() == null || draft.getPrefabPath() == null) {
            return "incomplete";
        }
        Vector3i anchor = draft.getPlotAnchor();
        if (anchor == null) {
            return "incomplete";
        }
        if (draft.isInsideBounds(anchor)) {
            return "anchorInside";
        }
        if (!PlotCreatorAnchorRules.isOutsideCorner(draft, anchor)) {
            return "anchorNotCorner";
        }
        String idErr = validateId(draft.getConstructionId(), plugin.getConstructionCatalog(), draft.getEditingConstructionId());
        if (idErr != null) {
            return idErr;
        }
        var prefabFile = CustomBuildingsPaths.resolvePrefabFile(plugin.getDataDirectory(), draft.getPrefabPath());
        if (prefabFile == null || !Files.isRegularFile(prefabFile)) {
            return "prefab_missing";
        }
        List<PlotBuildingKindRequirements.SubstepRequirement> steps =
            PlotBuildingKindRequirements.forDraft(draft, plugin);
        for (int i = 0; i < steps.size(); i++) {
            PlotBuildingKindRequirements.SubstepRequirement req = steps.get(i);
            if (countForSubstep(draft, req.type()) < req.minCount()) {
                return "substep_" + req.type().name();
            }
        }
        return null;
    }

    private static int countForSubstep(@Nonnull PlotCreatorDraft draft, @Nonnull PlotCreatorSubstepType type) {
        return switch (type) {
            case MANAGEMENT_BLOCK -> draft.getManagementBlockLocalPos() != null ? 1 : 0;
            case PRODUCTION_STORAGE -> draft.getProductionStorageLocalPos() != null ? 1 : 0;
            case TREASURY_BLOCK -> draft.getTreasuryLocalPos() != null ? 1 : 0;
            case SHOP_SPOT -> draft.getPlacedSpecialBlocks().size();
            case PLANNING_DESK_POI, WORK_POI, SLEEP_POI, EAT_POI, FUN_POI, SHOP_POI -> countPoiSubstep(draft, type);
            case INNKEEPER_SPAWN -> draft.getInnkeeperSpawnLocal() != null ? 1 : 0;
            case VISITOR_SPAWN -> draft.getVisitorSpawnLocals().size();
            case GUILD_MASTER_SPAWN -> draft.getGuildMasterSpawnLocal() != null ? 1 : 0;
            case ADVENTURER_SPAWN -> draft.getAdventurerSpawns().size();
        };
    }

    private static int countPoiSubstep(@Nonnull PlotCreatorDraft draft, @Nonnull PlotCreatorSubstepType type) {
        int n = 0;
        for (PlotCreatorPoiDraft p : draft.getPois()) {
            if (matchesPoiSubstep(p, type)) {
                n++;
            }
        }
        return n;
    }

    private static boolean matchesPoiSubstep(@Nonnull PlotCreatorPoiDraft p, @Nonnull PlotCreatorSubstepType type) {
        return switch (type) {
            case WORK_POI -> p.getTags().contains("WORK");
            case SLEEP_POI -> p.getTags().contains("SLEEP") || p.getTags().contains("ENERGY");
            case EAT_POI -> p.getTags().contains("EAT");
            case FUN_POI -> p.getTags().contains("FUN") || p.getTags().contains("SIT");
            case SHOP_POI -> p.getTags().contains("SHOP");
            case PLANNING_DESK_POI -> "Aetherhaven_Town_Planning_Desk".equals(p.getBlockTypeId());
            default -> false;
        };
    }
}
