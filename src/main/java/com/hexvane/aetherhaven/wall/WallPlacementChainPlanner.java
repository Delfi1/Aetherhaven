package com.hexvane.aetherhaven.wall;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pure wall-wand chaining logic (anchors, rotations, tower connections, allowed pads). No world/plugin/UI
 * dependencies beyond prefab math types.
 */
public final class WallPlacementChainPlanner {
    public enum PieceKind {
        SEGMENT,
        GATE,
        TOWER
    }

    public record ChainCommittedPiece(
        @Nonnull String constructionId,
        @Nonnull Vector3i signAnchor,
        int rotationSteps,
        @Nullable EnumSet<WallCardinal> towerConnectionDirs,
        @Nullable WallCardinal chainExpandDir
    ) {
        @Nonnull
        public Rotation prefabYaw() {
            return rotationStepsFrom(rotationSteps);
        }

        public boolean isTower() {
            return WallPieceGeometry.isTowerConstructionId(constructionId);
        }
    }

    public record ExpandPreviewPlan(
        @Nonnull Vector3i anchor,
        int rotationSteps,
        @Nonnull WallCardinal outgoingExpandDir,
        @Nonnull WallCardinal positionDir,
        @Nullable WallCardinal arrivalFromSide,
        @Nullable EnumSet<WallCardinal> towerConnections,
        @Nullable String resolvedConstructionId,
        @Nonnull EnumSet<WallCardinal> allowedExpandDirections
    ) {}

    private WallPlacementChainPlanner() {}

    @Nonnull
    public static ExpandPreviewPlan planExpandPreview(
        @Nonnull Vector3i currentAnchor,
        int currentRotationSteps,
        @Nonnull PieceKind pieceKind,
        @Nonnull List<ChainCommittedPiece> committed,
        @Nonnull WallCardinal outgoingExpandDir
    ) {
        return planExpandPreview(
            currentAnchor, currentRotationSteps, pieceKind, committed, outgoingExpandDir, null
        );
    }

    @Nonnull
    public static ExpandPreviewPlan planExpandPreview(
        @Nonnull Vector3i currentAnchor,
        int currentRotationSteps,
        @Nonnull PieceKind pieceKind,
        @Nonnull List<ChainCommittedPiece> committed,
        @Nonnull WallCardinal outgoingExpandDir,
        @Nullable FootprintAnchorResolver footprintResolver
    ) {
        ChainCommittedPiece last = last(committed);
        if (last == null) {
            boolean newIsTower = pieceKind == PieceKind.TOWER;
            EnumSet<WallCardinal> towerConnections = null;
            if (newIsTower) {
                towerConnections = EnumSet.of(outgoingExpandDir);
            }
            String resolvedId = constructionIdFor(pieceKind, towerConnections);
            int rot = outgoingExpandDir.rotationStepsForLocalNorthAlongAxis();
            if (newIsTower) {
                WallTowerPrefabResolver.ResolvedTower resolved = WallTowerPrefabResolver.resolve(towerConnections);
                if (resolved != null) {
                    resolvedId = resolved.constructionId();
                    rot = resolved.rotationSteps();
                }
            }
            return new ExpandPreviewPlan(
                currentAnchor.clone(),
                rot,
                outgoingExpandDir,
                outgoingExpandDir,
                null,
                towerConnections,
                resolvedId,
                EnumSet.allOf(WallCardinal.class)
            );
        }
        boolean newIsTower = pieceKind == PieceKind.TOWER;
        WallCardinal jointDir =
            newIsTower && !last.isTower() ? towerJointExpandDir(last, outgoingExpandDir) : outgoingExpandDir;
        WallCardinal seatDir =
            newIsTower && !last.isTower() ? towerSeatDirection(last, outgoingExpandDir) : jointDir;
        WallCardinal positionDir = jointDir;
        ChainCommittedPiece prev = previous(committed);
        int rotationSteps = rotationStepsForChainAfter(pieceKind, last, prev, outgoingExpandDir);
        Rotation newYaw = rotationStepsFrom(rotationSteps);
        Vector3i anchor;
        EnumSet<WallCardinal> towerConnections = null;
        String resolvedId = constructionIdFor(pieceKind, towerConnections);
        if (newIsTower) {
            Vector3i probeAnchor =
                computeChainedSignAnchor(
                    last, rotationSteps, newYaw, seatDir, outgoingExpandDir, true, pieceKind, null, null
                );
            towerConnections =
                towerConnectionsForOutgoing(
                    probeAnchor, last.signAnchor(), outgoingExpandDir, last.chainExpandDir(), jointDir
                );
            resolvedId = resolveTowerConstructionId(towerConnections);
            boolean straightEndCap =
                isStraightRunTowerEndCap(last.chainExpandDir(), outgoingExpandDir, jointDir);
            WallTowerPrefabResolver.ResolvedTower resolved = WallTowerPrefabResolver.resolve(towerConnections);
            if (resolved != null) {
                resolvedId = resolved.constructionId();
                boolean singleEndCap = towerConnections != null && towerConnections.size() == 1;
                if (!straightEndCap || singleEndCap) {
                    rotationSteps = resolved.rotationSteps();
                    newYaw = rotationStepsFrom(rotationSteps);
                }
            }
            anchor =
                computeChainedSignAnchor(
                    last,
                    rotationSteps,
                    newYaw,
                    seatDir,
                    outgoingExpandDir,
                    true,
                    pieceKind,
                    resolvedId,
                    footprintResolver
                );
            towerConnections =
                towerConnectionsForOutgoing(
                    anchor, last.signAnchor(), outgoingExpandDir, last.chainExpandDir(), jointDir
                );
            resolvedId = resolveTowerConstructionId(towerConnections);
        } else {
            anchor =
                computeChainedSignAnchor(
                    last,
                    rotationSteps,
                    newYaw,
                    positionDir,
                    outgoingExpandDir,
                    false,
                    pieceKind,
                    resolvedId,
                    footprintResolver
                );
        }
        WallCardinal arrival = positionDir.opposite();
        EnumSet<WallCardinal> allowed =
            allowedExpandDirections(pieceKind, committed, arrival);
        return new ExpandPreviewPlan(
            anchor,
            rotationSteps,
            outgoingExpandDir,
            positionDir,
            arrival,
            towerConnections,
            resolvedId,
            allowed
        );
    }

    @Nonnull
    public static ExpandPreviewPlan planAfterPlace(
        @Nonnull ChainCommittedPiece placed,
        @Nonnull PieceKind nextPieceKind,
        @Nonnull WallCardinal chainExpandDir
    ) {
        return planExpandPreview(
            placed.signAnchor().clone(),
            placed.rotationSteps(),
            nextPieceKind,
            List.of(placed),
            chainExpandDir
        );
    }

    @Nonnull
    public static Vector3i computeChainedSignAnchor(
        @Nonnull ChainCommittedPiece last,
        int newRotationSteps,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal positionDir,
        @Nonnull WallCardinal outgoingExpandDir,
        boolean newPieceIsTower,
        @Nonnull PieceKind pieceKind,
        @Nullable String resolvedConstructionId,
        @Nullable FootprintAnchorResolver footprintResolver
    ) {
        boolean lastTower = last.isTower();
        if (newPieceIsTower && !lastTower) {
            WallCardinal seatDir = towerSeatDirection(last, outgoingExpandDir);
            boolean straightEndCap =
                isStraightRunTowerEndCap(last.chainExpandDir(), outgoingExpandDir, towerJointExpandDir(last, outgoingExpandDir));
            if (footprintResolver != null) {
                Vector3i footprint =
                    footprintResolver.resolve(
                        last, newYaw, seatDir, true, pieceKind, resolvedConstructionId
                    );
                if (footprint != null) {
                    return footprint;
                }
            }
            String towerId =
                resolvedConstructionId != null
                    ? resolvedConstructionId
                    : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S;
            if (straightEndCap) {
                return WallPieceGeometry.seatTowerSignAtSegmentRunEnd(
                    last.constructionId(),
                    towerId,
                    last.signAnchor(),
                    last.prefabYaw(),
                    newYaw,
                    seatDir,
                    last.signAnchor()
                );
            }
            return WallPieceGeometry.nextSignPosition(
                last.constructionId(),
                last.signAnchor(),
                last.prefabYaw(),
                towerId,
                newYaw,
                seatDir,
                false,
                true
            );
        }
        String newConsId =
            resolvedConstructionId != null ? resolvedConstructionId : constructionIdFor(pieceKind, null);
        if (lastTower != newPieceIsTower || !last.constructionId().equals(newConsId)) {
            if (footprintResolver != null) {
                Vector3i footprint =
                    footprintResolver.resolve(
                        last, newYaw, positionDir, newPieceIsTower, pieceKind, resolvedConstructionId
                    );
                if (footprint != null
                    && (!newPieceIsTower
                        || isPlausibleTowerAnchor(last.signAnchor(), footprint))) {
                    return footprint;
                }
            }
        }
        return WallPieceGeometry.nextSignPosition(
            last.constructionId(),
            last.signAnchor(),
            last.prefabYaw(),
            newConsId,
            newYaw,
            positionDir,
            lastTower,
            newPieceIsTower
        );
    }

    @FunctionalInterface
    public interface FootprintAnchorResolver {
        @Nullable
        Vector3i resolve(
            @Nonnull ChainCommittedPiece last,
            @Nonnull Rotation newYaw,
            @Nonnull WallCardinal positionDir,
            boolean newPieceIsTower,
            @Nonnull PieceKind pieceKind,
            @Nullable String resolvedConstructionId
        );
    }

    /**
     * Which axis to use when seating a tower on the last segment. N/S wall runs attach at the chain end (run tip);
     * E/W runs attach on the face you clicked (N/S), not the east/west run end.
     */
    /**
     * Which wall face the new tower meets. Pads on the chain axis use that run end; perpendicular pads use the
     * long-side face you clicked (E/W on an N/S run, N/S on an E/W run).
     */
    /** Reject footprint anchors that collapsed to world origin or jumped far from the wall sign. */
    public static boolean isPlausibleTowerAnchor(@Nonnull Vector3i wallSign, @Nonnull Vector3i towerSign) {
        int dx = Math.abs(towerSign.x - wallSign.x);
        int dz = Math.abs(towerSign.z - wallSign.z);
        if (dx > 32 || dz > 32) {
            return false;
        }
        long wallDist2 = (long) wallSign.x * wallSign.x + (long) wallSign.z * wallSign.z;
        long towerDist2 = (long) towerSign.x * towerSign.x + (long) towerSign.z * towerSign.z;
        return wallDist2 <= 10_000 || towerDist2 >= 256;
    }

    @Nonnull
    public static WallCardinal towerJointExpandDir(@Nonnull ChainCommittedPiece last, @Nonnull WallCardinal outgoingExpandDir) {
        return last.chainExpandDir() == null ? outgoingExpandDir.opposite() : outgoingExpandDir;
    }

    /**
     * World face where the tower meets the wall segment. Straight-run pads use the pad direction (run end). Corner pads
     * (perpendicular to the chain) keep the tower at the chain tip ({@code chainExpandDir}) while {@code outgoingExpandDir}
     * only adds the second tower opening.
     */
    @Nonnull
    public static WallCardinal towerSeatDirection(
        @Nonnull ChainCommittedPiece last, @Nonnull WallCardinal outgoingExpandDir
    ) {
        WallCardinal jointDir = towerJointExpandDir(last, outgoingExpandDir);
        if (last.chainExpandDir() == null) {
            return jointDir;
        }
        if (isStraightRunTowerEndCap(last.chainExpandDir(), outgoingExpandDir, jointDir)) {
            return jointDir;
        }
        return last.chainExpandDir();
    }

    /** Run-axis tower tab: chain-forward or opposite run end (not a long-side corner). */
    public static boolean isStraightRunTowerEndCap(
        @Nullable WallCardinal chainExpandDir,
        @Nonnull WallCardinal outgoingExpandDir,
        @Nonnull WallCardinal positionDir
    ) {
        return chainExpandDir != null
            && positionDir == outgoingExpandDir
            && (outgoingExpandDir == chainExpandDir || outgoingExpandDir == chainExpandDir.opposite());
    }

    @Nonnull
    public static EnumSet<WallCardinal> towerConnectionsForOutgoing(
        @Nonnull Vector3i previewAnchor,
        @Nonnull Vector3i lastSignAnchor,
        @Nonnull WallCardinal outgoingExpandDir,
        @Nullable WallCardinal chainExpandDir,
        @Nonnull WallCardinal positionDir
    ) {
        EnumSet<WallCardinal> dirs = EnumSet.noneOf(WallCardinal.class);
        WallCardinal incoming = WallCardinal.fromVector(previewAnchor, lastSignAnchor);
        if (incoming != null) {
            dirs.add(incoming);
        } else {
            dirs.add(outgoingExpandDir.opposite());
        }
        if (!isStraightRunTowerEndCap(chainExpandDir, outgoingExpandDir, positionDir)) {
            dirs.add(outgoingExpandDir);
        }
        return dirs;
    }

    public static int rotationStepsForChainAfter(
        @Nonnull PieceKind pieceKind,
        @Nonnull ChainCommittedPiece last,
        @Nullable ChainCommittedPiece previous,
        @Nonnull WallCardinal expandDir
    ) {
        if (pieceKind == PieceKind.TOWER) {
            return last.rotationSteps();
        }
        if (last.isTower()) {
            if (last.towerConnectionDirs() != null
                && last.towerConnectionDirs().size() == 1
                && previous != null
                && !previous.isTower()
                && last.chainExpandDir() != null
                && (expandDir == last.chainExpandDir() || expandDir == last.chainExpandDir().opposite())) {
                return previous.rotationSteps();
            }
            if (last.towerConnectionDirs() != null && last.towerConnectionDirs().size() == 2) {
                boolean nsThrough =
                    last.towerConnectionDirs().contains(WallCardinal.NORTH)
                        && last.towerConnectionDirs().contains(WallCardinal.SOUTH);
                boolean ewThrough =
                    last.towerConnectionDirs().contains(WallCardinal.EAST)
                        && last.towerConnectionDirs().contains(WallCardinal.WEST);
                if (previous != null && !previous.isTower()) {
                    boolean prevAlongZ = (previous.rotationSteps() % 2) == 0;
                    if (nsThrough && prevAlongZ && (expandDir == WallCardinal.NORTH || expandDir == WallCardinal.SOUTH)) {
                        return previous.rotationSteps();
                    }
                    if (ewThrough && !prevAlongZ && (expandDir == WallCardinal.EAST || expandDir == WallCardinal.WEST)) {
                        return previous.rotationSteps();
                    }
                }
            }
            return expandDir.rotationStepsForLocalNorthAlongAxis();
        }
        return last.rotationSteps();
    }

    @Nonnull
    public static EnumSet<WallCardinal> allowedExpandDirections(
        @Nonnull PieceKind pieceKind,
        @Nonnull List<ChainCommittedPiece> committed,
        @Nullable WallCardinal arrivalFromSide
    ) {
        EnumSet<WallCardinal> allowed = EnumSet.allOf(WallCardinal.class);
        ChainCommittedPiece last = last(committed);
        if (pieceKind == PieceKind.TOWER && last != null && !last.isTower()) {
            WallCardinal back =
                last.chainExpandDir() != null ? last.chainExpandDir().opposite() : arrivalFromSide;
            if (back != null) {
                allowed.remove(back);
            }
            return allowed;
        }
        if (last != null && pieceKind != PieceKind.TOWER) {
            if (last.isTower()) {
                if (isSeededContinueFromEdit(last, committed)) {
                    blockExpandIntoTowerOpenings(allowed, last);
                } else {
                    retainTowerOutgoingDirections(allowed, last, arrivalFromSide, previous(committed));
                }
            } else {
                boolean alongZ = (last.rotationSteps() % 2) == 0;
                allowed.retainAll(
                    alongZ ? EnumSet.of(WallCardinal.NORTH, WallCardinal.SOUTH) : EnumSet.of(WallCardinal.EAST, WallCardinal.WEST)
                );
            }
        }
        if (arrivalFromSide != null) {
            allowed.remove(arrivalFromSide);
        }
        return allowed;
    }

    /** Single seeded piece from primary-use Continue (no {@code chainExpandDir} yet). */
    private static boolean isSeededContinueFromEdit(
        @Nonnull ChainCommittedPiece last, @Nonnull List<ChainCommittedPiece> committed
    ) {
        return committed.size() == 1 && last.chainExpandDir() == null;
    }

    /**
     * After Continue on an existing tower: block pads into existing openings (toward built wall), allow run
     * continuation and corners on the other faces.
     */
    private static void blockExpandIntoTowerOpenings(
        @Nonnull EnumSet<WallCardinal> allowed, @Nonnull ChainCommittedPiece last
    ) {
        if (last.towerConnectionDirs() != null) {
            for (WallCardinal face : last.towerConnectionDirs()) {
                allowed.remove(face);
            }
        }
    }

    private static void retainTowerOutgoingDirections(
        @Nonnull EnumSet<WallCardinal> allowed,
        @Nonnull ChainCommittedPiece last,
        @Nullable WallCardinal arrivalFromSide,
        @Nullable ChainCommittedPiece previous
    ) {
        WallCardinal back = arrivalFromSide;
        if (back == null && last.chainExpandDir() != null) {
            back = last.chainExpandDir().opposite();
        }
        if (last.towerConnectionDirs() != null && !last.towerConnectionDirs().isEmpty()) {
            EnumSet<WallCardinal> forward = EnumSet.noneOf(WallCardinal.class);
            for (WallCardinal face : last.towerConnectionDirs()) {
                if (back == null || face != back) {
                    forward.add(face);
                }
            }
            if (previous != null && !previous.isTower()) {
                WallCardinal incoming = WallCardinal.fromVector(last.signAnchor(), previous.signAnchor());
                if (incoming != null) {
                    forward.remove(incoming);
                }
            }
            if (!forward.isEmpty()) {
                allowed.retainAll(forward);
                return;
            }
        }
        if (last.chainExpandDir() != null) {
            allowed.retainAll(EnumSet.of(last.chainExpandDir()));
        }
    }

    @Nullable
    public static String resolveTowerConstructionId(@Nullable EnumSet<WallCardinal> towerConnections) {
        if (towerConnections == null || towerConnections.isEmpty()) {
            return AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S;
        }
        WallTowerPrefabResolver.ResolvedTower r = WallTowerPrefabResolver.resolve(towerConnections);
        return r != null ? r.constructionId() : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S;
    }

    @Nonnull
    private static String constructionIdFor(@Nonnull PieceKind pieceKind, @Nullable EnumSet<WallCardinal> towerConnections) {
        if (pieceKind == PieceKind.TOWER) {
            return resolveTowerConstructionId(towerConnections);
        }
        return switch (pieceKind) {
            case GATE -> AetherhavenConstants.CONSTRUCTION_PLOT_WALL_GATE;
            case SEGMENT -> AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
            default -> AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
        };
    }

    @Nullable
    private static ChainCommittedPiece last(@Nonnull List<ChainCommittedPiece> committed) {
        return committed.isEmpty() ? null : committed.get(committed.size() - 1);
    }

    @Nullable
    private static ChainCommittedPiece previous(@Nonnull List<ChainCommittedPiece> committed) {
        int n = committed.size();
        return n >= 2 ? committed.get(n - 2) : null;
    }

    @Nonnull
    public static Rotation rotationStepsFrom(int steps) {
        return switch ((steps % 4 + 4) % 4) {
            case 1 -> Rotation.Ninety;
            case 2 -> Rotation.OneEighty;
            case 3 -> Rotation.TwoSeventy;
            default -> Rotation.None;
        };
    }
}
