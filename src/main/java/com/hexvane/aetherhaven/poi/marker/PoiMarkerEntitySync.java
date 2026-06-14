package com.hexvane.aetherhaven.poi.marker;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Keeps prefab POI marker entities aligned with {@link com.hexvane.aetherhaven.poi.PoiRegistry} edits. */
public final class PoiMarkerEntitySync {
    private PoiMarkerEntitySync() {}

    public static void moveMarkerForPoi(
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull UUID poiRegistryId,
        int blockX,
        int blockY,
        int blockZ
    ) {
        Vector3d pos = new Vector3d(blockX + 0.5, blockY + 0.5, blockZ + 0.5);
        store.forEachChunk(
            Query.and(PoiMarkerEntity.getComponentType(), PoiMarkerDataComponent.getComponentType()),
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> cb) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    PoiMarkerDataComponent data = chunk.getComponent(i, PoiMarkerDataComponent.getComponentType());
                    if (data == null || data.getPoiRegistryId() == null || !poiRegistryId.equals(data.getPoiRegistryId())) {
                        continue;
                    }
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    Rotation3f rot = tc != null ? tc.getRotation() : new Rotation3f(0f, 0f, 0f);
                    commandBuffer.putComponent(ref, TransformComponent.getComponentType(), new TransformComponent(pos, rot));
                    HeadRotation head = store.getComponent(ref, HeadRotation.getComponentType());
                    if (head != null) {
                        head.teleportRotation(rot);
                        commandBuffer.putComponent(ref, HeadRotation.getComponentType(), head);
                    }
                    return;
                }
            }
        );
    }
}
