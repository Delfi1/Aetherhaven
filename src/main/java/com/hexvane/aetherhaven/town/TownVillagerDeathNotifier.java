package com.hexvane.aetherhaven.town;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.villager.AetherhavenRoleLabels;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Notifies online town members and writes server log when a town-linked NPC dies. */
public final class TownVillagerDeathNotifier {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CHAT_KEY = "aetherhaven_ui_town.aetherhaven.town.villagerDeath.chat";

    public enum DeathCategory {
        VISITOR("Inn visitor"),
        GUARD("Guard"),
        CITIZEN("Citizen"),
        VILLAGER("Villager"),
        TOURIST("Traveler");

        private final String logLabel;

        DeathCategory(@Nonnull String logLabel) {
            this.logLabel = logLabel;
        }

        @Nonnull
        public String logLabel() {
            return logLabel;
        }
    }

    private TownVillagerDeathNotifier() {}

    public static void notifyTownMembers(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> victimRef,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nullable String roleId,
        @Nonnull String bindingKind,
        @Nonnull DeathCategory category,
        @Nullable UUID entityUuid,
        @Nullable DeathComponent death
    ) {
        String displayName = resolveDisplayName(store, victimRef, plugin, roleId, bindingKind);
        String causeLabel = deathCauseLabel(death);
        Message chat =
            Message.translation(CHAT_KEY)
                .param("name", displayName)
                .param("category", category.logLabel());
        Query<EntityStore> q = Query.and(Player.getComponentType(), UUIDComponent.getComponentType(), PlayerRef.getComponentType());
        store.forEachChunk(
            q,
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent uc = chunk.getComponent(i, UUIDComponent.getComponentType());
                    PlayerRef pr = chunk.getComponent(i, PlayerRef.getComponentType());
                    if (uc == null || pr == null) {
                        continue;
                    }
                    if (!town.hasMemberOrOwner(uc.getUuid())) {
                        continue;
                    }
                    pr.sendMessage(chat);
                }
            }
        );
        LOGGER.atInfo().log(
            "Town %s: %s died (%s, cause=%s, kind=%s, entity=%s)",
            town.getDisplayName(),
            displayName,
            category.logLabel(),
            causeLabel,
            bindingKind,
            entityUuid != null ? entityUuid : "unknown"
        );
    }

    @Nonnull
    private static String deathCauseLabel(@Nullable DeathComponent death) {
        if (death == null) {
            return "unknown";
        }
        DamageCause cause = death.getDeathCause();
        if (cause != null && cause.getId() != null && !cause.getId().isBlank()) {
            return cause.getId();
        }
        return "unknown";
    }

    @Nonnull
    private static String resolveDisplayName(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> victimRef,
        @Nonnull AetherhavenPlugin plugin,
        @Nullable String roleId,
        @Nonnull String bindingKind
    ) {
        if (victimRef.isValid()) {
            String rid = roleId != null && !roleId.isBlank() ? roleId : "";
            return TownResidentDisplay.resolveFromEntity(store, victimRef, rid, plugin).displayName();
        }
        if (roleId != null && !roleId.isBlank()) {
            return AetherhavenRoleLabels.displayNameForRoleId(roleId);
        }
        return AetherhavenRoleLabels.listLinePlainEnglish(null, bindingKind);
    }
}
