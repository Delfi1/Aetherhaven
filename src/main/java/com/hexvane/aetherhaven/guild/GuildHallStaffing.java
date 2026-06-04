package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Whether a guild hall plot has a guild master assigned via the management block workplace dropdown. */
public final class GuildHallStaffing {
    private GuildHallStaffing() {}

    public static boolean hasGuildMasterAssigned(
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID townId,
        @Nonnull UUID guildHallPlotId
    ) {
        return findGuildMasterEntityUuid(store, townId, guildHallPlotId) != null;
    }

    @Nullable
    public static UUID findGuildMasterEntityUuid(
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID townId,
        @Nonnull UUID guildHallPlotId
    ) {
        final UUID[] found = new UUID[1];
        Query<EntityStore> q = Query.and(TownVillagerBinding.getComponentType(), UUIDComponent.getComponentType());
        store.forEachChunk(
            q,
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                if (found[0] != null) {
                    return;
                }
                for (int i = 0; i < chunk.size(); i++) {
                    TownVillagerBinding b = chunk.getComponent(i, TownVillagerBinding.getComponentType());
                    if (b == null || !townId.equals(b.getTownId())) {
                        continue;
                    }
                    if (!TownVillagerBinding.KIND_GUILD_MASTER.equals(b.getKind())) {
                        continue;
                    }
                    UUID jobPlot = b.getJobPlotId();
                    if (jobPlot == null || !jobPlot.equals(guildHallPlotId)) {
                        continue;
                    }
                    UUIDComponent uc = chunk.getComponent(i, UUIDComponent.getComponentType());
                    if (uc != null) {
                        found[0] = uc.getUuid();
                        return;
                    }
                }
            }
        );
        return found[0];
    }
}
