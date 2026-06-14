package com.hexvane.aetherhaven.guild.marker;

import com.hexvane.aetherhaven.poi.tool.PoiToolMarkerVisibility;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Prefab saveable marker for guild hall adventurer spawn slots. Rendered via per-player debug overlays while the POI
 * staff is held in adventurer spot mode.
 */
public final class AdventurerSpawnMarkerEntity extends Entity {
    @Nonnull
    public static final com.hypixel.hytale.codec.builder.BuilderCodec<AdventurerSpawnMarkerEntity> CODEC =
        com.hypixel.hytale.codec.builder.BuilderCodec.builder(
                AdventurerSpawnMarkerEntity.class,
                AdventurerSpawnMarkerEntity::new,
                Entity.CODEC
            )
            .build();

    @Nullable
    public static com.hypixel.hytale.component.ComponentType<EntityStore, AdventurerSpawnMarkerEntity> getComponentType() {
        return EntityModule.get().getComponentType(AdventurerSpawnMarkerEntity.class);
    }

    public AdventurerSpawnMarkerEntity() {}

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isHiddenFromLivingEntity(
        @Nonnull Ref<EntityStore> markerRef,
        @Nonnull Ref<EntityStore> viewerRef,
        @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        return PoiToolMarkerVisibility.isHiddenWorldMarker(viewerRef, componentAccessor);
    }
}
