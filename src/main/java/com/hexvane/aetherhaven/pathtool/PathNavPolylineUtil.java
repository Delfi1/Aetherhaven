package com.hexvane.aetherhaven.pathtool;

import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Utilities for extracting equidistant centerline points from spline samples. */
public final class PathNavPolylineUtil {
    private PathNavPolylineUtil() {}

    @Nonnull
    public static List<PathNavPoint> resamplePolyline(
        @Nonnull List<PathNavPoint> nodes,
        double spacingBlocks
    ) {
        if (nodes.size() < 2) {
            return new ArrayList<>(nodes);
        }
        ArrayList<PathSplineUtil.PathSample> samples = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            PathNavPoint p = nodes.get(i);
            Vector3d pos = new Vector3d(p.x, p.y, p.z);
            Vector3d forward = forwardAlongPolyline(nodes, i);
            Vector3d right = new Vector3d(-forward.z, 0.0, forward.x);
            samples.add(new PathSplineUtil.PathSample(pos, forward, right));
        }
        return resampleCenterline(samples, spacingBlocks);
    }

    @Nonnull
    private static Vector3d forwardAlongPolyline(@Nonnull List<PathNavPoint> nodes, int index) {
        if (index + 1 < nodes.size()) {
            PathNavPoint a = nodes.get(index);
            PathNavPoint b = nodes.get(index + 1);
            return unitXZ(b.x - a.x, b.z - a.z);
        }
        if (index > 0) {
            PathNavPoint a = nodes.get(index - 1);
            PathNavPoint b = nodes.get(index);
            return unitXZ(b.x - a.x, b.z - a.z);
        }
        return new Vector3d(0.0, 0.0, 1.0);
    }

    @Nonnull
    private static Vector3d unitXZ(double dx, double dz) {
        double len = Math.hypot(dx, dz);
        if (len < 1.0e-6) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return new Vector3d(dx / len, 0.0, dz / len);
    }

    @Nonnull
    public static List<PathNavPoint> resampleCenterline(
        @Nonnull List<PathSplineUtil.PathSample> samples,
        double spacingBlocks
    ) {
        List<PathNavPoint> out = new ArrayList<>();
        if (samples.isEmpty()) {
            return out;
        }
        double spacing = Math.max(0.25, spacingBlocks);
        Vector3d first = samples.get(0).position;
        out.add(new PathNavPoint(first.x(), first.y(), first.z()));

        Vector3d cursor = new Vector3d(first.x(), first.y(), first.z());
        double carry = 0.0;
        for (int i = 1; i < samples.size(); i++) {
            Vector3d segEnd = samples.get(i).position;
            Vector3d segStart = new Vector3d(cursor.x(), cursor.y(), cursor.z());
            double segDx = segEnd.x() - segStart.x();
            double segDy = segEnd.y() - segStart.y();
            double segDz = segEnd.z() - segStart.z();
            double segLen = Math.sqrt(segDx * segDx + segDy * segDy + segDz * segDz);
            if (segLen <= 1.0e-6) {
                cursor = segEnd;
                continue;
            }
            while (carry + segLen >= spacing) {
                double step = spacing - carry;
                double t = step / segLen;
                double nx = segStart.x() + segDx * t;
                double ny = segStart.y() + segDy * t;
                double nz = segStart.z() + segDz * t;
                out.add(new PathNavPoint(nx, ny, nz));
                segStart = new Vector3d(nx, ny, nz);
                segDx = segEnd.x() - segStart.x();
                segDy = segEnd.y() - segStart.y();
                segDz = segEnd.z() - segStart.z();
                segLen = Math.sqrt(segDx * segDx + segDy * segDy + segDz * segDz);
                carry = 0.0;
                if (segLen <= 1.0e-6) {
                    break;
                }
            }
            carry += segLen;
            cursor = segEnd;
        }

        Vector3d last = samples.get(samples.size() - 1).position;
        if (out.isEmpty() || distSq(out.get(out.size() - 1), last) > 1.0e-4) {
            out.add(new PathNavPoint(last.x(), last.y(), last.z()));
        }
        return out;
    }

    private static double distSq(@Nonnull PathNavPoint p, @Nonnull Vector3d v) {
        double dx = p.x - v.x();
        double dy = p.y - v.y();
        double dz = p.z - v.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
