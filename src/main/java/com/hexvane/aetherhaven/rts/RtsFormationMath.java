package com.hexvane.aetherhaven.rts;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class RtsFormationMath {
    private static final double SPACING = 2.0;

    private RtsFormationMath() {}

    @Nonnull
    public static List<Vector3d> lineOffsets(int unitCount) {
        List<Vector3d> out = new ArrayList<>(Math.max(1, unitCount));
        if (unitCount <= 0) {
            return out;
        }
        double start = -(unitCount - 1) * SPACING * 0.5;
        for (int i = 0; i < unitCount; i++) {
            out.add(new Vector3d(start + i * SPACING, 0, 0));
        }
        return out;
    }
}
