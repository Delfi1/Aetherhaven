package com.hexvane.aetherhaven.quest;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.plot.PlotTokenInventory;
import com.hexvane.aetherhaven.quest.data.QuestDefinition;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Grants the building's plot token item when a quest with {@link QuestDefinition#grantPlotTokenConstructionId()} starts. */
public final class QuestPlotTokenOnStart {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private QuestPlotTokenOnStart() {}

    public static void grantIfConfigured(
        @Nullable AetherhavenPlugin plugin,
        @Nullable QuestDefinition def,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        if (plugin == null || def == null) {
            return;
        }
        String cid = def.grantPlotTokenConstructionId();
        if (cid == null || cid.isBlank()) {
            return;
        }
        ConstructionDefinition cdef = plugin.getConstructionCatalog().get(cid.trim());
        if (cdef == null) {
            LOGGER.atWarning().log(
                "Unknown construction id for grantPlotTokenConstructionId: %s (quest %s)",
                cid,
                def.idOrEmpty()
            );
            return;
        }
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        PlotTokenInventory.giveToPlayer(player, cdef.getId(), 1, cdef.getDisplayName());
    }
}
