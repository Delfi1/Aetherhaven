package com.hexvane.aetherhaven.routevisual;

import com.hexvane.aetherhaven.AetherhavenConstants;
import javax.annotation.Nonnull;

/** Tunables for route particle rendering. Callers pass an audience list; no wand coupling. */
public final class RouteParticleConfig {
    public static final RouteParticleConfig DEFAULT = new RouteParticleConfig(
        AetherhavenConstants.ROUTE_PARTICLE_TRAIL_ID,
        AetherhavenConstants.ROUTE_PARTICLE_NODE_ID,
        2.0,
        20.0,
        10,
        1.0,
        3.0
    );

    /** Blue route preview for saved routes that are not selected (same density as default; green selected is brighter). */
    public static final RouteParticleConfig DIM = new RouteParticleConfig(
        AetherhavenConstants.ROUTE_PARTICLE_TRAIL_ID,
        AetherhavenConstants.ROUTE_PARTICLE_NODE_ID,
        2.0,
        20.0,
        10,
        1.0,
        3.0
    );

    public static final RouteParticleConfig SELECTED = new RouteParticleConfig(
        AetherhavenConstants.ROUTE_PARTICLE_TRAIL_SELECTED_ID,
        AetherhavenConstants.ROUTE_PARTICLE_NODE_SELECTED_ID,
        2.0,
        20.0,
        10,
        1.0,
        3.0
    );

    @Nonnull
    private final String trailParticleId;
    @Nonnull
    private final String nodeParticleId;
    private final double spacing;
    private final double maxSegmentLength;
    private final int tickRate;
    private final double groundYOffset;
    private final double nodeGroundYOffset;

    public RouteParticleConfig(
        @Nonnull String trailParticleId,
        @Nonnull String nodeParticleId,
        double spacing,
        double maxSegmentLength,
        int tickRate,
        double groundYOffset,
        double nodeGroundYOffset
    ) {
        this.trailParticleId = trailParticleId;
        this.nodeParticleId = nodeParticleId;
        this.spacing = spacing;
        this.maxSegmentLength = maxSegmentLength;
        this.tickRate = tickRate;
        this.groundYOffset = groundYOffset;
        this.nodeGroundYOffset = nodeGroundYOffset;
    }

    @Nonnull
    public String getTrailParticleId() {
        return trailParticleId;
    }

    @Nonnull
    public String getNodeParticleId() {
        return nodeParticleId;
    }

    public double getSpacing() {
        return spacing;
    }

    public double getMaxSegmentLength() {
        return maxSegmentLength;
    }

    public int getTickRate() {
        return tickRate;
    }

    public double getGroundYOffset() {
        return groundYOffset;
    }

    public double getNodeGroundYOffset() {
        return nodeGroundYOffset;
    }
}
