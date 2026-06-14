package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.rts.ui.RtsGuardRosterInput;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3i;

/** RTS tool clicks: profile filters, flag move orders. Guard selection is box-drag only. */
public final class RtsClickService {
    private RtsClickService() {}

    public static void handleToolClick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        ItemStack hand = hotbar != null ? hotbar.getActiveItem() : ItemStack.EMPTY;
        if (hand == null || hand.isEmpty()) {
            if (pr != null) {
                RtsDiagnostics.mouseClick(pr, "single-click-no-item", targetBlock, screen);
            }
            return;
        }
        if (pr != null) {
            RtsDiagnostics.mouseClick(pr, "single-click", targetBlock, screen);
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        Vector2fc pickScreen = screen != null ? screen : RtsScreenPickUtil.latestCameraScreenPoint(playerRef, store);
        if (plugin != null
            && RtsGuardRosterInput.tryConsumeRosterClick(
                playerRef, store, commandBuffer, session, town, plugin, pickScreen
            )) {
            if (commandBuffer != null) {
                commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
            } else {
                store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
            }
            return;
        }
        applyToolClick(playerRef, commandBuffer != null ? commandBuffer : store, session, town, hand, targetBlock, screen);
        if (commandBuffer != null) {
            commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        } else {
            store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        }
    }

    public static void handleSecondaryClick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen,
        @Nullable Ref<EntityStore> targetEntity
    ) {
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        ItemStack hand = hotbar != null ? hotbar.getActiveItem() : ItemStack.EMPTY;
        if (hand == null || hand.isEmpty() || !RtsInteractions.isRtsTool(hand)) {
            return;
        }
        if (pr != null) {
            RtsDiagnostics.mouseClick(pr, "secondary-click", targetBlock, screen);
        }
        ComponentAccessor<EntityStore> accessor = commandBuffer != null ? commandBuffer : store;
        boolean handled = RtsInteractions.applySecondaryTool(
            playerRef,
            accessor,
            store,
            session,
            hand,
            targetBlock,
            screen,
            targetEntity
        );
        if (handled && commandBuffer != null) {
            commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        } else if (handled) {
            store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        }
    }

    public static boolean handlePrimaryInteraction(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull ItemStack hand
    ) {
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        BlockPosition bp = context.getTargetBlock();
        Vector3i block = bp != null ? new Vector3i(bp.x, bp.y, bp.z) : null;
        if (pr != null) {
            RtsDiagnostics.primaryInteraction(pr, hand.getItemId(), block, context.getTargetEntity());
        }

        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        TownRecord town = plugin != null
            ? RtsSelectionService.townForSession(store, store.getExternalData().getWorld(), plugin, session)
            : null;
        if (town == null) {
            if (pr != null) {
                RtsDiagnostics.primaryInteractionFailed(pr, "no-town");
            }
            markFinished(context);
            return false;
        }

        Vector2fc screen = RtsScreenPickUtil.latestCameraScreenPoint(playerRef, store);
        if (plugin != null
            && RtsGuardRosterInput.tryConsumeRosterClick(
                playerRef, store, commandBuffer, session, town, plugin, screen
            )) {
            markFinished(context);
            return true;
        }

        if (RtsPrimaryDragTracker.onPrimaryPulse(playerRef, commandBuffer, store, session, block, hand)) {
            markFinished(context);
            return true;
        }

        boolean handled = applyToolClick(
            playerRef,
            commandBuffer,
            session,
            town,
            hand,
            block,
            null
        );
        commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        markFinished(context);
        return handled;
    }

    private static boolean applyToolClick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nonnull ItemStack hand,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        Store<EntityStore> store = playerRef.getStore();
        String itemId = hand.getItemId();
        AetherhavenPlugin plugin = AetherhavenPlugin.get();

        if (AetherhavenConstants.RTS_SELECT_ALL_ITEM_ID.equals(itemId)) {
            RtsSelectionService.selectAllGuards(store, session, town);
            RtsCommandFeedback.playProfileSelect(playerRef, accessor);
            return true;
        }
        if (AetherhavenConstants.RTS_SELECT_KNIGHT_ITEM_ID.equals(itemId)) {
            RtsSelectionService.selectOnlyByProfile(
                store, store.getExternalData().getWorld(), plugin, session, town, RtsSelectionService.ProfileFilter.KNIGHT
            );
            RtsCommandFeedback.playProfileSelect(playerRef, accessor);
            return true;
        }
        if (AetherhavenConstants.RTS_SELECT_ARCHER_ITEM_ID.equals(itemId)) {
            RtsSelectionService.selectOnlyByProfile(
                store, store.getExternalData().getWorld(), plugin, session, town, RtsSelectionService.ProfileFilter.ARCHER
            );
            RtsCommandFeedback.playProfileSelect(playerRef, accessor);
            return true;
        }
        if (AetherhavenConstants.RTS_SELECT_MAGE_ITEM_ID.equals(itemId)) {
            RtsSelectionService.selectOnlyByProfile(
                store, store.getExternalData().getWorld(), plugin, session, town, RtsSelectionService.ProfileFilter.MAGE
            );
            RtsCommandFeedback.playProfileSelect(playerRef, accessor);
            return true;
        }
        if (AetherhavenConstants.RTS_EXIT_ITEM_ID.equals(itemId)) {
            return true;
        }

        RtsScreenPickUtil.GroundPick pick = RtsScreenPickUtil.resolveCommandGroundPick(
            playerRef, store, session, targetBlock, screen
        );
        if (pick != null
            && AetherhavenConstants.RTS_FLAG_ITEM_ID.equals(itemId)
            && !session.getSelectedGuardUuids().isEmpty()) {
            PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (pr != null) {
                RtsDiagnostics.moveOrderPick(pr, session, pick, targetBlock, screen, "primary-click");
            }
            RtsOrderService.issueMoveOrder(playerRef, accessor, session, pick.x(), pick.y(), pick.z());
            return true;
        }
        return false;
    }

    private static void markFinished(@Nonnull InteractionContext context) {
        context.getState().state = InteractionState.Finished;
    }
}
