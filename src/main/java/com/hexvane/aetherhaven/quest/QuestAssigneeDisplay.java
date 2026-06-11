package com.hexvane.aetherhaven.quest;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.quest.data.QuestDefinition;
import com.hexvane.aetherhaven.town.HiredGuardRecord;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.town.TownResidentDisplay;
import com.hexvane.aetherhaven.tourist.TouristRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves quest target names for journal and UI. */
public final class QuestAssigneeDisplay {
    private QuestAssigneeDisplay() {}

    @Nullable
    public static String targetName(
        @Nonnull QuestDefinition def,
        @Nonnull TownRecord town,
        @Nullable Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin
    ) {
        String qid = def.idOrEmpty();
        if (qid.isEmpty()) {
            return null;
        }
        UUID target = town.getQuestTargetEntityUuid(qid);
        if (target == null) {
            return null;
        }
        if (store != null) {
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(target);
            if (ref != null && ref.isValid()) {
                NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
                if (npc != null && npc.getRoleName() != null) {
                    return TownResidentDisplay.resolveFromEntity(store, ref, npc.getRoleName().trim(), plugin).displayName();
                }
            }
        }
        if (AetherhavenConstants.QUEST_HOUSE_GUARD.equals(qid)) {
            for (HiredGuardRecord rec : town.getHiredGuardRecords()) {
                UUID u = rec.getEntityUuid();
                if (u != null && u.equals(target)) {
                    TownResidentDisplay.Resolved offline =
                        TownResidentDisplay.resolveOffline(
                            plugin,
                            guardRoleIdForRecord(rec, plugin),
                            rec.getCharacterId(),
                            null
                        );
                    return offline.displayName();
                }
            }
        }
        if (AetherhavenConstants.QUEST_HOUSE_TOWNSFOLK.equals(qid)) {
            for (TouristRecord rec : town.getTouristRecords()) {
                UUID u = rec.getEntityUuid();
                if (u != null && u.equals(target)) {
                    TownResidentDisplay.Resolved offline =
                        TownResidentDisplay.resolveOffline(
                            plugin,
                            AetherhavenConstants.NPC_TOWNSFOLK,
                            rec.getCharacterId(),
                            null
                        );
                    return offline.displayName();
                }
            }
        }
        return null;
    }

    @Nullable
    public static String targetName(
        @Nonnull String questId,
        @Nonnull TownRecord town,
        @Nullable Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nullable QuestCatalog catalog
    ) {
        if (catalog == null) {
            return null;
        }
        QuestDefinition def = catalog.get(questId);
        if (def == null || !def.assignByEntity()) {
            return null;
        }
        return targetName(def, town, store, plugin);
    }

    @Nonnull
    public static String personalizeObjectiveLine(@Nonnull String line, @Nonnull String targetName) {
        String name = targetName.trim();
        if (name.isEmpty()) {
            return line;
        }
        return line
            .replace("this guard", name)
            .replace("This guard", name)
            .replace("the guard", name)
            .replace("The guard", name)
            .replace("this visitor", name)
            .replace("This visitor", name)
            .replace("the visitor", name)
            .replace("The visitor", name);
    }

    @Nonnull
    private static String guardRoleIdForRecord(@Nonnull HiredGuardRecord rec, @Nonnull AetherhavenPlugin plugin) {
        String characterId = rec.getCharacterId();
        if (characterId != null && !characterId.isBlank()) {
            var def = plugin.getTownsfolkCharacterCatalog().byId(characterId);
            if (def != null && def.getEquipmentProfileId() != null && !def.getEquipmentProfileId().isBlank()) {
                var profile = plugin.getEquipmentProfileCatalog().byId(def.getEquipmentProfileId());
                if (profile != null && profile.getGuardNpcRole() != null && !profile.getGuardNpcRole().isBlank()) {
                    return profile.getGuardNpcRole().trim();
                }
            }
        }
        return AetherhavenConstants.NPC_GUARD_KNIGHT;
    }
}
