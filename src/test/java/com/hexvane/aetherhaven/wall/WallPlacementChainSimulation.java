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
        WallPlacementChainPlanner.ExpandPreviewPlan plan =
            WallPlacementChainPlanner.planExpandPreview(
                currentAnchor,
                currentRotationSteps,
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
        WallPlacementChainPlanner.ChainCommittedPiece last = lastCommitted();
        if (last == null) {
            return;
        }
        WallPlacementChainPlanner.ChainCommittedPiece prev =
            committed.size() >= 2 ? committed.get(committed.size() - 2) : null;
        int newRotationSteps =
            WallPlacementChainPlanner.rotationStepsForChainAfter(pieceKind, last, prev, outgoing);
        com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation newYaw =
            WallPlacementChainPlanner.rotationStepsFrom(newRotationSteps);
        boolean newIsTower = pieceKind == WallPlacementChainPlanner.PieceKind.TOWER;
        WallCardinal positionDir =
            newIsTower && !last.isTower()
                ? WallPlacementChainPlanner.towerJointExpandDir(last, outgoing)
                : outgoing;
        currentAnchor =
            WallPlacementChainPlanner.computeChainedSignAnchor(
                last, newRotationSteps, newYaw, positionDir, outgoing, newIsTower, pieceKind, null, null
            );
        currentRotationSteps = newRotationSteps;
        lastExpandDir = outgoing;
        arrivalFromSide = outgoing.opposite();
    }

    /** Preview only (expand pad without place). */
    @Nonnull
    public WallPlacementChainPlanner.ExpandPreviewPlan previewOnly(@Nonnull WallCardinal outgoing) {
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
