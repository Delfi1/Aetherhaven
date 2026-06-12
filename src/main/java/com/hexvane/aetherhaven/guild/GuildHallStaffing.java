package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.town.ResidentNpcRecord;
import com.hexvane.aetherhaven.town.TownRecord;
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

    /**
     * True when the guild master is assigned to this hall plot (management block), regardless of schedule or whether
     * their entity is loaded. Uses persisted {@link ResidentNpcRecord#getJobPlotId()} so adventurers stay when Lyra is
     * off duty; falls back to a loaded entity with matching {@link TownVillagerBinding#getJobPlotId()}.
     */
    public static boolean hasGuildMasterAssigned(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID townId,
        @Nonnull UUID guildHallPlotId
    ) {
        if (hasGuildMasterAssignedInRegistry(town, guildHallPlotId)) {
            return true;
        }
        return findGuildMasterEntityUuid(store, townId, guildHallPlotId) != null;
    }

    private static boolean hasGuildMasterAssignedInRegistry(
        @Nonnull TownRecord town,
        @Nonnull UUID guildHallPlotId
    ) {
        for (ResidentNpcRecord record : town.getResidentNpcRecords()) {
            if (!AetherhavenConstants.GUILD_MASTER_NPC_ROLE_ID.equalsIgnoreCase(record.getNpcRoleId().trim())) {
                continue;
            }
            if (!TownVillagerBinding.KIND_GUILD_MASTER.equals(record.getKind())) {
                continue;
            }
            UUID jobPlotId = record.getJobPlotId();
            if (jobPlotId != null && jobPlotId.equals(guildHallPlotId)) {
                return true;
            }
        }
        return false;
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
                    UUID jobPlot = b.getJobPlotId();
                    if (jobPlot == null || !jobPlot.equals(guildHallPlotId)) {
                        continue;
                    }
                    if (!TownVillagerBinding.KIND_GUILD_MASTER.equals(b.getKind())) {
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
