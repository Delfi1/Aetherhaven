package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownOnlinePresence;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkExistenceService;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolCheckoutRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolPersistence;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolState;
import com.hexvane.aetherhaven.townsfolk.TownsfolkSpawnService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Aligns persisted tourist rows with live entities; releases stale rows back to the spawn pool. */
public final class TouristReconcileService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TouristReconcileService() {}

    public static void scheduleAfterWorldLoad(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        world.execute(() -> reconcileOnWorldThread(world, plugin, false));
        plugin.scheduleOnWorld(world, () -> reconcileOnWorldThread(world, plugin, false), 2_000L);
        plugin.scheduleOnWorld(world, () -> reconcileOnWorldThread(world, plugin, true), 10_000L);
    }

    /** Reconcile tourists once a town member is online so unloaded chunks are not mistaken for missing NPCs. */
    public static void onTownMemberPlayerReady(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull UUID playerUuid) {
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownForPlayerInWorld(playerUuid);
        if (town == null) {
            return;
        }
        reconcileOnWorldThread(world, plugin, true);
    }

    public static void reconcileOnWorldThread(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        boolean releaseMissing
    ) {
        Store<EntityStore> store = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
        if (store == null) {
            LOGGER.atWarning().log("Tourist reconcile skipped: entity store not ready for world %s", world.getName());
            return;
        }

        WorldTimeResource wtr = store.getResource(WorldTimeResource.getResourceType());
        LocalDateTime gameTime = wtr != null ? wtr.getGameDateTime() : null;
        long currentEpochDay = gameTime != null ? gameTime.toLocalDate().toEpochDay() : Long.MIN_VALUE;

        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        Set<UUID> onlinePlayers = TownOnlinePresence.collectOnlinePlayerUuids(world);
        Map<String, TownsfolkExistenceService.LiveTownsfolkEntity> liveByCharacter =
            TownsfolkExistenceService.buildLiveIndex(store);
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        boolean townChanged = false;
        int synced = 0;
        int released = 0;

        for (TownRecord town : tm.allTowns()) {
            if (!world.getName().equals(town.getWorldName())) {
                continue;
            }
            boolean memberOnline = TownOnlinePresence.hasAffiliatedPlayerOnline(town, onlinePlayers);
            boolean changed = false;
            Set<String> seenCharacters = new HashSet<>();
            Iterator<TouristRecord> it = town.getTouristRecords().iterator();
            while (it.hasNext()) {
                TouristRecord rec = it.next();
                String characterId = rec.getCharacterId();
                if (characterId.isBlank()) {
                    it.remove();
                    changed = true;
                    continue;
                }
                if (seenCharacters.contains(characterId)) {
                    it.remove();
                    changed = true;
                    continue;
                }
                seenCharacters.add(characterId);

                if (!memberOnline) {
                    continue;
                }

                if (rec.isCitizen()) {
                    syncPoolCheckout(pool, town, rec, liveByCharacter.get(characterId));
                    continue;
                }

                if (rec.getSpawnEpochDay() <= 0L && currentEpochDay != Long.MIN_VALUE) {
                    rec.setSpawnEpochDay(currentEpochDay);
                    changed = true;
                }
                TouristPortalTickService.ensureLeaveHour(rec);

                if (gameTime != null && TouristPortalTickService.shouldTouristLeaveNow(rec, gameTime)) {
                    UUID entityUuid = rec.getEntityUuid();
                    if (entityUuid != null && isLiveTouristEntity(town, store, liveByCharacter, rec)) {
                        TouristPortalTickService.sendTouristHomeOrFinalize(
                            world, plugin, tm, store, town, rec, entityUuid
                        );
                    } else if (!rec.isInvitedToStay()) {
                        releaseStaleTouristRecord(world, plugin, rec);
                        it.remove();
                        changed = true;
                        released++;
                    }
                    continue;
                }

                if (isLiveTouristEntity(town, store, liveByCharacter, rec)) {
                    TownsfolkExistenceService.LiveTownsfolkEntity live = liveByCharacter.get(characterId);
                    UUID liveUuid = live != null ? live.entityUuid() : rec.getEntityUuid();
                    if (liveUuid != null) {
                        UUID recorded = rec.getEntityUuid();
                        if (recorded == null || !recorded.equals(liveUuid)) {
                            rec.setEntityUuid(liveUuid);
                            changed = true;
                            synced++;
                        }
                    }
                    Ref<EntityStore> ref = refForRecord(town, store, liveByCharacter, rec);
                    ensureAutonomyAfterBind(ref, store, plugin, town, world, rec);
                    syncPoolCheckout(pool, town, rec, live);
                    continue;
                }

                if (rec.isInvitedToStay()) {
                    continue;
                }

                if (releaseMissing) {
                    releaseStaleTouristRecord(world, plugin, rec);
                    it.remove();
                    changed = true;
                    released++;
                }
            }
            if (changed) {
                tm.updateTown(town);
                townChanged = true;
            }
        }

        if (townChanged) {
            TownsfolkPoolPersistence.save(world, plugin, pool);
            LOGGER.atInfo().log(
                "Tourist reconcile in world %s (releaseMissing=%s): synced %s, released %s",
                world.getName(),
                releaseMissing,
                synced,
                released
            );
        }
    }

    /** True while a tourist row still exists for this character (pool reclaim should not steal the checkout yet). */
    public static boolean isActiveTouristCharacter(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull String characterId
    ) {
        if (characterId.isBlank()) {
            return false;
        }
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        for (TownRecord town : tm.allTowns()) {
            if (!world.getName().equals(town.getWorldName())) {
                continue;
            }
            for (TouristRecord rec : town.getTouristRecords()) {
                if (characterId.equals(rec.getCharacterId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLiveTouristEntity(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull Map<String, TownsfolkExistenceService.LiveTownsfolkEntity> liveByCharacter,
        @Nonnull TouristRecord rec
    ) {
        String characterId = rec.getCharacterId();
        if (characterId.isBlank()) {
            return false;
        }
        TownsfolkExistenceService.LiveTownsfolkEntity live = liveByCharacter.get(characterId);
        if (live != null
            && town.getTownId().equals(live.townId())
            && TownsfolkAssignmentKinds.TOURIST.equalsIgnoreCase(live.assignmentKind().trim())) {
            return true;
        }
        UUID recorded = rec.getEntityUuid();
        if (recorded == null) {
            return false;
        }
        Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(recorded);
        return ref != null && ref.isValid();
    }

    @Nonnull
    public static Set<String> liveTouristCharacterIdsInTown(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store
    ) {
        Set<String> out = new HashSet<>();
        Map<String, TownsfolkExistenceService.LiveTownsfolkEntity> liveByCharacter =
            TownsfolkExistenceService.buildLiveIndex(store);
        for (TouristRecord rec : town.getTouristRecords()) {
            if (isLiveTouristEntity(town, store, liveByCharacter, rec)) {
                out.add(rec.getCharacterId());
            }
        }
        for (TownsfolkExistenceService.LiveTownsfolkEntity live : liveByCharacter.values()) {
            if (town.getTownId().equals(live.townId())
                && TownsfolkAssignmentKinds.TOURIST.equalsIgnoreCase(live.assignmentKind().trim())) {
                out.add(live.characterId());
            }
        }
        return out;
    }

    private static void releaseStaleTouristRecord(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TouristRecord rec
    ) {
        String characterId = rec.getCharacterId();
        if (!characterId.isBlank()) {
            TownsfolkSpawnService.release(world, plugin, characterId);
        }
    }

    private static void ensureAutonomyAfterBind(
        @Nullable Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull World world,
        @Nonnull TouristRecord rec
    ) {
        if (ref == null || !ref.isValid()) {
            return;
        }
        TouristAutonomyState autonomy = store.getComponent(ref, TouristAutonomyState.getComponentType());
        if (autonomy == null) {
            autonomy = TouristAutonomyState.fresh(System.currentTimeMillis());
        }
        UUID portalId = rec.getPortalId();
        if (portalId != null && autonomy.getHomePortalId() == null) {
            autonomy.setHomePortalId(portalId);
            store.putComponent(ref, TouristAutonomyState.getComponentType(), autonomy);
        }
        if (autonomy.getPhase() == TouristAutonomyState.PHASE_IDLE && !rec.isInvitedToStay()) {
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null) {
                TouristAutonomySystem.kickInitialVisitOnSpawn(ref, store, plugin, autonomy, town, world);
            }
        }
    }

    @Nullable
    private static Ref<EntityStore> refForRecord(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull Map<String, TownsfolkExistenceService.LiveTownsfolkEntity> liveByCharacter,
        @Nonnull TouristRecord rec
    ) {
        TownsfolkExistenceService.LiveTownsfolkEntity live = liveByCharacter.get(rec.getCharacterId());
        if (live != null && live.ref().isValid()) {
            return live.ref();
        }
        UUID recorded = rec.getEntityUuid();
        if (recorded == null) {
            return null;
        }
        return store.getExternalData().getRefFromUUID(recorded);
    }

    private static void syncPoolCheckout(
        @Nonnull TownsfolkPoolState pool,
        @Nonnull TownRecord town,
        @Nonnull TouristRecord rec,
        @Nullable TownsfolkExistenceService.LiveTownsfolkEntity live
    ) {
        String characterId = rec.getCharacterId();
        if (characterId.isBlank()) {
            return;
        }
        UUID entityUuid = live != null ? live.entityUuid() : rec.getEntityUuid();
        if (entityUuid == null) {
            return;
        }
        TownsfolkPoolCheckoutRecord checkout = pool.checkoutForCharacter(characterId);
        if (checkout == null) {
            pool.checkout(
                new TownsfolkPoolCheckoutRecord(
                    characterId,
                    town.getTownId().toString(),
                    entityUuid.toString(),
                    TownsfolkAssignmentKinds.TOURIST,
                    ""
                )
            );
            return;
        }
        if (!entityUuid.toString().equalsIgnoreCase(checkout.getEntityUuid())) {
            checkout.setEntityUuid(entityUuid.toString());
        }
        if (!TownsfolkAssignmentKinds.TOURIST.equalsIgnoreCase(checkout.getAssignmentKind().trim())) {
            checkout.setAssignmentKind(TownsfolkAssignmentKinds.TOURIST);
        }
        if (!town.getTownId().toString().equals(checkout.getTownId())) {
            checkout.setTownId(town.getTownId().toString());
        }
    }
}
