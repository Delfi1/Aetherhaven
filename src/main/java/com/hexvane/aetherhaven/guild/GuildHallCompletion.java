package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiRegistry;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.ResidentRegistryService;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** When the guild hall prefab completes, move Lyra from the inn pool to the hall WORK POI. */
public final class GuildHallCompletion {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GuildHallCompletion() {}

    public static void onGuildHallBuilt(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull UUID plotId,
        @Nonnull TownManager tm
    ) {
        town.setGuildHallActive(true);
        tm.updateTown(town);

        if (!town.hasQuestActiveOrCompleted(AetherhavenConstants.QUEST_BUILD_GUILD_HALL)) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (store == null) {
            return;
        }

        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry work = null;
        for (PoiEntry e : reg.listByTown(town.getTownId())) {
            if (plotId.equals(e.getPlotId()) && e.getTags().contains("WORK")) {
                work = e;
                break;
            }
        }
        if (work == null) {
            LOGGER.atWarning().log("No WORK POI for guild hall plot %s", plotId);
            return;
        }

        Ref<EntityStore> masterRef = findGuildMasterRef(store, town);
        if (masterRef == null || !masterRef.isValid()) {
            return;
        }
        TownVillagerBinding existingBinding = store.getComponent(masterRef, TownVillagerBinding.getComponentType());
        boolean alreadyAtHall =
            existingBinding != null
                && TownVillagerBinding.KIND_GUILD_MASTER.equals(existingBinding.getKind())
                && plotId.equals(existingBinding.getJobPlotId());
        UUIDComponent uuidComp = store.getComponent(masterRef, UUIDComponent.getComponentType());
        UUID masterUuid = uuidComp != null ? uuidComp.getUuid() : null;
        double targetX = work.getX() + 0.5;
        double targetY = work.getY() + 0.02;
        double targetZ = work.getZ() + 0.5;
        if (work.hasInteractionTarget()) {
            Double tx = work.getInteractionTargetX();
            Double ty = work.getInteractionTargetY();
            Double tz = work.getInteractionTargetZ();
            if (tx != null && ty != null && tz != null) {
                targetX = tx;
                targetY = ty;
                targetZ = tz;
            }
        }
        final double moveX = targetX;
        final double moveY = targetY;
        final double moveZ = targetZ;
        final boolean moveTransform = !alreadyAtHall;
        if (masterUuid != null) {
            town.getInnPoolNpcIds().removeIf(s -> {
                try {
                    return masterUuid.equals(UUID.fromString(s.trim()));
                } catch (Exception e) {
                    return false;
                }
            });
        }
        if (masterUuid != null) {
            final UUID masterEntityUuid = masterUuid;
            final UUID townId = town.getTownId();
            world.execute(
                () ->
                    applyGuildMasterEntityComponents(
                        world,
                        masterEntityUuid,
                        townId,
                        plotId,
                        moveTransform,
                        moveX,
                        moveY,
                        moveZ
                    )
            );
        }
        town.addInnVisitorPoolExcludedRoleId(AetherhavenConstants.GUILD_MASTER_NPC_ROLE_ID);
        if (masterUuid != null) {
            ResidentRegistryService.upsert(
                town,
                tm,
                AetherhavenConstants.GUILD_MASTER_NPC_ROLE_ID,
                TownVillagerBinding.KIND_GUILD_MASTER,
                plotId,
                masterUuid
            );
        }
        LOGGER.atInfo().log(
            "Moved guild master to guild hall at %s,%s,%s",
            work.hasInteractionTarget() ? work.getInteractionTargetX() : work.getX() + 0.5,
            work.hasInteractionTarget() ? work.getInteractionTargetY() : work.getY() + 0.02,
            work.hasInteractionTarget() ? work.getInteractionTargetZ() : work.getZ() + 0.5
        );
    }

    private static void applyGuildMasterEntityComponents(
        @Nonnull World world,
        @Nonnull UUID masterEntityUuid,
        @Nonnull UUID townId,
        @Nonnull UUID plotId,
        boolean moveTransform,
        double targetX,
        double targetY,
        double targetZ
    ) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (store == null) {
            return;
        }
        Ref<EntityStore> masterRef = store.getExternalData().getRefFromUUID(masterEntityUuid);
        if (masterRef == null || !masterRef.isValid()) {
            return;
        }
        if (moveTransform) {
            TransformComponent tc = store.getComponent(masterRef, TransformComponent.getComponentType());
            if (tc != null) {
                Vector3d p = tc.getPosition();
                p.x = targetX;
                p.y = targetY;
                p.z = targetZ;
                store.putComponent(masterRef, TransformComponent.getComponentType(), tc);
            }
        }
        store.putComponent(
            masterRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(townId, TownVillagerBinding.KIND_GUILD_MASTER, plotId, plotId)
        );
    }

    @Nullable
    private static Ref<EntityStore> findGuildMasterRef(@Nonnull Store<EntityStore> store, @Nonnull TownRecord town) {
        List<String> ids = town.getInnPoolNpcIds();
        for (String sid : ids) {
            Ref<EntityStore> ref = refForPoolEntry(store, sid, AetherhavenConstants.GUILD_MASTER_NPC_ROLE_ID);
            if (ref != null) {
                return ref;
            }
        }
        Ref<EntityStore> resident = findResidentGuildMasterRef(store, town);
        if (resident != null) {
            return resident;
        }
        UUID legacy = town.getGuildMasterEntityUuid();
        if (legacy != null) {
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(legacy);
            if (ref != null && ref.isValid()) {
                return ref;
            }
        }
        return null;
    }

    @Nullable
    private static Ref<EntityStore> refForPoolEntry(
        @Nonnull Store<EntityStore> store,
        @Nonnull String sid,
        @Nonnull String roleId
    ) {
        try {
            UUID u = UUID.fromString(sid.trim());
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
            if (ref == null || !ref.isValid()) {
                return null;
            }
            var npcType = NPCEntity.getComponentType();
            NPCEntity npc = npcType != null ? store.getComponent(ref, npcType) : null;
            if (npc != null && roleId.equals(npc.getRoleName())) {
                return ref;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static Ref<EntityStore> findResidentGuildMasterRef(@Nonnull Store<EntityStore> store, @Nonnull TownRecord town) {
        AtomicReference<Ref<EntityStore>> found = new AtomicReference<>();
        store.forEachEntityParallel(TownVillagerBinding.getComponentType(), (index, archetypeChunk, commandBuffer) -> {
            if (found.get() != null) {
                return;
            }
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            if (ref == null || !ref.isValid()) {
                return;
            }
            TownVillagerBinding b = archetypeChunk.getComponent(index, TownVillagerBinding.getComponentType());
            if (b == null || !b.getTownId().equals(town.getTownId())) {
                return;
            }
            if (!TownVillagerBinding.KIND_GUILD_MASTER.equals(b.getKind())
                && !TownVillagerBinding.KIND_VISITOR_GUILD_MASTER.equals(b.getKind())) {
                return;
            }
            var npcType = NPCEntity.getComponentType();
            NPCEntity npc = npcType != null ? archetypeChunk.getComponent(index, npcType) : null;
            if (npc != null && AetherhavenConstants.GUILD_MASTER_NPC_ROLE_ID.equals(npc.getRoleName())) {
                found.set(ref);
            }
        });
        return found.get();
    }
}
