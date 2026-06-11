package com.hexvane.aetherhaven.tourist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

public final class TouristPortalRecord {
    private UUID portalId = UUID.randomUUID();
    private String worldName = "";
    private int blockX;
    private int blockY;
    private int blockZ;
    private UUID townId = new UUID(0L, 0L);
    private UUID plotId = new UUID(0L, 0L);
    private long plannedDayEpochDay = Long.MIN_VALUE;
    private final List<Long> plannedSpawnEpochMinutes = new ArrayList<>();
    private final List<Long> executedSpawnEpochMinutes = new ArrayList<>();

    @Nonnull
    public UUID getPortalId() {
        return portalId;
    }

    public void setPortalId(@Nonnull UUID portalId) {
        this.portalId = portalId;
    }

    @Nonnull
    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(@Nonnull String worldName) {
        this.worldName = worldName != null ? worldName : "";
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public void setBlockPosition(@Nonnull Vector3i pos) {
        this.blockX = pos.x;
        this.blockY = pos.y;
        this.blockZ = pos.z;
    }

    @Nonnull
    public Vector3i getBlockPosition() {
        return new Vector3i(blockX, blockY, blockZ);
    }

    @Nonnull
    public UUID getTownId() {
        return townId;
    }

    public void setTownId(@Nonnull UUID townId) {
        this.townId = townId;
    }

    @Nonnull
    public UUID getPlotId() {
        return plotId;
    }

    public void setPlotId(@Nonnull UUID plotId) {
        this.plotId = plotId;
    }

    public long getPlannedDayEpochDay() {
        return plannedDayEpochDay;
    }

    public void setPlannedDayEpochDay(long plannedDayEpochDay) {
        this.plannedDayEpochDay = plannedDayEpochDay;
    }

    @Nonnull
    public List<Long> getPlannedSpawnEpochMinutes() {
        return plannedSpawnEpochMinutes;
    }

    @Nonnull
    public List<Long> getExecutedSpawnEpochMinutes() {
        return executedSpawnEpochMinutes;
    }

    public void clearDailyPlan() {
        plannedSpawnEpochMinutes.clear();
        executedSpawnEpochMinutes.clear();
    }
}
