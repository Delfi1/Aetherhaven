package com.hexvane.aetherhaven.poi.marker;

import com.hexvane.aetherhaven.poi.tool.PoiToolMarkerVisibility;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Prefab-saveable marker for villager autonomy POIs. Rendered via per-player debug overlays while the POI staff is held
 * in POI edit, placement, or remove mode.
 */
public final class PoiMarkerEntity extends Entity {
    @Nonnull
    public static final com.hypixel.hytale.codec.builder.BuilderCodec<PoiMarkerEntity> CODEC =
        com.hypixel.hytale.codec.builder.BuilderCodec.builder(PoiMarkerEntity.class, PoiMarkerEntity::new, Entity.CODEC).build();

    @Nullable
    public static com.hypixel.hytale.component.ComponentType<EntityStore, PoiMarkerEntity> getComponentType() {
        return EntityModule.get().getComponentType(PoiMarkerEntity.class);
    }

    public PoiMarkerEntity() {}

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
