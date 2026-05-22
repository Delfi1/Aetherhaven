package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.wall.WallCardinal;
import com.hexvane.aetherhaven.wall.WallPlacementChainPlanner;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Prefab footprint face alignment for segment↔tower joints (matches in-game preview/build). */
public final class WallPlacementFootprintAnchors {
    private static volatile ConstructionCatalog catalog;
    private static volatile java.util.Map<String, ConstructionDefinition> testDefinitions;

    private WallPlacementFootprintAnchors() {}

    public static void setCatalog(@Nonnull ConstructionCatalog loaded) {
        catalog = loaded;
        testDefinitions = null;
    }

    /** Unit tests: supply parsed building defs without initializing {@link ConstructionCatalog}. */
    public static void setDefinitions(@Nonnull java.util.Map<String, ConstructionDefinition> byId) {
        testDefinitions = java.util.Map.copyOf(byId);
        catalog = null;
    }

    @Nullable
    private static ConstructionDefinition definition(@Nonnull String id) {
        java.util.Map<String, ConstructionDefinition> test = testDefinitions;
        if (test != null) {
            return test.get(id);
        }
        return catalog().get(id);
    }

    @Nonnull
    private static ConstructionCatalog catalog() {
        ConstructionCatalog loaded = catalog;
        if (loaded == null) {
            synchronized (WallPlacementFootprintAnchors.class) {
                loaded = catalog;
                if (loaded == null) {
                    loaded =
                        ConstructionCatalog.loadFromAssetPacksOrClasspath(
                            WallPlacementFootprintAnchors.class.getClassLoader()
                        );
                    catalog = loaded;
                }
            }
        }
        return loaded;
    }

    @Nullable
    public static Vector3i resolveJoint(
        @Nonnull WallPlacementChainPlanner.ChainCommittedPiece last,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal positionDir,
        boolean newPieceIsTower,
        @Nonnull String newConstructionId
    ) {
        ConstructionDefinition fromDef = definition(last.constructionId());
        ConstructionDefinition toDef = definition(newConstructionId);
        if (fromDef == null || toDef == null) {
            return null;
        }
        boolean fromTower = last.isTower();
        WallPlacementChainUtil.JointAlignment alignment;
        if (fromTower && !newPieceIsTower) {
            alignment = WallPlacementChainUtil.JointAlignment.TOWER_TO_SEGMENT;
        } else if (!fromTower && newPieceIsTower) {
            alignment = WallPlacementChainUtil.JointAlignment.SEGMENT_TO_TOWER;
        } else {
            return null;
        }
        Vector3i joint =
            WallPlacementChainUtil.nextSignPositionFootprint(
                fromDef,
                last.signAnchor(),
                last.prefabYaw(),
                toDef,
                newYaw,
                positionDir,
                alignment,
                last.prefabYaw()
            );
        return joint;
    }

    @Nullable
    public static Vector3i resolveJointForPlanner(
        @Nonnull WallPlacementChainPlanner.ChainCommittedPiece last,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal positionDir,
        boolean newPieceIsTower,
        @Nonnull WallPlacementChainPlanner.PieceKind pieceKind,
        @Nullable String resolvedConstructionId
    ) {
        String toId =
            resolvedConstructionId != null
                ? resolvedConstructionId
                : defaultConstructionId(pieceKind, newPieceIsTower);
        return resolveJoint(last, newYaw, positionDir, newPieceIsTower, toId);
    }

    @Nonnull
    private static String defaultConstructionId(
        @Nonnull WallPlacementChainPlanner.PieceKind pieceKind, boolean newPieceIsTower
    ) {
        if (newPieceIsTower || pieceKind == WallPlacementChainPlanner.PieceKind.TOWER) {
            return AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S;
        }
        return switch (pieceKind) {
            case GATE -> AetherhavenConstants.CONSTRUCTION_PLOT_WALL_GATE;
            default -> AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
        };
    }

    @Nullable
    public static PlotFootprintRecord footprintAt(
        @Nonnull ConstructionDefinition def, @Nonnull Vector3i sign, @Nonnull Rotation yaw
    ) {
        return WallPlacementPrefabFootprints.footprintAt(def, sign, yaw);
    }

    /** True when tower solid volume overlaps wall solid volume in world space (horizontal axes). */
    public static boolean footprintsOverlap(
        @Nonnull ConstructionDefinition wallDef,
        @Nonnull Vector3i wallSign,
        @Nonnull Rotation wallYaw,
        @Nonnull ConstructionDefinition towerDef,
        @Nonnull Vector3i towerSign,
        @Nonnull Rotation towerYaw
    ) {
        PlotFootprintRecord wallFp = WallPlacementPrefabFootprints.footprintAt(wallDef, wallSign, wallYaw);
        PlotFootprintRecord towerFp = WallPlacementPrefabFootprints.footprintAt(towerDef, towerSign, towerYaw);
        if (wallFp == null || towerFp == null) {
            return false;
        }
        return wallFp.getMinX() <= towerFp.getMaxX()
            && towerFp.getMinX() <= wallFp.getMaxX()
            && wallFp.getMinZ() <= towerFp.getMaxZ()
            && towerFp.getMinZ() <= wallFp.getMaxZ();
    }
}
