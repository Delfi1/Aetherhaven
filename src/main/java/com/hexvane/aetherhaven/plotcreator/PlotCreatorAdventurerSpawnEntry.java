package com.hexvane.aetherhaven.plotcreator;

import javax.annotation.Nonnull;

/** Prefab-local adventurer stand cell and facing (yaw in prefab-local axes). */
public final class PlotCreatorAdventurerSpawnEntry {
    private final int localX;
    private final int localY;
    private final int localZ;
    private final float yawRadians;

    public PlotCreatorAdventurerSpawnEntry(int localX, int localY, int localZ, float yawRadians) {
        this.localX = localX;
        this.localY = localY;
        this.localZ = localZ;
        this.yawRadians = yawRadians;
    }

    public int getLocalX() {
        return localX;
    }

    public int getLocalY() {
        return localY;
    }

    public int getLocalZ() {
        return localZ;
    }

    public float getYawRadians() {
        return yawRadians;
    }

    @Nonnull
    public int[] localArray() {
        return new int[] {localX, localY, localZ};
    }

    public boolean matchesLocal(int x, int y, int z) {
        return localX == x && localY == y && localZ == z;
    }
}
