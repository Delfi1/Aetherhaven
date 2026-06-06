package com.hexvane.aetherhaven.town;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.guild.GuildHallAdventurerPoolService;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiInteractionKind;
import com.hexvane.aetherhaven.poi.PoiRegistry;
import com.hexvane.aetherhaven.production.ProductionWorkplaceKinds;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hexvane.aetherhaven.villager.data.VillagerDefinition;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Management UI: assign a villager whose job matches a completed production workplace plot. */
public final class WorkplacePlotAssignment {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private WorkplacePlotAssignment() {}

    /**
     * Clears the villager currently assigned to work at this plot (management block → Unassigned).
     *
     * @return null on success, or a short English reason for the player
     */
    @Nullable
    public static String tryClearWorker(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull UUID workplacePlotId,
        @Nonnull Store<EntityStore> store
    ) {
        PlotInstance plot = town.findPlotById(workplacePlotId);
        if (plot == null || plot.getState() != PlotInstanceState.COMPLETE) {
            return "Plot is not ready.";
        }
        String gameplayId = plugin.getConstructionCatalog().resolveGameplayConstructionId(plot.getConstructionId());
        String residentKind = ProductionWorkplaceKinds.residentBindingKindForGameplayConstruction(gameplayId);
        if (residentKind == null) {
            return "This building is not a workplace.";
        }

        Ref<EntityStore> npcRef = findWorkerOnPlot(store, town.getTownId(), workplacePlotId, residentKind);
        if (npcRef == null || !npcRef.isValid()) {
            if (AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL.equals(gameplayId)) {
                GuildHallAdventurerPoolService.clearAdventurersForHall(world, plugin, town, tm, store, plot);
            }
            return null;
        }

        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        String roleId = npc != null && npc.getRoleName() != null ? npc.getRoleName().trim() : null;
        String visitorKind = visitorKindForResidentKind(residentKind);
        store.putComponent(
            npcRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), visitorKind, null, null)
        );
        if (roleId != null && !roleId.isBlank()) {
            town.getInnVisitorPoolExcludedRoleIds().remove(roleId);
            UUIDComponent uuidComp = store.getComponent(npcRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                String sid = uuidComp.getUuid().toString();
                if (!town.getInnPoolNpcIds().contains(sid)) {
                    town.getInnPoolNpcIds().add(sid);
                }
                ResidentRegistryService.upsert(town, tm, roleId, visitorKind, null, uuidComp.getUuid());
            }
        }
        if (AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL.equals(gameplayId)) {
            GuildHallAdventurerPoolService.clearAdventurersForHall(world, plugin, town, tm, store, plot);
        }
        tm.updateTown(town);
        return null;
    }

    @Nonnull
    private static String visitorKindForResidentKind(@Nonnull String residentKind) {
        return switch (residentKind) {
            case TownVillagerBinding.KIND_GUILD_MASTER -> TownVillagerBinding.KIND_VISITOR_GUILD_MASTER;
            case TownVillagerBinding.KIND_INNKEEPER -> TownVillagerBinding.KIND_VISITOR_MERCHANT;
            case TownVillagerBinding.KIND_FARMER -> TownVillagerBinding.KIND_VISITOR_FARMER;
            case TownVillagerBinding.KIND_BLACKSMITH -> TownVillagerBinding.KIND_VISITOR_BLACKSMITH;
            case TownVillagerBinding.KIND_PRIESTESS -> TownVillagerBinding.KIND_VISITOR_PRIESTESS;
            case TownVillagerBinding.KIND_MINER -> TownVillagerBinding.KIND_VISITOR_MINER;
            case TownVillagerBinding.KIND_LOGGER -> TownVillagerBinding.KIND_VISITOR_LOGGER;
            case TownVillagerBinding.KIND_RANCHER -> TownVillagerBinding.KIND_VISITOR_RANCHER;
            case TownVillagerBinding.KIND_MERCHANT -> TownVillagerBinding.KIND_VISITOR_MERCHANT;
            default -> TownVillagerBinding.KIND_TOWNSFOLK;
        };
    }

    @Nullable
    private static Ref<EntityStore> findWorkerOnPlot(
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID townId,
        @Nonnull UUID workplacePlotId,
        @Nonnull String residentKind
    ) {
        AtomicReference<Ref<EntityStore>> found = new AtomicReference<>();
        Query<EntityStore> q = Query.and(TownVillagerBinding.getComponentType(), UUIDComponent.getComponentType());
        store.forEachChunk(
            q,
            (com.hypixel.hytale.component.ArchetypeChunk<EntityStore> chunk, com.hypixel.hytale.component.CommandBuffer<EntityStore> commandBuffer) -> {
                if (found.get() != null) {
                    return;
                }
                for (int i = 0; i < chunk.size(); i++) {
                    TownVillagerBinding b = chunk.getComponent(i, TownVillagerBinding.getComponentType());
                    if (b == null || !townId.equals(b.getTownId()) || !residentKind.equals(b.getKind())) {
                        continue;
                    }
                    UUID jobPlot = b.getJobPlotId();
                    if (jobPlot == null || !jobPlot.equals(workplacePlotId)) {
                        continue;
                    }
                    found.set(chunk.getReferenceTo(i));
                    return;
                }
            }
        );
        return found.get();
    }

    /**
     * @return null on success, or a short English reason for the player
     */
    @Nullable
    public static String tryAssignWorker(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull UUID workplacePlotId,
        @Nonnull UUID npcEntityUuid,
        @Nonnull Store<EntityStore> store
    ) {
        PlotInstance plot = town.findPlotById(workplacePlotId);
        if (plot == null || plot.getState() != PlotInstanceState.COMPLETE) {
            return "Plot is not ready.";
        }
        String gameplayId = plugin.getConstructionCatalog().resolveGameplayConstructionId(plot.getConstructionId());
        if (!ProductionWorkplaceKinds.supportsWorkerAssignment(gameplayId)) {
            return "This building is not a workplace.";
        }
        String kind = ProductionWorkplaceKinds.residentBindingKindForGameplayConstruction(gameplayId);
        if (kind == null) {
            return "No job role maps to this workplace.";
        }

        Ref<EntityStore> npcRef = store.getExternalData().getRefFromUUID(npcEntityUuid);
        if (npcRef == null || !npcRef.isValid()) {
            return "Villager not found.";
        }
        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc == null || npc.getRoleName() == null || npc.getRoleName().isBlank()) {
            return "Invalid villager.";
        }
        VillagerDefinition vdef = plugin.getVillagerDefinitionCatalog().byNpcRoleId(npc.getRoleName().trim());
        if (vdef == null) {
            return "Unknown villager role.";
        }
        String workId = vdef.getWorkConstructionId();
        if (workId == null || !workId.equals(gameplayId)) {
            return "That villager does not work at this building type.";
        }

        TownVillagerBinding binding = store.getComponent(npcRef, TownVillagerBinding.getComponentType());
        if (binding == null || !town.getTownId().equals(binding.getTownId())) {
            return "That villager is not in this town.";
        }

        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry work = findWorkPoi(reg, town.getTownId(), workplacePlotId);
        if (work == null) {
            LOGGER.atWarning().log("No WORK POI for workplace plot %s", workplacePlotId);
            return "No work station found on this plot.";
        }

        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc != null) {
            Vector3d p = tc.getPosition();
            p.x = work.getX() + 0.5;
            p.y = work.getY() + 0.02;
            p.z = work.getZ() + 0.5;
            store.putComponent(npcRef, TransformComponent.getComponentType(), tc);
        }

        UUIDComponent uuidComp = store.getComponent(npcRef, UUIDComponent.getComponentType());
        if (uuidComp != null) {
            UUID u = uuidComp.getUuid();
            town.getInnPoolNpcIds()
                .removeIf(
                    s -> {
                        try {
                            return u.equals(UUID.fromString(s.trim()));
                        } catch (Exception e) {
                            return false;
                        }
                    }
                );
        }

        store.putComponent(
            npcRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), kind, workplacePlotId, workplacePlotId)
        );
        town.addInnVisitorPoolExcludedRoleId(npc.getRoleName().trim());
        if (uuidComp != null) {
            ResidentRegistryService.upsert(town, tm, npc.getRoleName().trim(), kind, workplacePlotId, uuidComp.getUuid());
        }
        tm.updateTown(town);
        if (AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL.equals(gameplayId)) {
            GuildHallAdventurerPoolService.tryFillAfterGuildMasterAssigned(
                world,
                plugin,
                town,
                tm,
                workplacePlotId,
                store
            );
        }
        return null;
    }

    @Nullable
    private static PoiEntry findWorkPoi(@Nonnull PoiRegistry reg, @Nonnull UUID townId, @Nonnull UUID plotId) {
        for (PoiEntry e : reg.listByTown(townId)) {
            if (plotId.equals(e.getPlotId()) && isWorkPoi(e)) {
                return e;
            }
        }
        return null;
    }

    private static boolean isWorkPoi(@Nonnull PoiEntry poi) {
        return poi.getTags().contains("WORK") || poi.getInteractionKind() == PoiInteractionKind.WORK_SURFACE;
    }
}
