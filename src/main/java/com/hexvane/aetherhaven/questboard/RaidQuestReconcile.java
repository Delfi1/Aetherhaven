package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.map.RaidQuestCompassCache;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/** Drops dead or despawned raid mobs from slot tracking and compass cache (world thread only). */
public final class RaidQuestReconcile {
    private static final long RECONCILE_INTERVAL_MS = 1000L;
    private static final ConcurrentHashMap<String, Long> LAST_RECONCILE_MS = new ConcurrentHashMap<>();

    private RaidQuestReconcile() {}

    public static void maybeReconcileWorld(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin
    ) {
        String worldName = world.getName();
        long now = System.currentTimeMillis();
        Long last = LAST_RECONCILE_MS.get(worldName);
        if (last != null && now - last < RECONCILE_INTERVAL_MS) {
            return;
        }
        LAST_RECONCILE_MS.put(worldName, now);
        reconcileWorld(world, store, plugin);
    }

    public static void reconcileWorld(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin
    ) {
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        String worldName = world.getName();
        Set<TownRecord> changedTowns = new HashSet<>();
        for (TownRecord town : tm.allTowns()) {
            if (!worldName.equals(town.getWorldName())) {
                continue;
            }
            for (QuestBoardSlotRecord slot : town.acceptedBoardQuestsSnapshot()) {
                if (!slot.isRaidQuest()) {
                    continue;
                }
                if (reconcileSlot(worldName, store, town, slot)) {
                    changedTowns.add(town);
                }
            }
        }
        for (TownRecord town : changedTowns) {
            tm.updateTown(town);
        }
    }

    private static boolean reconcileSlot(
        @Nonnull String worldName,
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nonnull QuestBoardSlotRecord slot
    ) {
        List<String> uuids = new ArrayList<>(slot.raidSpawnedEntityUuidsOrEmpty());
        if (uuids.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Iterator<String> it = uuids.iterator();
        while (it.hasNext()) {
            String uuidStr = it.next();
            if (uuidStr == null || uuidStr.isBlank()) {
                it.remove();
                changed = true;
                continue;
            }
            UUID mobUuid;
            try {
                mobUuid = UUID.fromString(uuidStr.trim());
            } catch (IllegalArgumentException e) {
                it.remove();
                changed = true;
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(mobUuid);
            if (ref != null && ref.isValid()) {
                continue;
            }
            RaidQuestCompassCache.removeMob(worldName, mobUuid);
            it.remove();
            int need = slot.getRaidKillRequired();
            int progress = slot.getRaidKillProgress();
            slot.setRaidKillRequired(Math.max(progress, need - 1));
            changed = true;
        }
        if (changed) {
            slot.setRaidSpawnedEntityUuids(uuids);
        }

        for (RaidQuestCompassCache.Entry entry : RaidQuestCompassCache.entriesForTown(worldName, town.getTownId())) {
            if (!slot.instanceIdOrEmpty().equals(entry.instanceId())) {
                continue;
            }
            if (!uuids.contains(entry.mobUuid().toString())) {
                RaidQuestCompassCache.removeMob(worldName, entry.mobUuid());
            }
        }
        return changed;
    }
}
