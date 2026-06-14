package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.ui.TownVillagerRow;
import com.hexvane.aetherhaven.ui.TownVillagerDirectory;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardGiverDisplay {
    private QuestBoardGiverDisplay() {}

    @Nonnull
    public static String giverName(@Nonnull QuestBoardSlotRecord slot, @Nonnull Store<EntityStore> store, @Nonnull TownRecord town) {
        String fromUuid = nameFromEntityUuid(slot.getGiverEntityUuid(), store, town);
        if (!fromUuid.isBlank()) {
            return fromUuid;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return "";
        }
        String roleId = slot.getGiverRoleId();
        if (roleId != null && !roleId.isBlank()) {
            var def = plugin.getVillagerDefinitionCatalog().byNpcRoleId(roleId.trim());
            if (def != null && def.getDisplayName() != null && !def.getDisplayName().isBlank()) {
                return def.getDisplayName().trim();
            }
        }
        return "";
    }

    @Nonnull
    private static String nameFromEntityUuid(@Nullable String uuidStr, @Nonnull Store<EntityStore> store, @Nonnull TownRecord town) {
        if (uuidStr == null || uuidStr.isBlank()) {
            return "";
        }
        try {
            UUID u = UUID.fromString(uuidStr.trim());
            List<TownVillagerRow> rows = TownVillagerDirectory.listResidents(store, town);
            for (TownVillagerRow row : rows) {
                if (u.equals(row.entityUuid())) {
                    return row.label();
                }
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
            if (ref != null && ref.isValid()) {
                NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
                if (npc != null && npc.getRoleName() != null) {
                    AetherhavenPlugin plugin = AetherhavenPlugin.get();
                    if (plugin != null) {
                        var def = plugin.getVillagerDefinitionCatalog().byNpcRoleId(npc.getRoleName().trim());
                        if (def != null && def.getDisplayName() != null && !def.getDisplayName().isBlank()) {
                            return def.getDisplayName().trim();
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
        return "";
    }

    @Nonnull
    public static String portraitPath(@Nonnull QuestBoardSlotRecord slot, @Nonnull Store<EntityStore> store) {
        String roleId = slot.getGiverRoleId();
        if (roleId != null && !roleId.isBlank()) {
            return com.hexvane.aetherhaven.ui.NpcPortraitProvider.portraitPathForRoleId(roleId.trim());
        }
        String uuidStr = slot.getGiverEntityUuid();
        if (uuidStr != null && !uuidStr.isBlank()) {
            try {
                UUID u = UUID.fromString(uuidStr.trim());
                Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(u);
                if (ref != null && ref.isValid()) {
                    NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
                    if (npc != null && npc.getRoleName() != null) {
                        return com.hexvane.aetherhaven.ui.NpcPortraitProvider.portraitPathForRoleId(npc.getRoleName().trim());
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return com.hexvane.aetherhaven.ui.NpcPortraitProvider.portraitPathForRoleId("");
    }
}
