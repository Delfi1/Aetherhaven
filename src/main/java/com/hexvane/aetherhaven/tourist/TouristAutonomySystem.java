package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.autonomy.PoiAutonomyVisuals;
import com.hexvane.aetherhaven.autonomy.VillagerDoorUtil;
import com.hexvane.aetherhaven.autonomy.pathnav.PathNavGraphService;
import com.hexvane.aetherhaven.autonomy.pathnav.PathNavTravelWaypoints;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiRegistry;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.NavState;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

public final class TouristAutonomySystem extends EntityTickingSystem<EntityStore> {
    private static final double ARRIVE_HORIZONTAL_SQ = 1.5 * 1.5;
    private static final double RETURN_ARRIVE_HORIZONTAL_SQ = 2.75 * 2.75;
    private static final long TRAVEL_PHASE_MAX_MS = 120_000L;
    private static final int BLOCKED_FAIL_TICKS = 100;
    private static final long VISIT_MIN_MS = 45_000L;
    private static final long VISIT_MAX_MS = 120_000L;
    private static final long POI_USE_MIN_MS = 15_000L;
    private static final long POI_USE_MAX_MS = 35_000L;
    private static final long POI_PICK_MIN_DELAY_MS = 12_000L;
    private static final long POI_PICK_MAX_DELAY_MS = 28_000L;

    @Nonnull
    private final AetherhavenPlugin plugin;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();

    public TouristAutonomySystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            TownsfolkCharacterBinding.getComponentType(),
            TownVillagerBinding.getComponentType(),
            TouristAutonomyState.getComponentType(),
            NPCEntity.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        TownsfolkCharacterBinding tb = chunk.getComponent(index, TownsfolkCharacterBinding.getComponentType());
        TouristAutonomyState autonomy = chunk.getComponent(index, TouristAutonomyState.getComponentType());
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        TownVillagerBinding binding = chunk.getComponent(index, TownVillagerBinding.getComponentType());
        if (tb == null || autonomy == null || npc == null || binding == null) {
            return;
        }
        if (!TownsfolkAssignmentKinds.TOURIST.equals(tb.getAssignmentKind())) {
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        long now = resolveNowMs(store);
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.getTown(binding.getTownId());
        if (town == null) {
            return;
        }
        PoiRegistry poiRegistry = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        ConstructionCatalog catalog = plugin.getConstructionCatalog();

        switch (autonomy.getPhase()) {
            case TouristAutonomyState.PHASE_IDLE ->
                tickIdle(ref, store, commandBuffer, npc, autonomy, now, town, world, catalog);
            case TouristAutonomyState.PHASE_TRAVEL, TouristAutonomyState.PHASE_RETURNING ->
                tickTravel(ref, store, commandBuffer, npc, autonomy, now, town, world, poiRegistry);
            case TouristAutonomyState.PHASE_VISIT ->
                tickPlotVisit(ref, store, commandBuffer, npc, autonomy, now, town, world, poiRegistry, catalog);
            case TouristAutonomyState.PHASE_POI ->
                tickPlotPoi(ref, store, commandBuffer, npc, autonomy, now, poiRegistry);
            default -> {
                autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
                commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            }
        }
    }

    private void tickIdle(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull ConstructionCatalog catalog
    ) {
        if (now < autonomy.getNextDecisionEpochMs()) {
            return;
        }
        Random random = new Random(now ^ ref.hashCode());
        TouristPlotVisit pick =
            TouristDestinationResolver.pickVisitPlot(town, catalog, world, autonomy.getVisitPlotUuid(), random);
        if (pick == null) {
            autonomy.setNextDecisionEpochMs(now + 8000L);
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            clearAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }
        beginTravelToPlot(ref, store, commandBuffer, npc, autonomy, now, town, world, pick);
    }

    /** Immediately pick a plot and start walking (used right after portal spawn). */
    public static void kickInitialVisitOnSpawn(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TouristAutonomyState autonomy,
        @Nonnull TownRecord town,
        @Nonnull World world
    ) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            return;
        }
        ConstructionCatalog catalog = plugin.getConstructionCatalog();
        long now = resolveNowMs(store);
        Random random = new Random(now ^ ref.hashCode() ^ 0x5DEECE66DL);
        TouristPlotVisit pick = TouristDestinationResolver.pickVisitPlot(town, catalog, world, null, random);
        if (pick == null) {
            autonomy.setNextDecisionEpochMs(now + 3000L);
            store.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            return;
        }
        beginTravelToPlotOnStore(ref, store, plugin, npc, autonomy, now, town, world, pick);
        store.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        store.putComponent(ref, NPCEntity.getComponentType(), npc);
        applyAutonomyRoleStateOnStore(ref, npc, store);
    }

    private void beginTravelToPlot(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull TouristPlotVisit plot
    ) {
        beginTravelToPlotOnStore(ref, store, plugin, npc, autonomy, now, town, world, plot);
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
        applyAutonomyRoleState(ref, npc, commandBuffer);
    }

    private static void beginTravelTo(
        @Nonnull TouristAutonomyState autonomy,
        long now,
        double x,
        double y,
        double z,
        @Nonnull UUID destinationId
    ) {
        if (AetherhavenConstants.isTouristPortalReturnPoi(destinationId)) {
            autonomy.setPhase(TouristAutonomyState.PHASE_RETURNING);
        } else {
            autonomy.setPhase(TouristAutonomyState.PHASE_TRAVEL);
        }
        autonomy.setTravelTarget(x, y, z, destinationId);
        autonomy.setTravelStuckTicks(0);
        autonomy.setNextDecisionEpochMs(now + TRAVEL_PHASE_MAX_MS);
    }

    private static void beginTravelToPlotOnStore(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull TouristPlotVisit plot
    ) {
        autonomy.setVisitPlotId(plot.plotId());
        beginTravelTo(autonomy, now, plot.entryX(), plot.entryY(), plot.entryZ(), plot.destinationId());
        routeNpcToTarget(
            ref,
            store,
            plugin,
            npc,
            autonomy,
            town,
            world,
            new Vector3d(plot.entryX(), plot.entryY(), plot.entryZ())
        );
    }

    private void beginTravelToPoi(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull PoiEntry poi
    ) {
        double tx = poi.hasInteractionTarget() ? poi.getInteractionTargetX() + 0.5 : poi.getX() + 0.5;
        double ty = poi.hasInteractionTarget() ? poi.getInteractionTargetY() + 0.02 : poi.getY() + 0.02;
        double tz = poi.hasInteractionTarget() ? poi.getInteractionTargetZ() + 0.5 : poi.getZ() + 0.5;
        beginTravelTo(autonomy, now, tx, ty, tz, poi.getId());
        routeNpcToTarget(ref, store, plugin, npc, autonomy, town, world, new Vector3d(tx, ty, tz));
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
        applyAutonomyRoleState(ref, npc, commandBuffer);
    }

    private static void routeNpcToTarget(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull Vector3d finalTarget
    ) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) {
            AetherhavenWorldRegistries.getOrCreatePathToolRegistry(world, plugin);
            PathNavGraphService.PathNavFindResult navResult =
                AetherhavenWorldRegistries
                    .getOrCreatePathNavGraphService(world)
                    .findRouteResult(town.getTownId(), tc.getPosition(), finalTarget, plugin.getConfig().get());
            var route = navResult.waypoints();
            if (!route.isEmpty()) {
                route =
                    PathNavTravelWaypoints.prepareForSeek(
                        world,
                        tc.getPosition(),
                        route,
                        finalTarget,
                        (int) Math.floor(tc.getPosition().y)
                    );
            }
            if (!route.isEmpty()) {
                autonomy.setTravelWaypoints(route);
                Vector3d first = autonomy.getCurrentTravelWaypoint();
                npc.setLeashPoint(first != null ? first : finalTarget);
            } else {
                autonomy.clearTravelWaypoints();
                npc.setLeashPoint(finalTarget);
            }
        } else {
            autonomy.clearTravelWaypoints();
            npc.setLeashPoint(finalTarget);
        }
    }

    private static boolean supportsAutonomyPoiRoleState(@Nonnull NPCEntity npc) {
        if (npc.getRole() == null) {
            return false;
        }
        return npc.getRole().getStateSupport().getStateHelper().getStateIndex(AetherhavenConstants.NPC_STATE_AUTONOMY_POI) >= 0;
    }

    private void beginReturnToPortal(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world
    ) {
        if (!beginReturnToPortalOnStore(ref, store, plugin, npc, autonomy, now, town, world)) {
            autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            return;
        }
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
        applyAutonomyRoleState(ref, npc, commandBuffer);
    }

    /** Routes a tourist back to their home portal (safe outside tick and from tick). */
    public static boolean beginReturnToPortalOnStore(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world
    ) {
        UUID portalId = autonomy.getHomePortalId();
        if (portalId == null) {
            return false;
        }
        TouristPortalRecord portal =
            AetherhavenWorldRegistries.getOrCreateTouristPortalRegistry(world, plugin).get(portalId);
        if (portal == null) {
            return false;
        }
        Vector3i blockPos = portal.getBlockPosition();
        Vector3d feet = TouristPortalBlockUtil.returnStandPosition(world, blockPos);
        autonomy.clearVisitPlot();
        beginTravelTo(autonomy, now, feet.x, feet.y, feet.z, AetherhavenConstants.TOURIST_PORTAL_RETURN_POI_ID);
        routeNpcToTarget(ref, store, plugin, npc, autonomy, town, world, feet);
        return true;
    }

    private static boolean isReturningTravel(@Nonnull TouristAutonomyState autonomy) {
        return autonomy.getPhase() == TouristAutonomyState.PHASE_RETURNING
            || AetherhavenConstants.isTouristPortalReturnPoi(autonomy.getTargetPoiUuid());
    }

    private void tickTravel(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull PoiRegistry poiRegistry
    ) {
        boolean returning = isReturningTravel(autonomy);
        if (returning) {
            UUID target = autonomy.getTargetPoiUuid();
            if (target == null || !AetherhavenConstants.isTouristPortalReturnPoi(target)) {
                beginReturnToPortal(ref, store, commandBuffer, npc, autonomy, now, town, world);
                return;
            }
        }

        TransformComponent tcEarly = store.getComponent(ref, TransformComponent.getComponentType());
        if (returning && tcEarly != null) {
            TouristPortalRecord portal = resolveHomePortal(world, autonomy);
            if (portal != null
                && TouristPortalBlockUtil.isNearPortalDespawn(world, portal.getBlockPosition(), tcEarly.getPosition())) {
                finishReturnDespawn(ref, store, commandBuffer, autonomy, town, world);
                return;
            }
        }

        if (now >= autonomy.getNextDecisionEpochMs()) {
            failTravel(ref, store, commandBuffer, npc, autonomy, now, town, world);
            return;
        }

        TransformComponent tc = tcEarly != null ? tcEarly : store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }

        Vector3d pos = tc.getPosition();
        Vector3d leash = npc.getLeashPoint();
        double horizSq = (pos.x - leash.x) * (pos.x - leash.x) + (pos.z - leash.z) * (pos.z - leash.z);
        double arriveSq = returning ? RETURN_ARRIVE_HORIZONTAL_SQ : ARRIVE_HORIZONTAL_SQ;

        Vector3d currentWaypoint = autonomy.getCurrentTravelWaypoint();
        if (currentWaypoint != null && horizSq <= arriveSq) {
            if (autonomy.advanceTravelWaypoint()) {
                Vector3d next = autonomy.getCurrentTravelWaypoint();
                if (next != null) {
                    npc.setLeashPoint(next);
                    autonomy.setTravelStuckTicks(0);
                    commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
                    commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
                    applyAutonomyRoleState(ref, npc, commandBuffer);
                    return;
                }
            }
            autonomy.clearTravelWaypoints();
            npc.setLeashPoint(new Vector3d(autonomy.getTargetX(), autonomy.getTargetY(), autonomy.getTargetZ()));
        }

        VillagerDoorUtil.tryOpenDoorsTowardLeash(world, pos, leash, (x, y, z) -> {});

        MotionController mc = npc.getRole() != null ? npc.getRole().getActiveMotionController() : null;
        NavState nav = mc != null ? mc.getNavState() : NavState.INIT;
        if (nav == NavState.ABORTED) {
            failTravel(ref, store, commandBuffer, npc, autonomy, now, town, world);
            return;
        }
        if (nav == NavState.BLOCKED || nav == NavState.DEFER) {
            autonomy.setTravelStuckTicks(autonomy.getTravelStuckTicks() + 1);
            if (autonomy.getTravelStuckTicks() >= BLOCKED_FAIL_TICKS) {
                failTravel(ref, store, commandBuffer, npc, autonomy, now, town, world);
                return;
            }
        } else if (nav == NavState.PROGRESSING || nav == NavState.INIT) {
            autonomy.setTravelStuckTicks(0);
        }

        if (returning) {
            TouristPortalRecord portal = resolveHomePortal(world, autonomy);
            if (portal != null && TouristPortalBlockUtil.isNearPortalDespawn(world, portal.getBlockPosition(), pos)) {
                finishReturnDespawn(ref, store, commandBuffer, autonomy, town, world);
                return;
            }
        }

        if (horizSq <= arriveSq) {
            UUID targetId = autonomy.getTargetPoiUuid();
            if (targetId != null && AetherhavenConstants.isTouristPortalReturnPoi(targetId)) {
                finishReturnDespawn(ref, store, commandBuffer, autonomy, town, world);
                return;
            }

            PoiEntry poi = targetId != null ? poiRegistry.get(targetId) : null;
            UUID visitPlotId = autonomy.getVisitPlotUuid();
            if (poi != null && visitPlotId != null && visitPlotId.equals(poi.getPlotId())) {
                beginPlotPoiUse(ref, store, commandBuffer, npc, autonomy, now, poi);
                return;
            }

            if (visitPlotId != null && TouristPlotVisit.isPlotDestinationId(targetId, visitPlotId)) {
                beginPlotWanderVisit(ref, commandBuffer, npc, autonomy, now, ref.hashCode());
                return;
            }

            autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
            autonomy.setNextDecisionEpochMs(now + 3000L);
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            clearAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }

        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        applyAutonomyRoleState(ref, npc, commandBuffer);
    }

    private void beginPlotWanderVisit(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        int salt
    ) {
        autonomy.setPhase(TouristAutonomyState.PHASE_VISIT);
        autonomy.clearTravelWaypoints();
        autonomy.setTravelStuckTicks(0);
        long dur = VISIT_MIN_MS + Math.abs((now + salt) % (VISIT_MAX_MS - VISIT_MIN_MS + 1));
        autonomy.setPhaseEndEpochMs(now + dur);
        scheduleNextPoiPick(autonomy, now, salt);
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        clearAutonomyRoleState(ref, npc, commandBuffer);
    }

    private void beginPlotPoiUse(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull PoiEntry poi
    ) {
        autonomy.setPhase(TouristAutonomyState.PHASE_POI);
        autonomy.clearTravelWaypoints();
        autonomy.setLastPlotPoiId(poi.getId());
        long dur = POI_USE_MIN_MS + Math.abs(now % (POI_USE_MAX_MS - POI_USE_MIN_MS + 1));
        autonomy.setPhaseEndEpochMs(now + dur);
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        PoiAutonomyVisuals.beginPoiUse(ref, store, commandBuffer, poi);
        applyAutonomyRoleState(ref, npc, commandBuffer);
    }

    private void tickPlotVisit(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull PoiRegistry poiRegistry,
        @Nonnull ConstructionCatalog catalog
    ) {
        UUID plotId = autonomy.getVisitPlotUuid();
        PlotInstance plot = TouristDestinationResolver.findVisitPlot(town, plotId);
        if (plot == null) {
            autonomy.clearVisitPlot();
            autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
            autonomy.setNextDecisionEpochMs(now + 3000L);
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            clearAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }

        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null
            && !TouristDestinationResolver.isInsidePlotFootprint(
                tc.getPosition().x,
                tc.getPosition().z,
                plot,
                TouristDestinationResolver.plotEdgePadding()
            )) {
            TouristPlotVisit entry = findPlotVisit(town, catalog, world, plotId);
            if (entry != null) {
                beginTravelToPlot(ref, store, commandBuffer, npc, autonomy, now, town, world, entry);
            }
            return;
        }

        if (now >= autonomy.getPhaseEndEpochMs()) {
            autonomy.clearVisitPlot();
            autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
            autonomy.setNextDecisionEpochMs(now + 2000L);
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            clearAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }

        if (now >= autonomy.getNextPoiPickEpochMs()) {
            Random random = new Random(now ^ ref.hashCode() ^ plotId.hashCode());
            PoiEntry poi =
                TouristDestinationResolver.pickVisitPoiOnPlot(
                    town,
                    poiRegistry,
                    catalog,
                    plotId,
                    autonomy.getLastPlotPoiUuid(),
                    random
                );
            scheduleNextPoiPick(autonomy, now, ref.hashCode());
            if (poi != null) {
                beginTravelToPoi(ref, store, commandBuffer, npc, autonomy, now, town, world, poi);
                return;
            }
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        }

        clearAutonomyRoleState(ref, npc, commandBuffer);
    }

    private void tickPlotPoi(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull PoiRegistry poiRegistry
    ) {
        if (now < autonomy.getPhaseEndEpochMs()) {
            applyAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }
        UUID poiId = autonomy.getTargetPoiUuid();
        if (poiId != null) {
            PoiEntry poi = poiRegistry.get(poiId);
            if (poi != null) {
                PoiAutonomyVisuals.cleanupAfterPoiUse(ref, store, commandBuffer, poi);
            }
        }
        autonomy.setPhase(TouristAutonomyState.PHASE_VISIT);
        scheduleNextPoiPick(autonomy, now, ref.hashCode());
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        clearAutonomyRoleState(ref, npc, commandBuffer);
    }

    @Nullable
    private static TouristPlotVisit findPlotVisit(
        @Nonnull TownRecord town,
        @Nonnull ConstructionCatalog catalog,
        @Nonnull World world,
        @Nonnull UUID plotId
    ) {
        for (TouristPlotVisit visit : TouristDestinationResolver.listVisitPlots(town, catalog, world)) {
            if (plotId.equals(visit.plotId())) {
                return visit;
            }
        }
        return null;
    }

    private static void scheduleNextPoiPick(@Nonnull TouristAutonomyState autonomy, long now, int salt) {
        long span = POI_PICK_MAX_DELAY_MS - POI_PICK_MIN_DELAY_MS + 1;
        autonomy.setNextPoiPickEpochMs(now + POI_PICK_MIN_DELAY_MS + Math.abs((now + salt) % span));
    }

    private void finishReturnDespawn(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull TouristAutonomyState autonomy,
        @Nonnull TownRecord town,
        @Nonnull World world
    ) {
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return;
        }
        UUID entityUuid = uc.getUuid();
        UUID portalId = autonomy.getHomePortalId();
        commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        world.execute(() ->
            TouristPortalTickService.finalizeTouristDeparture(world, plugin, town, tm, entityUuid, portalId, store)
        );
    }

    private void failTravel(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc,
        @Nonnull TouristAutonomyState autonomy,
        long now,
        @Nonnull TownRecord town,
        @Nonnull World world
    ) {
        if (isReturningTravel(autonomy)) {
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            TouristPortalRecord portal = resolveHomePortal(world, autonomy);
            if (tc != null
                && portal != null
                && TouristPortalBlockUtil.isNearPortalDespawn(world, portal.getBlockPosition(), tc.getPosition())) {
                finishReturnDespawn(ref, store, commandBuffer, autonomy, town, world);
                return;
            }
            beginReturnToPortal(ref, store, commandBuffer, npc, autonomy, now, town, world);
            return;
        }
        if (autonomy.getVisitPlotUuid() != null) {
            autonomy.setPhase(TouristAutonomyState.PHASE_VISIT);
            autonomy.clearTravelWaypoints();
            commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
            clearAutonomyRoleState(ref, npc, commandBuffer);
            return;
        }
        autonomy.setPhase(TouristAutonomyState.PHASE_IDLE);
        autonomy.clearTravelWaypoints();
        autonomy.setNextDecisionEpochMs(now + 5000L);
        commandBuffer.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        clearAutonomyRoleState(ref, npc, commandBuffer);
    }

    @Nullable
    private static TouristPortalRecord resolveHomePortal(
        @Nonnull World world,
        @Nonnull TouristAutonomyState autonomy
    ) {
        UUID portalId = autonomy.getHomePortalId();
        if (portalId == null) {
            return null;
        }
        return AetherhavenWorldRegistries.getOrCreateTouristPortalRegistry(world, AetherhavenPlugin.get()).get(portalId);
    }

    private static long resolveNowMs(@Nonnull Store<EntityStore> store) {
        TimeResource tr = store.getResource(TimeResource.getResourceType());
        return tr != null ? tr.getNow().toEpochMilli() : System.currentTimeMillis();
    }

    private static void applyAutonomyRoleState(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (npc.getRole() == null) {
            return;
        }
        if (npc.getRole().getStateSupport().getStateHelper().getStateIndex(AetherhavenConstants.NPC_STATE_AUTONOMY_POI) < 0) {
            return;
        }
        npc.getRole().getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_AUTONOMY_POI, null, commandBuffer);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
    }

    public static void applyAutonomyRoleStateOnStore(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull Store<EntityStore> store
    ) {
        if (!supportsAutonomyPoiRoleState(npc)) {
            return;
        }
        npc.getRole().getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_AUTONOMY_POI, null, store);
        store.putComponent(ref, NPCEntity.getComponentType(), npc);
    }

    private static void clearAutonomyRoleState(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (npc.getRole() == null) {
            return;
        }
        String state = npc.getRole().getStateSupport().getStateName();
        if (state == null || !state.startsWith(AetherhavenConstants.NPC_STATE_AUTONOMY_POI)) {
            return;
        }
        npc.getRole().getStateSupport().setState(ref, "Idle", null, commandBuffer);
        npc.playAnimation(ref, AnimationSlot.Action, null, commandBuffer);
        npc.playAnimation(ref, AnimationSlot.Emote, null, commandBuffer);
        npc.playAnimation(ref, AnimationSlot.Status, null, commandBuffer);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
    }
}
