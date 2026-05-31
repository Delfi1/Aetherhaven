package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.construction.PrefabLocalOffset;
import com.hexvane.aetherhaven.guild.marker.AdventurerSpawnMarkerLocator;
import com.hexvane.aetherhaven.guild.marker.AdventurerSpawnSlot;
import com.hexvane.aetherhaven.time.AetherhavenMorningWindow;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hexvane.aetherhaven.townsfolk.TownsfolkSpawnService;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Dawn cycling pool of guard eligible townsfolk at the guild hall (unhired adventurers). */
public final class GuildHallAdventurerPoolService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float ADVENTURER_FILL_CHANCE = 0.5f;

    private GuildHallAdventurerPoolService() {}

    public static void scheduleTickFromHub(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull WorldTimeResource wtr) {
        world.execute(() -> tick(world, plugin, wtr));
    }

    public static void tick(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull WorldTimeResource wtr) {
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        Store<EntityStore> store = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
        if (store == null) {
            return;
        }
        int morningStart = plugin.getConfig().get().getGameMorningStartHour();
        int morningEndEx = plugin.getConfig().get().getGameMorningEndHourExclusive();

        for (TownRecord town : tm.allTowns()) {
            if (!world.getName().equals(town.getWorldName()) || !town.isGuildHallActive()) {
                continue;
            }
            PlotInstance hallPlot =
                town.findCompletePlotWithConstruction(plugin.getConstructionCatalog(), AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL);
            if (hallPlot == null) {
                continue;
            }
            ConstructionDefinition hallDef = plugin.getConstructionCatalog().get(hallPlot.getConstructionId());
            if (hallDef == null) {
                continue;
            }
            List<AdventurerSpawnSlot> spawnSlots = AdventurerSpawnMarkerLocator.resolveSpawnSlots(store, hallPlot, hallDef);
            if (spawnSlots.isEmpty()) {
                continue;
            }
            if (!isManagementChunkLoaded(world, hallPlot, hallDef)) {
                continue;
            }

            dedupeAdventurerIds(town, tm);
            pruneMissingAdventurers(town, store, tm);
            morningRefreshIfDue(world, plugin, town, tm, store, hallPlot, spawnSlots, wtr, morningStart, morningEndEx);
            fillEmptySlots(world, plugin, town, tm, store, spawnSlots, wtr);
        }
    }

    private static void dedupeAdventurerIds(@Nonnull TownRecord town, @Nonnull TownManager tm) {
        List<String> ids = town.getGuildHallAdventurerNpcIds();
        Set<String> seen = new HashSet<>();
        List<String> out = new ArrayList<>();
        for (String s : ids) {
            if (s != null && !s.isBlank() && seen.add(s.trim().toLowerCase())) {
                out.add(s.trim());
            }
        }
        if (out.size() != ids.size()) {
            ids.clear();
            ids.addAll(out);
            town.getGuildHallAdventurerSlotByNpcId().keySet().removeIf(k -> !out.contains(k));
            tm.updateTown(town);
        }
    }

    private static void pruneMissingAdventurers(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull TownManager tm
    ) {
        Iterator<String> it = town.getGuildHallAdventurerNpcIds().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            String sid = it.next();
            UUID u = parseUuid(sid);
            if (u == null) {
                it.remove();
                town.getGuildHallAdventurerSlotByNpcId().remove(sid);
                changed = true;
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
            if (ref != null && ref.isValid()) {
                continue;
            }
            if (ref == null) {
                continue;
            }
            it.remove();
            town.getGuildHallAdventurerSlotByNpcId().remove(u.toString());
            changed = true;
        }
        if (changed) {
            tm.updateTown(town);
        }
    }

    private static void morningRefreshIfDue(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotInstance hallPlot,
        @Nonnull List<AdventurerSpawnSlot> spawnSlots,
        @Nonnull WorldTimeResource wtr,
        int morningStart,
        int morningEndEx
    ) {
        long epochDay = wtr.getGameDateTime().toLocalDate().toEpochDay();
        Long lastDay = town.getGuildHallLastMorningEpochDay();
        if (lastDay != null && lastDay >= epochDay) {
            return;
        }
        if (!AetherhavenMorningWindow.isGameMorning(wtr, morningStart, morningEndEx)) {
            return;
        }

        despawnGuildHallAdventurersInPlot(world, plugin, town, store, hallPlot);

        town.getGuildHallAdventurerNpcIds().clear();
        town.getGuildHallAdventurerSlotByNpcId().clear();
        town.getGuildHallAdventurerFilledSlots().clear();

        long seed = town.getTownId().getLeastSignificantBits() ^ epochDay * 0x9E3779B97F4A7C15L;
        Random slotRandom = new Random(seed);
        for (int slot = 0; slot < spawnSlots.size(); slot++) {
            if (slotRandom.nextFloat() < ADVENTURER_FILL_CHANCE) {
                town.getGuildHallAdventurerFilledSlots().add(slot);
            }
        }

        town.setGuildHallLastMorningEpochDay(epochDay);
        tm.updateTown(town);
        LOGGER.atInfo().log(
            "Guild hall adventurer morning refresh for town %s: %s/%s slots",
            town.getTownId(),
            town.getGuildHallAdventurerFilledSlots().size(),
            spawnSlots.size()
        );
    }

    private static void despawnGuildHallAdventurersInPlot(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotInstance hallPlot
    ) {
        PlotFootprintRecord footprint = hallPlot.toFootprint();
        Set<UUID> toRemove = ConcurrentHashMap.newKeySet();

        for (String sid : new ArrayList<>(town.getGuildHallAdventurerNpcIds())) {
            UUID u = parseUuid(sid);
            if (u == null) {
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
            if (ref != null && ref.isValid() && isAdventurerEntity(town, store, ref, u)) {
                store.removeEntity(ref, RemoveReason.REMOVE);
                TownsfolkSpawnService.releaseByEntity(world, plugin, u);
            }
            toRemove.add(u);
        }

        store.forEachEntityParallel(TownVillagerBinding.getComponentType(), (index, archetypeChunk, commandBuffer) -> {
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            if (ref == null || !ref.isValid()) {
                return;
            }
            TownVillagerBinding b = archetypeChunk.getComponent(index, TownVillagerBinding.getComponentType());
            if (b == null || !b.getTownId().equals(town.getTownId()) || !TownVillagerBinding.KIND_TOWNSFOLK.equals(b.getKind())) {
                return;
            }
            TownsfolkCharacterBinding tb = archetypeChunk.getComponent(index, TownsfolkCharacterBinding.getComponentType());
            if (tb == null || !TownsfolkAssignmentKinds.isGuildHallAdventurer(tb.getAssignmentKind())) {
                return;
            }
            TransformComponent tc = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            if (tc == null) {
                return;
            }
            Vector3d pos = tc.getPosition();
            int bx = (int) Math.floor(pos.x);
            int by = (int) Math.floor(pos.y);
            int bz = (int) Math.floor(pos.z);
            if (!footprint.containsBlock(bx, by, bz)) {
                return;
            }
            UUIDComponent uc = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
            if (uc == null) {
                return;
            }
            UUID u = uc.getUuid();
            if (toRemove.add(u)) {
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                TownsfolkSpawnService.releaseByEntity(world, plugin, u);
            }
        });

        for (UUID u : toRemove) {
            town.getGuildHallAdventurerNpcIds().removeIf(s -> u.toString().equalsIgnoreCase(s != null ? s.trim() : ""));
            town.getGuildHallAdventurerSlotByNpcId().remove(u.toString());
        }
    }

    private static void fillEmptySlots(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull Store<EntityStore> store,
        @Nonnull List<AdventurerSpawnSlot> spawnSlots,
        @Nonnull WorldTimeResource wtr
    ) {
        Long lastDay = town.getGuildHallLastMorningEpochDay();
        long epochDay = wtr.getGameDateTime().toLocalDate().toEpochDay();
        if (lastDay == null || lastDay != epochDay) {
            return;
        }

        for (int slot : town.getGuildHallAdventurerFilledSlots()) {
            if (slot < 0 || slot >= spawnSlots.size()) {
                continue;
            }
            if (isSlotOccupied(town, store, slot)) {
                continue;
            }
            AdventurerSpawnSlot spawnSlot = spawnSlots.get(slot);
            Vector3d pos = spawnSlot.position();

            long seed =
                town.getTownId().getLeastSignificantBits()
                    ^ (long) world.getName().hashCode() << 1
                    ^ epochDay * 0x9E3779B97F4A7C15L
                    ^ slot;
            var spawned =
                TownsfolkSpawnService.trySpawn(
                    world,
                    plugin,
                    town,
                    store,
                    pos,
                    TownsfolkAssignmentKinds.GUILD_ADVENTURER,
                    null,
                    new Random(seed),
                    new Rotation3f(0.0F, spawnSlot.yawRadians(), 0.0F),
                    spawnSlot.yawRadians()
                );
            if (spawned.isEmpty()) {
                break;
            }
            String uuidStr = spawned.get().entityUuid().toString();
            town.getGuildHallAdventurerNpcIds().add(uuidStr);
            town.getGuildHallAdventurerSlotByNpcId().put(uuidStr, slot);
            tm.updateTown(town);
        }
    }

    private static boolean isSlotOccupied(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        int slot
    ) {
        for (Map.Entry<String, Integer> entry : town.getGuildHallAdventurerSlotByNpcId().entrySet()) {
            if (entry.getValue() == null || entry.getValue() != slot) {
                continue;
            }
            UUID u = parseUuid(entry.getKey());
            if (u == null) {
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
            if (ref != null && ref.isValid() && isAdventurerEntity(town, store, ref, u)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdventurerEntity(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UUID entityUuid
    ) {
        TownVillagerBinding b = store.getComponent(ref, TownVillagerBinding.getComponentType());
        if (b == null || !b.getTownId().equals(town.getTownId()) || !TownVillagerBinding.KIND_TOWNSFOLK.equals(b.getKind())) {
            return false;
        }
        TownsfolkCharacterBinding tb = store.getComponent(ref, TownsfolkCharacterBinding.getComponentType());
        if (tb == null || !TownsfolkAssignmentKinds.isGuildHallAdventurer(tb.getAssignmentKind())) {
            return false;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        return uc != null && entityUuid.equals(uc.getUuid());
    }

    public static boolean isGuildHallAdventurer(@Nonnull TownRecord town, @Nonnull UUID entityUuid) {
        String s = entityUuid.toString();
        for (String id : town.getGuildHallAdventurerNpcIds()) {
            if (s.equalsIgnoreCase(id != null ? id.trim() : "")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isManagementChunkLoaded(
        @Nonnull World world,
        @Nonnull PlotInstance plot,
        @Nonnull ConstructionDefinition def
    ) {
        Vector3i pos = managementBlockWorldPos(plot, def);
        if (pos == null) {
            pos = new Vector3i(plot.getSignX(), plot.getSignY(), plot.getSignZ());
        }
        return world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z)) != null;
    }

    @Nullable
    private static Vector3i managementBlockWorldPos(@Nonnull PlotInstance plot, @Nonnull ConstructionDefinition def) {
        int[] m = def.getManagementBlockLocalPos();
        if (m == null || m.length != 3) {
            return null;
        }
        Vector3i anchor = plot.resolvePrefabAnchorWorld(def);
        var yaw = plot.resolvePrefabYaw();
        Vector3i d = PrefabLocalOffset.rotate(yaw, m[0], m[1], m[2]);
        return new Vector3i(anchor.x + d.x, anchor.y + d.y, anchor.z + d.z);
    }

    @Nullable
    private static UUID parseUuid(@Nullable String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
