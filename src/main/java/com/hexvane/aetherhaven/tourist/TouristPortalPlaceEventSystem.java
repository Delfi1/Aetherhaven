package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.plotcreator.PlotCreatorSessions;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.PlotInstanceState;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class TouristPortalPlaceEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final AetherhavenPlugin plugin;

    public TouristPortalPlaceEventSystem(@Nonnull AetherhavenPlugin plugin) {
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
        if (hand == null || hand.isEmpty() || !AetherhavenConstants.TOURIST_PORTAL_ITEM_ID.equals(hand.getItemId())) {
            return;
        }
        Vector3i pos = new Vector3i(event.getTargetBlock());
        World world = store.getExternalData().getWorld();
        var playerRef = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(playerRef, Player.getComponentType());
        UUIDComponent uc = store.getComponent(playerRef, UUIDComponent.getComponentType());
        boolean creative = player != null && player.getGameMode() == GameMode.Creative;
        boolean plotCreatorBounds = false;
        if (uc != null) {
            var session = PlotCreatorSessions.get(uc.getUuid());
            plotCreatorBounds = session != null && session.getDraft().isInsideBounds(pos);
        }

        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownContainingBlock(world.getName(), pos.x(), pos.z());
        PlotInstance plot = town != null ? town.findCompletePlotContaining(pos.x(), pos.y(), pos.z()) : null;
        boolean inCompletePlot = plot != null && plot.getState() == PlotInstanceState.COMPLETE;

        if (!inCompletePlot && !plotCreatorBounds && !creative) {
            event.setCancelled(true);
            return;
        }

        UUID portalId = UUID.randomUUID();
        String townId = town != null ? town.getTownId().toString() : "";
        String plotId = inCompletePlot && plot != null ? plot.getPlotId().toString() : "";
        TouristPortalBlock block = new TouristPortalBlock(portalId.toString(), townId, plotId, false);

        if (!TouristPortalBlockUtil.writeBlockComponent(world, pos, block)) {
            world.execute(() -> finishDeferred(world, pos, portalId, town, plot, inCompletePlot));
            return;
        }
        finish(world, pos, portalId, town, plot, inCompletePlot);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    private static void finishDeferred(
        @Nonnull World world,
        @Nonnull Vector3i pos,
        @Nonnull UUID portalId,
        @Nullable TownRecord town,
        @Nullable PlotInstance plot,
        boolean inCompletePlot
    ) {
        TouristPortalBlockUtil.writeBlockComponent(world, pos, new TouristPortalBlock(
            portalId.toString(),
            town != null ? town.getTownId().toString() : "",
            inCompletePlot && plot != null ? plot.getPlotId().toString() : "",
            false
        ));
        finish(world, pos, portalId, town, plot, inCompletePlot);
    }

    private static void finish(
        @Nonnull World world,
        @Nonnull Vector3i pos,
        @Nonnull UUID portalId,
        @Nullable TownRecord town,
        @Nullable PlotInstance plot,
        boolean inCompletePlot
    ) {
        if (!inCompletePlot || town == null || plot == null) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        TouristPortalRegistry registry = AetherhavenWorldRegistries.getOrCreateTouristPortalRegistry(world, plugin);
        TouristPortalRecord existing = registry.getAtBlock(pos.x, pos.y, pos.z);
        if (existing != null) {
            registry.remove(existing.getPortalId());
        }
        TouristPortalRecord record = new TouristPortalRecord();
        record.setPortalId(portalId);
        record.setWorldName(world.getName());
        record.setBlockPosition(pos);
        record.setTownId(town.getTownId());
        record.setPlotId(plot.getPlotId());
        registry.put(record);
        TouristPortalBlockUtil.syncConfigToBlock(world, pos, record);
        TouristPortalPersistence.save(world, plugin, registry);
    }
}
