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



/** Footprint face alignment for segment↔tower joints. */

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

        return nextSignPositionFootprint(

            fromDef, fromSign, fromYaw, fromBuf, newDef, newYaw, newBuf, expandDir, JointAlignment.NONE

        );

    }



    public enum JointAlignment {

        NONE,

        /** Tower→segment: keep sign Z on E/W faces. */

        TOWER_TO_SEGMENT,

        /** Segment→tower: keep sign X on N/S faces (never sign Z on N/S — collapses anchors). */

        SEGMENT_TO_TOWER

    }



    @Nullable

    public static Vector3i nextSignPositionFootprint(

        @Nonnull ConstructionDefinition fromDef,

        @Nonnull Vector3i fromSign,

        @Nonnull Rotation fromYaw,

        @Nonnull IPrefabBuffer fromBuf,

        @Nonnull ConstructionDefinition newDef,

        @Nonnull Rotation newYaw,

        @Nonnull IPrefabBuffer newBuf,

        @Nonnull WallCardinal faceDir,

        @Nonnull JointAlignment jointAlignment

    ) {

        return nextSignPositionFootprint(

            fromDef, fromSign, fromYaw, fromBuf, newDef, newYaw, newBuf, faceDir, jointAlignment, fromYaw

        );

    }



    @Nullable

    public static Vector3i nextSignPositionFootprint(

        @Nonnull ConstructionDefinition fromDef,

        @Nonnull Vector3i fromSign,

        @Nonnull Rotation fromYaw,

        @Nonnull IPrefabBuffer fromBuf,

        @Nonnull ConstructionDefinition newDef,

        @Nonnull Rotation newYaw,

        @Nonnull IPrefabBuffer newBuf,

        @Nonnull WallCardinal faceDir,

        @Nonnull JointAlignment jointAlignment,

        @Nonnull Rotation alignYaw

    ) {

        Vector3i fromOrigin = fromDef.resolvePrefabAnchorWorld(fromSign, fromYaw);

        PlotFootprintRecord fromFp = PlotFootprintUtil.computeFootprint(fromOrigin, fromYaw, fromBuf);

        int centroidX = (fromFp.getMinX() + fromFp.getMaxX() + 1) / 2;

        int centroidZ = (fromFp.getMinZ() + fromFp.getMaxZ() + 1) / 2;

        Vector3i exitWorld =
            faceConnectionPoint(
                fromFp, faceDir, true, fromSign.x, fromSign.z, centroidX, centroidZ, jointAlignment, alignYaw
            );

        // Measure tower enter-face offset from prefab origin only (never mix wall sign coords into probe at 0,0,0).
        Vector3i probeSign =
            new Vector3i(0, AetherhavenConstants.PLOT_SIGN_BLOCK_Y_ABOVE_LOGICAL_ANCHOR, 0);
        Vector3i towerOriginProbe = newDef.resolvePrefabAnchorWorld(probeSign, newYaw);
        PlotFootprintRecord towerFpProbe = PlotFootprintUtil.computeFootprint(towerOriginProbe, newYaw, newBuf);
        int towerCx = (towerFpProbe.getMinX() + towerFpProbe.getMaxX() + 1) / 2;
        int towerCz = (towerFpProbe.getMinZ() + towerFpProbe.getMaxZ() + 1) / 2;
        WallCardinal enterFace = faceDir.opposite();
        Vector3i enterOnTowerProbe =
            faceConnectionPoint(
                towerFpProbe, enterFace, false, towerCx, towerCz, towerCx, towerCz, JointAlignment.NONE, newYaw
            );
        Vector3i enterOffset =
            new Vector3i(
                enterOnTowerProbe.x - towerOriginProbe.x,
                enterOnTowerProbe.y - towerOriginProbe.y,
                enterOnTowerProbe.z - towerOriginProbe.z
            );

        Vector3i newOrigin =
            new Vector3i(
                exitWorld.x - enterOffset.x,
                exitWorld.y - enterOffset.y,
                exitWorld.z - enterOffset.z
            );
        Vector3i sign = signFromPrefabOrigin(newDef, newOrigin, newYaw);
        return alignTowerSignToWall(sign, fromSign, faceDir, alignYaw);

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

        return nextSignPositionFootprint(fromDef, fromSign, fromYaw, newDef, newYaw, expandDir, JointAlignment.NONE, fromYaw);

    }



    @Nullable

    public static Vector3i nextSignPositionFootprint(

        @Nonnull ConstructionDefinition fromDef,

        @Nonnull Vector3i fromSign,

        @Nonnull Rotation fromYaw,

        @Nonnull ConstructionDefinition newDef,

        @Nonnull Rotation newYaw,

        @Nonnull WallCardinal faceDir,

        @Nonnull JointAlignment jointAlignment,

        @Nonnull Rotation alignYaw

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

                fromDef, fromSign, fromYaw, fromBuf, newDef, newYaw, newBuf, faceDir, jointAlignment, alignYaw

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

        @Nonnull JointAlignment jointAlignment,

        @Nonnull Rotation alignYaw

    ) {

        int cy = (fp.getMinY() + fp.getMaxY() + 1) / 2;

        int x = centroidX;

        int z = centroidZ;

        boolean runAlongZ = runAlongLocalZ(alignYaw);

        switch (jointAlignment) {

            case TOWER_TO_SEGMENT, SEGMENT_TO_TOWER -> {

                if (runAlongZ) {

                    if (outward == WallCardinal.EAST || outward == WallCardinal.WEST) {

                        z = signZ;

                    } else {

                        x = signX;

                    }

                } else {

                    if (outward == WallCardinal.NORTH || outward == WallCardinal.SOUTH) {

                        x = signX;

                    } else {

                        z = signZ;

                    }

                }

            }

            case NONE -> {}

        }

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



    private static boolean runAlongLocalZ(@Nonnull Rotation yaw) {

        return switch (yaw) {

            case Ninety, TwoSeventy -> false;

            default -> true;

        };

    }

    /** After footprint seating, snap the tower sign to the wall row/column for the face you clicked. */
    @Nonnull
    private static Vector3i alignTowerSignToWall(
        @Nonnull Vector3i towerSign,
        @Nonnull Vector3i wallSign,
        @Nonnull WallCardinal wallOutwardFace,
        @Nonnull Rotation wallYaw
    ) {
        boolean runAlongZ = runAlongLocalZ(wallYaw);
        if (runAlongZ) {
            if (wallOutwardFace == WallCardinal.EAST || wallOutwardFace == WallCardinal.WEST) {
                return new Vector3i(towerSign.x, towerSign.y, wallSign.z);
            }
            return new Vector3i(wallSign.x, towerSign.y, towerSign.z);
        }
        if (wallOutwardFace == WallCardinal.NORTH || wallOutwardFace == WallCardinal.SOUTH) {
            return new Vector3i(wallSign.x, towerSign.y, towerSign.z);
        }
        return new Vector3i(towerSign.x, towerSign.y, wallSign.z);
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


