package com.hexvane.aetherhaven.poi.tool;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** POI remove mode: secondary click removes marker-backed or registry POIs near the target block. */
public final class PoiToolRemoveActions {
    private PoiToolRemoveActions() {}

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
        if (state == null || state.getMode() != PoiToolMode.PoiRemove) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        Store<EntityStore> store = commandBuffer.getStore();
        @Nullable
        Vector3i targetBlock = PoiToolSpawnMarkerActions.resolveTargetBlock(playerRef, store, context);
        if (PoiToolPlacementActions.tryRemoveMarker(playerRef, commandBuffer, world, context, store, targetBlock)) {
            context.getState().state = InteractionState.Finished;
            return;
        }
        if (targetBlock != null && PoiToolPlacementActions.tryRemoveNearestRegistryPoi(playerRef, commandBuffer, world, targetBlock, state)) {
            context.getState().state = InteractionState.Finished;
            return;
        }
        PoiToolInteractions.send(playerRef, commandBuffer, Message.translation("aetherhaven_world_debug.aetherhaven.poi.noPoiInRange"));
        context.getState().state = InteractionState.Finished;
    }
}
