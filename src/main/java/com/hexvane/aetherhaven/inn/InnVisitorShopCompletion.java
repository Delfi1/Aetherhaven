package com.hexvane.aetherhaven.inn;

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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** When a shop prefab completes, move the matching inn visitor to the shop WORK POI. Quest completes on dialogue. */
public final class InnVisitorShopCompletion {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private InnVisitorShopCompletion() {}

    public static void onShopBuilt(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull UUID shopPlotId,
        @Nonnull TownManager tm,
        @Nonnull ShopPromotionConfig config
    ) {
        if (!town.hasQuestActiveOrCompleted(config.shopQuestId())) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();

        PoiRegistry reg = AetherhavenWorldRegistries.getOrCreatePoiRegistry(world, plugin);
        PoiEntry work = null;
        for (PoiEntry e : reg.listByTown(town.getTownId())) {
            if (shopPlotId.equals(e.getPlotId()) && e.getTags().contains("WORK")) {
                work = e;
                break;
            }
        }
        if (work == null) {
            LOGGER.atWarning().log("No WORK POI for %s shop plot %s", config.logLabel(), shopPlotId);
            return;
        }

        double tx = work.getX() + 0.5;
        double ty = work.getY();
        double tz = work.getZ() + 0.5;

        Ref<EntityStore> npcRef = findInnPoolNpcRef(store, town, config.npcRoleId());
        if (npcRef == null || !npcRef.isValid()) {
            return;
        }
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3d p = tc.getPosition();
        p.x = tx;
        p.y = ty + 0.02;
        p.z = tz;
        store.putComponent(npcRef, TransformComponent.getComponentType(), tc);
        UUIDComponent uuidComp = store.getComponent(npcRef, UUIDComponent.getComponentType());
        UUID npcUuid = uuidComp != null ? uuidComp.getUuid() : null;
        if (npcUuid != null) {
            town.getInnPoolNpcIds().removeIf(s -> {
                try {
                    return npcUuid.equals(UUID.fromString(s.trim()));
                } catch (Exception e) {
                    return false;
                }
            });
        }
        store.putComponent(
            npcRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), config.residentKind(), shopPlotId, shopPlotId)
        );
        town.addInnVisitorPoolExcludedRoleId(config.npcRoleId());
        if (uuidComp != null) {
            ResidentRegistryService.upsert(
                town,
                tm,
                config.npcRoleId(),
                config.residentKind(),
                shopPlotId,
                uuidComp.getUuid()
            );
        }
        tm.updateTown(town);
        LOGGER.atInfo().log("Moved %s to shop at %s,%s,%s", config.logLabel(), work.getX(), work.getY(), work.getZ());
    }

    @Nullable
    private static Ref<EntityStore> findInnPoolNpcRef(
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nonnull String npcRoleId
    ) {
        List<String> ids = town.getInnPoolNpcIds();
        for (String sid : ids) {
            try {
                UUID u = UUID.fromString(sid.trim());
                Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                var npcType = NPCEntity.getComponentType();
                NPCEntity npc = npcType != null ? store.getComponent(ref, npcType) : null;
                if (npc != null && npcRoleId.equals(npc.getRoleName())) {
                    return ref;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
