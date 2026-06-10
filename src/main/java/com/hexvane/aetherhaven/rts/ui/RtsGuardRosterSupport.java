package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.patrol.PatrolGuardDirectory;
import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hexvane.aetherhaven.rts.RtsGuardDirectory;
import com.hexvane.aetherhaven.rts.RtsGuardHealthUtil;
import com.hexvane.aetherhaven.rts.RtsSelectionService;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RtsGuardRosterSupport {
    private RtsGuardRosterSupport() {}

    public record GuardRow(
        @Nonnull UUID entityUuid,
        @Nonnull String displayName,
        @Nonnull String portraitPath,
        float healthFraction,
        boolean selected
    ) {}

    @Nonnull
    public static List<GuardRow> listRows(
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Set<UUID> living = new HashSet<>();
        for (Ref<EntityStore> ref : RtsGuardDirectory.livingGuardRefs(town, store)) {
            var uc = store.getComponent(ref, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
            if (uc != null) {
                living.add(uc.getUuid());
            }
        }
        List<GuardRow> rows = new ArrayList<>();
        for (PatrolGuardDirectory.PatrolGuardRow guard : PatrolGuardDirectory.listGuards(store, town, plugin)) {
            if (!living.contains(guard.entityUuid())) {
                continue;
            }
            Ref<EntityStore> ref = RtsGuardDirectory.findByUuid(store, guard.entityUuid());
            float health = ref != null ? RtsGuardHealthUtil.healthFraction(ref, store) : 1f;
            rows.add(
                new GuardRow(
                    guard.entityUuid(),
                    guard.displayName(),
                    guard.portraitPath(),
                    health,
                    RtsSelectionService.isSelected(session, guard.entityUuid())
                )
            );
        }
        rows.sort(Comparator.comparing(GuardRow::displayName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    public static void open(
        @Nonnull Player player,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef pr,
        @Nonnull UUID townId,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town
    ) {
        RtsGuardRosterHudSupport.show(player, pr, townId, listRows(store, town, plugin, session));
    }

    public static void close(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        RtsGuardRosterHudSupport.removeHud(player, playerRef);
    }

    public static void refreshActive(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nonnull AetherhavenPlugin plugin
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        RtsGuardRosterHud hud = RtsGuardRosterHudSupport.activeHud(player);
        if (hud != null) {
            hud.refresh(session, listRows(store, town, plugin, session));
        }
    }

    @Nullable
    public static RtsGuardRosterHud activeHud(@Nonnull Player player) {
        return RtsGuardRosterHudSupport.activeHud(player);
    }
}
