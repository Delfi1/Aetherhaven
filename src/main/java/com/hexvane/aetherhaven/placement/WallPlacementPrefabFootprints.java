package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.prefab.PrefabResolveUtil;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.placement.PlotFootprintUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class WallPlacementPrefabFootprints {
    private WallPlacementPrefabFootprints() {}

    @Nullable
    static PlotFootprintRecord footprintAt(
        @Nonnull ConstructionDefinition def, @Nonnull Vector3i sign, @Nonnull Rotation yaw
    ) {
        Path path = PrefabResolveUtil.resolvePrefabPath(def.getPrefabPath());
        if (path == null) {
            return null;
        }
        IPrefabBuffer buf = PrefabBufferUtil.getCached(path);
        try {
            Vector3i origin = def.resolvePrefabAnchorWorld(sign, yaw);
            return PlotFootprintUtil.computeFootprint(origin, yaw, buf);
        } finally {
            buf.release();
        }
    }
}
