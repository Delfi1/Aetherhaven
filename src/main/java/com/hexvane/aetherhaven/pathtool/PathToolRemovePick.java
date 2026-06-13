package com.hexvane.aetherhaven.pathtool;

import org.joml.Vector3d;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Ray pick against committed path undo cells. */
public final class PathToolRemovePick {
    private static final double CELL_PICK_RADIUS = 0.55;

    private PathToolRemovePick() {}

    /**
     * @return id of the closest path whose undo cell is hit along the ray, or null
     */
    @Nullable
    public static UUID pickPathId(
        @Nonnull Vector3d origin,
        @Nonnull Vector3d direction,
        double maxDistance,
        @Nonnull List<PathCommitRecord> records
    ) {
        double dx = direction.x();
        double dy = direction.y();
        double dz = direction.z();
        double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dLen < 1.0e-6) {
            return null;
        }
        dx /= dLen;
        dy /= dLen;
        dz /= dLen;
        double r2 = CELL_PICK_RADIUS * CELL_PICK_RADIUS;
        double bestT = Double.POSITIVE_INFINITY;
        UUID bestId = null;
        for (PathCommitRecord rec : records) {
            if (rec == null || rec.id == null || rec.undo == null) {
                continue;
            }
            UUID id;
            try {
                id = rec.getIdUuid();
            } catch (Exception e) {
                continue;
            }
            for (PathToolUndoCell c : rec.undo) {
                if (c == null) {
                    continue;
                }
                double cx = c.x + 0.5;
                double cy = c.y + 0.5;
                double cz = c.z + 0.5;
                double lx = origin.x() - cx;
                double ly = origin.y() - cy;
                double lz = origin.z() - cz;
                double b = 2.0 * (dx * lx + dy * ly + dz * lz);
                double cc = lx * lx + ly * ly + lz * lz - r2;
                double disc = b * b - 4.0 * cc;
                if (disc < 0.0) {
                    continue;
                }
                double s = Math.sqrt(disc);
                double t0 = 0.5 * (-b - s);
                double t1 = 0.5 * (-b + s);
                for (int i = 0; i < 2; i++) {
                    double t = i == 0 ? t0 : t1;
                    if (t > 0.0 && t <= maxDistance + 1.0e-3 && t < bestT) {
                        bestT = t;
                        bestId = id;
                    }
                }
            }
        }
        return bestId;
    }
}
