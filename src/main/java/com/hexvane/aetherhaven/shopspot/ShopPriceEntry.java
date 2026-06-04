package com.hexvane.aetherhaven.shopspot;

import javax.annotation.Nonnull;

/** Catalog price for one purchase lot (single item or a fixed batch). */
public final class ShopPriceEntry {
    private final long goldPerBatch;
    private final int batchSize;

    public ShopPriceEntry(long goldPerBatch, int batchSize) {
        this.goldPerBatch = Math.max(0L, goldPerBatch);
        this.batchSize = Math.max(1, batchSize);
    }

    public long getGoldPerBatch() {
        return goldPerBatch;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isBatched() {
        return batchSize > 1;
    }

    /** Whole item count for {@code batchCount} lots. */
    public int itemsForBatchCount(int batchCount) {
        return Math.max(0, batchCount) * batchSize;
    }

    /** Lots available from an item stock count (stock must already be a multiple of batch size). */
    public int batchCountFromItemStock(int itemStock) {
        if (itemStock <= 0) {
            return 0;
        }
        return itemStock / batchSize;
    }

    public static int alignItemStockToBatches(int itemStock, int batchSize) {
        int bs = Math.max(1, batchSize);
        if (itemStock <= 0) {
            return 0;
        }
        return (itemStock / bs) * bs;
    }

    public static boolean isValidListingQuantity(int itemQuantity, @Nonnull ShopPriceEntry entry) {
        if (itemQuantity <= 0) {
            return false;
        }
        if (!entry.isBatched()) {
            return true;
        }
        return itemQuantity % entry.getBatchSize() == 0;
    }
}
