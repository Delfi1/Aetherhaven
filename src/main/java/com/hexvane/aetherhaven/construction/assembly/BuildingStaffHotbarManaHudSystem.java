package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventorySetActiveSlotEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventorySystems;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Refreshes mana HUD and stat sync when the player selects a building staff on the hotbar. */
public final class BuildingStaffHotbarManaHudSystem {
    private BuildingStaffHotbarManaHudSystem() {}

    public static final class SlotChangeHandler extends EntityEventSystem<EntityStore, InventorySetActiveSlotEvent> {
        @Nonnull
        private final Set<Dependency<EntityStore>> dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, InventorySystems.ActiveSlotChangedEntityEventSystem.class)
        );

        public SlotChangeHandler() {
            super(InventorySetActiveSlotEvent.class);
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return dependencies;
        }

        @Override
        public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InventorySetActiveSlotEvent event
        ) {
            if (event.getInventorySectionId() != InventoryComponent.HOTBAR_SECTION_ID) {
                return;
            }
            ItemStack hand = InventoryComponent.getItemInHand(store, chunk.getReferenceTo(index));
            if (hand == null || hand.isEmpty() || !BuildingStaffTiers.isBuildingStaff(hand.getItemId())) {
                return;
            }
            Player player = chunk.getComponent(index, Player.getComponentType());
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (player == null || playerRef == null) {
                return;
            }
            BuildingStaffManaHudSupport.syncManaHudForHeldStaff(chunk.getReferenceTo(index), store, commandBuffer, player, playerRef);
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(
                Player.getComponentType(),
                InventoryComponent.Hotbar.getComponentType(),
                PlayerRef.getComponentType()
            );
        }
    }
}
