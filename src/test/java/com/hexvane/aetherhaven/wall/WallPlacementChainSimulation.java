package com.hexvane.aetherhaven.wall;

import com.hypixel.hytale.math.vector.Vector3i;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Drives {@link WallPlacementChainPlanner} the same way the UI expand+place flow does (geometry only). */
public final class WallPlacementChainSimulation {
    private final List<WallPlacementChainPlanner.ChainCommittedPiece> committed = new ArrayList<>();
    private Vector3i currentAnchor;
    private int currentRotationSteps;
    private WallPlacementChainPlanner.PieceKind pieceKind = WallPlacementChainPlanner.PieceKind.SEGMENT;
    private WallCardinal lastExpandDir;
    private WallCardinal arrivalFromSide;

    private WallPlacementChainSimulation(@Nonnull Vector3i startAnchor) {
        this.currentAnchor = startAnchor.clone();
        this.currentRotationSteps = 0;
    }

    @Nonnull
    public static WallPlacementChainSimulation start(int x, int y, int z) {
        return new WallPlacementChainSimulation(new Vector3i(x, y, z));
    }

    @Nonnull
    public WallPlacementChainSimulation pieceKind(@Nonnull WallPlacementChainPlanner.PieceKind kind) {
        this.pieceKind = kind;
        if (kind == WallPlacementChainPlanner.PieceKind.TOWER) {
            WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
            if (last != null) {
                WallCardinal expand =
                    lastExpandDir != null ? lastExpandDir : last.chainExpandDir();
                if (expand != null) {
                    previewOnly(expand);
                }
            }
        }
        return this;
    }

    /** Expand pad + place + chain (matches in-game arrow click). */
    @Nonnull
    public WallPlacementChainSimulation expandPlace(@Nonnull WallCardinal outgoing) {
        previewOnly(outgoing);
        if (upgradeLastTowerForOutgoingPad(outgoing)) {
            previewOnly(outgoing);
        }
        WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
        Vector3i anchorBase = last == null ? currentAnchor : last.signAnchor();
        int rotBase = last == null ? currentRotationSteps : last.rotationSteps();
        WallPlacementChainPlanner.ExpandPreviewPlan plan =
            WallPlacementChainPlanner.planExpandPreview(
                anchorBase.clone(),
                rotBase,
                pieceKind,
                committed,
                outgoing
            );
        applyPlan(plan);
        committed.add(
            new WallPlacementChainPlanner.ChainCommittedPiece(
                plan.resolvedConstructionId(),
                currentAnchor.clone(),
                currentRotationSteps,
                plan.towerConnections(),
                outgoing
            )
        );
        if (pieceKind == WallPlacementChainPlanner.PieceKind.TOWER) {
            pieceKind = WallPlacementChainPlanner.PieceKind.SEGMENT;
        }
        extendPreviewAfterPlace(outgoing);
        return this;
    }

    /** Same as {@link com.hexvane.aetherhaven.placement.WallPlacementSession#extendPreview}. */
    private void extendPreviewAfterPlace(@Nonnull WallCardinal outgoing) {
        if (lastCommitted() != null && lastCommitted().isTower()) {
            pieceKind = WallPlacementChainPlanner.PieceKind.SEGMENT;
        }
        WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
        if (last == null) {
            return;
        }
        WallPlacementChainPlanner.ExpandPreviewPlan plan =
            WallPlacementChainPlanner.planExpandPreview(
                last.signAnchor().clone(),
                last.rotationSteps(),
                pieceKind,
                committed,
                outgoing
            );
        applyPlan(plan);
    }

    /** Same as {@link com.hexvane.aetherhaven.placement.WallPlacementSession#applyOutgoingDirectionToLastTower}. */
    private boolean upgradeLastTowerForOutgoingPad(@Nonnull WallCardinal outgoing) {
        if (pieceKind != WallPlacementChainPlanner.PieceKind.SEGMENT) {
            return false;
        }
        WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
        if (last == null
            || !last.isTower()
            || last.towerConnectionDirs() == null
            || last.towerConnectionDirs().size() != 1) {
            return false;
        }
        EnumSet<WallCardinal> pair =
            WallTowerAutoConnector.connectionsForCorner(last.towerConnectionDirs(), outgoing);
        WallTowerPrefabResolver.ResolvedTower resolved = WallTowerAutoConnector.resolve(pair);
        if (resolved == null) {
            return false;
        }
        int idx = committed.size() - 1;
        committed.set(
            idx,
            new WallPlacementChainPlanner.ChainCommittedPiece(
                resolved.constructionId(),
                last.signAnchor(),
                resolved.rotationSteps(),
                pair,
                last.chainExpandDir()
            )
        );
        return true;
    }

    /** Preview only (expand pad without place). */
    @Nonnull
    public WallPlacementChainPlanner.ExpandPreviewPlan previewOnly(@Nonnull WallCardinal outgoing) {
        upgradeLastTowerForOutgoingPad(outgoing);
        WallPlacementChainPlanner.ExpandPreviewPlan plan =
            WallPlacementChainPlanner.planExpandPreview(
                currentAnchor,
                currentRotationSteps,
                pieceKind,
                committed,
                outgoing
            );
        applyPlan(plan);
        return plan;
    }

    private void applyPlan(@Nonnull WallPlacementChainPlanner.ExpandPreviewPlan plan) {
        currentAnchor = plan.anchor().clone();
        currentRotationSteps = plan.rotationSteps();
        lastExpandDir = plan.outgoingExpandDir();
        arrivalFromSide = plan.arrivalFromSide();
    }

    @Nonnull
    public EnumSet<WallCardinal> towerConnectionsOnLastCommit() {
        WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
        return last != null && last.towerConnectionDirs() != null
            ? EnumSet.copyOf(last.towerConnectionDirs())
            : EnumSet.noneOf(WallCardinal.class);
    }

    @Nonnull
    public Vector3i anchor() {
        return currentAnchor.clone();
    }

    public int rotationSteps() {
        return currentRotationSteps;
    }

    @Nonnull
    public List<WallPlacementChainPlanner.ChainCommittedPiece> committed() {
        return List.copyOf(committed);
    }

    @Nullable
    public WallPlacementChainPlanner.ChainCommittedPiece lastCommitted() {
        return committed.isEmpty() ? null : committed.get(committed.size() - 1);
    }
}
