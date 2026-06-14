package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.EntityStatsUpdate;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Keeps the vanilla mana HUD visible while a building staff is held. Item JSON sets
 * {@code DisplayEntityStatsHUD: ["Mana"]} (same pattern as crossbows + {@code Ammo}). Hotbar selection also
 * queues a self {@link EntityStatsUpdate} so the client shows mana immediately without opening inventory.
 */
public final class BuildingStaffManaHudSupport {
    private BuildingStaffManaHudSupport() {}

    public static void syncManaHudForHeldStaff(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Player player,
        @Nonnull PlayerRef playerRefComponent
    ) {
        EntityStatMap map = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (map != null) {
            map.getStatModifiersManager().recalculateEntityStatModifiers(playerRef, map, commandBuffer);
        }
        player.getHudManager().showHudComponents(playerRefComponent, HudComponent.Mana);
        if (map != null) {
            queueSelfEntityStatsUpdate(playerRef, store, map);
        }
    }

    private static void queueSelfEntityStatsUpdate(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull EntityStatMap map
    ) {
        EntityTrackerSystems.Visible visible = store.getComponent(playerRef, EntityTrackerSystems.Visible.getComponentType());
        if (visible == null) {
            return;
        }
        EntityTrackerSystems.EntityViewer selfViewer = visible.visibleTo.get(playerRef);
        if (selfViewer == null || visible.newlyVisibleTo.containsKey(playerRef)) {
            return;
        }
        selfViewer.queueUpdate(playerRef, new EntityStatsUpdate(map.createInitUpdate(true)));
    }
}
