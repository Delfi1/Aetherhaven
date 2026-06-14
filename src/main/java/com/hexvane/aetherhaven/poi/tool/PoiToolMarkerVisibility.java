package com.hexvane.aetherhaven.poi.tool;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Prefab POI / adventurer spawn markers stay hidden in the world; {@link PoiToolVisualizationSystem} spawns per-player
 * {@link PoiDebugLabelEntity} overlays while the POI debug staff is held in the matching mode.
 */
public final class PoiToolMarkerVisibility {
    private PoiToolMarkerVisibility() {}

    /** World marker entities are never rendered directly. */
    public static boolean isHiddenWorldMarker(
        @Nonnull Ref<EntityStore> viewerRef,
        @Nonnull ComponentAccessor<EntityStore> componentAccessor
    ) {
        return true;
    }

    public static boolean showsRegistryAndPrefabPoiLabels(@Nonnull PoiToolMode mode) {
        return mode == PoiToolMode.PoiEdit || mode == PoiToolMode.PoiPlacement || mode == PoiToolMode.PoiRemove;
    }

    public static boolean showsAdventurerSpawnLabels(@Nonnull PoiToolMode mode) {
        return mode == PoiToolMode.AdventurerSpawnMarker;
    }
}
