package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.construction.MaterialRequirement;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves {@link com.hypixel.hytale.server.core.ui.AssetImage} paths for material requirement lines. */
public final class UiMaterialIcons {
    private UiMaterialIcons() {}

    @Nullable
    public static String assetPathFor(@Nonnull MaterialRequirement line) {
        String rt = line.getResourceTypeId();
        if (rt != null && !rt.isBlank()) {
            ResourceType asset = ResourceType.getAssetMap().getAsset(rt.trim());
            if (asset == null) {
                return null;
            }
            String icon = asset.getIcon();
            return icon != null && !icon.isBlank() ? icon.trim() : null;
        }
        String itemId = line.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String id = itemId.trim();
        Item item = Item.getAssetMap().getAsset(id);
        if (item == null) {
            return null;
        }
        return ItemAssetImagePath.forItem(item, id);
    }
}
