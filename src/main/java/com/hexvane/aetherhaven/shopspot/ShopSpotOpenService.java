package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Shops are open during in-game daylight and closed at night. Stock (or a player listing) is required to buy; an empty
 * stall during the day is shown as sold out in the HUD, not as closed.
 */
public final class ShopSpotOpenService {
    /** Scaled day band aligned with {@link WorldTimeResource} daylight (approx. sunrise through sunset). */
    private static final double DAY_START = 0.25;
    private static final double DAY_END = 0.75;

    private ShopSpotOpenService() {}

    public static boolean isGameDay(@Nonnull Store<EntityStore> store) {
        WorldTimeResource wtr = store.getResource(WorldTimeResource.getResourceType());
        return wtr != null && isGameDay(wtr);
    }

    public static boolean isGameDay(@Nonnull WorldTimeResource wtr) {
        return wtr.isScaledDayTimeWithinRange(DAY_START, DAY_END);
    }

    /** True when the shop can accept purchases (daytime and in stock). */
    public static boolean isOpen(
        @Nonnull ShopSpotRecord record,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull Store<EntityStore> store
    ) {
        return isGameDay(store) && record.hasStock();
    }
}
