package com.hexvane.aetherhaven.floatinggift;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.config.AetherhavenPluginConfig;
import com.hexvane.aetherhaven.jewelry.JewelryChestLoot;
import com.hexvane.aetherhaven.jewelry.LootChestBonusApplier;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

public final class FloatingGiftChestLoot {
    private FloatingGiftChestLoot() {}

    public static void fill(@Nonnull SimpleItemContainer inv, @Nonnull FloatingGiftType type) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        AetherhavenPluginConfig cfg = plugin.getConfig().get();
        FloatingGiftLootBundle bundle = FloatingGiftLootFiles.loadBundle(plugin);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        List<ItemStack> stacks = new ArrayList<>();
        switch (type) {
            case REGULAR -> addRegularLoot(stacks, bundle, rnd);
            case GREEN -> addGreenLoot(stacks, bundle, cfg, rnd);
            case RED -> addRedLoot(stacks, bundle, rnd);
        }
        addZoneFillerLoot(stacks, bundle, rnd);
        placeStacksRandomly(inv, stacks, rnd);
        LootChestBonusApplier.tryInjectGoldCoinsToContainer(inv, cfg, rnd, true);
    }

    private static void addRegularLoot(
        @Nonnull List<ItemStack> out,
        @Nonnull FloatingGiftLootBundle bundle,
        @Nonnull ThreadLocalRandom rnd
    ) {
        ItemStack blueprint = bundle.tableFor(FloatingGiftType.REGULAR).rollStack(rnd);
        if (blueprint != null && !ItemStack.isEmpty(blueprint)) {
            out.add(blueprint);
        }
    }

    private static void addGreenLoot(
        @Nonnull List<ItemStack> out,
        @Nonnull FloatingGiftLootBundle bundle,
        @Nonnull AetherhavenPluginConfig cfg,
        @Nonnull ThreadLocalRandom rnd
    ) {
        ItemStack jewelry = JewelryChestLoot.rollForChest(rnd, cfg);
        if (!ItemStack.isEmpty(jewelry)) {
            out.add(jewelry);
        }
        ItemStack extra = bundle.tableFor(FloatingGiftType.GREEN).rollStack(rnd);
        if (extra != null && !ItemStack.isEmpty(extra)) {
            out.add(extra);
        }
    }

    private static void addRedLoot(
        @Nonnull List<ItemStack> out,
        @Nonnull FloatingGiftLootBundle bundle,
        @Nonnull ThreadLocalRandom rnd
    ) {
        out.addAll(bundle.tableFor(FloatingGiftType.RED).rollUniqueStacks(bundle.getRedFurnitureRolls(), rnd));
    }

    private static void addZoneFillerLoot(
        @Nonnull List<ItemStack> out,
        @Nonnull FloatingGiftLootBundle bundle,
        @Nonnull ThreadLocalRandom rnd
    ) {
        ItemModule itemModule = ItemModule.get();
        if (!itemModule.isEnabled()) {
            return;
        }
        int min = bundle.getFillerRollsMin();
        int max = bundle.getFillerRollsMax();
        int rolls = min + (max > min ? rnd.nextInt(max - min + 1) : 0);
        String droplistId = bundle.getFillerDroplistId();
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> rolled = itemModule.getRandomItemDrops(droplistId);
            for (ItemStack stack : rolled) {
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    out.add(stack);
                }
            }
        }
    }

    private static void placeStacksRandomly(
        @Nonnull SimpleItemContainer inv,
        @Nonnull List<ItemStack> stacks,
        @Nonnull ThreadLocalRandom rnd
    ) {
        for (ItemStack stack : stacks) {
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            short slot = LootChestBonusApplier.randomEmptySlot(inv, rnd);
            if (slot < 0) {
                break;
            }
            ItemStackSlotTransaction tx = inv.addItemStackToSlot(slot, stack);
            if (!tx.succeeded()) {
                break;
            }
        }
    }
}
