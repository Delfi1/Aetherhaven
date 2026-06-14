package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Tiered bonus mana regen while a building staff is held. Runs after vanilla {@code Mana} regen and max-stat
 * recalculation so bonus applies even while channeling (vanilla regen is blocked by the Charging condition).
 * Bonus regen only applies after {@link BuildingStaffTiers#IDLE_MANA_REGEN_DELAY_NS} without holding secondary.
 */
public final class BuildingStaffManaRegenSystem extends EntityTickingSystem<EntityStore>
    implements EntityStatsSystems.StatModifyingSystem {
    private static final int STRIP_LEGACY_DURABILITY_INTERVAL_TICKS = 40;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemDependency<>(Order.AFTER, EntityStatsSystems.Recalculate.class),
        new SystemDependency<>(Order.AFTER, EntityStatsModule.PlayerRegenerateStatsSystem.class)
    );

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), EntityStatMap.getComponentType());
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        ItemStack hand = InventoryComponent.getItemInHand(store, playerRef);
        if (hand != null && !hand.isEmpty() && BuildingStaffTiers.isBuildingStaff(hand.getItemId())) {
            EntityStatMap map = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
            BuildingStaffMana.applyHeldRegenBonus(map, playerRef, store, hand.getItemId(), dt);
        }

        if ((store.getExternalData().getWorld().getTick() + index) % STRIP_LEGACY_DURABILITY_INTERVAL_TICKS != 0) {
            return;
        }
        stripLegacyStaffDurability(playerRef, store);
    }

    /** Removes building-mana durability from pre-mana-bar staff versions so tools show no fake durability bar. */
    private static void stripLegacyStaffDurability(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        CombinedItemContainer inv = InventoryComponent.getCombined(store, playerRef, InventoryComponent.EVERYTHING);
        if (inv == null) {
            return;
        }
        for (short slot = 0; slot < inv.getCapacity(); slot++) {
            ItemStack stack = inv.getItemStack(slot);
            if (stack == null || stack.isEmpty() || !BuildingStaffTiers.isBuildingStaff(stack.getItemId())) {
                continue;
            }
            if (stack.isUnbreakable()) {
                continue;
            }
            ItemStack cleaned = new ItemStack(stack.getItemId(), stack.getQuantity());
            ItemStackSlotTransaction tx = inv.replaceItemStackInSlot(slot, stack, cleaned);
            if (!tx.succeeded()) {
                return;
            }
        }
    }
}

