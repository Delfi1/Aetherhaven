package com.hexvane.aetherhaven.jewelry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.MessageUtil;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.component.Store;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Per-stack jewelry tooltips via native {@link ItemDisplayMetadata} (same approach as Simple Enchantments
 * {@code NativeTooltipManager}). Inventory packets send {@link ItemStack#toPacket()} metadata; the client reads
 * {@code ItemDisplay.Description} for tooltip text.
 *
 * <p>Rolled {@link JewelryRarity} tier borders use {@link JewelryTooltipPacketAdapter} virtual item ids (DTL-style
 * {@code qualityIndex} on cloned {@code ItemBase} definitions). Tooltip text uses {@link JewelryTooltipMessages} via
 * stack {@code ItemDisplay} metadata and a bold tier-colored rarity banner line.</p>
 */
public final class JewelryNativeTooltipManager {

    private JewelryNativeTooltipManager() {}

    /** Writes {@link ItemDisplayMetadata} description from rolled jewelry BSON on this stack. */
    @Nonnull
    public static ItemStack apply(@Nonnull ItemStack stack) {
        if (ItemStack.isEmpty(stack) || !JewelryItemIds.isJewelry(stack.getItemId())) {
            return clearDisplay(stack);
        }
        if (!JewelryMetadata.hasJewelryMeta(stack)) {
            return clearDisplay(stack);
        }
        Message description = buildDescription(stack);
        if (description == null) {
            return clearDisplay(stack);
        }
        return stack.withMetadata(ItemDisplayMetadata.KEYED_CODEC, new ItemDisplayMetadata(null, description));
    }

    @Nullable
    private static Message buildDescription(@Nonnull ItemStack stack) {
        Message description = JewelryTooltipMessages.forStack(stack);
        String plain = MessageUtil.formatMessageToPlainString(description.getFormattedMessage());
        if (plain.isBlank()) {
            return null;
        }
        return description;
    }

    @Nonnull
    private static ItemStack clearDisplay(@Nonnull ItemStack stack) {
        if (stack.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC) == null) {
            return stack;
        }
        return stack.withMetadata(ItemDisplayMetadata.KEYED_CODEC, null);
    }

    public static void refreshAllPlayers() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (PlayerRef playerRef : universe.getPlayers()) {
            refreshPlayer(playerRef);
        }
    }

    public static void refreshPlayer(@Nonnull UUID playerUuid) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        PlayerRef playerRef = universe.getPlayer(playerUuid);
        if (playerRef != null) {
            refreshPlayer(playerRef);
        }
    }

    public static void refreshPlayer(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        PlayerRef pref = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pref != null) {
            refreshPlayer(pref);
        }
    }

    public static void refreshPlayer(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        Runnable finish =
            () -> {
                refreshInventory(ref, store);
                AetherhavenPlugin plugin = AetherhavenPlugin.get();
                if (plugin != null && plugin.getJewelryTooltipPacketAdapter() != null) {
                    plugin.getJewelryTooltipPacketAdapter().refreshPlayer(playerRef.getUuid());
                }
            };
        if (world != null && world.isAlive() && !world.isInThread()) {
            world.execute(finish);
        } else {
            finish.run();
        }
    }

    private static void refreshInventory(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        CombinedItemContainer inv = InventoryComponent.getCombined(store, playerRef, InventoryComponent.EVERYTHING);
        if (inv == null) {
            return;
        }
        for (short slot = 0; slot < inv.getCapacity(); slot++) {
            ItemStack current = inv.getItemStack(slot);
            if (ItemStack.isEmpty(current) || !JewelryItemIds.isJewelry(current.getItemId())) {
                continue;
            }
            ItemStack updated = apply(JewelryMetadata.ensureRolled(current));
            if (!updated.isEquivalentType(current)) {
                ItemStackSlotTransaction tx = inv.replaceItemStackInSlot(slot, current, updated);
                if (!tx.succeeded()) {
                    continue;
                }
            }
        }
    }
}
