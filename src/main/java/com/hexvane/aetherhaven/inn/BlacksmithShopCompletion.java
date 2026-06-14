package com.hexvane.aetherhaven.inn;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.autonomy.VillagerAutonomySystem;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** When the blacksmith shop prefab completes, move Garren from the inn pool to the WORK POI. Quest completes on dialogue. */
public final class BlacksmithShopCompletion {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private BlacksmithShopCompletion() {}

    public static void onShopBuilt(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull UUID shopPlotId,
        @Nonnull TownManager tm
    ) {
        if (!town.hasQuestActiveOrCompleted(AetherhavenConstants.QUEST_BLACKSMITH_SHOP)) {
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
            LOGGER.atWarning().log("No WORK POI for blacksmith shop plot %s", shopPlotId);
            return;
        }

        Ref<EntityStore> blacksmithRef = findBlacksmithRef(store, town);
        if (blacksmithRef == null || !blacksmithRef.isValid()) {
            return;
        }
        UUIDComponent uuidComp = store.getComponent(blacksmithRef, UUIDComponent.getComponentType());
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
            blacksmithRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), TownVillagerBinding.KIND_BLACKSMITH, shopPlotId, shopPlotId)
        );
        VillagerAutonomySystem.promptWorkplaceTravel(
            blacksmithRef,
            store,
            VillagerAutonomySystem.resolveAutonomyNowMs(store)
        );
        town.addInnVisitorPoolExcludedRoleId(AetherhavenConstants.NPC_BLACKSMITH);
        if (uuidComp != null) {
            ResidentRegistryService.upsert(
                town,
                tm,
                AetherhavenConstants.NPC_BLACKSMITH,
                TownVillagerBinding.KIND_BLACKSMITH,
                shopPlotId,
                uuidComp.getUuid()
            );
        }
        tm.updateTown(town);
        LOGGER.atInfo().log("Assigned blacksmith to shop plot %s; pathing to work POI", shopPlotId);
    }

    @Nullable
    private static Ref<EntityStore> findBlacksmithRef(@Nonnull Store<EntityStore> store, @Nonnull TownRecord town) {
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
                if (npc != null && AetherhavenConstants.NPC_BLACKSMITH.equals(npc.getRoleName())) {
                    return ref;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
