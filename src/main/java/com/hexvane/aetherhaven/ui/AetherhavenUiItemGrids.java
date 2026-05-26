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
        commandBuilder.set(itemGridSelector + ".Slots", new ItemGridSlot[] {new ItemGridSlot(stack)});
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
