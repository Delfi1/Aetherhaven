package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Drives commanded guards: travel, hold, engage, return per order mode and combat stance. */
public final class GuardRtsCommandSystem extends EntityTickingSystem<EntityStore> {
    private static final double ARRIVE_SQ =
        AetherhavenConstants.RTS_ARRIVE_RADIUS * AetherhavenConstants.RTS_ARRIVE_RADIUS;

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();
    @SuppressWarnings("unused")
    private final AetherhavenPlugin plugin;

    public GuardRtsCommandSystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(GuardRtsCommandState.getComponentType(), NPCEntity.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        GuardRtsCommandState cmd = chunk.getComponent(index, GuardRtsCommandState.getComponentType());
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (cmd == null || npc == null || npc.getRole() == null) {
            return;
        }
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3d pos = tc.getPosition();
        switch (cmd.getPhase()) {
            case TRAVELING -> tickTraveling(ref, npc, cmd, pos, store, commandBuffer);
            case HOLDING -> tickHolding(ref, npc, cmd, pos, store, commandBuffer);
            case ENGAGING -> tickEngaging(ref, npc, cmd, pos, store, commandBuffer);
            case RETURNING -> tickReturning(ref, npc, cmd, pos, commandBuffer);
        }
        commandBuffer.putComponent(ref, GuardRtsCommandState.getComponentType(), cmd);
        commandBuffer.putComponent(ref, NPCEntity.getComponentType(), npc);
    }

    private void tickTraveling(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (arrived(pos, cmd)) {
            cmd.setPhase(RtsCommandPhase.HOLDING);
            return;
        }
        npc.setLeashPoint(new Vector3d(cmd.getHoldX(), cmd.getHoldY(), cmd.getHoldZ()));

        if (cmd.getOrderMode() == RtsOrderMode.ATTACK_MOVE && cmd.getCombatStance() != RtsCombatStance.HOLD_FIRE) {
            Ref<EntityStore> hostile = findTravelEngageTarget(store, cmd, pos, npc);
            if (hostile != null) {
                beginEngage(ref, npc, cmd, hostile, store, commandBuffer);
                return;
            }
        }

        suppressOutOfRangeCombatDuringTravel(ref, npc, pos, store, commandBuffer);
        ensureRtsCommandMotion(ref, npc, commandBuffer);
    }

    private void tickHolding(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!arrived(pos, cmd)) {
            cmd.setPhase(RtsCommandPhase.TRAVELING);
            return;
        }
        npc.setLeashPoint(new Vector3d(cmd.getHoldX(), cmd.getHoldY(), cmd.getHoldZ()));
        ensureRtsCommandMotion(ref, npc, commandBuffer);
        if (cmd.getCombatStance() == RtsCombatStance.HOLD_FIRE && !cmd.isFocusFire()) {
            return;
        }
        if (cmd.isFocusFire() && cmd.getTargetEntityUuid() != null) {
            Ref<EntityStore> target = RtsGuardDirectory.findByUuid(store, cmd.getTargetEntityUuid());
            if (target != null && target.isValid() && RtsHostileQuery.isAggressiveNpc(target, store)) {
                beginEngage(ref, npc, cmd, target, store, commandBuffer);
            } else {
                cmd.setFocusFire(false);
                cmd.setTargetEntityUuid(null);
            }
            return;
        }
        Ref<EntityStore> hostile = findHoldEngageTarget(store, cmd, pos);
        if (hostile != null) {
            beginEngage(ref, npc, cmd, hostile, store, commandBuffer);
        }
    }

    private void tickEngaging(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        UUID targetId = cmd.getTargetEntityUuid();
        Ref<EntityStore> target = targetId != null ? RtsGuardDirectory.findByUuid(store, targetId) : null;
        Role role = npc.getRole();
        String state = role.getStateSupport().getStateName();

        if (target == null || !target.isValid() || !RtsHostileQuery.isAggressiveNpc(target, store)) {
            endEngagement(ref, npc, cmd, pos, store, commandBuffer);
            return;
        }

        if (!withinEngagementLimits(cmd, pos, target, store)) {
            if (atOrderDestination(pos, cmd)) {
                cmd.setPhase(RtsCommandPhase.RETURNING);
            }
            endEngagement(ref, npc, cmd, pos, store, commandBuffer);
            return;
        }

        if (!atOrderDestination(pos, cmd)
            && cmd.getOrderMode() == RtsOrderMode.ATTACK_MOVE
            && !isWithinHorizontalRange(store, pos, target, RtsGuardCombatRanges.attackEngageRange(npc))) {
            endEngagement(ref, npc, cmd, pos, store, commandBuffer);
            return;
        }

        RtsGuardCombatSupport.lockCombatTarget(npc, target, store);
        if (!state.contains("Combat")) {
            role.getStateSupport().setState(ref, "Combat", null, commandBuffer);
        }
    }

    private void tickReturning(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        npc.setLeashPoint(new Vector3d(cmd.getHoldX(), cmd.getHoldY(), cmd.getHoldZ()));
        ensureRtsCommandMotion(ref, npc, commandBuffer);
        if (arrived(pos, cmd)) {
            cmd.setPhase(RtsCommandPhase.HOLDING);
            cmd.setTargetEntityUuid(null);
            cmd.setFocusFire(false);
        }
    }

    private void beginEngage(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Ref<EntityStore> target,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        UUID tu = RtsHostileQuery.entityUuid(target, store);
        if (tu == null) {
            return;
        }
        cmd.setPhase(RtsCommandPhase.ENGAGING);
        cmd.setTargetEntityUuid(tu);
        RtsGuardCombatSupport.lockCombatTarget(npc, target, store);
        RtsGuardCombatSupport.promptCounterAttack(ref, target, store, commandBuffer);
        npc.getRole().getStateSupport().setState(ref, "Combat", null, commandBuffer);
    }

    private void endEngagement(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        cmd.setTargetEntityUuid(null);
        if (!cmd.isFocusFire()) {
            cmd.setPhase(atOrderDestination(pos, cmd) ? RtsCommandPhase.HOLDING : RtsCommandPhase.TRAVELING);
        }
        Role role = npc.getRole();
        RtsGuardCombatSupport.clearCombatTarget(npc, store);
        String state = role.getStateSupport().getStateName();
        if (state.contains("Combat")) {
            role.getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_GUARD_RTS_COMMAND, null, commandBuffer);
        }
    }

    @Nullable
    private static Ref<EntityStore> findTravelEngageTarget(
        @Nonnull Store<EntityStore> store,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull NPCEntity npc
    ) {
        double engageRange = RtsGuardCombatRanges.attackEngageRange(npc);
        Ref<EntityStore> hostile = RtsHostileQuery.nearestHostile(
            store,
            pos.x,
            pos.y,
            pos.z,
            engageRange
        );
        if (hostile == null) {
            return null;
        }
        TransformComponent htc = store.getComponent(hostile, TransformComponent.getComponentType());
        if (htc == null) {
            return null;
        }
        Vector3d hp = htc.getPosition();
        double dist = RtsHostileQuery.horizontalDistance(pos.x, pos.z, hp.x, hp.z);
        if (dist > engageRange) {
            return null;
        }
        if (cmd.getCombatStance() == RtsCombatStance.STAND_GROUND) {
            if (dist > AetherhavenConstants.RTS_STAND_GROUND_RANGE) {
                return null;
            }
        }
        return hostile;
    }

    /** Keep traveling to the order point until a hostile is actually in weapon range. */
    private static void suppressOutOfRangeCombatDuringTravel(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull Vector3d pos,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Role role = npc.getRole();
        String state = role.getStateSupport().getStateName();
        if (!state.contains("Combat")) {
            return;
        }
        double engageRange = RtsGuardCombatRanges.attackEngageRange(npc);
        Ref<EntityStore> hostile = RtsHostileQuery.nearestHostile(
            store,
            pos.x,
            pos.y,
            pos.z,
            AetherhavenConstants.RTS_DEFEND_RADIUS
        );
        if (hostile != null && isWithinHorizontalRange(store, pos, hostile, engageRange)) {
            return;
        }
        role.getMarkedEntitySupport().setMarkedEntity(RtsGuardCombatSupport.LOCKED_TARGET_SLOT, null);
        role.getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_GUARD_RTS_COMMAND, null, commandBuffer);
    }

    private static boolean isWithinHorizontalRange(
        @Nonnull Store<EntityStore> store,
        @Nonnull Vector3d from,
        @Nonnull Ref<EntityStore> target,
        double range
    ) {
        TransformComponent htc = store.getComponent(target, TransformComponent.getComponentType());
        if (htc == null) {
            return false;
        }
        Vector3d hp = htc.getPosition();
        return RtsHostileQuery.horizontalDistance(from.x, from.z, hp.x, hp.z) <= range;
    }

    @Nullable
    private static Ref<EntityStore> findHoldEngageTarget(
        @Nonnull Store<EntityStore> store,
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos
    ) {
        Ref<EntityStore> nearGuard = RtsHostileQuery.nearestHostile(
            store,
            pos.x,
            pos.y,
            pos.z,
            AetherhavenConstants.RTS_DEFEND_RADIUS
        );
        Ref<EntityStore> nearHold = RtsHostileQuery.nearestHostile(
            store,
            cmd.getHoldX(),
            cmd.getHoldY(),
            cmd.getHoldZ(),
            AetherhavenConstants.RTS_DEFEND_RADIUS
        );
        Ref<EntityStore> hostile = nearerHostile(pos, nearGuard, nearHold, store);
        if (hostile == null) {
            return null;
        }
        if (cmd.getCombatStance() == RtsCombatStance.STAND_GROUND) {
            TransformComponent htc = store.getComponent(hostile, TransformComponent.getComponentType());
            if (htc == null) {
                return null;
            }
            Vector3d hp = htc.getPosition();
            double dist = RtsHostileQuery.horizontalDistance(pos.x, pos.z, hp.x, hp.z);
            if (dist > AetherhavenConstants.RTS_STAND_GROUND_RANGE) {
                return null;
            }
        }
        return hostile;
    }

    @Nullable
    private static Ref<EntityStore> nearerHostile(
        @Nonnull Vector3d pos,
        @Nullable Ref<EntityStore> a,
        @Nullable Ref<EntityStore> b,
        @Nonnull Store<EntityStore> store
    ) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        TransformComponent at = store.getComponent(a, TransformComponent.getComponentType());
        TransformComponent bt = store.getComponent(b, TransformComponent.getComponentType());
        if (at == null) {
            return b;
        }
        if (bt == null) {
            return a;
        }
        Vector3d ap = at.getPosition();
        Vector3d bp = bt.getPosition();
        double aSq = horizontalDistanceSq(pos.x, pos.z, ap.x, ap.z);
        double bSq = horizontalDistanceSq(pos.x, pos.z, bp.x, bp.z);
        return aSq <= bSq ? a : b;
    }

    private static boolean withinEngagementLimits(
        @Nonnull GuardRtsCommandState cmd,
        @Nonnull Vector3d pos,
        @Nonnull Ref<EntityStore> target,
        @Nonnull Store<EntityStore> store
    ) {
        double guardToHold = RtsHostileQuery.horizontalDistance(pos.x, pos.z, cmd.getHoldX(), cmd.getHoldZ());

        if (cmd.getOrderMode() == RtsOrderMode.ATTACK_MOVE && !atOrderDestination(pos, cmd)) {
            return guardToHold <= AetherhavenConstants.RTS_ATTACK_MOVE_CHASE_RADIUS;
        }

        double leash = leashRadius(cmd);
        if (leash > 0.01 && guardToHold > leash) {
            return false;
        }

        TransformComponent htc = store.getComponent(target, TransformComponent.getComponentType());
        if (htc == null) {
            return true;
        }
        Vector3d hp = htc.getPosition();
        double enemyToHold = RtsHostileQuery.horizontalDistance(hp.x, hp.z, cmd.getHoldX(), cmd.getHoldZ());
        return enemyToHold <= AetherhavenConstants.RTS_DEFEND_RADIUS + Math.max(leash, 0.0);
    }

    private static boolean atOrderDestination(@Nonnull Vector3d pos, @Nonnull GuardRtsCommandState cmd) {
        return arrived(pos, cmd);
    }

    private static boolean arrived(@Nonnull Vector3d pos, @Nonnull GuardRtsCommandState cmd) {
        return horizontalDistanceSq(pos.x, pos.z, cmd.getHoldX(), cmd.getHoldZ()) <= ARRIVE_SQ;
    }

    private static double horizontalDistanceSq(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return dx * dx + dz * dz;
    }

    private static double leashRadius(@Nonnull GuardRtsCommandState cmd) {
        if (cmd.isFocusFire()) {
            return AetherhavenConstants.RTS_AGGRESSIVE_LEASH_RADIUS;
        }
        return switch (cmd.getCombatStance()) {
            case DEFENSIVE -> AetherhavenConstants.RTS_DEFENSIVE_LEASH_RADIUS;
            case AGGRESSIVE -> AetherhavenConstants.RTS_AGGRESSIVE_LEASH_RADIUS;
            case STAND_GROUND -> AetherhavenConstants.RTS_STAND_GROUND_LEASH_RADIUS;
            case HOLD_FIRE -> 0;
        };
    }

    private static void ensureRtsCommandMotion(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull NPCEntity npc,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        String state = npc.getRole().getStateSupport().getStateName();
        if (!state.contains(AetherhavenConstants.NPC_STATE_GUARD_RTS_COMMAND)) {
            npc.getRole()
                .getStateSupport()
                .setState(ref, AetherhavenConstants.NPC_STATE_GUARD_RTS_COMMAND, null, commandBuffer);
        }
    }
}
