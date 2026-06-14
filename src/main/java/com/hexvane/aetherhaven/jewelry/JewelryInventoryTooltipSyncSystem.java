package com.hexvane.aetherhaven.jewelry;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Rolls/syncs jewelry metadata when items enter or change in a player inventory. */
public final class JewelryInventoryTooltipSyncSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    public JewelryInventoryTooltipSyncSystem() {
        super(InventoryChangeEvent.class);
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InventoryChangeEvent event
    ) {
        if (JewelryInventoryTooltipSync.isSyncing()) {
            return;
        }
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        ItemStack after = findJewelryAfterStack(event.getTransaction());
        if (after != null) {
            JewelryInventoryTooltipSync.syncSlotIfJewelry(playerRef, store, after);
            JewelryNativeTooltipManager.refreshPlayer(playerRef, store);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Nullable
    private static ItemStack findJewelryAfterStack(@Nullable Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        if (transaction instanceof SlotTransaction slot) {
            ItemStack after = slot.getSlotAfter();
            if (!ItemStack.isEmpty(after) && JewelryItemIds.isJewelry(after.getItemId())) {
                return after;
            }
            return null;
        }
        if (transaction instanceof ListTransaction<?> list) {
            for (Transaction entry : list.getList()) {
                ItemStack hit = findJewelryAfterStack(entry);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }
}
