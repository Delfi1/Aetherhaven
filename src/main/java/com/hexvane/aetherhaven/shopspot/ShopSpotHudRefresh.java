package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Pushes shop HUD updates immediately (e.g. after a purchase). */
public final class ShopSpotHudRefresh {
    private ShopSpotHudRefresh() {}

    public static void refreshFocused(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        ShopSpotPlayerComponent st = store.getComponent(playerRef, ShopSpotPlayerComponent.getComponentType());
        if (player == null || pr == null || st == null) {
            return;
        }
        UUID focusedId = st.getFocusedSpotId();
        if (focusedId == null) {
            return;
        }
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = registry.get(focusedId);
        if (record == null) {
            return;
        }
        TownRecord town = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin).getTown(record.getTownId());
        if (town == null) {
            return;
        }
        boolean gameDay = ShopSpotOpenService.isGameDay(store);
        ShopSpotLookAtSystem.invalidateSignature(pr.getUuid());
        if (!ShopSpotHudSupport.isActive(player)) {
            ShopSpotHudSupport.obtainHud(player, pr);
        }
        ShopSpotHudSupport.obtainHud(player, pr).refresh(world, record, town, gameDay, pr.getUuid(), plugin);
    }

    public static void refreshAtBlock(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull Vector3i targetBlock
    ) {
        ShopSpotRecord record = ShopSpotBlockInteractSupport.resolveRecord(world, plugin, targetBlock);
        if (record == null) {
            return;
        }
        ShopSpotPlayerComponent st = store.getComponent(playerRef, ShopSpotPlayerComponent.getComponentType());
        if (st != null) {
            st.setFocusedSpotId(record.getSpotId());
        }
        refreshFocused(playerRef, store, world, plugin);
    }
}
