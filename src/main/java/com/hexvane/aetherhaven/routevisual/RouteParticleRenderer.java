package com.hexvane.aetherhaven.routevisual;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Per audience route particle rendering. Does not assume any specific tool or item. */
public final class RouteParticleRenderer {
    private RouteParticleRenderer() {}

    public static void renderPolyline(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull List<org.joml.Vector3d> points,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull RouteParticleConfig config
    ) {
        if (points.size() < 2 || audience.isEmpty()) {
            return;
        }
        List<org.joml.Vector3d> samples =
            RoutePolylineUtil.samplePolyline(points, config.getSpacing(), config.getMaxSegmentLength());
        for (org.joml.Vector3d sample : samples) {
            spawnTrailAt(world, store, sample, audience, config);
        }
    }

    public static void renderTrailToward(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull org.joml.Vector3d from,
        @Nonnull org.joml.Vector3d to,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull RouteParticleConfig config
    ) {
        if (audience.isEmpty()) {
            return;
        }
        List<org.joml.Vector3d> samples =
            RoutePolylineUtil.sampleToward(from, to, config.getSpacing(), config.getMaxSegmentLength());
        for (org.joml.Vector3d sample : samples) {
            spawnTrailAt(world, store, sample, audience, config);
        }
    }

    public static void renderNodeMarkers(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull List<org.joml.Vector3d> points,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull RouteParticleConfig config
    ) {
        if (points.isEmpty() || audience.isEmpty()) {
            return;
        }
        for (org.joml.Vector3d p : points) {
            Vector3d pos = new Vector3d(p.x, p.y, p.z);
            ParticleUtil.spawnParticleEffect(config.getNodeParticleId(), pos, audience, store);
        }
    }

    private static void spawnTrailAt(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull org.joml.Vector3d sample,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull RouteParticleConfig config
    ) {
        double y = groundY(world, sample.x, sample.y, sample.z, config.getGroundYOffset());
        Vector3d pos = new Vector3d(sample.x, y, sample.z);
        ParticleUtil.spawnParticleEffect(config.getTrailParticleId(), pos, audience, store);
    }

    private static double groundY(@Nonnull World world, double x, double y, double z, double offset) {
        Vector3d ground =
            TargetUtil.getTargetLocation(world, blockId -> blockId != 0, x, y + 3.0, z, 0.0, -1.0, 0.0, 10.0);
        return ground != null ? ground.y + offset : y + offset;
    }
}
