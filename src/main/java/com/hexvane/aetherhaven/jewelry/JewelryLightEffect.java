package com.hexvane.aetherhaven.jewelry;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Applies merged {@link DynamicLight} from equipped jewelry that defines {@link Item#getLight()}. */
public final class JewelryLightEffect {
    private JewelryLightEffect() {}

    public static void apply(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> components,
        @Nonnull PlayerJewelryLoadout loadout
    ) {
        ColorLight best = resolveBestLight(loadout);
        if (best == null) {
            components.tryRemoveComponent(playerRef, DynamicLight.getComponentType());
            return;
        }
        DynamicLight next = new DynamicLight(best);
        DynamicLight existing = components.getComponent(playerRef, DynamicLight.getComponentType());
        if (existing != null) {
            components.putComponent(playerRef, DynamicLight.getComponentType(), next);
        } else {
            components.addComponent(playerRef, DynamicLight.getComponentType(), next);
        }
    }

    @Nullable
    private static ColorLight resolveBestLight(@Nonnull PlayerJewelryLoadout loadout) {
        ColorLight best = null;
        int bestRadius = -1;
        int bestRgb = -1;
        int bestSlot = Integer.MAX_VALUE;
        for (int slot = 0; slot < JewelrySlot.COUNT; slot++) {
            ItemStack equipped = loadout.getSlot(slot);
            if (ItemStack.isEmpty(equipped)) {
                continue;
            }
            Item item = Item.getAssetMap().getAsset(equipped.getItemId());
            if (item == null) {
                continue;
            }
            ColorLight light = item.getLight();
            if (light == null) {
                continue;
            }
            int radius = light.radius & 0xFF;
            int rgb = (light.red & 0xFF) + (light.green & 0xFF) + (light.blue & 0xFF);
            if (radius > bestRadius
                || (radius == bestRadius && rgb > bestRgb)
                || (radius == bestRadius && rgb == bestRgb && slot < bestSlot)) {
                bestRadius = radius;
                bestRgb = rgb;
                bestSlot = slot;
                best = new ColorLight(light);
            }
        }
        return best;
    }
}
