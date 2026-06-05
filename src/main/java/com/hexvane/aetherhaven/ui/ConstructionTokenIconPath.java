package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.plotcreator.CustomBuildingsPaths;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves isometric thumbnail paths for plot token crafting UI (not the sign item model). */
public final class ConstructionTokenIconPath {
    private ConstructionTokenIconPath() {}

    @Nonnull
    public static String forConstruction(@Nonnull ConstructionDefinition def, @Nullable Path dataDirectory) {
        String id = def.getId().trim();
        if (dataDirectory != null) {
            Path customIcon = CustomBuildingsPaths.iconFile(dataDirectory, id);
            if (Files.isRegularFile(customIcon)) {
                return CustomBuildingsPaths.iconAssetPath(id);
            }
        }
        String tokenItemId = def.getPlotTokenItemId();
        if (tokenItemId != null && AetherhavenConstants.PLOT_TOKEN_UNIFIED.equals(tokenItemId.trim())) {
            return CustomBuildingsPaths.iconAssetPath(id);
        }
        if (tokenItemId != null
            && !tokenItemId.isBlank()
            && !AetherhavenConstants.PLOT_TOKEN_UNIFIED.equals(tokenItemId.trim())) {
            Item item = Item.getAssetMap().getAsset(tokenItemId.trim());
            return ItemAssetImagePath.forItem(item, tokenItemId.trim());
        }
        return CustomBuildingsPaths.iconAssetPath(id);
    }

    @Nonnull
    public static String forConstruction(@Nonnull ConstructionDefinition def) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        Path dataDir = plugin != null ? plugin.getDataDirectory() : null;
        return forConstruction(def, dataDir);
    }
}
