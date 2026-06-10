package com.hexvane.aetherhaven.rts.ui;

import javax.annotation.Nonnull;

/** Pixel layout for {@code RtsGuardRosterPanel.ui} on the 1920×1080 HUD reference resolution. */
public final class RtsGuardRosterLayout {
    public static final float REF_WIDTH = 1920f;
    public static final float REF_HEIGHT = 1080f;
    public static final int PANEL_MAX_WIDTH = 720;
    public static final int PANEL_BOTTOM = 145;
    public static final int PANEL_PADDING_H = 16;
    public static final int PANEL_PADDING_TOP = 12;
    public static final int PANEL_PADDING_BOTTOM = 12;
    public static final int INNER_WIDTH = PANEL_MAX_WIDTH - (PANEL_PADDING_H * 2);
    public static final int TAB_HEIGHT = 30;
    public static final int TAB_OVERLAP = 8;
    public static final int ROW_HEIGHT = 72;
    public static final int CELL_WIDTH = 68;
    public static final int CELL_MARGIN_RIGHT = 8;
    public static final int CELL_PITCH = CELL_WIDTH + CELL_MARGIN_RIGHT;
    public static final int PORTRAIT_SIZE = 56;
    public static final int PORTRAIT_ROW_TOP_INSET = 4;

    public record Layout(
        int guardCount,
        int shellWidth,
        int shellHeight,
        int shellLeft,
        int shellTop,
        int panelTop,
        int gridAreaLeft,
        int gridAreaTop,
        int gridHeight,
        int rowCount,
        int columnsPerRow
    ) {}

    private RtsGuardRosterLayout() {}

    public static int columnsPerRow() {
        return Math.max(1, INNER_WIDTH / CELL_PITCH);
    }

    public static int rowCount(int guardCount) {
        if (guardCount <= 0) {
            return 0;
        }
        return (guardCount + columnsPerRow() - 1) / columnsPerRow();
    }

    public static int itemsInRow(int guardCount, int rowIndex) {
        int cols = columnsPerRow();
        int start = rowIndex * cols;
        return Math.min(cols, guardCount - start);
    }

    @Nonnull
    public static Layout layoutFor(int guardCount) {
        int safeCount = Math.max(0, guardCount);
        int cols = columnsPerRow();
        int rows = rowCount(safeCount);
        int gridHeight = rows * ROW_HEIGHT;
        int panelBodyHeight = PANEL_PADDING_TOP + gridHeight + PANEL_PADDING_BOTTOM;
        int shellHeight = TAB_HEIGHT + panelBodyHeight - TAB_OVERLAP;
        int shellWidth = PANEL_MAX_WIDTH;
        int shellLeft = (Math.round(REF_WIDTH) - shellWidth) / 2;
        int shellBottom = Math.round(REF_HEIGHT) - PANEL_BOTTOM;
        int shellTop = shellBottom - shellHeight;
        int panelTop = shellTop + TAB_HEIGHT - TAB_OVERLAP;
        int gridAreaLeft = shellLeft + PANEL_PADDING_H;
        int gridAreaTop = panelTop + PANEL_PADDING_TOP;
        return new Layout(
            safeCount,
            shellWidth,
            shellHeight,
            shellLeft,
            shellTop,
            panelTop,
            gridAreaLeft,
            gridAreaTop,
            gridHeight,
            rows,
            cols
        );
    }

    public static int rowLeftPx(@Nonnull Layout layout, int rowIndex) {
        int count = itemsInRow(layout.guardCount(), rowIndex);
        int rowWidth = count * CELL_PITCH;
        return layout.gridAreaLeft() + (INNER_WIDTH - rowWidth) / 2;
    }

    public static int portraitInsetPx() {
        return (CELL_WIDTH - PORTRAIT_SIZE) / 2;
    }
}
