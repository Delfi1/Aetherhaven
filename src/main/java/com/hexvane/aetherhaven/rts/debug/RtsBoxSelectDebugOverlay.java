package com.hexvane.aetherhaven.rts.debug;

import com.hexvane.aetherhaven.debug.DebugLineCylinderUtil;
import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hexvane.aetherhaven.rts.RtsScreenPickUtil;
import com.hypixel.hytale.math.matrix.Matrix4dUtil;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3f;

/**
 * World-space selection column wireframe — same {@link DisplayDebug} cylinders as
 * {@link com.hexvane.aetherhaven.placement.PlotPlacementWireframeOverlay}.
 */
public final class RtsBoxSelectDebugOverlay {
    /** Match plot placement staff outline duration (refreshed each drag tick). */
    private static final float DISPLAY_SECONDS = 6f * 60f * 60f;
    private static final double LINE_THICKNESS = 0.04;
    private static final double MIN_EDGE_LENGTH = 0.25;
    /** Visual only — guard selection uses a full-height X/Z column with no Y bounds. */
    private static final double COLUMN_HEIGHT = 16.0;
    private static final int LINE_FLAGS = DebugUtils.FLAG_NO_WIREFRAME;

    private RtsBoxSelectDebugOverlay() {}

    public static boolean isEnabled(@Nonnull UUID playerId) {
        return RtsBoxSelectDebug.isEnabled(playerId);
    }

    public static void clear(@Nullable PlayerRef player) {
        if (player == null) {
            return;
        }
        player.getPacketHandler().write(new ClearDebugShapes());
    }

    /** Refreshes the wireframe from the current HUD drag rect; no-op when disabled or drag too small. */
    public static void refresh(@Nullable PlayerRef player, @Nonnull RtsCommandPlayerComponent session) {
        if (player == null || !isEnabled(player.getUuid())) {
            return;
        }
        if (!hasVisibleDrag(session)) {
            return;
        }
        RtsScreenPickUtil.WorldAabb column = RtsScreenPickUtil.pickHudRectWorldColumn(session);
        if (column == null) {
            return;
        }
        showColumn(player, column);
    }

    public static void showSelectionColumn(
        @Nonnull PlayerRef player,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        RtsScreenPickUtil.WorldAabb column = RtsScreenPickUtil.pickHudRectWorldColumn(session);
        if (column == null) {
            clear(player);
            return;
        }
        showColumn(player, column);
    }

    public static void showColumn(@Nonnull PlayerRef player, @Nonnull RtsScreenPickUtil.WorldAabb column) {
        clear(player);
        double minX = column.minX();
        double maxX = column.maxX();
        double minZ = column.minZ();
        double maxZ = column.maxZ();
        double[] expanded = expandDegenerate(minX, maxX, minZ, maxZ);
        minX = expanded[0];
        maxX = expanded[1];
        minZ = expanded[2];
        maxZ = expanded[3];

        double groundY = column.groundY();
        double topY = groundY + COLUMN_HEIGHT;
        Vector3f color = DebugUtils.COLOR_WHITE;

        addBoxEdges(player, minX, maxX, minZ, maxZ, groundY, topY, color);
    }

    static boolean hasVisibleDrag(@Nonnull RtsCommandPlayerComponent session) {
        float dx = session.getBoxScreenEndX() - session.getBoxScreenStartX();
        float dy = session.getBoxScreenEndY() - session.getBoxScreenStartY();
        return dx * dx + dy * dy >= 0.012f * 0.012f;
    }

    @Nonnull
    private static double[] expandDegenerate(double minX, double maxX, double minZ, double maxZ) {
        if (maxX - minX < MIN_EDGE_LENGTH) {
            double cx = (minX + maxX) * 0.5;
            minX = cx - MIN_EDGE_LENGTH * 0.5;
            maxX = cx + MIN_EDGE_LENGTH * 0.5;
        }
        if (maxZ - minZ < MIN_EDGE_LENGTH) {
            double cz = (minZ + maxZ) * 0.5;
            minZ = cz - MIN_EDGE_LENGTH * 0.5;
            maxZ = cz + MIN_EDGE_LENGTH * 0.5;
        }
        return new double[] { minX, maxX, minZ, maxZ };
    }

    private static void addBoxEdges(
        @Nonnull PlayerRef player,
        double minX,
        double maxX,
        double minZ,
        double maxZ,
        double bottomY,
        double topY,
        @Nonnull Vector3f color
    ) {
        sendLineCylinder(player, minX, bottomY, minZ, maxX, bottomY, minZ, color);
        sendLineCylinder(player, maxX, bottomY, minZ, maxX, bottomY, maxZ, color);
        sendLineCylinder(player, maxX, bottomY, maxZ, minX, bottomY, maxZ, color);
        sendLineCylinder(player, minX, bottomY, maxZ, minX, bottomY, minZ, color);
        sendLineCylinder(player, minX, topY, minZ, maxX, topY, minZ, color);
        sendLineCylinder(player, maxX, topY, minZ, maxX, topY, maxZ, color);
        sendLineCylinder(player, maxX, topY, maxZ, minX, topY, maxZ, color);
        sendLineCylinder(player, minX, topY, maxZ, minX, topY, minZ, color);
        sendLineCylinder(player, minX, bottomY, minZ, minX, topY, minZ, color);
        sendLineCylinder(player, maxX, bottomY, minZ, maxX, topY, minZ, color);
        sendLineCylinder(player, maxX, bottomY, maxZ, maxX, topY, maxZ, color);
        sendLineCylinder(player, minX, bottomY, maxZ, minX, topY, maxZ, color);
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
        if (length < 0.001) {
            return;
        }
        Matrix4d matrix = DebugLineCylinderUtil.segmentMatrix(startX, startY, startZ, endX, endY, endZ, LINE_THICKNESS, length);
        if (matrix == null) {
            return;
        }
        DisplayDebug packet = new DisplayDebug(
            DebugShape.Cylinder,
            Matrix4dUtil.asFloatData(matrix),
            color,
            DISPLAY_SECONDS,
            (byte) LINE_FLAGS,
            null,
            DebugUtils.DEFAULT_OPACITY
        );
        player.getPacketHandler().write(packet);
    }
}
