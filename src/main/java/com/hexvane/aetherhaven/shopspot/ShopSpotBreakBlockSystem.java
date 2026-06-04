package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class ShopSpotBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final AetherhavenPlugin plugin;

    public ShopSpotBreakBlockSystem(@Nonnull AetherhavenPlugin plugin) {
        super(BreakBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull BreakBlockEvent event
    ) {
        if (event.isCancelled()) {
            return;
        }
        if (!AetherhavenConstants.SHOP_SPOT_BLOCK_TYPE_ID.equals(event.getBlockType().getId())) {
            return;
        }
        Vector3i pos = event.getTargetBlock();
        World world = store.getExternalData().getWorld();
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = registry.getAtBlock(pos.x(), pos.y(), pos.z());
        if (record == null) {
            UUID spotId = ShopSpotBlockUtil.spotIdAt(world, pos);
            if (spotId != null) {
                record = registry.get(spotId);
            }
        }

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        boolean creative = player != null && player.getGameMode() == GameMode.Creative;
        if (creative) {
            ShopSpotInteractionCleanup.healLegacyQuantityOverlay(playerRef, commandBuffer);
            if (record != null) {
                ShopSpotDisplayService.removeDisplay(world, store, commandBuffer, plugin, registry, record);
                registry.remove(record.getSpotId());
                ShopSpotPersistence.save(world, plugin, registry);
            }
            return;
        }

        if (record != null) {
            event.setCancelled(true);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
