package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3i;

public final class RtsInteractions {
    private static final String P = "aetherhaven_rts.aetherhaven.rts";

    private RtsInteractions() {}

    public static boolean isRtsTool(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isRtsToolId(stack.getItemId());
    }

    public static boolean isRtsToolId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return AetherhavenConstants.RTS_FLAG_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_SWORD_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_SELECT_ALL_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_SELECT_KNIGHT_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_SELECT_ARCHER_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_SELECT_MAGE_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_STANCE_BANNER_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_FREE_ITEM_ID.equals(id)
            || AetherhavenConstants.RTS_EXIT_ITEM_ID.equals(id);
    }

    public static void handlePrimary(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ItemStack hand = InventoryComponent.getItemInHand(commandBuffer, playerRef);
        if (!isRtsTool(hand)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        TownRecord town = RtsSelectionService.townForSession(store, store.getExternalData().getWorld(), AetherhavenPlugin.get(), session);
        if (town == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        RtsClickService.handlePrimaryInteraction(playerRef, commandBuffer, context, store, session, hand);
    }

    public static void handleSecondary(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ItemStack hand = InventoryComponent.getItemInHand(commandBuffer, playerRef);
        if (!isRtsTool(hand)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        TownRecord town = RtsSelectionService.townForSession(store, store.getExternalData().getWorld(), AetherhavenPlugin.get(), session);
        if (town == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        BlockPosition bp = resolveInteractionBlock(context);
        Vector3i block = bp != null ? new Vector3i(bp.x, bp.y, bp.z) : null;
        Vector2fc screen = RtsScreenPickUtil.latestCameraScreenPoint(playerRef, store);
        boolean handled = applySecondaryTool(
            playerRef,
            commandBuffer,
            store,
            session,
            hand,
            block,
            screen,
            context.getTargetEntity()
        );
        context.getState().state = handled ? InteractionState.Finished : InteractionState.Failed;
    }

    public static boolean applySecondaryTool(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull ItemStack hand,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen,
        @Nullable Ref<EntityStore> targetEntity
    ) {
        String itemId = hand.getItemId();
        if (AetherhavenConstants.RTS_FLAG_ITEM_ID.equals(itemId)) {
            if (session.getSelectedGuardUuids().isEmpty()) {
                return false;
            }
            RtsScreenPickUtil.GroundPick pick = RtsScreenPickUtil.resolveCommandGroundPick(
                playerRef, store, session, targetBlock, screen
            );
            if (pick == null) {
                return false;
            }
            PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (pr != null) {
                RtsDiagnostics.moveOrderPick(pr, session, pick, targetBlock, screen, "secondary");
            }
            RtsOrderService.issueMoveOrder(
                playerRef,
                accessor,
                session,
                pick.x(),
                pick.y(),
                pick.z()
            );
            return true;
        }
        if (AetherhavenConstants.RTS_SWORD_ITEM_ID.equals(itemId)) {
            if (session.getSelectedGuardUuids().isEmpty()) {
                notify(playerRef, accessor, P + ".errorNoSelection");
                return false;
            }
            Ref<EntityStore> target = RtsHostileQuery.resolveFocusTarget(
                playerRef,
                store,
                session,
                targetBlock,
                screen,
                targetEntity
            );
            if (target == null) {
                notify(playerRef, accessor, P + ".errorNotEnemy");
                return false;
            }
            if (RtsOrderService.issueFocusAttack(playerRef, accessor, session, target)) {
                notify(playerRef, accessor, P + ".orderFocusAttack");
                return true;
            }
            return false;
        }
        if (AetherhavenConstants.RTS_FREE_ITEM_ID.equals(itemId)) {
            RtsOrderService.freeSelected(accessor, playerRef, session);
            session.clearSelection();
            RtsCommandFeedback.playFreeGuards(playerRef, accessor);
            accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
            return true;
        }
        return false;
    }

    @Nullable
    private static BlockPosition resolveInteractionBlock(@Nonnull InteractionContext context) {
        InteractionSyncData sync = context.getClientState();
        if (sync != null && sync.blockPosition != null) {
            return sync.blockPosition;
        }
        return context.getTargetBlock();
    }

    public static void handleOrderCycle(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        session.cycleOrderMode();
        RtsCommandFeedback.playProfileSelect(playerRef, commandBuffer);
        commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
    }

    public static void handleStanceCycle(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        session.cycleStanceMode();
        RtsOrderService.applyStanceToSelected(commandBuffer, playerRef, session);
        RtsCommandFeedback.playStanceChange(playerRef, commandBuffer);
        commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        notify(playerRef, commandBuffer, stanceKey(session));
    }

    public static void handleStop(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        RtsOrderService.stopSelected(playerRef, commandBuffer, session);
        RtsCommandFeedback.playFreeGuards(playerRef, commandBuffer);
    }

    public static void handleCommandPostUse(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3i targetBlock,
        @Nonnull Store<EntityStore> store
    ) {
        World world = store.getExternalData().getWorld();
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session != null && session.isActive()) {
            notify(playerRef, commandBuffer, P + ".errorUseExitTool");
            return;
        }
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownContainingBlock(world.getName(), targetBlock.x(), targetBlock.z());
        if (town == null) {
            notify(playerRef, commandBuffer, P + ".errorNotInTown");
            return;
        }
        RtsCommandService.enter(playerRef, commandBuffer, town, targetBlock.x(), targetBlock.y(), targetBlock.z(), plugin);
    }

    @Nonnull
    public static String toolHelpKey(@Nullable ItemStack hand) {
        if (hand == null || hand.isEmpty()) {
            return P + ".helpFlag";
        }
        return switch (hand.getItemId()) {
            case AetherhavenConstants.RTS_SWORD_ITEM_ID -> P + ".helpSword";
            case AetherhavenConstants.RTS_SELECT_ALL_ITEM_ID -> P + ".helpSelectAll";
            case AetherhavenConstants.RTS_STANCE_BANNER_ITEM_ID -> P + ".helpStance";
            case AetherhavenConstants.RTS_FREE_ITEM_ID -> P + ".helpFree";
            case AetherhavenConstants.RTS_EXIT_ITEM_ID -> P + ".helpExit";
            case AetherhavenConstants.RTS_SELECT_KNIGHT_ITEM_ID,
                AetherhavenConstants.RTS_SELECT_ARCHER_ITEM_ID,
                AetherhavenConstants.RTS_SELECT_MAGE_ITEM_ID -> P + ".helpSelectType";
            default -> P + ".helpFlag";
        };
    }

    @Nonnull
    private static String stanceKey(@Nonnull RtsCommandPlayerComponent session) {
        return switch (session.getStanceMode()) {
            case DEFENSIVE -> P + ".stanceDefensive";
            case AGGRESSIVE -> P + ".stanceAggressive";
            case STAND_GROUND -> P + ".stanceStandGround";
            case HOLD_FIRE -> P + ".stanceHoldFire";
        };
    }

    private static void notify(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull String key
    ) {
        PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr != null) {
            NotificationUtil.sendNotification(pr.getPacketHandler(), Message.translation(key), NotificationStyle.Default);
        }
    }
}
