package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.systems.RoleSystems;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Clears stale NPC marked targets pointing at RTS commanders (mirrors creative non-detectable cleanup in
 * {@link RoleSystems.PreBehaviourSupportTickSystem}).
 */
public final class RtsCommanderNpcStealthSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
        new SystemDependency<>(Order.AFTER, RoleSystems.PreBehaviourSupportTickSystem.class),
        new SystemDependency<>(Order.BEFORE, RoleSystems.BehaviourTickSystem.class)
    );

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            return;
        }
        clearHiddenCommanderTargets(npc.getRole(), commandBuffer);
    }

    /** One-shot sweep when entering RTS so mobs already targeting the commander drop them immediately. */
    public static void clearMarkedTargetsForPlayer(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        if (!playerRef.isValid()) {
            return;
        }
        store.forEachChunk(
            NPCEntity.getComponentType(),
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    NPCEntity npc = chunk.getComponent(i, NPCEntity.getComponentType());
                    if (npc == null || npc.getRole() == null) {
                        continue;
                    }
                    if (clearPlayerFromMarkedTargets(npc.getRole(), playerRef)) {
                        commandBuffer.putComponent(chunk.getReferenceTo(i), NPCEntity.getComponentType(), npc);
                    }
                }
            }
        );
    }

    private static void clearHiddenCommanderTargets(
        @Nonnull Role role,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        MarkedEntitySupport marked = role.getMarkedEntitySupport();
        Ref<EntityStore>[] targets = marked.getEntityTargets();
        boolean changed = false;
        for (int i = 0; i < targets.length; i++) {
            Ref<EntityStore> target = targets[i];
            if (target == null || !target.isValid()) {
                continue;
            }
            if (accessor.getComponent(target, Player.getComponentType()) == null) {
                continue;
            }
            if (RtsCommanderNpcDetection.isHiddenFromNpcs(target, accessor)) {
                targets[i] = null;
                changed = true;
            }
        }
        if (changed) {
            role.getStateSupport().update(accessor);
        }
    }

    private static boolean clearPlayerFromMarkedTargets(
        @Nonnull Role role,
        @Nonnull Ref<EntityStore> playerRef
    ) {
        MarkedEntitySupport marked = role.getMarkedEntitySupport();
        Ref<EntityStore>[] targets = marked.getEntityTargets();
        boolean changed = false;
        for (int i = 0; i < targets.length; i++) {
            if (playerRef.equals(targets[i])) {
                targets[i] = null;
                changed = true;
            }
        }
        return changed;
    }
}
