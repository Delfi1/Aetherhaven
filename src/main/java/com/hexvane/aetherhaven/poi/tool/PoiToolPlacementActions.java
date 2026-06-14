package com.hexvane.aetherhaven.poi.tool;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.marker.MarkerEntityProximity;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiRegistry;
import com.hexvane.aetherhaven.poi.marker.PoiMarkerDataComponent;
import com.hexvane.aetherhaven.poi.marker.PoiMarkerEntity;
import com.hexvane.aetherhaven.poi.marker.PoiMarkerPlacementService;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.ui.PoiMarkerConfigPage;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** POI placement mode: open config GUI on a plot block. */
public final class PoiToolPlacementActions {
    private PoiToolPlacementActions() {}

    public static void handleSecondary(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull InteractionContext context
    ) {
        if (!PoiToolInteractions.hasPoiToolPermission(playerRef, commandBuffer)) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        PoiToolInteractions.ensureState(playerRef, commandBuffer);
        PoiToolPlayerComponent state = commandBuffer.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (state == null || state.getMode() != PoiToolMode.PoiPlacement) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        Store<EntityStore> store = commandBuffer.getStore();
        @Nullable
        Vector3i targetBlock = PoiToolSpawnMarkerActions.resolveTargetBlock(playerRef, store, context);
        if (targetBlock == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownContainingBlock(world.getName(), targetBlock.x(), targetBlock.z());
        if (town == null) {
            PoiToolInteractions.send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noTownPlot"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        PlotInstance plot = town.findCompletePlotContaining(targetBlock.x(), targetBlock.y(), targetBlock.z());
        if (plot == null) {
            PoiToolInteractions.send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noTownPlot"));
            context.getState().state = InteractionState.Failed;
            return;
        }
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null || pr == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        if (player.getPageManager().getCustomPage() != null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        state.setPendingPlacement(targetBlock, town.getTownId(), plot.getPlotId());
        player.getPageManager().openCustomPage(playerRef, store, new PoiMarkerConfigPage(pr));
        context.getState().state = InteractionState.Finished;
    }

    public static boolean tryRemoveMarker(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull InteractionContext context,
        @Nonnull Store<EntityStore> store,
        @Nullable Vector3i targetBlock
    ) {
        @Nullable
        Ref<EntityStore> markerRef =
            MarkerEntityProximity.resolveTarget(store, context, PoiMarkerEntity.getComponentType(), targetBlock, 2.0);
        if (markerRef == null || !markerRef.isValid()) {
            return false;
        }
        PoiMarkerDataComponent data = store.getComponent(markerRef, PoiMarkerDataComponent.getComponentType());
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin != null && data != null) {
            PoiMarkerPlacementService.unregisterLinkedPoi(world, plugin, data);
        }
        commandBuffer.removeEntity(markerRef, RemoveReason.REMOVE);
        PoiToolPlayerComponent toolState = store.getComponent(playerRef, PoiToolPlayerComponent.getComponentType());
        if (toolState != null && data != null && data.getPoiRegistryId() != null && data.getPoiRegistryId().equals(toolState.getSelectedPoiId())) {
            toolState.setSelectedPoiId(null);
        }
        PoiToolInteractions.send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.removed"));
        return true;
    }

    public static boolean tryRemoveNearestRegistryPoi(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull World world,
        @Nonnull Vector3i targetBlock,
        @Nullable PoiToolPlayerComponent toolState
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return false;
        }
        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry nearest = PoiToolInteractions.findNearestPoi(reg, targetBlock);
        if (nearest == null) {
            return false;
        }
        reg.unregister(nearest.getId());
        if (toolState != null && nearest.getId().equals(toolState.getSelectedPoiId())) {
            toolState.setSelectedPoiId(null);
        }
        PoiToolInteractions.send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.removed"));
        return true;
    }
}
