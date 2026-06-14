package com.hexvane.aetherhaven.pathtool;

import com.hexvane.aetherhaven.config.PathToolStyleDefinition;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Maps path style definitions to a double chest grid (9 columns x 6 rows). */
public final class PathToolStyleEditorHelper {
    public static final short STYLE_CAPACITY = PathToolStyleDefinition.STYLE_GRID_SLOTS;

    private PathToolStyleEditorHelper() {}

    @Nonnull
    public static SimpleItemContainer createContainer() {
        return new SimpleItemContainer(STYLE_CAPACITY);
    }

    public static void loadStyleIntoContainer(@Nonnull SimpleItemContainer container, @Nonnull PathToolStyleDefinition style) {
        container.clear();
        if (style.hasColumnLayout()) {
            loadColumns(container, style.getColumns());
            return;
        }
        List<List<String>> cols = legacyToColumns(style.getCenterBlockIds());
        loadColumns(container, cols);
    }

    @Nonnull
    public static PathToolStyleDefinition snapshotContainer(@Nonnull SimpleItemContainer container, @Nonnull String name) {
        PathToolStyleDefinition d = new PathToolStyleDefinition();
        d.setName(name);
        List<List<String>> columns = new ArrayList<>();
        for (int col = 0; col < PathToolStyleDefinition.STYLE_GRID_COLUMNS; col++) {
            List<String> weighted = new ArrayList<>();
            for (int row = 0; row < PathToolStyleDefinition.STYLE_GRID_ROWS; row++) {
                short slot = slotIndex(col, row);
                ItemStack stack = container.getItemStack(slot);
                if (ItemStack.isEmpty(stack)) {
                    continue;
                }
                @Nullable
                String blockId = blockIdFromStack(stack);
                if (blockId == null) {
                    continue;
                }
                int qty = Math.max(1, stack.getQuantity());
                for (int q = 0; q < qty; q++) {
                    weighted.add(blockId);
                }
            }
            columns.add(weighted);
        }
        d.setColumns(columns);
        d.setCenterBlockIds(null);
        return d;
    }

    public static boolean hasAnyBlock(@Nonnull PathToolStyleDefinition style) {
        if (style.hasColumnLayout()) {
            for (List<String> col : style.getColumns()) {
                if (!col.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return !style.getCenterBlockIds().isEmpty();
    }

    @Nullable
    private static String blockIdFromStack(@Nonnull ItemStack stack) {
        String itemId = stack.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || !item.hasBlockType()) {
            return null;
        }
        String blockId = item.getBlockId();
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        return blockId.trim();
    }

    private static void loadColumns(@Nonnull SimpleItemContainer container, @Nonnull List<List<String>> columns) {
        for (int col = 0; col < PathToolStyleDefinition.STYLE_GRID_COLUMNS && col < columns.size(); col++) {
            List<String> pool = columns.get(col);
            if (pool == null || pool.isEmpty()) {
                continue;
            }
            int row = 0;
            for (String blockId : pool) {
                if (blockId == null || blockId.isBlank() || row >= PathToolStyleDefinition.STYLE_GRID_ROWS) {
                    continue;
                }
                @Nullable
                String itemId = itemIdForBlock(blockId);
                if (itemId == null) {
                    continue;
                }
                short slot = slotIndex(col, row);
                container.setItemStackForSlot(slot, new ItemStack(itemId, 1));
                row++;
            }
        }
    }

    @Nonnull
    private static List<List<String>> legacyToColumns(@Nonnull List<String> centerBlockIds) {
        List<List<String>> cols = new ArrayList<>();
        for (int i = 0; i < PathToolStyleDefinition.STYLE_GRID_COLUMNS; i++) {
            cols.add(new ArrayList<>());
        }
        for (String id : centerBlockIds) {
            for (int col = 0; col < PathToolStyleDefinition.STYLE_GRID_COLUMNS; col++) {
                cols.get(col).add(id);
            }
        }
        return cols;
    }

    @Nullable
    private static String itemIdForBlock(@Nonnull String blockId) {
        Item item = Item.getAssetMap().getAsset(blockId);
        if (item != null && item.hasBlockType()) {
            return item.getId();
        }
        return blockId;
    }

    public static short slotIndex(int col, int row) {
        return (short) (row * PathToolStyleDefinition.STYLE_GRID_COLUMNS + col);
    }

    public static void returnAllItems(@Nonnull ItemContainer container, @Nonnull com.hypixel.hytale.server.core.entity.entities.Player player, @Nonnull com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref, @Nonnull com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store) {
        for (short i = 0; i < container.getCapacity(); i++) {
            ItemStack stack = container.getItemStack(i);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            player.giveItem(stack, ref, store);
        }
        container.clear();
    }
}
