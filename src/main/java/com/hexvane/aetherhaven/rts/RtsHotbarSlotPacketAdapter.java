package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Adopts the client's requested hotbar slot on the server during RTS command mode before vanilla
 * {@code InteractionModule} compares {@code MouseInteraction.activeSlot} to the server value.
 */
public final class RtsHotbarSlotPacketAdapter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private PacketFilter inboundFilter;

    public void register() {
        inboundFilter = PacketAdapters.registerInbound((PlayerPacketFilter) this::onInboundPacket);
        LOGGER.atInfo().log("RTS hotbar slot packet adapter registered");
    }

    public void deregister() {
        if (inboundFilter != null) {
            try {
                PacketAdapters.deregisterInbound(inboundFilter);
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to deregister RTS hotbar slot inbound filter: %s", e.getMessage());
            }
            inboundFilter = null;
        }
    }

    private boolean onInboundPacket(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (packet instanceof MouseInteraction mouse) {
            queueHotbarSlotAdoption(playerRef, (byte) mouse.activeSlot);
        } else if (packet instanceof SyncInteractionChains sync && sync.updates != null) {
            for (SyncInteractionChain chain : sync.updates) {
                if (chain != null) {
                    queueHotbarSlotAdoption(playerRef, (byte) chain.activeHotbarSlot);
                }
            }
        }
        return false;
    }

    private static void queueHotbarSlotAdoption(@Nonnull PlayerRef playerRef, byte requestedSlot) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> adoptHotbarSlotIfCommanding(ref, store, requestedSlot));
    }

    static void adoptHotbarSlotIfCommanding(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        byte requestedSlot
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null) {
            return;
        }
        if (requestedSlot < 0 || requestedSlot >= hotbar.getInventory().getCapacity()) {
            return;
        }
        if (hotbar.getActiveSlot() == requestedSlot) {
            return;
        }
        hotbar.setActiveSlot(requestedSlot, playerRef, store);
    }
}
