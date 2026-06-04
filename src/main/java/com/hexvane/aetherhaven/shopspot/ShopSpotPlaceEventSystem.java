package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.PlotInstanceState;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.ui.ShopSpotConfigPage;
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
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class ShopSpotPlaceEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private static final String MSG = "aetherhaven_shop.aetherhaven.shop";

    private final AetherhavenPlugin plugin;

    public ShopSpotPlaceEventSystem(@Nonnull AetherhavenPlugin plugin) {
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
        if (hand == null || hand.isEmpty() || !AetherhavenConstants.SHOP_SPOT_ITEM_ID.equals(hand.getItemId())) {
            return;
        }
        Vector3i pos = new Vector3i(event.getTargetBlock());
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownContainingBlock(world.getName(), pos.x(), pos.z());
        if (town == null) {
            event.setCancelled(true);
            send(archetypeChunk, commandBuffer, index, Message.translation(MSG + ".notInTown"));
            return;
        }
        PlotInstance plot = town.findCompletePlotContaining(pos.x(), pos.y(), pos.z());
        if (plot == null || plot.getState() != PlotInstanceState.COMPLETE) {
            event.setCancelled(true);
            send(archetypeChunk, commandBuffer, index, Message.translation(MSG + ".notInPlot"));
            return;
        }
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr == null) {
            event.setCancelled(true);
            return;
        }
        UUID spotId = UUID.randomUUID();
        if (!ShopSpotBlockUtil.writeBlockComponent(world, pos, spotId.toString(), town.getTownId().toString(), plot.getPlotId().toString())) {
            world.execute(() ->
                finishPlacementDeferred(world, pos, spotId, town, plot, playerRef, pr)
            );
            return;
        }
        finishPlacement(world, pos, spotId, town, plot, playerRef, pr, commandBuffer);
    }

    private void finishPlacement(
        @Nonnull World world,
        @Nonnull Vector3i pos,
        @Nonnull UUID spotId,
        @Nonnull TownRecord town,
        @Nonnull PlotInstance plot,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComp,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = new ShopSpotRecord();
        record.setSpotId(spotId);
        record.setWorldName(world.getName());
        record.setBlockPosition(pos);
        record.setTownId(town.getTownId());
        record.setPlotId(plot.getPlotId());
        record.setLootTableId(AetherhavenConstants.SHOP_LOOT_TABLE_GIFTS);
        record.setDisplayYawRadians(ShopSpotDisplayRotation.yawFromBlockAt(world, pos));
        registry.put(record);
        ShopSpotPersistence.save(world, plugin, registry);

        ShopSpotPlayerComponent st = commandBuffer.getComponent(playerRef, ShopSpotPlayerComponent.getComponentType());
        if (st == null) {
            st = new ShopSpotPlayerComponent();
        }
        st.setPendingPlacement(spotId, town.getTownId(), plot.getPlotId(), pos);
        commandBuffer.putComponent(playerRef, ShopSpotPlayerComponent.getComponentType(), st);

        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if (player != null && player.getPageManager().getCustomPage() == null) {
            player.getPageManager().openCustomPage(playerRef, commandBuffer.getStore(), new ShopSpotConfigPage(playerRefComp));
        }
    }

    private void finishPlacementDeferred(
        @Nonnull World world,
        @Nonnull Vector3i pos,
        @Nonnull UUID spotId,
        @Nonnull TownRecord town,
        @Nonnull PlotInstance plot,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef playerRefComp
    ) {
        ShopSpotBlockUtil.writeBlockComponent(
            world, pos, spotId.toString(), town.getTownId().toString(), plot.getPlotId().toString()
        );
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (store == null) {
            return;
        }
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = new ShopSpotRecord();
        record.setSpotId(spotId);
        record.setWorldName(world.getName());
        record.setBlockPosition(pos);
        record.setTownId(town.getTownId());
        record.setPlotId(plot.getPlotId());
        record.setLootTableId(AetherhavenConstants.SHOP_LOOT_TABLE_GIFTS);
        record.setDisplayYawRadians(ShopSpotDisplayRotation.yawFromBlockAt(world, pos));
        registry.put(record);
        ShopSpotPersistence.save(world, plugin, registry);

        ShopSpotPlayerComponent st = store.getComponent(playerRef, ShopSpotPlayerComponent.getComponentType());
        if (st == null) {
            st = new ShopSpotPlayerComponent();
        }
        st.setPendingPlacement(spotId, town.getTownId(), plot.getPlotId(), pos);
        store.putComponent(playerRef, ShopSpotPlayerComponent.getComponentType(), st);

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player != null && player.getPageManager().getCustomPage() == null) {
            player.getPageManager().openCustomPage(playerRef, store, new ShopSpotConfigPage(playerRefComp));
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
