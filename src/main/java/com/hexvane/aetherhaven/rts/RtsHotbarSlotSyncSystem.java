package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryActiveSlotRequestEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InventorySetActiveSlotEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Event-driven hotbar slot sync so mouse clicks pass InteractionModule activeSlot checks. */
public final class RtsHotbarSlotSyncSystem {
    private RtsHotbarSlotSyncSystem() {}

    public static final class SlotChangeHandler extends EntityEventSystem<EntityStore, InventorySetActiveSlotEvent> {
        public SlotChangeHandler() {
            super(InventorySetActiveSlotEvent.class);
        }

        @Override
        public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InventorySetActiveSlotEvent event
        ) {
            if (event.getInventorySectionId() != -1) {
                return;
            }
            RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
            if (session == null || !session.isActive()) {
                return;
            }
            PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
            InventoryComponent.Hotbar hotbar = chunk.getComponent(index, InventoryComponent.Hotbar.getComponentType());
            if (pr == null || hotbar == null) {
                return;
            }
            RtsHotbarSync.syncActiveSlot(pr, hotbar.getActiveSlot(), "set-active-slot-event");
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(
                RtsCommandPlayerComponent.getComponentType(),
                Player.getComponentType(),
                InventoryComponent.Hotbar.getComponentType()
            );
        }
    }

    /** Client scroll / slot request — adopt the requested slot on the server during command mode. */
    public static final class ActiveSlotRequestHandler extends EntityEventSystem<EntityStore, InventoryActiveSlotRequestEvent> {
        public ActiveSlotRequestHandler() {
            super(InventoryActiveSlotRequestEvent.class);
        }

        @Override
        public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InventoryActiveSlotRequestEvent event
        ) {
            if (event.getInventorySectionId() != InventoryComponent.HOTBAR_SECTION_ID) {
                return;
            }
            RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
            if (session == null || !session.isActive()) {
                return;
            }
            Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
            InventoryComponent.Hotbar hotbar = chunk.getComponent(index, InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) {
                return;
            }
            byte requested = event.getNewSlot();
            if (hotbar.getActiveSlot() != requested) {
                hotbar.setActiveSlot(requested, playerRef, commandBuffer);
            }
            event.setCancelled(true);
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(
                RtsCommandPlayerComponent.getComponentType(),
                Player.getComponentType(),
                InventoryComponent.Hotbar.getComponentType()
            );
        }
    }
}
