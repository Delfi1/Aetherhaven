package com.hexvane.aetherhaven.patrol;

import org.joml.Vector3d;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Ray sphere pick for patrol wand draft nodes and saved route nodes. */
public final class PatrolWandRayPick {
    private PatrolWandRayPick() {}

    @Nullable
    public static PatrolWandNode pickDraftNode(
        @Nonnull Vector3d origin,
        @Nonnull Vector3d direction,
        double maxDistance,
        @Nonnull List<PatrolWandNode> nodes,
        double nodeRadius
    ) {
        return pickInternal(origin, direction, maxDistance, nodes, nodeRadius);
    }

    @Nullable
    public static PatrolRouteRecord pickSavedRoute(
        @Nonnull Vector3d origin,
        @Nonnull Vector3d direction,
        double maxDistance,
        @Nonnull List<PatrolRouteRecord> routes,
        double nodeRadius
    ) {
        double r2 = nodeRadius * nodeRadius;
        double bestT = Double.POSITIVE_INFINITY;
        PatrolRouteRecord best = null;
        for (PatrolRouteRecord route : routes) {
            if (route == null || route.nodes == null) {
                continue;
            }
            for (PatrolRouteNode n : route.nodes) {
                if (n == null) {
                    continue;
                }
                double t = raySphereT(origin, direction, maxDistance, n.x, n.y, n.z, r2);
                if (t >= 0.0 && t < bestT) {
                    bestT = t;
                    best = route;
                }
            }
        }
        return best;
    }

    @Nullable
    private static PatrolWandNode pickInternal(
        @Nonnull Vector3d origin,
        @Nonnull Vector3d direction,
        double maxDistance,
        @Nonnull List<PatrolWandNode> nodes,
        double nodeRadius
    ) {
        double r2 = nodeRadius * nodeRadius;
        double bestT = Double.POSITIVE_INFINITY;
        PatrolWandNode best = null;
        for (PatrolWandNode n : nodes) {
            double t = raySphereT(origin, direction, maxDistance, n.getX(), n.getY(), n.getZ(), r2);
            if (t >= 0.0 && t < bestT) {
                bestT = t;
                best = n;
            }
        }
        return best;
    }

    private static double raySphereT(
        @Nonnull Vector3d origin,
        @Nonnull Vector3d direction,
        double maxDistance,
        double cx,
        double cy,
        double cz,
        double r2
    ) {
        double dx = direction.x();
        double dy = direction.y();
        double dz = direction.z();
        double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dLen < 1.0e-6) {
            return -1.0;
        }
        dx /= dLen;
        dy /= dLen;
        dz /= dLen;
        double lx = origin.x() - cx;
        double ly = origin.y() - cy;
        double lz = origin.z() - cz;
        double b = 2.0 * (dx * lx + dy * ly + dz * lz);
        double c = lx * lx + ly * ly + lz * lz - r2;
        double disc = b * b - 4.0 * c;
        if (disc < 0.0) {
            return -1.0;
        }
        double s = Math.sqrt(disc);
        double t0 = 0.5 * (-b - s);
        double t1 = 0.5 * (-b + s);
        for (int i = 0; i < 2; i++) {
            double t = i == 0 ? t0 : t1;
            if (t > 0.0 && t <= maxDistance + 1.0e-3) {
                return t;
            }
        }
        return -1.0;
    }
}
