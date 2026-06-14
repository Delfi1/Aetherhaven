package com.hexvane.aetherhaven.jewelry;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Pushes virtual item definitions to the client for custom UI jewelry grids. */
public final class JewelryTooltipUiSupport {

    private JewelryTooltipUiSupport() {}

    public static void ensureVirtualItemForStack(@Nonnull PlayerRef playerRef, @Nonnull ItemStack stack) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        JewelryTooltipPacketAdapter adapter = plugin.getJewelryTooltipPacketAdapter();
        if (adapter != null) {
            adapter.ensureVirtualItemForStack(playerRef, stack);
        }
    }
}
