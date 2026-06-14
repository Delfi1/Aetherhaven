package com.hexvane.aetherhaven.patrol;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** One patrol waypoint in world space. */
public final class PatrolRouteNode {
    @SerializedName("x")
    public double x;

    @SerializedName("y")
    public double y;

    @SerializedName("z")
    public double z;

    public PatrolRouteNode() {}

    public PatrolRouteNode(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Nonnull
    public org.joml.Vector3d toVector() {
        return new org.joml.Vector3d(x, y, z);
    }
}
