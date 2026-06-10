package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.rts.ui.RtsBoxSelectHudSupport;
import com.hexvane.aetherhaven.rts.ui.RtsCommandHudSupport;
import com.hexvane.aetherhaven.rts.ui.RtsGuardRosterInput;
import com.hexvane.aetherhaven.rts.debug.RtsBoxSelectDebugOverlay;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseMotionEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3i;

/** Mouse packet / camera polling + Primary-pulse drag for box select. */
public final class RtsMouseInputListener {
    private static final float MIN_BOX_SCREEN_DRAG = 0.012f;

    private RtsMouseInputListener() {}

    public static void register(@Nonnull EventRegistry eventRegistry) {
        eventRegistry.register(PlayerMouseButtonEvent.class, RtsMouseInputListener::onMouseButton);
        eventRegistry.register(PlayerMouseMotionEvent.class, RtsMouseInputListener::onMouseMotion);
    }

    private static void onMouseButton(@Nonnull PlayerMouseButtonEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }

        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        MouseButtonType buttonType = event.getMouseButton().mouseButtonType;
        MouseButtonState state = event.getMouseButton().state;

        if (buttonType == MouseButtonType.Left) {
            if (pr != null) {
                RtsDiagnostics.mouseClick(pr, "event-" + state.name(), event.getTargetBlock(), event.getScreenPoint());
            }
            processLeftButton(
                playerRef,
                store,
                null,
                session,
                state,
                event.getTargetBlock(),
                event.getScreenPoint()
            );
            return;
        }

        if (buttonType == MouseButtonType.Right && state == MouseButtonState.Pressed) {
            if (pr != null) {
                RtsDiagnostics.mouseClick(pr, "event-right-press", event.getTargetBlock(), event.getScreenPoint());
            }
            RtsClickService.handleSecondaryClick(
                playerRef,
                store,
                null,
                session,
                event.getTargetBlock(),
                event.getScreenPoint(),
                event.getTargetEntityRef()
            );
        }
    }

    private static void onMouseMotion(@Nonnull PlayerMouseMotionEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        handleMotion(playerRef, store, null, session, event.getTargetBlock(), event.getScreenPoint());
    }

    public static void processLeftButton(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull MouseButtonState state,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        TownRecord town = RtsSelectionService.townForSession(store, store.getExternalData().getWorld(), plugin, session);
        if (town == null) {
            return;
        }

        logHotbarOnClick(playerRef, store, state == MouseButtonState.Pressed ? "press" : "release");

        if (state == MouseButtonState.Pressed) {
            if (RtsGuardRosterInput.tryConsumeRosterClick(
                playerRef, store, commandBuffer, session, town, plugin, screen
            )) {
                persistSession(playerRef, store, commandBuffer, session);
                return;
            }
            beginBoxSelect(playerRef, store, session, targetBlock, screen);
            persistSession(playerRef, store, commandBuffer, session);
            refreshBoxHud(playerRef, store, session);
            return;
        }
        if (state != MouseButtonState.Released) {
            return;
        }

        finishBoxSelect(playerRef, store, commandBuffer, session, town, targetBlock, screen);
    }

    public static void beginBoxSelect(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        screen = resolveScreen(playerRef, store, screen);
        session.clearCameraFollow();
        session.setBoxSelectActive(true);
        session.setBoxWorldAnchorReady(false);
        session.clearOrthoCalibration();
        applyScreenPoint(session, targetBlock, screen, true);
        applyWorldPick(playerRef, store, session, targetBlock, screen, true);
        refreshSelectionOverlay(playerRef, store, session);
    }

    public static void updateBoxDrag(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        screen = resolveScreen(playerRef, store, screen);
        applyScreenPoint(session, targetBlock, screen, false);
        applyWorldPick(playerRef, store, session, targetBlock, screen, false);
        refreshSelectionOverlay(playerRef, store, session);
    }

    public static void handleMotion(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        if (!session.isBoxSelectActive()) {
            return;
        }
        updateBoxDrag(playerRef, store, session, targetBlock, screen);
        persistSession(playerRef, store, commandBuffer, session);
        refreshBoxHud(playerRef, store, session);
    }

    public static void finishBoxSelect(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc releaseScreen
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        Vector2fc screen = resolveScreen(playerRef, store, releaseScreen);
        if (screen != null) {
            session.setBoxScreenEnd(
                RtsScreenPickUtil.cameraRawToNormalizedX(screen.x()),
                RtsScreenPickUtil.cameraRawToNormalizedY(screen.y())
            );
        }
        if (targetBlock != null) {
            applyWorldPick(playerRef, store, session, targetBlock, screen, false);
        }

        if (screenBoxDragged(session)) {
            applyCompletedBoxSelection(playerRef, store, session, town);
            persistSession(playerRef, store, commandBuffer, session);
        } else {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null
                || !RtsGuardRosterInput.tryConsumeRosterClick(
                    playerRef, store, commandBuffer, session, town, plugin, screen
                )) {
                handleToolClickWithoutDrag(playerRef, store, commandBuffer, session, town, targetBlock, releaseScreen);
            } else {
                persistSession(playerRef, store, commandBuffer, session);
            }
        }

        refreshSelectionOverlay(playerRef, store, session);
        cancelBoxSelect(playerRef, store, commandBuffer, session);
    }

    private static void applyCompletedBoxSelection(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town
    ) {
        session.clearSelection();
        if (!screenBoxDragged(session)) {
            refreshStatusHud(playerRef, store, session);
            return;
        }

        String method;
        RtsScreenPickUtil.WorldAabb column = RtsScreenPickUtil.pickHudRectWorldColumn(session);
        if (column != null) {
            RtsSelectionService.addGuardsInWorldColumn(
                store,
                session,
                town,
                column.minX(),
                column.maxX(),
                column.minZ(),
                column.maxZ()
            );
            method = session.getOrthoHalfWidth() > 0.0 || session.getOrthoHalfHeight() > 0.0
                ? "ortho-hud-calibrated"
                : "ortho-hud-default";
        } else {
            method = "none";
        }

        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr != null) {
            RtsDiagnostics.boxSelectionComplete(
                pr,
                method,
                session,
                column,
                session.getSelectedGuardUuids().size()
            );
        }
        if (!session.getSelectedGuardUuids().isEmpty()) {
            RtsCommandFeedback.playBoxSelect(playerRef, store);
        }
        refreshStatusHud(playerRef, store, session);
    }

    private static void refreshStatusHud(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null || pr == null) {
            return;
        }
        ItemStack hand = InventoryComponent.getItemInHand(store, playerRef);
        RtsCommandHudSupport.obtainHud(player, pr).refresh(session, RtsInteractions.toolHelpKey(hand));
    }

    public static void cancelBoxSelect(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        session.setBoxSelectActive(false);
        session.clearBoxScreen();
        persistSession(playerRef, store, commandBuffer, session);
        refreshBoxHud(playerRef, store, session);
    }

    private static void refreshSelectionOverlay(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        RtsBoxSelectDebugOverlay.refresh(pr, session);
    }

    @Nullable
    private static Vector2fc resolveScreen(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable Vector2fc screen
    ) {
        if (screen != null) {
            return screen;
        }
        return RtsScreenPickUtil.latestCameraScreenPoint(playerRef, store);
    }

    private static void applyScreenPoint(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen,
        boolean start
    ) {
        if (!RtsScreenPickUtil.isUsableScreenPoint(screen, targetBlock)) {
            return;
        }
        float x = RtsScreenPickUtil.cameraRawToNormalizedX(screen.x());
        float y = RtsScreenPickUtil.cameraRawToNormalizedY(screen.y());
        if (start || !session.isBoxScreenAnchorReady()) {
            session.setBoxScreenStart(x, y);
            session.setBoxScreenAnchorReady(true);
        }
        session.setBoxScreenEnd(x, y);
    }

    private static void applyWorldPick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen,
        boolean start
    ) {
        if (targetBlock == null) {
            return;
        }
        RtsScreenPickUtil.GroundPick pick = new RtsScreenPickUtil.GroundPick(
            targetBlock.x() + 0.5,
            targetBlock.y() + 0.5,
            targetBlock.z() + 0.5
        );
        session.setBoxWorldAnchorReady(true);
        if (screen != null) {
            float nx = RtsScreenPickUtil.cameraRawToNormalizedX(screen.x());
            float ny = RtsScreenPickUtil.cameraRawToNormalizedY(screen.y());
            RtsScreenPickUtil.calibrateOrthoFromSample(session, nx, ny, pick.x(), pick.z());
        }
        if (start) {
            session.setBoxStart(pick.x(), pick.z());
            session.setBoxEnd(pick.x(), pick.z());
            session.setBoxGroundY(pick.y());
        } else {
            session.setBoxEnd(pick.x(), pick.z());
            session.setBoxGroundY(pick.y());
        }
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

    private static void handleToolClickWithoutDrag(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen
    ) {
        RtsClickService.handleToolClick(playerRef, store, commandBuffer, session, town, targetBlock, screen);
    }

    private static boolean screenBoxDragged(@Nonnull RtsCommandPlayerComponent session) {
        double dx = session.getBoxScreenEndX() - session.getBoxScreenStartX();
        double dy = session.getBoxScreenEndY() - session.getBoxScreenStartY();
        return dx * dx + dy * dy >= MIN_BOX_SCREEN_DRAG * MIN_BOX_SCREEN_DRAG;
    }

    public static void refreshBoxHud(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null || pr == null) {
            RtsDiagnostics.boxHudSkipped("no-player");
            return;
        }
        if (session.isBoxSelectActive()) {
            RtsBoxSelectHudSupport.showForDrag(player, pr).refresh(session);
        } else {
            RtsBoxSelectHudSupport.hideSelection(player, pr);
        }
    }

    private static void logHotbarOnClick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull String action
    ) {
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (pr != null && hotbar != null) {
            RtsDiagnostics.hotbarClick(pr, hotbar.getActiveSlot(), action);
        }
    }
}
