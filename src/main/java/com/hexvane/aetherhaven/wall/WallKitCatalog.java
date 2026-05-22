package com.hexvane.aetherhaven.wall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.prefab.PrefabRotation;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wall placement offsets from {@code Server/Aetherhaven/WallKit/wall_kit_geometry.json}. Offsets are authored in
 * prefab-local space; callers pass <em>world</em> {@link WallCardinal} directions and placement {@link Rotation}.
 */
public final class WallKitCatalog {
    public enum OffsetKind {
        /** Segment-to-segment chain connection. */
        CHAIN,
        /** Segment or tower mesh face for segment↔tower joints. */
        TOWER_JOINT,
        /** One block outside segment AABB (tower seats outside wall volume). */
        EXTERIOR,
        /** Tower connection face only. */
        TOWER_CONNECTION
    }

    private static final String RESOURCE = "Server/Aetherhaven/WallKit/wall_kit_geometry.json";

    private static volatile WallKitCatalog loaded;

    private final int chainSpan;
    private final Map<String, ResolvedPiece> pieces;

    private WallKitCatalog(int chainSpan, @Nonnull Map<String, ResolvedPiece> pieces) {
        this.chainSpan = chainSpan;
        this.pieces = pieces;
    }

    @Nonnull
    public static WallKitCatalog get() {
        WallKitCatalog ref = loaded;
        if (ref == null) {
            synchronized (WallKitCatalog.class) {
                ref = loaded;
                if (ref == null) {
                    ref = load();
                    loaded = ref;
                }
            }
        }
        return ref;
    }

    /** Tests may replace the catalog (e.g. after parsing JSON in isolation). */
    public static void setForTests(@Nonnull WallKitCatalog catalog) {
        loaded = catalog;
    }

    public int chainSpan() {
        return chainSpan;
    }

    @Nonnull
    public ResolvedPiece piece(@Nonnull String constructionId) {
        ResolvedPiece p = pieces.get(constructionId);
        if (p != null) {
            return p;
        }
        if (WallPieceGeometry.isTowerConstructionId(constructionId)) {
            ResolvedPiece tower = pieces.get("wall_tower_default");
            if (tower != null) {
                return tower;
            }
        }
        ResolvedPiece segment = pieces.get(AetherhavenConstants.CONSTRUCTION_PLOT_WALL_SEGMENT);
        if (segment != null) {
            return segment;
        }
        throw new IllegalStateException("wall kit geometry missing for " + constructionId);
    }

    /**
     * World-space offset from logical anchor to the attach point on {@code worldDir}, after rotating the matching
     * prefab-local face offset by {@code yaw}.
     */
    @Nonnull
    public Vector3i worldOffsetFromAnchor(
        @Nonnull String constructionId, @Nonnull Rotation yaw, @Nonnull WallCardinal worldDir, @Nonnull OffsetKind kind
    ) {
        return piece(constructionId).worldOffset(yaw, worldDir, kind);
    }

    /** True when the segment's run axis (from kit, rotated) lies along world Z (N/S expansion). */
    public boolean runAlongWorldZ(@Nonnull String segmentConstructionId, @Nonnull Rotation segmentYaw) {
        return piece(segmentConstructionId).runAlongWorldZ(segmentYaw);
    }

    @Nonnull
    private static WallKitCatalog load() {
        Gson gson = new GsonBuilder().create();
        try (InputStream in = WallKitCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing classpath resource: " + RESOURCE);
            }
            Root root = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Root.class);
            if (root == null || root.pieces == null) {
                throw new IllegalStateException("empty wall kit geometry");
            }
            Map<String, WallKitPieceDefinition> raw = new LinkedHashMap<>(root.pieces);
            Map<String, ResolvedPiece> resolved = new LinkedHashMap<>();
            for (String id : raw.keySet()) {
                resolveInto(id, raw, resolved, new LinkedHashMap<>());
            }
            int span = root.chainSpan > 0 ? root.chainSpan : 16;
            return new WallKitCatalog(span, Map.copyOf(resolved));
        } catch (Exception e) {
            throw new IllegalStateException("failed to load " + RESOURCE, e);
        }
    }

    private static void resolveInto(
        @Nonnull String id,
        @Nonnull Map<String, WallKitPieceDefinition> raw,
        @Nonnull Map<String, ResolvedPiece> resolved,
        @Nonnull Map<String, ResolvedPiece> pending
    ) {
        if (resolved.containsKey(id)) {
            return;
        }
        if (pending.containsKey(id)) {
            throw new IllegalStateException("cycle in wall kit extends: " + id);
        }
        WallKitPieceDefinition def = raw.get(id);
        if (def == null) {
            throw new IllegalStateException("unknown wall kit piece: " + id);
        }
        pending.put(id, null);
        EnumMap<WallCardinal, WallKitPieceDefinition.FaceOffsets> faces = new EnumMap<>(WallCardinal.class);
        Vector3i runAxis = new Vector3i(0, 0, 1);
        int towerHalf = 4;
        WallKitPieceDefinition.Bounds bounds = null;
        String parentId = def.getExtendsId();
        if (parentId != null && !parentId.isBlank()) {
            resolveInto(parentId.trim(), raw, resolved, pending);
            ResolvedPiece parent = resolved.get(parentId.trim());
            faces.putAll(parent.faces);
            runAxis = parent.runAxisLocal.clone();
            towerHalf = parent.towerConnectionHalf;
            bounds = parent.bounds;
        }
        for (var e : def.resolvedFaces().entrySet()) {
            faces.put(e.getKey(), e.getValue());
        }
        if (def.getBounds() != null) {
            bounds = def.getBounds();
        }
        if (def.towerConnectionHalf() > 0 && (parentId == null || def.towerConnectionHalf() != 4)) {
            towerHalf = def.towerConnectionHalf();
        }
        if (def.hasRunAxis()) {
            runAxis = def.runAxisLocal();
        }
        resolved.put(
            id,
            new ResolvedPiece(id, Map.copyOf(faces), runAxis.clone(), towerHalf, bounds)
        );
        pending.remove(id);
    }

    private static final class Root {
        @SerializedName("chainSpan")
        int chainSpan;

        @SerializedName("pieces")
        Map<String, WallKitPieceDefinition> pieces;
    }

    public static final class ResolvedPiece {
        private final String id;
        private final Map<WallCardinal, WallKitPieceDefinition.FaceOffsets> faces;
        private final Vector3i runAxisLocal;
        private final int towerConnectionHalf;
        @Nullable
        private final WallKitPieceDefinition.Bounds bounds;

        ResolvedPiece(
            @Nonnull String id,
            @Nonnull Map<WallCardinal, WallKitPieceDefinition.FaceOffsets> faces,
            @Nonnull Vector3i runAxisLocal,
            int towerConnectionHalf,
            @Nullable WallKitPieceDefinition.Bounds bounds
        ) {
            this.id = id;
            this.faces = faces;
            this.runAxisLocal = runAxisLocal;
            this.towerConnectionHalf = towerConnectionHalf;
            this.bounds = bounds;
        }

        public int towerConnectionHalf() {
            return towerConnectionHalf;
        }

        @Nonnull
        public Vector3i worldOffset(@Nonnull Rotation yaw, @Nonnull WallCardinal worldDir, @Nonnull OffsetKind kind) {
            for (WallCardinal localFace : WallCardinal.values()) {
                Vector3i local = localOffset(localFace, kind);
                if (local == null) {
                    continue;
                }
                Vector3i rotated = local.clone();
                PrefabRotation.fromRotation(yaw).rotate(rotated);
                WallCardinal points = WallCardinal.fromVector(new Vector3i(0, 0, 0), rotated);
                if (points == worldDir) {
                    return rotated;
                }
            }
            Vector3i fallback = localOffset(worldDir, kind);
            if (fallback == null) {
                throw new IllegalStateException("no " + kind + " offset on " + worldDir + " for wall kit " + id);
            }
            Vector3i rotated = fallback.clone();
            PrefabRotation.fromRotation(yaw).rotate(rotated);
            return rotated;
        }

        public boolean runAlongWorldZ(@Nonnull Rotation yaw) {
            Vector3i run = runAxisLocal.clone();
            PrefabRotation.fromRotation(yaw).rotate(run);
            return Math.abs(run.z) >= Math.abs(run.x);
        }

        @Nullable
        private Vector3i localOffset(@Nonnull WallCardinal localFace, @Nonnull OffsetKind kind) {
            WallKitPieceDefinition.FaceOffsets fo = faces.get(localFace);
            if (fo == null) {
                return null;
            }
            return switch (kind) {
                case CHAIN -> fo.chainLocal();
                case TOWER_JOINT -> fo.towerLocal();
                case EXTERIOR -> fo.exteriorLocal();
                case TOWER_CONNECTION -> {
                    Vector3i t = fo.towerLocal();
                    if (t != null) {
                        yield t;
                    }
                    yield switch (localFace) {
                        case NORTH -> new Vector3i(0, 0, -towerConnectionHalf);
                        case SOUTH -> new Vector3i(0, 0, towerConnectionHalf);
                        case EAST -> new Vector3i(towerConnectionHalf, 0, 0);
                        case WEST -> new Vector3i(-towerConnectionHalf, 0, 0);
                    };
                }
            };
        }
    }
}
