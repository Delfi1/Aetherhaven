package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hexvane.aetherhaven.rts.RtsSelectionService;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.ui.UiSoundEffects;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2fc;

public final class RtsGuardRosterInput {
    private static final long DOUBLE_CLICK_MS = 350L;

    private RtsGuardRosterInput() {}

    public static boolean tryConsumeRosterClick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nonnull AetherhavenPlugin plugin,
        @Nullable Vector2fc screen
    ) {
        if (screen == null) {
            screen = com.hexvane.aetherhaven.rts.RtsScreenPickUtil.latestCameraScreenPoint(playerRef, store);
        }
        List<RtsGuardRosterSupport.GuardRow> rows = RtsGuardRosterSupport.listRows(store, town, plugin, session);
        if (!RtsGuardRosterHitTest.hitsPanel(screen, rows.size())) {
            return false;
        }
        int index = RtsGuardRosterHitTest.pickPortraitIndex(screen, rows.size());
        if (index < 0) {
            return true;
        }
        RtsGuardRosterSupport.GuardRow row = rows.get(index);
        long now = System.currentTimeMillis();
        if (session.matchesRosterDoubleClick(row.entityUuid(), now, DOUBLE_CLICK_MS)) {
            startCameraFollow(playerRef, store, commandBuffer, session, row.entityUuid());
            return true;
        }
        RtsSelectionService.toggleGuard(session, row.entityUuid());
        session.recordRosterClick(row.entityUuid(), now);
        persistSession(playerRef, store, commandBuffer, session);
        RtsGuardRosterSupport.refreshActive(playerRef, store, session, town, plugin);
        return true;
    }

    private static void startCameraFollow(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull UUID guardUuid
    ) {
        session.setCameraFollowGuardUuid(guardUuid);
        session.clearRosterClickTracking();
        persistSession(playerRef, store, commandBuffer, session);
        UiSoundEffects.play2dUi(playerRef, store, AetherhavenConstants.EVENT_TITLE_SHORT_SUCCESS_SOUND_ID);
    }

    private static void persistSession(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        if (commandBuffer != null) {
            commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        } else {
            store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        }
    }
}
