package com.hexvane.aetherhaven.inn;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.PlotInstanceState;
import com.hexvane.aetherhaven.town.ResidentNpcRecord;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves which inn plot hosts visitor spawns (innkeeper workplace assignment, not first-built inn). */
public final class InnPlotResolver {
    private InnPlotResolver() {}

    /**
     * Inn plot where morning visitors and bell respawns should occur. Uses persisted innkeeper {@link
     * ResidentNpcRecord#getJobPlotId()} and loaded {@link TownVillagerBinding#getJobPlotId()} so assignment survives
     * unloaded chunks; falls back to the first complete inn when no innkeeper workplace is known.
     */
    @Nullable
    public static PlotInstance resolveInnPlotForVisitors(
        @Nonnull TownRecord town,
        @Nonnull ConstructionCatalog constructionCatalog,
        @Nullable Store<EntityStore> store
    ) {
        UUID jobPlotId = resolveInnkeeperJobPlotId(town, store);
        if (jobPlotId != null) {
            PlotInstance plot = town.findPlotById(jobPlotId);
            if (isCompleteInnPlot(plot, constructionCatalog)) {
                return plot;
            }
        }
        return town.findCompletePlotWithConstruction(constructionCatalog, AetherhavenConstants.CONSTRUCTION_PLOT_INN);
    }

    @Nullable
    public static ConstructionDefinition resolveInnDefinition(
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull PlotInstance innPlot
    ) {
        return plugin.getConstructionCatalog().get(innPlot.getConstructionId());
    }

    private static boolean isCompleteInnPlot(
        @Nullable PlotInstance plot,
        @Nonnull ConstructionCatalog constructionCatalog
    ) {
        if (plot == null || plot.getState() != PlotInstanceState.COMPLETE) {
            return false;
        }
        return AetherhavenConstants.CONSTRUCTION_PLOT_INN.equals(
            constructionCatalog.resolveGameplayConstructionId(plot.getConstructionId())
        );
    }

    @Nullable
    private static UUID resolveInnkeeperJobPlotId(@Nonnull TownRecord town, @Nullable Store<EntityStore> store) {
        for (ResidentNpcRecord record : town.getResidentNpcRecords()) {
            if (!AetherhavenConstants.INNKEEPER_NPC_ROLE_ID.equalsIgnoreCase(record.getNpcRoleId().trim())) {
                continue;
            }
            if (!TownVillagerBinding.KIND_INNKEEPER.equals(record.getKind())) {
                continue;
            }
            UUID jobPlotId = record.getJobPlotId();
            if (jobPlotId != null) {
                return jobPlotId;
            }
        }
        if (store != null) {
            UUID fromBinding = findInnkeeperJobPlotInStore(store, town.getTownId());
            if (fromBinding != null) {
                return fromBinding;
            }
        }
        UUID innkeeperUuid = town.getInnkeeperEntityUuid();
        if (innkeeperUuid != null && store != null) {
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(innkeeperUuid);
            if (ref != null && ref.isValid()) {
                TownVillagerBinding b = store.getComponent(ref, TownVillagerBinding.getComponentType());
                if (b != null
                    && town.getTownId().equals(b.getTownId())
                    && TownVillagerBinding.KIND_INNKEEPER.equals(b.getKind())) {
                    return b.getJobPlotId();
                }
            }
        }
        return null;
    }

    @Nullable
    private static UUID findInnkeeperJobPlotInStore(@Nonnull Store<EntityStore> store, @Nonnull UUID townId) {
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
                    if (!TownVillagerBinding.KIND_INNKEEPER.equals(b.getKind())) {
                        continue;
                    }
                    UUID jobPlot = b.getJobPlotId();
                    if (jobPlot != null) {
                        found[0] = jobPlot;
                        return;
                    }
                }
            }
        );
        return found[0];
    }
}
