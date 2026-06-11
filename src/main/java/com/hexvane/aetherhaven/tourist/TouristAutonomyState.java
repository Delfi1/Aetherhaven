package com.hexvane.aetherhaven.tourist;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class TouristAutonomyState implements Component<EntityStore> {
    public static final int PHASE_IDLE = 0;
    public static final int PHASE_TRAVEL = 1;
    public static final int PHASE_VISIT = 2;
    public static final int PHASE_RETURNING = 3;
    /** Brief POI interaction while visiting a plot. */
    public static final int PHASE_POI = 4;

    @Nonnull
    public static final BuilderCodec<TouristAutonomyState> CODEC =
        BuilderCodec.builder(TouristAutonomyState.class, TouristAutonomyState::new)
            .append(new KeyedCodec<>("Phase", Codec.INTEGER), (v, x) -> v.phase = x, v -> v.phase)
            .add()
            .append(new KeyedCodec<>("HomePortalId", Codec.STRING), (v, x) -> v.homePortalId = x != null ? x : "", v -> v.homePortalId)
            .add()
            .append(new KeyedCodec<>("TargetPoiId", Codec.STRING), (v, x) -> v.targetPoiId = x, v -> v.targetPoiId)
            .add()
            .append(new KeyedCodec<>("TargetX", Codec.DOUBLE), (v, x) -> v.targetX = x, v -> v.targetX)
            .add()
            .append(new KeyedCodec<>("TargetY", Codec.DOUBLE), (v, x) -> v.targetY = x, v -> v.targetY)
            .add()
            .append(new KeyedCodec<>("TargetZ", Codec.DOUBLE), (v, x) -> v.targetZ = x, v -> v.targetZ)
            .add()
            .append(new KeyedCodec<>("PhaseEndMs", Codec.LONG), (v, x) -> v.phaseEndEpochMs = x, v -> v.phaseEndEpochMs)
            .add()
            .append(new KeyedCodec<>("NextPickMs", Codec.LONG), (v, x) -> v.nextDecisionEpochMs = x, v -> v.nextDecisionEpochMs)
            .add()
            .append(new KeyedCodec<>("TravelStuckTicks", Codec.INTEGER), (v, x) -> v.travelStuckTicks = x, v -> v.travelStuckTicks)
            .add()
            .append(
                new KeyedCodec<>("TravelWaypoints", Codec.STRING),
                (v, x) -> v.decodeTravelWaypoints(x),
                v -> v.encodeTravelWaypoints()
            )
            .add()
            .append(
                new KeyedCodec<>("TravelWaypointIndex", Codec.INTEGER),
                (v, x) -> v.travelWaypointIndex = x != null ? Math.max(0, x) : 0,
                v -> v.travelWaypointIndex
            )
            .add()
            .append(new KeyedCodec<>("VisitPlotId", Codec.STRING), (v, x) -> v.visitPlotId = x != null ? x : "", v -> v.visitPlotId)
            .add()
            .append(new KeyedCodec<>("NextPoiPickMs", Codec.LONG), (v, x) -> v.nextPoiPickEpochMs = x, v -> v.nextPoiPickEpochMs)
            .add()
            .append(new KeyedCodec<>("LastPlotPoiId", Codec.STRING), (v, x) -> v.lastPlotPoiId = x, v -> v.lastPlotPoiId)
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, TouristAutonomyState> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(TouristAutonomyState.class, "AetherhavenTouristAutonomyState", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, TouristAutonomyState> getComponentType() {
        ComponentType<EntityStore, TouristAutonomyState> t = componentType;
        if (t == null) {
            throw new IllegalStateException("TouristAutonomyState not registered");
        }
        return t;
    }

    private int phase = PHASE_IDLE;
    private String homePortalId = "";
    @Nullable
    private String targetPoiId;
    private double targetX;
    private double targetY;
    private double targetZ;
    private long phaseEndEpochMs;
    private long nextDecisionEpochMs;
    private int travelStuckTicks;
    private final ArrayList<Vector3d> travelWaypoints = new ArrayList<>();
    private int travelWaypointIndex;
    private String visitPlotId = "";
    private long nextPoiPickEpochMs;
    @Nullable
    private String lastPlotPoiId;

    @Nonnull
    public static TouristAutonomyState fresh(long nowMs) {
        TouristAutonomyState s = new TouristAutonomyState();
        s.nextDecisionEpochMs = nowMs;
        return s;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    @Nullable
    public UUID getHomePortalId() {
        if (homePortalId == null || homePortalId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(homePortalId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setHomePortalId(@Nullable UUID id) {
        this.homePortalId = id != null ? id.toString() : "";
    }

    @Nullable
    public UUID getTargetPoiUuid() {
        if (targetPoiId == null || targetPoiId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(targetPoiId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setTravelTarget(double x, double y, double z, @Nonnull UUID destinationId) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetPoiId = destinationId.toString();
    }

    @Nullable
    public UUID getVisitPlotUuid() {
        if (visitPlotId == null || visitPlotId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(visitPlotId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setVisitPlotId(@Nullable UUID plotId) {
        this.visitPlotId = plotId != null ? plotId.toString() : "";
    }

    public void clearVisitPlot() {
        visitPlotId = "";
        nextPoiPickEpochMs = 0L;
        lastPlotPoiId = null;
    }

    public long getNextPoiPickEpochMs() {
        return nextPoiPickEpochMs;
    }

    public void setNextPoiPickEpochMs(long nextPoiPickEpochMs) {
        this.nextPoiPickEpochMs = nextPoiPickEpochMs;
    }

    @Nullable
    public UUID getLastPlotPoiUuid() {
        if (lastPlotPoiId == null || lastPlotPoiId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(lastPlotPoiId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setLastPlotPoiId(@Nullable UUID poiId) {
        this.lastPlotPoiId = poiId != null ? poiId.toString() : null;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public long getPhaseEndEpochMs() {
        return phaseEndEpochMs;
    }

    public void setPhaseEndEpochMs(long phaseEndEpochMs) {
        this.phaseEndEpochMs = phaseEndEpochMs;
    }

    public long getNextDecisionEpochMs() {
        return nextDecisionEpochMs;
    }

    public void setNextDecisionEpochMs(long nextDecisionEpochMs) {
        this.nextDecisionEpochMs = nextDecisionEpochMs;
    }

    public int getTravelStuckTicks() {
        return travelStuckTicks;
    }

    public void setTravelStuckTicks(int travelStuckTicks) {
        this.travelStuckTicks = travelStuckTicks;
    }

    public void setTravelWaypoints(@Nonnull List<Vector3d> points) {
        travelWaypoints.clear();
        travelWaypointIndex = 0;
        travelWaypoints.addAll(points);
    }

    public void clearTravelWaypoints() {
        travelWaypoints.clear();
        travelWaypointIndex = 0;
    }

    public void clearTravelTarget() {
        targetPoiId = null;
        targetX = 0.0;
        targetY = 0.0;
        targetZ = 0.0;
        clearTravelWaypoints();
    }

    @Nullable
    public Vector3d getCurrentTravelWaypoint() {
        if (travelWaypointIndex < 0 || travelWaypointIndex >= travelWaypoints.size()) {
            return null;
        }
        return travelWaypoints.get(travelWaypointIndex);
    }

    public boolean advanceTravelWaypoint() {
        if (travelWaypointIndex + 1 < travelWaypoints.size()) {
            travelWaypointIndex++;
            return true;
        }
        travelWaypointIndex = travelWaypoints.size();
        return false;
    }

    private void decodeTravelWaypoints(@Nullable String raw) {
        travelWaypoints.clear();
        travelWaypointIndex = 0;
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String part : raw.split(";")) {
            String[] xyz = part.split(",");
            if (xyz.length != 3) {
                continue;
            }
            try {
                travelWaypoints.add(new Vector3d(Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2])));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
    }

    @Nonnull
    private String encodeTravelWaypoints() {
        if (travelWaypoints.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < travelWaypoints.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            Vector3d v = travelWaypoints.get(i);
            sb.append(v.x).append(',').append(v.y).append(',').append(v.z);
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        TouristAutonomyState c = fresh(nextDecisionEpochMs);
        c.phase = phase;
        c.homePortalId = homePortalId;
        c.targetPoiId = targetPoiId;
        c.targetX = targetX;
        c.targetY = targetY;
        c.targetZ = targetZ;
        c.phaseEndEpochMs = phaseEndEpochMs;
        c.travelStuckTicks = travelStuckTicks;
        c.travelWaypoints.addAll(travelWaypoints);
        c.travelWaypointIndex = travelWaypointIndex;
        c.visitPlotId = visitPlotId;
        c.nextPoiPickEpochMs = nextPoiPickEpochMs;
        c.lastPlotPoiId = lastPlotPoiId;
        return c;
    }
}
