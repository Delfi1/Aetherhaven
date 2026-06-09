package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

/** Frees guards still commanded after RTS ended, crashed, or the commander logged out. */
public final class RtsOrphanedGuardRecoverySystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();

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
        if (cmd == null) {
            return;
        }
        UUID commanderUuid = cmd.getCommanderPlayerUuid();
        if (commanderUuid != null && isCommanderActivelyCommanding(store, commanderUuid)) {
            return;
        }
        Ref<EntityStore> guardRef = chunk.getReferenceTo(index);
        RtsGuardRelease.release(guardRef, store, commandBuffer);
    }

    private static boolean isCommanderActivelyCommanding(@Nonnull Store<EntityStore> store, @Nonnull UUID commanderUuid) {
        Ref<EntityStore> commanderRef = store.getExternalData().getRefFromUUID(commanderUuid);
        if (commanderRef == null || !commanderRef.isValid()) {
            return false;
        }
        RtsCommandPlayerComponent session = store.getComponent(commanderRef, RtsCommandPlayerComponent.getComponentType());
        return session != null && session.isActive();
    }
}
