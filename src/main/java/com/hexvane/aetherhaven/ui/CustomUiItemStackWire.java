package com.hexvane.aetherhaven.ui;

import javax.annotation.Nonnull;
import org.bson.BsonDocument;
import org.bson.BsonValue;

/**
 * Custom UI {@code ItemGrid} slots decode stack metadata as {@code ClientItemMetadata}. Mod BSON ({@code ItemDisplay},
 * {@code AetherhavenPlotToken}, etc.) must be stripped from outbound {@code CustomPage} command payloads.
 */
public final class CustomUiItemStackWire {
    private CustomUiItemStackWire() {}

    public static boolean walkValue(@Nonnull BsonValue value) {
        if (value.isDocument()) {
            return walkDocument(value.asDocument());
        }
        if (value.isArray()) {
            boolean modified = false;
            for (BsonValue element : value.asArray()) {
                modified |= walkValue(element);
            }
            return modified;
        }
        return false;
    }

    private static boolean walkDocument(@Nonnull BsonDocument doc) {
        boolean modified = false;
        BsonValue wrapped = doc.get("ItemStack");
        if (wrapped != null && wrapped.isDocument()) {
            modified |= sanitizeItemStackDocument(wrapped.asDocument());
        }
        modified |= sanitizeItemStackDocument(doc);
        for (var entry : doc.entrySet()) {
            if ("ItemStack".equals(entry.getKey())) {
                continue;
            }
            modified |= walkValue(entry.getValue());
        }
        return modified;
    }

    /** Returns true if {@code Metadata} was removed from an item-stack-shaped document. */
    public static boolean sanitizeItemStackDocument(@Nonnull BsonDocument doc) {
        if (!looksLikeItemStackDocument(doc)) {
            return false;
        }
        if (doc.containsKey("Metadata")) {
            doc.remove("Metadata");
            return true;
        }
        return false;
    }

    private static boolean looksLikeItemStackDocument(@Nonnull BsonDocument doc) {
        BsonValue id = doc.get("Id");
        if (id != null && id.isString()) {
            return true;
        }
        id = doc.get("ItemId");
        return id != null && id.isString();
    }
}
