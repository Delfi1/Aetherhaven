package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class ShopSpotBootstrap {
    private ShopSpotBootstrap() {}

    public static void reconcileAfterWorldLoad(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        world.execute(
            () -> {
                ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
                Store<EntityStore> store = world.getEntityStore().getStore();
                var tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
                for (ShopSpotRecord record : registry.allRecords()) {
                    if (!ShopSpotDisplayService.isSpotChunkLoaded(world, record)) {
                        continue;
                    }
                    ShopSpotDisplayService.purgeOrphanDisplayEntities(world, store, registry, record);
                    record.setDisplayEntityUuid(null);
                    record.setListingDisplaySignature(null);
                    TownRecord town = tm.getTown(record.getTownId());
                    if (town != null) {
                        ShopSpotDisplayService.syncDisplay(world, store, plugin, registry, record, town);
                    }
                }
                ShopSpotPersistence.save(world, plugin, registry);
            }
        );
    }
}
