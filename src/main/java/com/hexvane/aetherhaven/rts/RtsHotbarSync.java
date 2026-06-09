package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.inventory.UpdatePlayerInventory;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Pushes hotbar contents and active slot to the client after server-side swaps. */
public final class RtsHotbarSync {
    private RtsHotbarSync() {}

    /** Hotbar items changed — full inventory push plus active slot. */
    public static void syncToClient(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComponent,
        @Nonnull InventoryComponent.Hotbar hotbar
    ) {
        hotbar.markDirty();
        playerRefComponent
            .getPacketHandler()
            .writeNoCache(new UpdatePlayerInventory(null, null, hotbar.getInventory().toPacket(), null, null, null));
        syncActiveSlot(playerRefComponent, hotbar.getActiveSlot(), "full-hotbar-sync");
    }

    /** Slot-only correction — keeps client activeSlot matching server for mouse interactions. */
    public static void syncActiveSlot(@Nonnull PlayerRef playerRefComponent, byte slot) {
        syncActiveSlot(playerRefComponent, slot, "slot-only");
    }

    public static void syncActiveSlot(@Nonnull PlayerRef playerRefComponent, byte slot, @Nonnull String reason) {
        playerRefComponent.getPacketHandler().writeNoCache(new SetActiveSlot(-1, slot));
        RtsDiagnostics.hotbarSync(playerRefComponent, reason, slot);
    }
}
