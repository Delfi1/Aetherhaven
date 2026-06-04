package com.hexvane.aetherhaven.plotcreator;

import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Plot sign must sit on one of the four exterior ground corners of the marked bounds, never inside. */
public final class PlotCreatorAnchorRules {
    private PlotCreatorAnchorRules() {}

    public static boolean hasBounds(@Nonnull PlotCreatorDraft draft) {
        return draft.getCornerFirst() != null && draft.getCornerSecond() != null;
    }

    /**
     * True when {@code pos} is diagonally outside the footprint on X and Z (one of four outer corners).
     * Y is not restricted to the vertical span so players can click the surface block at that corner.
     */
    public static boolean isOutsideCorner(@Nonnull PlotCreatorDraft draft, @Nonnull Vector3i pos) {
        if (!hasBounds(draft)) {
            return false;
        }
        if (draft.isInsideBounds(pos)) {
            return false;
        }
        Vector3i min = draft.boundsMin();
        Vector3i max = draft.boundsMax();
        boolean xOnOuterFace = pos.x == min.x - 1 || pos.x == max.x + 1;
        boolean zOnOuterFace = pos.z == min.z - 1 || pos.z == max.z + 1;
        return xOnOuterFace && zOnOuterFace;
    }
}
