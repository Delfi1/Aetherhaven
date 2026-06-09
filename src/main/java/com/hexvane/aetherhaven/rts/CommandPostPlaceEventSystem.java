package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.PlotInstanceState;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class CommandPostPlaceEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final String MSG = "aetherhaven_rts.aetherhaven.rts";

    private final AetherhavenPlugin plugin;

    public CommandPostPlaceEventSystem(@Nonnull AetherhavenPlugin plugin) {
        super(PlaceBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull PlaceBlockEvent event
    ) {
        ItemStack hand = event.getItemInHand();
        if (hand == null || hand.isEmpty() || !AetherhavenConstants.COMMAND_POST_ITEM_ID.equals(hand.getItemId())) {
            return;
        }
        Vector3i pos = new Vector3i(event.getTargetBlock());
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownContainingBlock(world.getName(), pos.x(), pos.z());
        if (town == null) {
            event.setCancelled(true);
            send(archetypeChunk, commandBuffer, index, Message.translation(MSG + ".errorNotInTown"));
            return;
        }
        PlotInstance plot = town.findCompletePlotContaining(pos.x(), pos.y(), pos.z());
        if (plot == null || plot.getState() != PlotInstanceState.COMPLETE) {
            event.setCancelled(true);
            send(archetypeChunk, commandBuffer, index, Message.translation(MSG + ".errorNotInPlot"));
            return;
        }
        if (!CommandPostBlockUtil.writeTownId(world, pos, town.getTownId().toString())) {
            world.execute(() -> CommandPostBlockUtil.writeTownId(world, pos, town.getTownId().toString()));
        }
    }

    private void send(
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        int index,
        @Nonnull Message msg
    ) {
        PlayerRef pr = commandBuffer.getComponent(chunk.getReferenceTo(index), PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(msg);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
