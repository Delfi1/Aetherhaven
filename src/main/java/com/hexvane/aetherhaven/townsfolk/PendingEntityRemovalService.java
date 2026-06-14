package com.hexvane.aetherhaven.townsfolk;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.EntityChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nonnull;

/** Queues NPC despawn until the next {@link EntityStore} tick so chunk save never sees invalidated refs. */
public final class PendingEntityRemovalService {
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<UUID>> PENDING_BY_WORLD = new ConcurrentHashMap<>();

    private PendingEntityRemovalService() {}

    public static void schedule(@Nonnull World world, @Nonnull UUID entityUuid) {
        if (!world.isAlive()) {
            return;
        }
        PENDING_BY_WORLD.computeIfAbsent(world.getName(), k -> new ConcurrentLinkedQueue<>()).add(entityUuid);
    }

    public static void scheduleAll(@Nonnull World world, @Nonnull List<UUID> entityUuids) {
        if (!world.isAlive() || entityUuids.isEmpty()) {
            return;
        }
        ConcurrentLinkedQueue<UUID> queue = PENDING_BY_WORLD.computeIfAbsent(world.getName(), k -> new ConcurrentLinkedQueue<>());
        for (UUID entityUuid : entityUuids) {
            if (entityUuid != null) {
                queue.add(entityUuid);
            }
        }
    }

    /** Called from {@link PendingEntityRemovalSystem} once per entity store tick (before chunk store tick). */
    static void flush(@Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        String worldName = world.getName();
        if (!world.isAlive()) {
            PENDING_BY_WORLD.remove(worldName);
            return;
        }
        ConcurrentLinkedQueue<UUID> queue = PENDING_BY_WORLD.get(worldName);
        if (queue == null) {
            return;
        }
        UUID entityUuid;
        while ((entityUuid = queue.poll()) != null) {
            removeNow(world, store, entityUuid);
        }
    }

    private static void removeNow(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull UUID entityUuid) {
        Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(entityUuid);
        if (ref == null || !ref.isValid()) {
            return;
        }
        detachFromEntityChunk(world, store, ref);
        if (ref.isValid()) {
            store.removeEntity(ref, RemoveReason.REMOVE);
        }
    }

    private static void detachFromEntityChunk(
        @Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref
    ) {
        boolean removed = false;
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            Ref<ChunkStore> chunkRef = transform.getChunkRef();
            if (chunkRef != null && chunkRef.isValid()) {
                removed = removeEntityReferenceFromChunk(world, chunkRef, ref);
            }
        }
        if (!removed) {
            removeEntityReferenceFromLoadedChunks(world, ref);
        }
    }

    private static boolean removeEntityReferenceFromChunk(
        @Nonnull World world, @Nonnull Ref<ChunkStore> chunkRef, @Nonnull Ref<EntityStore> ref
    ) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        EntityChunk entityChunk = chunkStore.getComponent(chunkRef, EntityChunk.getComponentType());
        if (entityChunk == null || !entityChunk.getEntityReferences().contains(ref)) {
            return false;
        }
        entityChunk.removeEntityReference(ref);
        return true;
    }

    /** Fallback when {@link TransformComponent#getChunkRef()} was cleared (e.g. transform replaced without chunk linkage). */
    private static void removeEntityReferenceFromLoadedChunks(@Nonnull World world, @Nonnull Ref<EntityStore> ref) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        chunkStore.forEachChunk(
            EntityChunk.getComponentType(),
            (archetypeChunk, commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<ChunkStore> chunkRef = archetypeChunk.getReferenceTo(i);
                    if (chunkRef == null || !chunkRef.isValid()) {
                        continue;
                    }
                    removeEntityReferenceFromChunk(world, chunkRef, ref);
                }
            }
        );
    }

    /** Drops invalidated entity refs so chunk serialization never sees them. */
    static void pruneInvalidEntityReferences(@Nonnull Store<ChunkStore> chunkStore) {
        chunkStore.forEachChunk(
            EntityChunk.getComponentType(),
            (archetypeChunk, commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<ChunkStore> chunkRef = archetypeChunk.getReferenceTo(i);
                    if (chunkRef == null || !chunkRef.isValid()) {
                        continue;
                    }
                    EntityChunk entityChunk = chunkStore.getComponent(chunkRef, EntityChunk.getComponentType());
                    if (entityChunk == null) {
                        continue;
                    }
                    Set<Ref<EntityStore>> entityReferences = entityChunk.getEntityReferences();
                    if (entityReferences.isEmpty()) {
                        continue;
                    }
                    List<Ref<EntityStore>> stale = null;
                    for (Ref<EntityStore> entityRef : entityReferences) {
                        if (!entityRef.isValid()) {
                            if (stale == null) {
                                stale = new ArrayList<>();
                            }
                            stale.add(entityRef);
                        }
                    }
                    if (stale != null) {
                        for (Ref<EntityStore> entityRef : stale) {
                            entityChunk.removeEntityReference(entityRef);
                        }
                    }
                }
            }
        );
    }
}
