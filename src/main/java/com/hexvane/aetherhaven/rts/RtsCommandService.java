package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.rts.camera.TopDownCameraService;
import com.hexvane.aetherhaven.rts.debug.RtsBoxSelectDebug;
import com.hexvane.aetherhaven.rts.ui.RtsBoxSelectHudSupport;
import com.hexvane.aetherhaven.rts.ui.RtsCommandHudSupport;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RtsCommandService {
    private static final int TELEPORT_SETTLE_MAX_ATTEMPTS = 120;

    private RtsCommandService() {}

    public static boolean enter(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull TownRecord town,
        double postX,
        double postY,
        double postZ,
        @Nonnull AetherhavenPlugin plugin
    ) {
        Store<EntityStore> store = playerRef.getStore();
        World world = store.getExternalData().getWorld();
        RtsCommandPlayerComponent existing = accessor.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (existing != null && existing.isActive()) {
            return true;
        }
        if (!RtsGuardDirectory.townHasLivingGuard(town, store)) {
            notify(playerRef, accessor, "aetherhaven_rts.aetherhaven.rts.errorNoGuards");
            return false;
        }
        PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
        Player player = accessor.getComponent(playerRef, Player.getComponentType());
        if (pr == null || player == null) {
            return false;
        }
        RtsExitMovementGuard.clear(pr.getUuid());
        RtsCommandPlayerComponent session = existing != null ? existing : new RtsCommandPlayerComponent();
        session.setActive(true);
        session.setTownId(town.getTownId().toString());
        session.setPostPosition(postX, postY, postZ);
        TransformComponent enterTc = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        double exitX = postX + 0.5;
        double exitY = postY + 1.0;
        double exitZ = postZ + 0.5;
        float exitYaw = 0f;
        float exitPitch = 0f;
        float exitRoll = 0f;
        if (enterTc != null) {
            org.joml.Vector3d body = enterTc.getPosition();
            exitX = body.x;
            exitY = body.y;
            exitZ = body.z;
            exitYaw = enterTc.getRotation().yaw();
            exitPitch = enterTc.getRotation().pitch();
            exitRoll = enterTc.getRotation().roll();
        }
        session.setExitBody(exitX, exitY, exitZ);
        RtsExitPositionCache.save(pr.getUuid(), exitX, exitY, exitZ, exitYaw, exitPitch, exitRoll);
        double groundY = postY + 1.0;
        double startX = postX + 0.5;
        double startZ = postZ + 0.5;
        session.setDistance(TopDownCameraService.DEFAULT_DISTANCE);
        session.setFocus(startX, groundY, startZ);
        session.setOrderMode(RtsOrderMode.ATTACK_MOVE);
        session.setStanceMode(RtsCombatStance.DEFENSIVE);
        session.clearSelection();
        session.clearPanWish();
        session.clearLastAbsSample();
        session.setShiftModifierHeld(false);
        session.setSprintModifierHeld(false);
        session.setBoxSelectDebug(RtsBoxSelectDebug.isEnabled(pr.getUuid()));
        session.markCameraDirty();
        session.setSavedHotbarJson(RtsInventorySwap.saveHotbar(playerRef, accessor));
        session.setSessionExitedSafely(false);
        accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        RtsInventorySwap.equipCommandTools(playerRef, accessor);
        TransformComponent tc = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        double bodyY = TopDownCameraService.commanderBodyY(groundY, session.getDistance());
        if (tc != null) {
            org.joml.Vector3d aerial = new org.joml.Vector3d(startX, bodyY, startZ);
            var rot = tc.getRotation();
            applyBodyTeleport(playerRef, accessor, aerial, rot.pitch(), rot.yaw(), rot.roll());
        }
        RtsCommanderVisibility.hideCommander(world, playerRef);
        RtsDiagnostics.enter(pr);
        InventoryComponent.Hotbar equippedHotbar = accessor.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        if (equippedHotbar != null) {
            RtsHotbarSync.syncToClient(playerRef, pr, equippedHotbar);
        }
        RtsHudVisibility.showGameplayHud(player, pr);
        RtsCommandHudSupport.obtainHud(player, pr).show();
        RtsBoxSelectHudSupport.obtainHud(player, pr).ensureShown();
        RtsEntityViewSupport.enter(playerRef, accessor, session);
        runWhenTeleportSettled(world, playerRef, store, 0, () -> finishEnterClientSetup(playerRef, store, pr, player, session));
        return true;
    }

    private static void finishEnterClientSetup(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef pr,
        @Nonnull Player player,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        RtsCommandPlayerComponent live = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (live == null || !live.isActive()) {
            return;
        }
        TopDownCameraService.apply(pr, session.getDistance());
        session.clearCameraDirty();
        store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        RtsMovementSupport.applyCommandModeProfile(pr, playerRef, store);
        RtsCommanderNpcVisibility.apply(playerRef, store);
    }

    public static void exit(@Nonnull Ref<EntityStore> playerRef, @Nonnull ComponentAccessor<EntityStore> accessor) {
        Store<EntityStore> store = playerRef.getStore();
        RtsCommandPlayerComponent session = accessor.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        World world = store.getExternalData().getWorld();
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin != null && session.getTownId() != null && !session.getTownId().isBlank()) {
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
            try {
                TownRecord town = tm.getTown(UUID.fromString(session.getTownId()));
                if (town != null) {
                    freeAllCommandedGuardsInTown(accessor, store, town);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        String saved = session.getSavedHotbarJson();
        ExitPlan plan = resolveExitPlan(session, accessor.getComponent(playerRef, PlayerRef.getComponentType()));

        Player player = accessor.getComponent(playerRef, Player.getComponentType());
        PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());

        RtsMoveOrderVisuals.clearCommander(playerRef, store);

        session.setActive(false);
        session.clearSelection();
        accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);

        RtsCommanderNpcVisibility.restore(playerRef, store);

        RtsEntityViewSupport.restore(playerRef, accessor, session);
        RtsCommanderVisibility.showCommander(world, playerRef);
        teleportCommanderToExit(playerRef, accessor, plan);

        if (player != null && pr != null) {
            RtsCommandHudSupport.removeHud(player, pr);
            RtsBoxSelectHudSupport.removeHud(player, pr);
            RtsHudVisibility.showGameplayHud(player, pr);
        }
        RtsInventorySwap.restoreHotbar(playerRef, accessor, saved);

        session.setSavedHotbarJson("");
        session.setSessionExitedSafely(true);
        accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);

        if (pr != null) {
            runWhenTeleportSettled(world, playerRef, store, 0, () -> finishExitClientRestore(playerRef, store, pr, plan));
        }
    }

    private static void finishExitClientRestore(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef pr,
        @Nonnull ExitPlan plan
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session != null && session.isActive()) {
            return;
        }

        RtsExitMovementGuard.arm(pr.getUuid(), plan.x(), plan.y(), plan.z(), plan.yaw(), plan.pitch(), plan.roll());
        RtsMovementSupport.restore(pr, playerRef, store);
        CameraManager camera = store.getComponent(playerRef, CameraManager.getComponentType());
        if (camera != null) {
            camera.resetCamera(pr);
        } else {
            TopDownCameraService.reset(pr);
        }

        RtsExitPositionCache.clear(pr.getUuid());
    }

    private static void runWhenTeleportSettled(
        @Nonnull World world,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        int attempt,
        @Nonnull Runnable action
    ) {
        world.execute(() -> {
            if (!playerRef.isValid()) {
                return;
            }
            PendingTeleport pending = store.getComponent(playerRef, PendingTeleport.getComponentType());
            if (pending != null && !pending.isEmpty() && attempt < TELEPORT_SETTLE_MAX_ATTEMPTS) {
                runWhenTeleportSettled(world, playerRef, store, attempt + 1, action);
                return;
            }
            action.run();
        });
    }

    private static ExitPlan resolveExitPlan(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable PlayerRef pr
    ) {
        double exitX = session.getExitX();
        double exitY = session.getExitY();
        double exitZ = session.getExitZ();
        float exitYaw = 0f;
        float exitPitch = 0f;
        float exitRoll = 0f;
        if (pr != null) {
            RtsExitPositionCache.ExitSnapshot cached = RtsExitPositionCache.peek(pr.getUuid());
            if (cached != null) {
                exitX = cached.x();
                exitY = cached.y();
                exitZ = cached.z();
                exitYaw = cached.yaw();
                exitPitch = cached.pitch();
                exitRoll = cached.roll();
            }
        }
        boolean usedPostFallback = false;
        if (!session.hasExitBody() || !isPlausibleBody(exitX, exitY, exitZ)) {
            usedPostFallback = true;
            exitX = session.getPostX() + 0.5;
            exitY = session.getPostY() + 1.0;
            exitZ = session.getPostZ() + 0.5;
        }
        return new ExitPlan(exitX, exitY, exitZ, exitYaw, exitPitch, exitRoll, usedPostFallback);
    }

    private static void applyBodyTeleport(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull org.joml.Vector3d dest,
        float pitch,
        float yaw,
        float roll
    ) {
        TransformComponent tc = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        tc.getPosition().set(dest);
        tc.getRotation().set(pitch, yaw, roll);
        accessor.putComponent(playerRef, TransformComponent.getComponentType(), tc);
        accessor.addComponent(
            playerRef,
            Teleport.getComponentType(),
            Teleport.createForPlayer(dest, tc.getRotation())
        );
        Velocity vel = accessor.getComponent(playerRef, Velocity.getComponentType());
        if (vel != null) {
            vel.setZero();
            accessor.putComponent(playerRef, Velocity.getComponentType(), vel);
        }
    }

    private static void teleportCommanderToExit(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull ExitPlan plan
    ) {
        TransformComponent tc = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        org.joml.Vector3d dest = new org.joml.Vector3d(plan.x(), plan.y(), plan.z());
        applyBodyTeleport(playerRef, accessor, dest, plan.pitch(), plan.yaw(), plan.roll());
    }

    public static void exit(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store) {
        exit(playerRef, (ComponentAccessor<EntityStore>) store);
    }

    /**
     * Restores the hotbar saved when the player last entered RTS mode.
     * Used after a crash or unclean shutdown that skipped normal exit cleanup.
     */
    @Nonnull
    public static RecoverSavedInventoryResult recoverSavedInventory(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        RtsCommandPlayerComponent session = accessor.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.hasRecoverableSavedHotbar()) {
            return RecoverSavedInventoryResult.NONE_SAVED;
        }
        if (session.isActive()) {
            exit(playerRef, accessor);
            return RecoverSavedInventoryResult.RESTORED_AND_EXITED;
        }
        if (recoverUncleanSession(playerRef, accessor, accessor.getComponent(playerRef, PlayerRef.getComponentType()))) {
            return RecoverSavedInventoryResult.RESTORED;
        }
        return RecoverSavedInventoryResult.NONE_SAVED;
    }

    /**
     * Auto-recovery after login when the last RTS session did not exit cleanly.
     *
     * @return true if hotbar was restored
     */
    public static boolean recoverUncleanSession(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nullable PlayerRef pr
    ) {
        RtsCommandPlayerComponent session = accessor.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || session.isActive() || !session.needsUncleanSessionRecovery()) {
            return false;
        }
        String saved = session.getSavedHotbarJson();
        RtsInventorySwap.restoreHotbar(playerRef, accessor, saved);
        session.setSavedHotbarJson("");
        session.setSessionExitedSafely(true);
        accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        releaseOrphanedTownGuards(accessor, session);
        if (pr != null) {
            RtsMovementSupport.restore(pr, playerRef, accessor);
            CameraManager camera = accessor.getComponent(playerRef, CameraManager.getComponentType());
            if (camera != null) {
                camera.resetCamera(pr);
            } else {
                TopDownCameraService.reset(pr);
            }
        }
        return true;
    }

    public enum RecoverSavedInventoryResult {
        RESTORED,
        RESTORED_AND_EXITED,
        NONE_SAVED
    }

    public static void freeAllCommandedGuardsInTown(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town
    ) {
        for (Ref<EntityStore> ref : RtsGuardDirectory.livingGuardRefs(town, store)) {
            if (accessor.getComponent(ref, GuardRtsCommandState.getComponentType()) != null) {
                freeGuard(ref, accessor);
            }
        }
    }

    public static void freeGuard(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        if (accessor.getComponent(ref, GuardRtsCommandState.getComponentType()) == null) {
            return;
        }
        RtsGuardRelease.release(ref, accessor, null);
    }

    public static void freeGuard(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        RtsGuardRelease.release(ref, accessor, commandBuffer);
    }

    @Nonnull
    public static RtsCommandPlayerComponent requireSession(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            throw new IllegalStateException("RTS command mode not active");
        }
        return session;
    }

    public static void clampFocusToTerritory(
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull TownRecord town,
        @Nonnull TownManager townManager
    ) {
        int overlap = com.hexvane.aetherhaven.AetherhavenConstants.RTS_TERRITORY_OVERLAP_BLOCKS;
        int cx = town.getCharterX();
        int cz = town.getCharterZ();
        int r = town.getTerritoryChunkRadius() * 16 + overlap;
        double minX = cx - r;
        double maxX = cx + r;
        double minZ = cz - r;
        double maxZ = cz + r;
        double fx = Math.max(minX, Math.min(maxX, session.getFocusX()));
        double fz = Math.max(minZ, Math.min(maxZ, session.getFocusZ()));
        session.setFocus(fx, session.getFocusY(), fz);
    }

    private static boolean isPlausibleBody(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return false;
        }
        if (Math.abs(x) < 0.5 && Math.abs(z) < 0.5 && y < 2.0) {
            return false;
        }
        return y > -64 && y < 320;
    }

    private static void releaseOrphanedTownGuards(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        String townId = session.getTownId();
        if (townId == null || townId.isBlank()) {
            return;
        }
        Store<EntityStore> store = accessor instanceof Store<EntityStore> s ? s : null;
        if (store == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        try {
            TownRecord town = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin).getTown(UUID.fromString(townId));
            if (town != null) {
                freeAllCommandedGuardsInTown(accessor, store, town);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static void notify(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull String key
    ) {
        PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr != null) {
            NotificationUtil.sendNotification(pr.getPacketHandler(), Message.translation(key), NotificationStyle.Warning);
        }
    }

    private record ExitPlan(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll,
        boolean usedPostFallback
    ) {}
}
