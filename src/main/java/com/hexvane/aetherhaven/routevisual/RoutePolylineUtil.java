package com.hexvane.aetherhaven.routevisual;

import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Samples equidistant points along a polyline. */
public final class RoutePolylineUtil {
    private RoutePolylineUtil() {}

    /**
     * Returns world positions sampled along consecutive segments. Does not ground snap; callers may adjust Y.
     */
    @Nonnull
    public static List<Vector3d> samplePolyline(@Nonnull List<Vector3d> points, double spacing, double maxSegmentLength) {
        List<Vector3d> out = new ArrayList<>();
        if (points.size() < 2 || spacing <= 0.0) {
            return out;
        }
        for (int seg = 0; seg < points.size() - 1; seg++) {
            Vector3d a = points.get(seg);
            Vector3d b = points.get(seg + 1);
            sampleSegment(a, b, spacing, maxSegmentLength, out, seg > 0);
        }
        return out;
    }

    private static void sampleSegment(
        @Nonnull Vector3d a,
        @Nonnull Vector3d b,
        double spacing,
        double maxSegmentLength,
        @Nonnull List<Vector3d> out,
        boolean skipFirst
    ) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) {
            return;
        }
        double nx = dx / len;
        double ny = dy / len;
        double nz = dz / len;
        double clampedLen = Math.min(len, maxSegmentLength);
        double start = skipFirst ? spacing : 0.0;
        for (double d = start; d <= clampedLen + 1.0e-6; d += spacing) {
            out.add(new Vector3d(a.x + nx * d, a.y + ny * d, a.z + nz * d));
        }
    }

    /**
     * Samples from {@code from} toward {@code to} up to {@code maxDistance}.
     */
    @Nonnull
    public static List<Vector3d> sampleToward(
        @Nonnull Vector3d from,
        @Nonnull Vector3d to,
        double spacing,
        double maxDistance
    ) {
        List<Vector3d> out = new ArrayList<>();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5 || spacing <= 0.0) {
            return out;
        }
        double nx = dx / len;
        double ny = dy / len;
        double nz = dz / len;
        double clamped = Math.min(len, maxDistance);
        for (double d = spacing; d <= clamped; d += spacing) {
            out.add(new Vector3d(from.x + nx * d, from.y + ny * d, from.z + nz * d));
        }
        return out;
    }
}
