package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.economy.GoldCoinPayment;
import com.hexvane.aetherhaven.equipment.VillagerEquipmentService;
import com.hexvane.aetherhaven.equipment.data.EquipmentProfileDefinition;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.HiredGuardRecord;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolPersistence;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolState;
import com.hexvane.aetherhaven.townsfolk.data.TownsfolkCharacterDefinition;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class GuardHireService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GuardHireService() {}

    public static boolean canAfford(
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull CombinedItemContainer inventory,
        @Nonnull UUID playerUuid,
        @Nonnull String profileId
    ) {
        EquipmentProfileDefinition profile = plugin.getEquipmentProfileCatalog().byId(profileId);
        if (profile == null) {
            return false;
        }
        long cost = profile.getHireGoldCost();
        return GoldCoinPayment.canAfford(town, inventory, cost, town.playerCanSpendTreasuryGold(playerUuid));
    }

    public static long hireCost(@Nonnull AetherhavenPlugin plugin, @Nonnull String profileId) {
        EquipmentProfileDefinition profile = plugin.getEquipmentProfileCatalog().byId(profileId);
        return profile != null ? profile.getHireGoldCost() : 0L;
    }

    @Nullable
    public static String equipmentProfileForNpc(
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store
    ) {
        TownsfolkCharacterBinding tb = store.getComponent(npcRef, TownsfolkCharacterBinding.getComponentType());
        if (tb == null) {
            return null;
        }
        TownsfolkCharacterDefinition def = plugin.getTownsfolkCharacterCatalog().byId(tb.getCharacterId());
        if (def == null) {
            return null;
        }
        String profileId = def.getEquipmentProfileId();
        return profileId != null ? profileId : "guard_knight";
    }

    public static boolean tryHire(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store
    ) {
        UUIDComponent pu = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (pu == null || !town.hasMemberOrOwner(pu.getUuid())) {
            return false;
        }
        if (!GuildHallAdventurerPoolService.isGuildHallAdventurer(town, npcUuid(store, npcRef))) {
            return false;
        }
        TownsfolkCharacterBinding tb = store.getComponent(npcRef, TownsfolkCharacterBinding.getComponentType());
        TownVillagerBinding binding = store.getComponent(npcRef, TownVillagerBinding.getComponentType());
        if (tb == null || binding == null || !town.getTownId().equals(binding.getTownId())) {
            return false;
        }
        if (!TownsfolkAssignmentKinds.isGuildHallAdventurer(tb.getAssignmentKind())) {
            return false;
        }

        String profileId = equipmentProfileForNpc(plugin, npcRef, store);
        if (profileId == null) {
            return false;
        }
        EquipmentProfileDefinition profile = plugin.getEquipmentProfileCatalog().byId(profileId);
        if (profile == null) {
            return false;
        }
        long cost = profile.getHireGoldCost();
        CombinedItemContainer inv = InventoryComponent.getCombined(store, playerRef, InventoryComponent.EVERYTHING);
        if (inv == null) {
            return false;
        }
        if (cost > 0 && !GoldCoinPayment.trySpend(town, inv, cost, town.playerCanSpendTreasuryGold(pu.getUuid()))) {
            return false;
        }

        UUID entityUuid = npcUuid(store, npcRef);
        if (entityUuid == null) {
            return false;
        }

        town.getGuildHallAdventurerNpcIds().removeIf(s -> entityUuid.toString().equalsIgnoreCase(s != null ? s.trim() : ""));
        town.getGuildHallAdventurerSlotByNpcId().remove(entityUuid.toString());

        var guildPlot = town.findCompletePlotWithConstruction(
            plugin.getConstructionCatalog(),
            AetherhavenConstants.CONSTRUCTION_PLOT_GUILD_HALL
        );
        UUID jobPlot = guildPlot != null ? guildPlot.getPlotId() : null;

        store.putComponent(
            npcRef,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), TownVillagerBinding.KIND_GUARD, jobPlot, jobPlot)
        );
        store.putComponent(
            npcRef,
            TownsfolkCharacterBinding.getComponentType(),
            new TownsfolkCharacterBinding(
                tb.getCharacterId(),
                tb.getActivePersonalityId(),
                TownsfolkAssignmentKinds.GUARD,
                tb.getModelAssetId(),
                tb.getPersonalityIds()
            )
        );

        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        var checkout = pool.checkoutForCharacter(tb.getCharacterId());
        if (checkout != null) {
            checkout.setHired(true);
            TownsfolkPoolPersistence.save(world, plugin, pool);
        }

        town.getHiredGuardRecords().removeIf(r -> tb.getCharacterId().equalsIgnoreCase(r.getCharacterId()));
        town.getHiredGuardRecords().add(new HiredGuardRecord(tb.getCharacterId(), entityUuid, profileId, false));
        tm.updateTown(town);

        GuardHireCleanup.prepareForGuardDuty(npcRef, store, guildPlot);
        changeToGuardRole(npcRef, store, profile);
        VillagerEquipmentService.applyProfile(npcRef, store, null, plugin.getEquipmentProfileCatalog(), profileId);
        LOGGER.atInfo().log("Hired guard %s for town %s", tb.getCharacterId(), town.getTownId());
        return true;
    }

    private static void changeToGuardRole(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull EquipmentProfileDefinition profile
    ) {
        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npc == null || npcPlugin == null || npc.getRole() == null) {
            return;
        }
        String roleName = profile.getGuardNpcRole();
        int guardIndex = npcPlugin.getIndex(roleName);
        if (guardIndex < 0) {
            LOGGER.atWarning().log("Guard NPC role %s not found", roleName);
            return;
        }
        Role role = npc.getRole();
        RoleChangeSystem.requestRoleChange(npcRef, role, guardIndex, false, "Idle", null, store);
    }

    @Nullable
    private static UUID npcUuid(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> npcRef) {
        UUIDComponent uc = store.getComponent(npcRef, UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }

    public static boolean isUnhousedHiredGuard(@Nonnull TownRecord town, @Nonnull UUID entityUuid) {
        for (HiredGuardRecord rec : town.getHiredGuardRecords()) {
            if (rec.isCitizen()) {
                continue;
            }
            UUID u = rec.getEntityUuid();
            if (u != null && u.equals(entityUuid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUnhousedHiredGuard(@Nonnull TownRecord town, @Nonnull AetherhavenPlugin plugin) {
        return firstUnhousedHiredGuardUuid(town, plugin) != null;
    }

    @Nullable
    public static UUID firstUnhousedHiredGuardUuid(@Nonnull TownRecord town, @Nonnull AetherhavenPlugin plugin) {
        for (HiredGuardRecord rec : town.getHiredGuardRecords()) {
            if (rec.isCitizen()) {
                continue;
            }
            UUID u = rec.getEntityUuid();
            if (u != null && !town.isNpcHomeResidentOnHousePlot(u, plugin.getConstructionCatalog())) {
                return u;
            }
        }
        return null;
    }

    public static boolean isGuardHouseQuestTargetHoused(@Nonnull TownRecord town, @Nonnull AetherhavenPlugin plugin) {
        UUID target = town.getGuardHouseQuestTargetEntityUuid();
        return target != null && town.isNpcHomeResidentOnHousePlot(target, plugin.getConstructionCatalog());
    }
}
