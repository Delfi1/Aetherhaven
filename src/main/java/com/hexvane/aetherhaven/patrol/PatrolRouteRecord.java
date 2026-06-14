package com.hexvane.aetherhaven.patrol;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Persisted guard patrol route for a town. */
public final class PatrolRouteRecord {
    @SerializedName("id")
    public String id;

    @SerializedName("townId")
    public String townId;

    @SerializedName("displayName")
    public String displayName;

    @SerializedName("nodes")
    public List<PatrolRouteNode> nodes = new ArrayList<>();

    @SerializedName("assignedGuardUuid")
    @Nullable
    public String assignedGuardUuid;

    /** When true the route loops from last point back to first; otherwise guards ping-pong along the path. */
    @SerializedName("closedLoop")
    public boolean closedLoop;

    public PatrolRouteRecord() {}

    @Nullable
    public UUID getIdUuid() {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(id.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public UUID getTownIdUuid() {
        if (townId == null || townId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(townId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public UUID getAssignedGuardUuidParsed() {
        if (assignedGuardUuid == null || assignedGuardUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(assignedGuardUuid.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nonnull
    public List<org.joml.Vector3d> nodePositions() {
        List<org.joml.Vector3d> out = new ArrayList<>();
        if (nodes == null) {
            return out;
        }
        for (PatrolRouteNode n : nodes) {
            if (n != null) {
                out.add(n.toVector());
            }
        }
        return out;
    }

    @Nonnull
    public String safeDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName.trim() : "Patrol";
    }

    public boolean isClosedLoop() {
        return closedLoop;
    }
}
