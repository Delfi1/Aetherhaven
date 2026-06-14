package com.hexvane.aetherhaven.poi.marker;

import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.construction.PrefabLocalOffset;
import com.hexvane.aetherhaven.poi.PoiEntry;
import com.hexvane.aetherhaven.poi.PoiInteractionKind;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Collects prefab POI marker entities inside a completed plot footprint. */
public final class PoiMarkerLocator {
    private PoiMarkerLocator() {}

    public record LocalMarkerRow(int localX, int localY, int localZ, @Nonnull PoiMarkerDataComponent data) {}

    @Nonnull
    public static List<LocalMarkerRow> collectInPlot(
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotInstance plot,
        @Nonnull ConstructionDefinition def
    ) {
        Vector3i anchor = plot.resolvePrefabAnchorWorld(def);
        Rotation yaw = plot.resolvePrefabYaw();
        PlotFootprintRecord fp = plot.toFootprint();
        List<LocalMarkerRow> rows = new ArrayList<>();
        store.forEachChunk(
            Query.and(PoiMarkerEntity.getComponentType(), PoiMarkerDataComponent.getComponentType(), TransformComponent.getComponentType()),
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    PoiMarkerDataComponent data = chunk.getComponent(i, PoiMarkerDataComponent.getComponentType());
                    if (tc == null || data == null) {
                        continue;
                    }
                    Vector3d p = tc.getPosition();
                    if (!footprintContains(fp, p.x, p.y, p.z)) {
                        continue;
                    }
                    int bx = (int) Math.floor(p.x);
                    int by = (int) Math.floor(p.y);
                    int bz = (int) Math.floor(p.z);
                    int dx = bx - anchor.x;
                    int dy = by - anchor.y;
                    int dz = bz - anchor.z;
                    Vector3i local = PrefabLocalOffset.inverseRotateWorldDelta(yaw, dx, dy, dz);
                    rows.add(new LocalMarkerRow(local.x, local.y, local.z, data));
                }
            }
        );
        return rows;
    }

    @Nonnull
    public static PoiEntry toRegistryEntry(
        @Nonnull UUID poiId,
        @Nonnull TownRecord town,
        @Nonnull UUID plotId,
        int worldX,
        int worldY,
        int worldZ,
        @Nonnull PoiMarkerDataComponent data
    ) {
        return new PoiEntry(
            poiId,
            town.getTownId(),
            worldX,
            worldY,
            worldZ,
            new HashSet<>(data.getTags()),
            data.getCapacity(),
            plotId,
            null,
            data.getInteractionKind(),
            data.isMountOnUse(),
            data.getEquipmentProfileId(),
            null,
            null,
            null
        );
    }

    @Nonnull
    public static String localKey(int localX, int localY, int localZ) {
        return localX + "," + localY + "," + localZ;
    }

    @Nonnull
    public static Set<String> markerLocalKeys(@Nonnull List<LocalMarkerRow> markers) {
        Set<String> keys = new HashSet<>();
        for (LocalMarkerRow row : markers) {
            keys.add(localKey(row.localX(), row.localY(), row.localZ()));
        }
        return keys;
    }

    private static boolean footprintContains(@Nonnull PlotFootprintRecord fp, double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        return bx >= fp.getMinX()
            && bx <= fp.getMaxX()
            && by >= fp.getMinY()
            && by <= fp.getMaxY()
            && bz >= fp.getMinZ()
            && bz <= fp.getMaxZ();
    }
}
