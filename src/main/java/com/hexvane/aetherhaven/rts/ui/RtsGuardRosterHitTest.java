package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.rts.RtsScreenPickUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2fc;

/** Maps screen clicks to guard portrait indices in the bottom roster strip. */
public final class RtsGuardRosterHitTest {
    private RtsGuardRosterHitTest() {}

    public static boolean hitsPanel(@Nullable Vector2fc screen, int guardCount) {
        if (screen == null || guardCount <= 0) {
            return false;
        }
        RtsGuardRosterLayout.Layout layout = RtsGuardRosterLayout.layoutFor(guardCount);
        int px = screenPixelX(screen);
        int py = screenPixelY(screen);
        int right = layout.shellLeft() + layout.shellWidth();
        int bottom = layout.shellTop() + layout.shellHeight();
        return px >= layout.shellLeft() && px <= right && py >= layout.shellTop() && py <= bottom;
    }

    /**
     * @return portrait index in roster order, or {@code -1} when the click missed portraits
     */
    public static int pickPortraitIndex(@Nullable Vector2fc screen, int guardCount) {
        if (screen == null || guardCount <= 0) {
            return -1;
        }
        RtsGuardRosterLayout.Layout layout = RtsGuardRosterLayout.layoutFor(guardCount);
        int px = screenPixelX(screen);
        int py = screenPixelY(screen);
        if (py < layout.gridAreaTop() || py >= layout.gridAreaTop() + layout.gridHeight()) {
            return -1;
        }

        int relY = py - layout.gridAreaTop();
        int row = relY / RtsGuardRosterLayout.ROW_HEIGHT;
        if (row < 0 || row >= layout.rowCount()) {
            return -1;
        }

        int rowTop = layout.gridAreaTop() + (row * RtsGuardRosterLayout.ROW_HEIGHT) + RtsGuardRosterLayout.PORTRAIT_ROW_TOP_INSET;
        int portraitBottom = rowTop + RtsGuardRosterLayout.PORTRAIT_SIZE;
        if (py < rowTop || py > portraitBottom) {
            return -1;
        }

        int rowLeft = RtsGuardRosterLayout.rowLeftPx(layout, row);
        int relX = px - rowLeft;
        if (relX < 0) {
            return -1;
        }

        int col = relX / RtsGuardRosterLayout.CELL_PITCH;
        int itemsInRow = RtsGuardRosterLayout.itemsInRow(guardCount, row);
        if (col < 0 || col >= itemsInRow) {
            return -1;
        }

        int cellOffset = relX % RtsGuardRosterLayout.CELL_PITCH;
        int inset = RtsGuardRosterLayout.portraitInsetPx();
        if (cellOffset < inset || cellOffset >= inset + RtsGuardRosterLayout.PORTRAIT_SIZE) {
            return -1;
        }

        return (row * layout.columnsPerRow()) + col;
    }

    private static int screenPixelX(@Nonnull Vector2fc screen) {
        float nx = RtsScreenPickUtil.cameraRawToNormalizedX(screen.x());
        return RtsScreenPickUtil.toPixelX(nx);
    }

    private static int screenPixelY(@Nonnull Vector2fc screen) {
        float ny = RtsScreenPickUtil.cameraRawToNormalizedY(screen.y());
        return RtsScreenPickUtil.toPixelY(ny);
    }
}
