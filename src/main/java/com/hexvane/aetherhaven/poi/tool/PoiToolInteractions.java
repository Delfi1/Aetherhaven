package com.hexvane.aetherhaven.poi.tool;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hexvane.aetherhaven.marker.MarkerFacingYaw;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.joml.Vector3d;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiMoveValidation;
import com.hexvane.aetherhaven.poi.PoiRegistry;
import com.hexvane.aetherhaven.poi.marker.PoiMarkerEntitySync;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Shared helpers for POI tool block interactions. */
public final class PoiToolInteractions {
    /** Max squared distance (block units) from clicked block to POI cell for selection. */
    private static final long SELECT_MAX_DIST_SQ = 9L;

    private PoiToolInteractions() {}

    public static boolean hasPoiToolPermission(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
        return pr != null && pr.hasPermission(AetherhavenConstants.PERMISSION_POI_TOOL);
    }

    public static boolean isPoiToolItem(@Nullable ItemStack stack) {
        return stack != null
            && !stack.isEmpty()
            && AetherhavenConstants.POI_TOOL_ITEM_ID.equals(stack.getItemId());
    }

    public static void ensureState(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PoiToolPlayerComponent existing = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (existing == null) {
            commandBuffer.addComponent(playerRef, PoiToolPlayerComponent.getComponentType(), new PoiToolPlayerComponent());
        }
    }

    @Nullable
    public static PoiEntry findNearestPoi(
        @Nonnull PoiRegistry registry,
        @Nonnull Vector3i targetBlock
    ) {
        int tx = targetBlock.x;
        int ty = targetBlock.y;
        int tz = targetBlock.z;
        long best = Long.MAX_VALUE;
        PoiEntry bestEntry = null;
        for (PoiEntry e : registry.allEntries()) {
            long dx = (long) e.getX() - tx;
            long dy = (long) e.getY() - ty;
            long dz = (long) e.getZ() - tz;
            long d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= SELECT_MAX_DIST_SQ && d2 < best) {
                best = d2;
                bestEntry = e;
            }
        }
        return bestEntry;
    }

    public static void handleSelect(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull Vector3i targetBlock,
        @Nonnull InteractionContext context
    ) {
        if (!hasPoiToolPermission(playerRef, commandBuffer)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ensureState(playerRef, commandBuffer);
        PoiToolPlayerComponent state = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (state == null || state.getMode() != PoiToolMode.PoiEdit) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry nearest = findNearestPoi(reg, targetBlock);
        if (nearest == null) {
            state.setSelectedPoiId(null);
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noPoiInRange"));
            return;
        }
        state.setSelectedPoiId(nearest.getId());
        send(
            playerRef,
            commandBuffer,
            Message.translation("aetherhaven_world_debug.aetherhaven.poi.selected")
                .param("id", nearest.getId().toString())
                .param("x", String.valueOf(nearest.getX()))
                .param("y", String.valueOf(nearest.getY()))
                .param("z", String.valueOf(nearest.getZ()))
        );
    }

    public static void handleMove(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull Vector3i targetBlock,
        @Nonnull InteractionContext context
    ) {
        if (!hasPoiToolPermission(playerRef, commandBuffer)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ensureState(playerRef, commandBuffer);
        PoiToolPlayerComponent state = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (state == null || state.getMode() != PoiToolMode.PoiEdit) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID id = state.getSelectedPoiId();
        if (id == null) {
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noPoiSelected"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry current = reg.get(id);
        if (current == null) {
            state.setSelectedPoiId(null);
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.selectedPoiGone"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        int nx = targetBlock.x;
        int ny = targetBlock.y;
        int nz = targetBlock.z;
        if (!PoiMoveValidation.matchesExpectedBlock(world, nx, ny, nz, current.getBlockTypeId())) {
            send(
                playerRef,
                commandBuffer,
                Message.translation("aetherhaven_world_debug.aetherhaven.poi.targetBlockMismatch")
                    .param("type", current.getBlockTypeId() != null ? current.getBlockTypeId() : "")
            );
            context.getState().state = InteractionState.Failed;
            return;
        }
        PoiEntry moved = current.copyWithPosition(nx, ny, nz);
        reg.replace(moved);
        PoiMarkerEntitySync.moveMarkerForPoi(commandBuffer.getStore(), commandBuffer, id, nx, ny, nz);
        send(
            playerRef,
            commandBuffer,
            Message.translation("aetherhaven_world_debug.aetherhaven.poi.moved")
                .param("x", String.valueOf(nx))
                .param("y", String.valueOf(ny))
                .param("z", String.valueOf(nz))
        );
    }

    /**
     * Sets the autonomy leash / Seek goal for the selected POI (any block). Use on the POI anchor block again to
     * clear and fall back to the furniture cell center.
     */
    public static void handleSetInteractionTarget(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull Vector3i targetBlock,
        @Nonnull InteractionContext context
    ) {
        if (!hasPoiToolPermission(playerRef, commandBuffer)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ensureState(playerRef, commandBuffer);
        PoiToolPlayerComponent state = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (state == null || state.getMode() != PoiToolMode.PoiEdit) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID id = state.getSelectedPoiId();
        if (id == null) {
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noPoiBlock"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry current = reg.get(id);
        if (current == null) {
            state.setSelectedPoiId(null);
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.selectedPoiGone"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        int nx = targetBlock.x;
        int ny = targetBlock.y;
        int nz = targetBlock.z;
        if (nx == current.getX() && ny == current.getY() && nz == current.getZ()) {
            PoiEntry cleared = current.copyWithInteractionTarget(null, null, null, null);
            reg.replace(cleared);
            send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.clearedInteractionTarget"));
            return;
        }
        int standY = VillagerBlockUtil.findStandY(world, nx, nz, ny + 2);
        double wx = nx + 0.5;
        double wz = nz + 0.5;
        double wy = standY != Integer.MIN_VALUE ? standY + 0.02 : ny + 0.5;
        float facingYaw = 0f;
        TransformComponent playerTc = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTc != null) {
            facingYaw = MarkerFacingYaw.yawFacingToward(new Vector3d(wx, wy, wz), playerTc.getPosition());
        }
        PoiEntry updated = current.copyWithInteractionTarget(wx, wy, wz, facingYaw);
        reg.replace(updated);
        send(
            playerRef,
            commandBuffer,
            Message.translation("aetherhaven_world_debug.aetherhaven.poi.setInteractionTarget")
                .param("x", String.format(Locale.US, "%.2f", wx))
                .param("y", String.format(Locale.US, "%.2f", wy))
                .param("z", String.format(Locale.US, "%.2f", wz))
        );
    }

    public static void handleCycleMode(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull InteractionContext context
    ) {
        if (!hasPoiToolPermission(playerRef, commandBuffer)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ensureState(playerRef, commandBuffer);
        PoiToolPlayerComponent state = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (state == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        state.cycleMode();
        String messageId =
            switch (state.getMode()) {
                case PoiEdit -> "aetherhaven_items.aetherhaven.poiTool.modeCycledToEdit";
                case PoiPlacement -> "aetherhaven_items.aetherhaven.poiTool.modeCycledToPlacement";
                case PoiRemove -> "aetherhaven_items.aetherhaven.poiTool.modeCycledToRemove";
                case AdventurerSpawnMarker -> "aetherhaven_items.aetherhaven.poiTool.modeCycledToAdventurerSpots";
            };
        send(playerRef, commandBuffer, Message.translation(messageId));
        poiToast(playerRef, commandBuffer, messageId);
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (player != null && pr != null && player.getPageManager().getCustomPage() == null) {
            PoiToolHudSupport.obtainPoiToolHud(player, pr).refresh(state);
            PoiToolVisualizationSystem.noteHudMode(pr.getUuid(), state.getMode());
        }
        TransformComponent tc = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        if (tc != null && pr != null) {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin != null) {
                PoiToolVisualizationSystem.scheduleRefreshForPlayer(
                    commandBuffer.getStore().getExternalData().getWorld(),
                    pr.getUuid(),
                    plugin
                );
            }
        }
        context.getState().state = InteractionState.Finished;
    }

    public static void send(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Message message
    ) {
        PlayerRef pr = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(message);
        }
    }

    private static void poiToast(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull String messageId
    ) {
        PlayerRef pr = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        NotificationUtil.sendNotification(
            pr.getPacketHandler(),
            Message.translation(messageId),
            NotificationStyle.Success
        );
    }
}
