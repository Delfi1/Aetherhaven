package com.hexvane.aetherhaven.town;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Completed town wall piece (not listed in the quest journal). */
public final class WallSegmentRecord {
    @SerializedName("segmentId")
    private String segmentId;

    @SerializedName("constructionId")
    private String constructionId;

    @SerializedName("minX")
    private int minX;

    @SerializedName("minY")
    private int minY;

    @SerializedName("minZ")
    private int minZ;

    @SerializedName("maxX")
    private int maxX;

    @SerializedName("maxY")
    private int maxY;

    @SerializedName("maxZ")
    private int maxZ;

    @SerializedName("prefabAnchorX")
    private int prefabAnchorX;

    @SerializedName("prefabAnchorY")
    private int prefabAnchorY;

    @SerializedName("prefabAnchorZ")
    private int prefabAnchorZ;

    @Nullable
    @SerializedName("prefabYaw")
    private String prefabYaw;

    @SerializedName("completedAtEpochMs")
    private long completedAtEpochMs;

    public WallSegmentRecord() {}

    public WallSegmentRecord(
        @Nonnull UUID segmentId,
        @Nonnull String constructionId,
        @Nonnull PlotFootprintRecord footprint,
        int prefabAnchorX,
        int prefabAnchorY,
        int prefabAnchorZ,
        @Nonnull Rotation yaw,
        long completedAtEpochMs
    ) {
        this.segmentId = segmentId.toString();
        this.constructionId = constructionId;
        this.minX = footprint.getMinX();
        this.minY = footprint.getMinY();
        this.minZ = footprint.getMinZ();
        this.maxX = footprint.getMaxX();
        this.maxY = footprint.getMaxY();
        this.maxZ = footprint.getMaxZ();
        this.prefabAnchorX = prefabAnchorX;
        this.prefabAnchorY = prefabAnchorY;
        this.prefabAnchorZ = prefabAnchorZ;
        this.prefabYaw = yaw.name();
        this.completedAtEpochMs = completedAtEpochMs;
    }

    @Nonnull
    public UUID getSegmentId() {
        return UUID.fromString(segmentId);
    }

    @Nonnull
    public String getConstructionId() {
        return constructionId != null ? constructionId : "";
    }

    @Nonnull
    public PlotFootprintRecord toFootprint() {
        return new PlotFootprintRecord(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int getPrefabAnchorX() {
        return prefabAnchorX;
    }

    public int getPrefabAnchorY() {
        return prefabAnchorY;
    }

    public int getPrefabAnchorZ() {
        return prefabAnchorZ;
    }

    @Nonnull
    public Rotation resolvePrefabYaw() {
        if (prefabYaw == null || prefabYaw.isBlank()) {
            return Rotation.None;
        }
        try {
            return Rotation.valueOf(prefabYaw.trim());
        } catch (IllegalArgumentException e) {
            return Rotation.None;
        }
    }

    public long getCompletedAtEpochMs() {
        return completedAtEpochMs;
    }

    public boolean intersectsFootprint(@Nonnull PlotFootprintRecord candidate) {
        return toFootprint().intersects(candidate);
    }

    public boolean containsBlock(int x, int y, int z) {
        return toFootprint().containsBlock(x, y, z);
    }
}
