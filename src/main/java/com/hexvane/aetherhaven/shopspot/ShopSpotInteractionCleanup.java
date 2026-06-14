package com.hexvane.aetherhaven.shopspot;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Clears removed LMB/RMB quantity-overlay bindings if a player still has them (breaks block breaking). */
public final class ShopSpotInteractionCleanup {
    private static final String LEGACY_QTY_DEC_ROOT = "Aetherhaven_Shop_Spot_Qty_Dec_Root";
    private static final String LEGACY_QTY_INC_ROOT = "Aetherhaven_Shop_Spot_Qty_Inc_Root";

    private ShopSpotInteractionCleanup() {}

    public static void healLegacyQuantityOverlay(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Store<EntityStore> store = commandBuffer.getStore();
        Interactions existing = store.getComponent(playerRef, Interactions.getComponentType());
        if (existing == null) {
            return;
        }
        boolean stuck =
            LEGACY_QTY_DEC_ROOT.equals(existing.getInteractionId(InteractionType.Primary))
                || LEGACY_QTY_INC_ROOT.equals(existing.getInteractionId(InteractionType.Secondary));
        if (stuck) {
            commandBuffer.removeComponent(playerRef, Interactions.getComponentType());
        }
    }
}
