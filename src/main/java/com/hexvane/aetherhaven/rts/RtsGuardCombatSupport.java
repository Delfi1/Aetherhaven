package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Bridges Java RTS engage logic to guard role combat (LockedTarget + hostile memory). */
public final class RtsGuardCombatSupport {
    /** Default marked-entity slot used by guard {@code Target} sensors in combat instructions. */
    public static final String LOCKED_TARGET_SLOT = "LockedTarget";

    private RtsGuardCombatSupport() {}

    public static void lockCombatTarget(
        @Nonnull NPCEntity npc,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        Role role = npc.getRole();
        if (role == null) {
            return;
        }
        role.getMarkedEntitySupport().setMarkedEntity(LOCKED_TARGET_SLOT, targetRef);
        rememberHostile(npc.getReference(), targetRef, accessor);
    }

    public static void clearCombatTarget(@Nonnull NPCEntity npc, @Nonnull ComponentAccessor<EntityStore> accessor) {
        Role role = npc.getRole();
        if (role == null) {
            return;
        }
        MarkedEntitySupport marked = role.getMarkedEntitySupport();
        marked.setMarkedEntity(LOCKED_TARGET_SLOT, null);
        marked.setMarkedEntity("CombatTargets", null);
        Ref<EntityStore> guardRef = npc.getReference();
        if (guardRef != null && guardRef.isValid()) {
            clearHostileMemory(guardRef, accessor);
        }
    }

    public static void rememberHostile(
        @Nullable Ref<EntityStore> guardRef,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        if (guardRef == null || !guardRef.isValid()) {
            return;
        }
        TargetMemory memory = accessor.getComponent(guardRef, TargetMemory.getComponentType());
        if (memory == null) {
            return;
        }
        Int2FloatOpenHashMap hostiles = memory.getKnownHostiles();
        if (hostiles.put(targetRef.getIndex(), memory.getRememberFor()) <= 0.0F) {
            memory.getKnownHostilesList().add(targetRef);
        }
        memory.setClosestHostile(targetRef);
    }

    public static void clearHostileMemory(
        @Nonnull Ref<EntityStore> guardRef,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        TargetMemory memory = accessor.getComponent(guardRef, TargetMemory.getComponentType());
        if (memory == null) {
            return;
        }
        memory.getKnownHostiles().clear();
        memory.getKnownHostilesList().clear();
        memory.setClosestHostile(null);
    }

    /** Marks the hostile so it will fight the engaging guard, not only flee the commander. */
    public static void promptCounterAttack(
        @Nonnull Ref<EntityStore> guardRef,
        @Nonnull Ref<EntityStore> hostileRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        if (guardRef.equals(hostileRef)) {
            return;
        }
        NPCEntity hostile = accessor.getComponent(hostileRef, NPCEntity.getComponentType());
        if (hostile == null || hostile.getRole() == null) {
            return;
        }
        rememberHostile(hostileRef, guardRef, accessor);
        hostile.getRole().getMarkedEntitySupport().setMarkedEntity(LOCKED_TARGET_SLOT, guardRef);
        String state = hostile.getRole().getStateSupport().getStateName();
        if (state.contains("Combat")) {
            return;
        }
        ComponentAccessor<EntityStore> stateAccessor = commandBuffer != null ? commandBuffer : accessor;
        hostile.getRole().getStateSupport().setState(hostileRef, "Combat", null, stateAccessor);
        if (commandBuffer != null) {
            commandBuffer.putComponent(hostileRef, NPCEntity.getComponentType(), hostile);
        } else {
            accessor.putComponent(hostileRef, NPCEntity.getComponentType(), hostile);
        }
    }
}
