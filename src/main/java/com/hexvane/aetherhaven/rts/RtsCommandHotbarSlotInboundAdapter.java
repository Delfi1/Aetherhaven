package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * RTS command-mode packet shims for hotbar selection and {@code LookAtPlane} mouse input.
 *
 * <ul>
 *   <li>{@code MouseInteraction} — feed {@link com.hypixel.hytale.server.core.entity.entities.player.CameraManager}
 *       only; never change hotbar slot or run vanilla interaction slot checks.</li>
 *   <li>{@code SyncInteractionChains} — hotbar {@code SwapFrom}/{@code SwapTo} batches only; Primary tool chains pass through.</li>
 *   <li>{@code SetActiveSlot(-1)} inbound — adopt explicit client slot changes (vanilla would disconnect).</li>
 *   <li>{@code SetActiveSlot(-1)} outbound — block vanilla hotbar snap-back during RTS;
 *       {@link RtsHotbarSync} hotbar rewrites may still push a slot to the client.</li>
 * </ul>
 */
public final class RtsCommandHotbarSlotInboundAdapter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final AtomicBoolean ALLOW_OUTBOUND_HOTBAR_SLOT = new AtomicBoolean(false);

    @Nullable
    private PacketFilter inboundFilter;
    @Nullable
    private PacketFilter outboundFilter;

    public void register() {
        inboundFilter = PacketAdapters.registerInbound((PlayerPacketFilter) this::onInboundPacket);
        outboundFilter = PacketAdapters.registerOutbound((PlayerPacketFilter) this::onOutboundPacket);
        LOGGER.atInfo().log("RTS command hotbar/camera packet adapter registered");
    }

    public void deregister() {
        if (inboundFilter != null) {
            try {
                PacketAdapters.deregisterInbound(inboundFilter);
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to deregister RTS command inbound filter: %s", e.getMessage());
            }
            inboundFilter = null;
        }
        if (outboundFilter != null) {
            try {
                PacketAdapters.deregisterOutbound(outboundFilter);
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to deregister RTS command outbound filter: %s", e.getMessage());
            }
            outboundFilter = null;
        }
    }

    static void runWithOutboundHotbarSlotSync(@Nonnull Runnable action) {
        ALLOW_OUTBOUND_HOTBAR_SLOT.set(true);
        try {
            action.run();
        } finally {
            ALLOW_OUTBOUND_HOTBAR_SLOT.set(false);
        }
    }

    private static boolean isCommanding(@Nonnull PlayerRef playerRef) {
        return RtsCommandingSessionIndex.isCommanding(playerRef.getUuid());
    }

    private boolean onInboundPacket(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!isCommanding(playerRef)) {
            return false;
        }
        if (packet instanceof MouseInteraction mouse) {
            RtsCommandCameraInput.queueFeed(playerRef, mouse);
            return true;
        }
        if (packet instanceof SyncInteractionChains chains) {
            SyncInteractionChain[] updates = chains.updates;
            if (updates != null && RtsCommandHotbarSlotSync.tryHandleHotbarSlotChains(playerRef, updates)) {
                return true;
            }
            return false;
        }
        if (packet instanceof SetActiveSlot slotPacket && slotPacket.inventorySectionId == -1) {
            RtsCommandHotbarSlotSync.handleSetActiveSlot(playerRef, (byte) slotPacket.activeSlot);
            return true;
        }
        return false;
    }

    private boolean onOutboundPacket(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (ALLOW_OUTBOUND_HOTBAR_SLOT.get()) {
            return false;
        }
        if (!isCommanding(playerRef)) {
            return false;
        }
        return packet instanceof SetActiveSlot slotPacket && slotPacket.inventorySectionId == -1;
    }
}
