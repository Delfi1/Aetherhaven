package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.wall.WallCardinal;
import com.hexvane.aetherhaven.wall.WallPieceGeometry;
import com.hexvane.aetherhaven.wall.WallPlacementChainPlanner;
import com.hexvane.aetherhaven.wall.WallTowerAutoConnector;
import com.hexvane.aetherhaven.wall.WallTowerPrefabResolver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WallPlacementSession {
    public enum PieceKind {
        SEGMENT(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT),
        GATE(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_GATE),
        TOWER(null);

        @Nullable
        public final String fixedConstructionId;

        PieceKind(@Nullable String fixedConstructionId) {
            this.fixedConstructionId = fixedConstructionId;
        }
    }

    public static final class CommittedStep {
        @Nonnull
        public final UUID plotId;
        @Nonnull
        public final String constructionId;
        @Nonnull
        public final Vector3i signAnchor;
        public final int rotationSteps;

        @Nullable
        public final EnumSet<WallCardinal> towerConnectionDirs;

        /** World direction from the previous committed sign to this one (pad direction on place). */
        @Nullable
        public final WallCardinal chainExpandDir;

        public CommittedStep(
            @Nonnull UUID plotId,
            @Nonnull String constructionId,
            @Nonnull Vector3i signAnchor,
            int rotationSteps
        ) {
            this(plotId, constructionId, signAnchor, rotationSteps, null, null);
        }

        public CommittedStep(
            @Nonnull UUID plotId,
            @Nonnull String constructionId,
            @Nonnull Vector3i signAnchor,
            int rotationSteps,
            @Nullable EnumSet<WallCardinal> towerConnectionDirs
        ) {
            this(plotId, constructionId, signAnchor, rotationSteps, towerConnectionDirs, null);
        }

        public CommittedStep(
            @Nonnull UUID plotId,
            @Nonnull String constructionId,
            @Nonnull Vector3i signAnchor,
            int rotationSteps,
            @Nullable EnumSet<WallCardinal> towerConnectionDirs,
            @Nullable WallCardinal chainExpandDir
        ) {
            this.plotId = plotId;
            this.constructionId = constructionId;
            this.signAnchor = signAnchor.clone();
            this.rotationSteps = rotationSteps;
            this.towerConnectionDirs =
                towerConnectionDirs == null || towerConnectionDirs.isEmpty()
                    ? null
                    : EnumSet.copyOf(towerConnectionDirs);
            this.chainExpandDir = chainExpandDir;
        }

        @Nonnull
        public Rotation getPrefabYaw() {
            return rotationStepsFrom(rotationSteps);
        }
    }

    @Nonnull
    private final World world;

    @Nonnull
    private Vector3i currentAnchor;

    private int currentRotationSteps;

    @Nonnull
    private PieceKind pieceKind = PieceKind.SEGMENT;

    @Nonnull
    private final EnumSet<WallCardinal> towerConnections = EnumSet.noneOf(WallCardinal.class);

    @Nonnull
    private final List<CommittedStep> committed = new ArrayList<>();

    @Nonnull
    private final List<Ref<EntityStore>> previewEntityRefs = new ObjectArrayList<>();

    private boolean birdsEyeSnapshotValid;
    private double birdsEyeSnapshotX;
    private double birdsEyeSnapshotY;
    private double birdsEyeSnapshotZ;
    @Nonnull
    private WallCardinal cameraViewFromSide = WallCardinal.SOUTH;

    @Nullable
    private UUID editTargetPlotId;

    @Nullable
    private UUID editTargetSegmentId;

    private boolean removeConfirmOpen;

    /** Last world direction used when chaining with the placement pad (for tower auto-connect). */
    @Nullable
    private WallCardinal lastExpandDir;

    /** World side the previous committed piece lies on relative to {@link #currentAnchor}. */
    @Nullable
    private WallCardinal arrivalFromSide;

    /** Pad direction for the piece currently being placed (set before {@code tryPlace}). */
    @Nullable
    private WallCardinal placementExpandDir;

    /** Last geometry direction used to place the preview (joint face on segment for towers). */
    @Nullable
    private WallCardinal lastPositionDir;

    private static boolean defaultDebugLogging = true;

    private boolean debugLogging = defaultDebugLogging;

    public WallPlacementSession(@Nonnull World world, @Nonnull Vector3i startAnchor) {
        this.world = world;
        this.currentAnchor = startAnchor.clone();
        this.currentRotationSteps = 0;
    }

    public static boolean isDefaultDebugLogging() {
        return defaultDebugLogging;
    }

    public static void setDefaultDebugLogging(boolean defaultDebugLogging) {
        WallPlacementSession.defaultDebugLogging = defaultDebugLogging;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    @Nonnull
    public String describeState() {
        CommittedStep last = getLastCommitted();
        String lastDesc =
            last == null
                ? "none"
                : last.constructionId
                    + "@"
                    + last.signAnchor
                    + " rot="
                    + last.rotationSteps
                    + " chain="
                    + last.chainExpandDir
                    + " towerFaces="
                    + WallPlacementDebug.formatDirs(last.towerConnectionDirs);
        return "piece="
            + pieceKind
            + " anchor="
            + currentAnchor
            + " rot="
            + currentRotationSteps
            + " arrival="
            + arrivalFromSide
            + " lastExpand="
            + lastExpandDir
            + " placeExpand="
            + placementExpandDir
            + " positionDir="
            + lastPositionDir
            + " towerConn="
            + WallPlacementDebug.formatDirs(towerConnections)
            + " allowed="
            + WallPlacementDebug.formatAllowed(allowedExpandDirections())
            + " commits="
            + committed.size()
            + " last="
            + lastDesc
            + " resolved="
            + resolveConstructionId();
    }

    @Nonnull
    public World getWorld() {
        return world;
    }

    @Nonnull
    public Vector3i getCurrentAnchor() {
        return currentAnchor.clone();
    }

    public void setCurrentAnchor(@Nonnull Vector3i anchor) {
        this.currentAnchor = anchor.clone();
    }

    public int getCurrentRotationSteps() {
        return currentRotationSteps;
    }

    public void setCurrentRotationSteps(int steps) {
        this.currentRotationSteps = (steps % 4 + 4) % 4;
    }

    @Nonnull
    public Rotation getCurrentPrefabYaw() {
        return rotationStepsFrom(currentRotationSteps);
    }

    @Nonnull
    public PieceKind getPieceKind() {
        return pieceKind;
    }

    public void setPieceKind(@Nonnull PieceKind pieceKind) {
        if (pieceKind == PieceKind.TOWER && !canPlaceTowerNow()) {
            this.pieceKind = PieceKind.SEGMENT;
            towerConnections.clear();
            realignPreviewToLastCommitted();
            return;
        }
        this.pieceKind = pieceKind;
        clearBirdsEyeSnapshot();
        if (pieceKind != PieceKind.TOWER) {
            towerConnections.clear();
        } else {
            CommittedStep last = getLastCommitted();
            if (lastExpandDir == null && last != null && last.chainExpandDir != null) {
                lastExpandDir = last.chainExpandDir;
            }
            refreshArrivalFromSide();
        }
        realignPreviewToLastCommitted();
    }

    /**
     * Recomputes {@link #currentAnchor} for the selected piece type so wall/gate/tower previews sit at the correct
     * joint distance from the last committed piece.
     */
    public void realignPreviewToLastCommitted() {
        WallCardinal expandDir = resolveChainExpandDir();
        if (expandDir == null) {
            return;
        }
        previewExpandDirection(expandDir);
    }

    /**
     * Picks where the current preview piece will sit relative to the last commit (does not place). Expand pads call
     * this; use the Place button to commit.
     */
    public void previewExpandDirection(@Nonnull WallCardinal expandDir) {
        CommittedStep last = getLastCommitted();
        if (last == null) {
            lastExpandDir = expandDir;
            placementExpandDir = expandDir;
            currentRotationSteps = expandDir.rotationStepsForLocalNorthAlongAxis();
            arrivalFromSide = null;
            return;
        }
        applyExpandPreviewPlan(
            WallPlacementChainPlanner.planExpandPreview(
                last.signAnchor.clone(),
                last.rotationSteps,
                toPlannerPieceKind(pieceKind),
                toPlannerCommitted(),
                expandDir,
                null
            )
        );
    }

    private void applyExpandPreviewPlan(@Nonnull WallPlacementChainPlanner.ExpandPreviewPlan plan) {
        currentAnchor = plan.anchor().clone();
        currentRotationSteps = plan.rotationSteps();
        lastExpandDir = plan.outgoingExpandDir();
        placementExpandDir = plan.outgoingExpandDir();
        lastPositionDir = plan.positionDir();
        arrivalFromSide = plan.arrivalFromSide();
        towerConnections.clear();
        if (plan.towerConnections() != null) {
            towerConnections.addAll(plan.towerConnections());
        }
    }

    @Nullable
    private WallCardinal resolveChainExpandDir() {
        if (lastExpandDir != null) {
            return lastExpandDir;
        }
        CommittedStep last = getLastCommitted();
        if (last != null && last.chainExpandDir != null) {
            return last.chainExpandDir;
        }
        if (arrivalFromSide != null) {
            return arrivalFromSide.opposite();
        }
        return null;
    }

    /** Towers cannot be placed twice in a row; use wall/gate after a tower until another segment is placed. */
    public boolean canPlaceTowerNow() {
        CommittedStep last = getLastCommitted();
        return last == null || !WallPieceGeometry.isTowerConstructionId(last.constructionId);
    }

    public void afterTowerCommittedSwitchToWall() {
        pieceKind = PieceKind.SEGMENT;
        towerConnections.clear();
    }

    @Nonnull
    public EnumSet<WallCardinal> getTowerConnections() {
        return EnumSet.copyOf(towerConnections);
    }

    /** Before placing: if the last commit is a 1-connection tower, add the outgoing face from the next pad press. */
    public boolean applyOutgoingDirectionToLastTower(@Nonnull WallCardinal outgoingExpandDir) {
        CommittedStep last = getLastCommitted();
        if (last == null || last.towerConnectionDirs == null || last.towerConnectionDirs.size() != 1) {
            return false;
        }
        EnumSet<WallCardinal> pair =
            WallTowerAutoConnector.connectionsForCorner(last.towerConnectionDirs, outgoingExpandDir);
        WallTowerPrefabResolver.ResolvedTower resolved = WallTowerAutoConnector.resolve(pair);
        if (resolved == null) {
            return false;
        }
        int idx = committed.size() - 1;
        committed.set(
            idx,
            new CommittedStep(
                last.plotId,
                resolved.constructionId(),
                last.signAnchor,
                resolved.rotationSteps(),
                pair,
                last.chainExpandDir
            )
        );
        return true;
    }

    /**
     * Sets tower connections for the pad click without moving {@link #currentAnchor}. Incoming face is where the chain
     * arrived; outgoing is the pad direction.
     */
    public void prepareTowerPlacementForClick(@Nonnull WallCardinal outgoingExpandDir) {
        previewExpandDirection(outgoingExpandDir);
    }

    public void setPlacementExpandDir(@Nullable WallCardinal placementExpandDir) {
        this.placementExpandDir = placementExpandDir;
    }

    @Nullable
    public WallCardinal getPlacementExpandDir() {
        return placementExpandDir;
    }

    @Nullable
    public WallCardinal getLastExpandDir() {
        return lastExpandDir;
    }

    @Nonnull
    public EnumSet<WallCardinal> allowedExpandDirections() {
        return WallPlacementChainPlanner.allowedExpandDirections(
            toPlannerPieceKind(pieceKind), toPlannerCommitted(), arrivalFromSide
        );
    }

    private void refreshArrivalFromSide() {
        CommittedStep last = getLastCommitted();
        if (last == null) {
            arrivalFromSide = null;
            return;
        }
        if (pieceKind == PieceKind.TOWER && last.chainExpandDir != null) {
            arrivalFromSide = last.chainExpandDir.opposite();
            return;
        }
        arrivalFromSide = WallCardinal.fromVector(currentAnchor, last.signAnchor);
    }

    /** Restores preview anchor, piece type, and chaining state after undo removes the last commit. */
    public void restoreStateAfterUndo() {
        towerConnections.clear();
        placementExpandDir = null;
        CommittedStep last = getLastCommitted();
        if (last == null) {
            pieceKind = PieceKind.SEGMENT;
            lastExpandDir = null;
            arrivalFromSide = null;
            return;
        }
        if (WallPieceGeometry.isTowerConstructionId(last.constructionId)) {
            pieceKind = PieceKind.SEGMENT;
        } else {
            pieceKind = PieceKind.SEGMENT;
        }
        if (last.chainExpandDir != null) {
            WallCardinal expand = last.chainExpandDir;
            int newRotationSteps = rotationStepsForChainAfter(last, expand);
            Rotation newYaw = rotationStepsFrom(newRotationSteps);
            currentAnchor = computeChainedSignAnchor(last, newRotationSteps, newYaw, expand, expand, false);
            currentRotationSteps = newRotationSteps;
            lastExpandDir = expand;
            arrivalFromSide = expand.opposite();
        } else {
            currentAnchor = last.signAnchor.clone();
            currentRotationSteps = last.rotationSteps;
            lastExpandDir = null;
            arrivalFromSide = null;
        }
    }

    @Nonnull
    public String resolveConstructionId() {
        if (pieceKind == PieceKind.TOWER) {
            WallTowerPrefabResolver.ResolvedTower r = WallTowerPrefabResolver.resolve(towerConnections);
            return r != null ? r.constructionId() : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_TOWER_ENDCAP_S;
        }
        return pieceKind.fixedConstructionId != null ? pieceKind.fixedConstructionId : AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT;
    }

    public void applyTowerResolvedRotation() {
        if (pieceKind != PieceKind.TOWER) {
            return;
        }
        WallTowerPrefabResolver.ResolvedTower r = WallTowerPrefabResolver.resolve(towerConnections);
        if (r != null) {
            currentRotationSteps = r.rotationSteps();
        }
    }

    @Nonnull
    public List<CommittedStep> getCommitted() {
        return List.copyOf(committed);
    }

    @Nullable
    public CommittedStep getLastCommitted() {
        return committed.isEmpty() ? null : committed.get(committed.size() - 1);
    }

    @Nullable
    public CommittedStep getPreviousCommitted() {
        int n = committed.size();
        return n >= 2 ? committed.get(n - 2) : null;
    }

    public void addCommitted(@Nonnull CommittedStep step) {
        committed.add(step);
    }

    @Nullable
    public CommittedStep undoLastCommitted() {
        if (committed.isEmpty()) {
            return null;
        }
        return committed.remove(committed.size() - 1);
    }

    public void extendPreview(@Nonnull WallCardinal expandDir) {
        lastExpandDir = expandDir;
        CommittedStep last = getLastCommitted();
        if (last != null) {
            if (pieceKind == PieceKind.TOWER) {
                applyExpandPreviewPlan(
                    WallPlacementChainPlanner.planExpandPreview(
                        last.signAnchor.clone(),
                        last.rotationSteps,
                        toPlannerPieceKind(pieceKind),
                        toPlannerCommitted(),
                        expandDir,
                        null
                    )
                );
                return;
            }
            int newRotationSteps = rotationStepsForChainAfter(last, expandDir);
            Rotation newYaw = rotationStepsFrom(newRotationSteps);
            currentAnchor =
                computeChainedSignAnchor(
                    last, newRotationSteps, newYaw, expandDir, expandDir, false
                );
            currentRotationSteps = newRotationSteps;
            arrivalFromSide = expandDir.opposite();
            return;
        }
        Vector3i delta = expandDir.rotateOffset(new Vector3i(0, 0, WallPieceGeometry.segmentChainSpan()));
        currentAnchor = new Vector3i(currentAnchor.x + delta.x, currentAnchor.y, currentAnchor.z + delta.z);
        currentRotationSteps = expandDir.rotationStepsForLocalNorthAlongAxis();
        arrivalFromSide = expandDir.opposite();
    }

    /**
     * Straight wall/gate runs keep the previous yaw. After a tower, match the wall segment run before the tower.
     * Tower previews use {@link WallPlacementChainPlanner#planExpandPreview} (rotation and anchor stay in sync).
     */
    private int rotationStepsForChainAfter(@Nonnull CommittedStep last, @Nonnull WallCardinal expandDir) {
        CommittedStep previous = getPreviousCommitted();
        return WallPlacementChainPlanner.rotationStepsForChainAfter(
            toPlannerPieceKind(pieceKind),
            toPlannerPiece(last),
            previous == null ? null : toPlannerPiece(previous),
            expandDir
        );
    }

    @Nullable
    public EnumSet<WallCardinal> towerConnectionsForCommit() {
        if (pieceKind != PieceKind.TOWER || towerConnections.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(towerConnections);
    }

    public void nudgeY(int dy) {
        currentAnchor = new Vector3i(currentAnchor.x, currentAnchor.y + dy, currentAnchor.z);
    }

    @Nonnull
    public List<Ref<EntityStore>> getPreviewEntityRefs() {
        return previewEntityRefs;
    }

    @Nullable
    public UUID getEditTargetPlotId() {
        return editTargetPlotId;
    }

    public void setEditTargetPlotId(@Nullable UUID editTargetPlotId) {
        this.editTargetPlotId = editTargetPlotId;
        this.editTargetSegmentId = null;
    }

    @Nullable
    public UUID getEditTargetSegmentId() {
        return editTargetSegmentId;
    }

    public void setEditTargetSegmentId(@Nullable UUID editTargetSegmentId) {
        this.editTargetSegmentId = editTargetSegmentId;
        this.editTargetPlotId = null;
    }

    public boolean hasEditTarget() {
        return editTargetPlotId != null || editTargetSegmentId != null;
    }

    public boolean isRemoveConfirmOpen() {
        return removeConfirmOpen;
    }

    public void setRemoveConfirmOpen(boolean removeConfirmOpen) {
        this.removeConfirmOpen = removeConfirmOpen;
    }

    public void clearBirdsEyeSnapshot() {
        birdsEyeSnapshotValid = false;
    }

    public void setBirdsEyeSnapshot(double x, double y, double z) {
        birdsEyeSnapshotX = x;
        birdsEyeSnapshotY = y;
        birdsEyeSnapshotZ = z;
        birdsEyeSnapshotValid = true;
    }

    public boolean hasBirdsEyeSnapshot() {
        return birdsEyeSnapshotValid;
    }

    public double getBirdsEyeSnapshotX() {
        return birdsEyeSnapshotX;
    }

    public double getBirdsEyeSnapshotY() {
        return birdsEyeSnapshotY;
    }

    public double getBirdsEyeSnapshotZ() {
        return birdsEyeSnapshotZ;
    }

    @Nonnull
    public WallCardinal getCameraViewFromSide() {
        return cameraViewFromSide;
    }

    public void setCameraViewFromSide(@Nonnull WallCardinal side) {
        this.cameraViewFromSide = side;
    }

    @Nonnull
    private Vector3i computeChainedSignAnchor(
        @Nonnull CommittedStep last,
        int newRotationSteps,
        @Nonnull Rotation newYaw,
        @Nonnull WallCardinal positionDir,
        @Nonnull WallCardinal outgoingExpandDir,
        boolean newPieceIsTower
    ) {
        return WallPlacementChainPlanner.computeChainedSignAnchor(
            toPlannerPiece(last),
            newRotationSteps,
            newYaw,
            positionDir,
            outgoingExpandDir,
            newPieceIsTower,
            toPlannerPieceKind(pieceKind),
            null,
            null
        );
    }

    @Nonnull
    private List<WallPlacementChainPlanner.ChainCommittedPiece> toPlannerCommitted() {
        return committed.stream().map(this::toPlannerPiece).toList();
    }

    @Nonnull
    private WallPlacementChainPlanner.ChainCommittedPiece toPlannerPiece(@Nonnull CommittedStep step) {
        return new WallPlacementChainPlanner.ChainCommittedPiece(
            step.constructionId, step.signAnchor, step.rotationSteps, step.towerConnectionDirs, step.chainExpandDir
        );
    }

    @Nonnull
    private static WallPlacementChainPlanner.PieceKind toPlannerPieceKind(@Nonnull PieceKind kind) {
        return switch (kind) {
            case GATE -> WallPlacementChainPlanner.PieceKind.GATE;
            case TOWER -> WallPlacementChainPlanner.PieceKind.TOWER;
            default -> WallPlacementChainPlanner.PieceKind.SEGMENT;
        };
    }

    @Nonnull
    public static Rotation rotationStepsFrom(int steps) {
        return WallPlacementChainPlanner.rotationStepsFrom(steps);
    }
}
