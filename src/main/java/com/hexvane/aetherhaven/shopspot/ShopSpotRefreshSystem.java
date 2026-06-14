package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Syncs floating display props when open/closed changes; does not reroll NPC stock (see {@link ShopSpotDailyRerollService}). */
public final class ShopSpotRefreshSystem {
    private ShopSpotRefreshSystem() {}

    public static void onGameMinute(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull WorldTimeResource wtr
    ) {
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        var tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        for (ShopSpotRecord record : registry.allRecords()) {
            TownRecord town = tm.getTown(record.getTownId());
            if (town != null) {
                ShopSpotDisplayService.syncDisplay(world, store, plugin, registry, record, town);
            }
        }
    }
}
