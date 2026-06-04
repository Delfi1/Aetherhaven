package com.hexvane.aetherhaven.placement;

import com.hexvane.aetherhaven.debug.DebugLineCylinderUtil;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.math.matrix.Matrix4dUtil;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3f;

/**
 * Axis-aligned box edges as thin debug cylinders. Uses {@link DebugUtils#FLAG_NO_WIREFRAME} so only the tinted solid
 * draws — default debug also draws black wireframe edges that dominate the fill at a distance.
 */
public final class PlotPlacementWireframeOverlay {
    private static final float OUTLINE_DISPLAY_SECONDS = 6f * 60f * 60f;
    private static final int MAX_SIBLING_PLOT_OUTLINES = 24;
    private static final double LINE_THICKNESS = 0.04;
    private static final int LINE_FLAGS = DebugUtils.FLAG_NO_WIREFRAME;

    private PlotPlacementWireframeOverlay() {}

    public static void clearFor(@Nullable PlayerRef player) {
        if (player == null) {
            return;
        }
        player.getPacketHandler().write(new ClearDebugShapes());
    }

    public static void send(
        @Nonnull PlayerRef player,
        @Nonnull PlotFootprintRecord placementFootprint,
        boolean placementValid,
        @Nullable TownRecord town
    ) {
        clearFor(player);
        sendWithoutClear(player, placementFootprint, placementValid, town);
    }

    /** Adds footprint wireframes without {@link ClearDebugShapes} so other tools' debug overlays stay visible. */
    public static void sendWithoutClear(
        @Nonnull PlayerRef player,
        @Nonnull PlotFootprintRecord placementFootprint,
        boolean placementValid,
        @Nullable TownRecord town
    ) {
        Vector3f placeColor = placementValid ? DebugUtils.COLOR_WHITE : DebugUtils.COLOR_RED;
        addBoxEdges(player, placementFootprint, placeColor);
        if (town == null) {
            return;
        }
        List<PlotInstance> plots = new ArrayList<>(town.getPlotInstances());
        if (plots.size() > MAX_SIBLING_PLOT_OUTLINES) {
            plots.sort(Comparator.comparingDouble(p -> aabbCenterDistSq(p.toFootprint(), placementFootprint)));
            plots = plots.subList(0, MAX_SIBLING_PLOT_OUTLINES);
        }
        for (PlotInstance p : plots) {
            addBoxEdges(player, p.toFootprint(), DebugUtils.COLOR_SILVER);
        }
    }

    private static double aabbCenterDistSq(@Nonnull PlotFootprintRecord a, @Nonnull PlotFootprintRecord b) {
        double ax = (a.getMinX() + a.getMaxX() + 1) * 0.5;
        double ay = (a.getMinY() + a.getMaxY() + 1) * 0.5;
        double az = (a.getMinZ() + a.getMaxZ() + 1) * 0.5;
        double bx = (b.getMinX() + b.getMaxX() + 1) * 0.5;
        double by = (b.getMinY() + b.getMaxY() + 1) * 0.5;
        double bz = (b.getMinZ() + b.getMaxZ() + 1) * 0.5;
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void addBoxEdges(@Nonnull PlayerRef player, @Nonnull PlotFootprintRecord fp, @Nonnull Vector3f color) {
        double minX = fp.getMinX();
        double minY = fp.getMinY();
        double minZ = fp.getMinZ();
        double maxX = fp.getMaxX() + 1.0;
        double maxY = fp.getMaxY() + 1.0;
        double maxZ = fp.getMaxZ() + 1.0;

        sendLineCylinder(player, minX, minY, minZ, maxX, minY, minZ, color);
        sendLineCylinder(player, maxX, minY, minZ, maxX, minY, maxZ, color);
        sendLineCylinder(player, maxX, minY, maxZ, minX, minY, maxZ, color);
        sendLineCylinder(player, minX, minY, maxZ, minX, minY, minZ, color);
        sendLineCylinder(player, minX, maxY, minZ, maxX, maxY, minZ, color);
        sendLineCylinder(player, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        sendLineCylinder(player, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        sendLineCylinder(player, minX, maxY, maxZ, minX, maxY, minZ, color);
        sendLineCylinder(player, minX, minY, minZ, minX, maxY, minZ, color);
        sendLineCylinder(player, maxX, minY, minZ, maxX, maxY, minZ, color);
        sendLineCylinder(player, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        sendLineCylinder(player, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private static void sendLineCylinder(
        @Nonnull PlayerRef player,
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        @Nonnull Vector3f color
    ) {
        double dirX = endX - startX;
        double dirY = endY - startY;
        double dirZ = endZ - startZ;
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        Matrix4d matrix = DebugLineCylinderUtil.segmentMatrix(startX, startY, startZ, endX, endY, endZ, LINE_THICKNESS, length);
        if (matrix == null) {
            return;
        }
        DisplayDebug packet =
            new DisplayDebug(
                DebugShape.Cylinder,
                Matrix4dUtil.asFloatData(matrix),
                color,
                OUTLINE_DISPLAY_SECONDS,
                (byte) LINE_FLAGS,
                null,
                DebugUtils.DEFAULT_OPACITY
            );
        player.getPacketHandler().write(packet);
    }
}
