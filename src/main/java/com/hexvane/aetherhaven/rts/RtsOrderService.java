package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class RtsOrderService {
    private RtsOrderService() {}

    public static void issueMoveOrder(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session,
        double groundX,
        double groundY,
        double groundZ
    ) {
        Store<EntityStore> store = playerRef.getStore();
        List<UUID> selected = new ArrayList<>(session.getSelectedGuardUuids());
        if (selected.isEmpty()) {
            return;
        }
        List<Vector3d> offsets = RtsFormationMath.lineOffsets(selected.size());
        UUID commander = commanderUuid(playerRef, accessor);
        for (int i = 0; i < selected.size(); i++) {
            Ref<EntityStore> guardRef = RtsGuardDirectory.findByUuid(store, selected.get(i));
            if (guardRef == null) {
                continue;
            }
            Vector3d off = i < offsets.size() ? offsets.get(i) : new Vector3d();
            GuardRtsCommandState cmd = accessor.getComponent(guardRef, GuardRtsCommandState.getComponentType());
            if (cmd == null) {
                cmd = new GuardRtsCommandState();
            }
            cmd.setOrderMode(session.getOrderMode());
            cmd.setCombatStance(session.getStanceMode());
            cmd.setHold(groundX + off.x, groundY + off.y, groundZ + off.z);
            cmd.setPhase(RtsCommandPhase.TRAVELING);
            cmd.setFocusFire(false);
            cmd.setTargetEntityUuid(null);
            cmd.setCommanderPlayerUuid(commander);
            accessor.putComponent(guardRef, GuardRtsCommandState.getComponentType(), cmd);
        }
        RtsMoveOrderVisuals.spawn(
            store,
            playerRef,
            groundX,
            groundY,
            groundZ,
            selected,
            store.getExternalData().getWorld().getTick()
        );
    }

    public static void issueFocusAttack(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull Ref<EntityStore> targetRef
    ) {
        Store<EntityStore> store = playerRef.getStore();
        if (!RtsHostileQuery.isAggressiveNpc(targetRef, store)) {
            return;
        }
        UUID targetUuid = RtsHostileQuery.entityUuid(targetRef, store);
        if (targetUuid == null) {
            return;
        }
        UUID commander = commanderUuid(playerRef, accessor);
        for (UUID guardId : session.getSelectedGuardUuids()) {
            Ref<EntityStore> guardRef = RtsGuardDirectory.findByUuid(store, guardId);
            if (guardRef == null) {
                continue;
            }
            GuardRtsCommandState cmd = accessor.getComponent(guardRef, GuardRtsCommandState.getComponentType());
            if (cmd == null) {
                cmd = new GuardRtsCommandState();
                var tc = accessor.getComponent(guardRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
                if (tc != null) {
                    var p = tc.getPosition();
                    cmd.setHold(p.x, p.y, p.z);
                }
            }
            cmd.setFocusFire(true);
            cmd.setTargetEntityUuid(targetUuid);
            cmd.setPhase(RtsCommandPhase.ENGAGING);
            cmd.setCombatStance(RtsCombatStance.AGGRESSIVE);
            cmd.setCommanderPlayerUuid(commander);
            accessor.putComponent(guardRef, GuardRtsCommandState.getComponentType(), cmd);
        }
    }

    public static void stopSelected(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Store<EntityStore> store = playerRef.getStore();
        UUID commander = commanderUuid(playerRef, accessor);
        for (UUID guardId : session.getSelectedGuardUuids()) {
            Ref<EntityStore> guardRef = RtsGuardDirectory.findByUuid(store, guardId);
            if (guardRef == null) {
                continue;
            }
            var tc = accessor.getComponent(guardRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc == null) {
                continue;
            }
            var p = tc.getPosition();
            GuardRtsCommandState cmd = accessor.getComponent(guardRef, GuardRtsCommandState.getComponentType());
            if (cmd == null) {
                cmd = new GuardRtsCommandState();
            }
            cmd.setHold(p.x, p.y, p.z);
            cmd.setPhase(RtsCommandPhase.HOLDING);
            cmd.setOrderMode(session.getOrderMode());
            cmd.setCombatStance(RtsCombatStance.AGGRESSIVE);
            cmd.setFocusFire(false);
            cmd.setTargetEntityUuid(null);
            cmd.setCommanderPlayerUuid(commander);
            accessor.putComponent(guardRef, GuardRtsCommandState.getComponentType(), cmd);
        }
    }

    public static void applyStanceToSelected(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Store<EntityStore> store = playerRef.getStore();
        for (UUID guardId : session.getSelectedGuardUuids()) {
            Ref<EntityStore> guardRef = RtsGuardDirectory.findByUuid(store, guardId);
            if (guardRef == null) {
                continue;
            }
            GuardRtsCommandState cmd = accessor.getComponent(guardRef, GuardRtsCommandState.getComponentType());
            if (cmd != null) {
                cmd.setCombatStance(session.getStanceMode());
                accessor.putComponent(guardRef, GuardRtsCommandState.getComponentType(), cmd);
            }
        }
    }

    public static void freeSelected(
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        Store<EntityStore> store = playerRef.getStore();
        for (UUID guardId : new ArrayList<>(session.getSelectedGuardUuids())) {
            Ref<EntityStore> guardRef = RtsGuardDirectory.findByUuid(store, guardId);
            if (guardRef != null) {
                RtsCommandService.freeGuard(guardRef, accessor);
            }
        }
    }

    @Nonnull
    private static UUID commanderUuid(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        UUIDComponent uc = accessor.getComponent(playerRef, UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : new UUID(0L, 0L);
    }
}
