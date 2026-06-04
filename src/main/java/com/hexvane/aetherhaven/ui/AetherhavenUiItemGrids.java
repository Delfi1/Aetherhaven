package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.jewelry.JewelryItemIds;
import com.hexvane.aetherhaven.jewelry.JewelryTooltipMessages;
import com.hexvane.aetherhaven.jewelry.JewelryTooltipWire;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Helpers for {@code ItemGrid.Slots}.
 *
 * <p>Jewelry grids use {@link JewelryTooltipWire#forItemGrid} (virtual id, no metadata) plus plain
 * {@link ItemGridSlot#setDescription(String)}. {@link com.hexvane.aetherhaven.jewelry.JewelryTooltipPacketAdapter}
 * strips any leaked metadata from outbound {@code CustomPage} packets.</p>
 */
public final class AetherhavenUiItemGrids {
    private AetherhavenUiItemGrids() {}

    /**
     * Stack safe for {@link ItemGridSlot}: custom UI decodes metadata as {@code ClientItemMetadata}; strip BSON /
     * {@code ItemDisplay} blobs that crash the client (see {@link com.hexvane.aetherhaven.jewelry.JewelryTooltipWire}).
     */
    @Nonnull
    public static ItemStack plainStackForUi(@Nonnull String itemId, int quantity) {
        ItemStack probe = new ItemStack(itemId, quantity);
        return new ItemStack(itemId, probe.getQuantity(), probe.getDurability(), probe.getMaxDurability(), null);
    }

    @Nonnull
    public static ItemGridSlot plainSlotForUi(@Nonnull String itemId) {
        return new ItemGridSlot(plainStackForUi(itemId, 1));
    }

    @Nonnull
    public static ItemGridSlot jewelrySlotForUi(@Nonnull ItemStack inventoryJewelryStack) {
        ItemGridSlot slot = new ItemGridSlot(JewelryTooltipWire.forItemGrid(inventoryJewelryStack));
        if (!ItemStack.isEmpty(inventoryJewelryStack) && JewelryItemIds.isJewelry(inventoryJewelryStack.getItemId())) {
            String desc = JewelryTooltipMessages.toPlainEnglishDescription(inventoryJewelryStack);
            if (desc != null && !desc.isBlank()) {
                slot.setDescription(desc);
            }
        }
        return slot;
    }

    public static void setSingleSlot(@Nonnull UICommandBuilder commandBuilder, @Nonnull String itemGridSelector, @Nonnull ItemGridSlot slot) {
        commandBuilder.set(itemGridSelector + ".Slots", new ItemGridSlot[] {slot});
    }

    public static void setSingleSlot(@Nonnull UICommandBuilder commandBuilder, @Nonnull String itemGridSelector, @Nonnull ItemStack stack) {
        if (!ItemStack.isEmpty(stack) && JewelryItemIds.isJewelry(stack.getItemId())) {
            setSingleSlot(commandBuilder, itemGridSelector, jewelrySlotForUi(stack));
            return;
        }
        commandBuilder.set(itemGridSelector + ".Slots", new ItemGridSlot[] {new ItemGridSlot(plainStackForUi(stack.getItemId(), stack.getQuantity()))});
    }

    public static void setSingleSlotEmpty(@Nonnull UICommandBuilder commandBuilder, @Nonnull String itemGridSelector) {
        commandBuilder.set(itemGridSelector + ".Slots", new ItemGridSlot[] {new ItemGridSlot()});
    }

    public static void setSlots(@Nonnull UICommandBuilder commandBuilder, @Nonnull String itemGridSelector, @Nonnull ItemGridSlot[] slots) {
        commandBuilder.set(itemGridSelector + ".Slots", slots);
    }

    public static void hide(@Nonnull UICommandBuilder commandBuilder, @Nonnull String itemGridSelector) {
        commandBuilder.set(itemGridSelector + ".Slots", new ItemGridSlot[0]);
    }
}
