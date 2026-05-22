package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.prefab.PrefabResolveUtil;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.wall.WallCardinal;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.prefab.PrefabRotation;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Footprint face alignment for wall joints (tower→segment uses per-face sign coords; segment→tower uses geometry). */
public final class WallPlacementChainUtil {
    private WallPlacementChainUtil() {}

    @Nullable
    public static Vector3i nextSignPositionFootprint(
        @Nonnull ConstructionDefinition fromDef,
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull IPrefabBuffer fromBuf,
        @Nonnull ConstructionDefinition newDef,
        @Nonnull Rotation newYaw,
        @Nonnull IPrefabBuffer newBuf,
        @Nonnull WallCardinal expandDir
    ) {
        return nextSignPositionFootprint(fromDef, fromSign, fromYaw, fromBuf, newDef, newYaw, newBuf, expandDir, false);
    }

    /**
     * @param towerToSegment when true (tower→segment only), E/W faces use sign Z and N/S faces use sign X so the
     *     segment sits flush outside the tower without overlapping. Do not use for segment→tower (probe math breaks).
     */
    @Nullable
    public static Vector3i nextSignPositionFootprint(
        @Nonnull ConstructionDefinition fromDef,
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull IPrefabBuffer fromBuf,
        @Nonnull ConstructionDefinition newDef,
        @Nonnull Rotation newYaw,
        @Nonnull IPrefabBuffer newBuf,
        @Nonnull WallCardinal expandDir,
        boolean towerToSegment
    ) {
        Vector3i fromOrigin = fromDef.resolvePrefabAnchorWorld(fromSign, fromYaw);
        PlotFootprintRecord fromFp = PlotFootprintUtil.computeFootprint(fromOrigin, fromYaw, fromBuf);
        int centroidX = (fromFp.getMinX() + fromFp.getMaxX() + 1) / 2;
        int centroidZ = (fromFp.getMinZ() + fromFp.getMaxZ() + 1) / 2;
        Vector3i exitWorld =
            faceConnectionPoint(fromFp, expandDir, true, fromSign.x, fromSign.z, centroidX, centroidZ, towerToSegment);

        Vector3i probeSign =
            new Vector3i(0, AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR, 0);
        Vector3i newOriginProbe = newDef.resolvePrefabAnchorWorld(probeSign, newYaw);
        PlotFootprintRecord newFpProbe = PlotFootprintUtil.computeFootprint(newOriginProbe, newYaw, newBuf);
        WallCardinal enterFace = expandDir.opposite();
        Vector3i enterWorldProbe =
            faceConnectionPoint(newFpProbe, enterFace, false, fromSign.x, fromSign.z, centroidX, centroidZ, towerToSegment);
        Vector3i enterOffset =
            new Vector3i(
                enterWorldProbe.x - newOriginProbe.x,
                enterWorldProbe.y - newOriginProbe.y,
                enterWorldProbe.z - newOriginProbe.z
            );

        Vector3i newOrigin =
            new Vector3i(
                exitWorld.x - enterOffset.x,
                exitWorld.y - enterOffset.y,
                exitWorld.z - enterOffset.z
            );
        return signFromPrefabOrigin(newDef, newOrigin, newYaw);
    }

    @Nullable
    public static Vector3i nextSignPositionFootprint(
        @Nonnull ConstructionDefinition fromDef,
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull ConstructionDefinition newDef,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal expandDir
    ) {
        return nextSignPositionFootprint(fromDef, fromSign, fromYaw, newDef, newYaw, expandDir, false);
    }

    @Nullable
    public static Vector3i nextSignPositionFootprint(
        @Nonnull ConstructionDefinition fromDef,
        @Nonnull Vector3i fromSign,
        @Nonnull Rotation fromYaw,
        @Nonnull ConstructionDefinition newDef,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal expandDir,
        boolean towerToSegment
    ) {
        Path fromPath = PrefabResolveUtil.resolvePrefabPath(fromDef.getPrefabPath());
        Path newPath = PrefabResolveUtil.resolvePrefabPath(newDef.getPrefabPath());
        if (fromPath == null || newPath == null) {
            return null;
        }
        IPrefabBuffer fromBuf = PrefabBufferUtil.getCached(fromPath);
        IPrefabBuffer newBuf = PrefabBufferUtil.getCached(newPath);
        try {
            return nextSignPositionFootprint(
                fromDef, fromSign, fromYaw, fromBuf, newDef, newYaw, newBuf, expandDir, towerToSegment
            );
        } finally {
            fromBuf.release();
            newBuf.release();
        }
    }

    @Nonnull
    private static Vector3i faceConnectionPoint(
        @Nonnull PlotFootprintRecord fp,
        @Nonnull WallCardinal outward,
        boolean outsideFootprint,
        int signX,
        int signZ,
        int centroidX,
        int centroidZ,
        boolean towerToSegment
    ) {
        int cy = (fp.getMinY() + fp.getMaxY() + 1) / 2;
        int x =
            towerToSegment && (outward == WallCardinal.NORTH || outward == WallCardinal.SOUTH) ? signX : centroidX;
        int z =
            towerToSegment && (outward == WallCardinal.EAST || outward == WallCardinal.WEST) ? signZ : centroidZ;
        if (outsideFootprint) {
            return switch (outward) {
                case NORTH -> new Vector3i(x, cy, fp.getMinZ() - 1);
                case SOUTH -> new Vector3i(x, cy, fp.getMaxZ() + 1);
                case EAST -> new Vector3i(fp.getMaxX() + 1, cy, z);
                case WEST -> new Vector3i(fp.getMinX() - 1, cy, z);
            };
        }
        return switch (outward) {
            case NORTH -> new Vector3i(x, cy, fp.getMinZ());
            case SOUTH -> new Vector3i(x, cy, fp.getMaxZ());
            case EAST -> new Vector3i(fp.getMaxX(), cy, z);
            case WEST -> new Vector3i(fp.getMinX(), cy, z);
        };
    }

    @Nonnull
    private static Vector3i signFromPrefabOrigin(
        @Nonnull ConstructionDefinition def, @Nonnull Vector3i prefabOrigin, @Nonnull Rotation yaw
    ) {
        int[] o = def.getPlotAnchorOffset();
        Vector3i off = new Vector3i(o[0], o[1], o[2]);
        PrefabRotation.fromRotation(yaw).rotate(off);
        Vector3i logical =
            new Vector3i(prefabOrigin.x - off.x, prefabOrigin.y - off.y, prefabOrigin.z - off.z);
        return new Vector3i(
            logical.x,
            logical.y + AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR,
            logical.z
        );
    }
}
