package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Player {@code Mana} stat costs and tiered bonus regen while a building staff is held. */
public final class BuildingStaffMana {
    private BuildingStaffMana() {}

    public static boolean bypassesManaCost(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        return player != null && player.getGameMode() == GameMode.Creative;
    }

    public static boolean canAffordBlock(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        if (bypassesManaCost(playerRef, store)) {
            return true;
        }
        EntityStatMap map = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (map == null) {
            return false;
        }
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (manaIndex == Integer.MIN_VALUE) {
            return false;
        }
        EntityStatValue mana = map.get(manaIndex);
        return mana != null && mana.get() >= BuildingStaffTiers.MANA_COST_PER_BLOCK;
    }

    /**
     * Deducts {@link BuildingStaffTiers#MANA_COST_PER_BLOCK} after a block was placed. No-op in Creative.
     *
     * @return false when mana could not be consumed
     */
    public static boolean consumeForBlock(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Store<EntityStore> store = commandBuffer.getStore();
        if (bypassesManaCost(playerRef, store)) {
            return true;
        }
        EntityStatMap map = commandBuffer.getComponent(playerRef, EntityStatMap.getComponentType());
        if (map == null) {
            return false;
        }
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (manaIndex == Integer.MIN_VALUE) {
            return false;
        }
        EntityStatValue mana = map.get(manaIndex);
        if (mana == null || mana.get() < BuildingStaffTiers.MANA_COST_PER_BLOCK) {
            return false;
        }
        map.subtractStatValue(EntityStatMap.Predictable.SELF, manaIndex, BuildingStaffTiers.MANA_COST_PER_BLOCK);
        commandBuffer.putComponent(playerRef, EntityStatMap.getComponentType(), map);
        return true;
    }

    /** Bonus mana per second while this staff tier is held and secondary has been idle recently. */
    public static void applyHeldRegenBonus(
        @Nonnull EntityStatMap map,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull String staffItemId,
        float dtSeconds
    ) {
        if (dtSeconds <= 0f) {
            return;
        }
        if (bypassesManaCost(playerRef, store)) {
            return;
        }
        if (!isIdleForBonusRegen(playerRef, store)) {
            return;
        }
        float bonusPerSecond = BuildingStaffTiers.heldManaRegenPerSecond(staffItemId);
        if (bonusPerSecond <= 0f) {
            return;
        }
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (manaIndex == Integer.MIN_VALUE) {
            return;
        }
        EntityStatValue mana = map.get(manaIndex);
        if (mana == null || mana.get() >= mana.getMax()) {
            return;
        }
        map.addStatValue(manaIndex, bonusPerSecond * dtSeconds);
    }

    private static boolean isIdleForBonusRegen(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        BuildingStaffAssemblyChannelComponent channel =
            store.getComponent(playerRef, BuildingStaffAssemblyChannelComponent.getComponentType());
        if (channel == null) {
            return true;
        }
        return channel.isIdleForManaRegen(System.nanoTime(), BuildingStaffTiers.IDLE_MANA_REGEN_DELAY_NS);
    }
}
