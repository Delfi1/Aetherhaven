package com.hexvane.aetherhaven.wall;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.math.vector.Vector3i;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Parsed wall-kit geometry for one construction id (offsets in prefab-local space). */
public final class WallKitPieceDefinition {
    @SerializedName("extends")
    @Nullable
    private String extendsId;

    @SerializedName("runAxis")
    @Nullable
    private String runAxis;

    @SerializedName("towerConnectionHalf")
    private int towerConnectionHalf;

    @SerializedName("bounds")
    @Nullable
    private Bounds bounds;

    @SerializedName("faces")
    private Map<String, FaceOffsets> faces = Map.of();

    @Nullable
    public String getExtendsId() {
        return extendsId;
    }

    public boolean hasRunAxis() {
        return runAxis != null && !runAxis.isBlank();
    }

    /** Prefab-local run axis for segments: {@code Z} (default) or {@code X}. */
    @Nonnull
    public Vector3i runAxisLocal() {
        if (runAxis != null && runAxis.equalsIgnoreCase("X")) {
            return new Vector3i(1, 0, 0);
        }
        return new Vector3i(0, 0, 1);
    }

    public int towerConnectionHalf() {
        return towerConnectionHalf > 0 ? towerConnectionHalf : 4;
    }

    @Nullable
    public Bounds getBounds() {
        return bounds;
    }

    @Nonnull
    public EnumMap<WallCardinal, FaceOffsets> resolvedFaces() {
        EnumMap<WallCardinal, FaceOffsets> map = new EnumMap<>(WallCardinal.class);
        for (Map.Entry<String, FaceOffsets> e : faces.entrySet()) {
            WallCardinal face = WallCardinal.valueOf(e.getKey().toUpperCase());
            map.put(face, e.getValue());
        }
        return map;
    }

    public static final class Bounds {
        @SerializedName("min")
        private int[] min = new int[] {0, 0, 0};

        @SerializedName("max")
        private int[] max = new int[] {0, 0, 0};

        @Nonnull
        public int[] min() {
            return min != null && min.length == 3 ? min : new int[] {0, 0, 0};
        }

        @Nonnull
        public int[] max() {
            return max != null && max.length == 3 ? max : new int[] {0, 0, 0};
        }
    }

    public static final class FaceOffsets {
        @SerializedName("chain")
        @Nullable
        private int[] chain;

        @SerializedName("tower")
        @Nullable
        private int[] tower;

        @SerializedName("exterior")
        @Nullable
        private int[] exterior;

        @Nullable
        public Vector3i chainLocal() {
            return toVec(chain);
        }

        @Nullable
        public Vector3i towerLocal() {
            return toVec(tower);
        }

        @Nullable
        public Vector3i exteriorLocal() {
            return toVec(exterior);
        }

        @Nullable
        private static Vector3i toVec(@Nullable int[] a) {
            if (a == null || a.length != 3) {
                return null;
            }
            return new Vector3i(a[0], a[1], a[2]);
        }
    }
}
