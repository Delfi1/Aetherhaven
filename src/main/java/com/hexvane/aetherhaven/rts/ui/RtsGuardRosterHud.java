package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

/** Display-only bottom guard portrait strip for RTS command mode. Clicks are handled server-side. */
public final class RtsGuardRosterHud extends CustomUIHud {
    private static final String GRID = "#RosterShell #RosterPanel #PortraitGrid";
    private static final String TITLE = "#RosterShell #RosterTitleTab #RosterTitle";
    private static final String RTS_LANG = "aetherhaven_rts.aetherhaven.rts";

    private final UUID townId;
    private int boundCellCount;
    private int boundRowCount;

    public RtsGuardRosterHud(@Nonnull PlayerRef playerRef, @Nonnull UUID townId) {
        super(playerRef, AetherhavenConstants.RTS_GUARD_ROSTER_HUD_KEY, 0);
        this.townId = townId;
    }

    @Nonnull
    public UUID getTownId() {
        return townId;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/RtsGuardRosterPanel.ui");
    }

    public void rebuild(@Nonnull List<RtsGuardRosterSupport.GuardRow> rows) {
        RtsGuardRosterLayout.Layout layout = RtsGuardRosterLayout.layoutFor(rows.size());
        UICommandBuilder cmd = new UICommandBuilder();
        applyGridLayout(cmd, layout);
        cmd.set(TITLE + ".TextSpans", Message.translation(RTS_LANG + ".rosterTitle"));
        cmd.clear(GRID);
        appendWrappedRows(cmd, rows, layout);
        boundCellCount = rows.size();
        boundRowCount = layout.rowCount();
        applyRowVisuals(cmd, rows, layout);
        this.update(false, cmd);
    }

    public void refresh(@Nonnull List<RtsGuardRosterSupport.GuardRow> rows) {
        RtsGuardRosterLayout.Layout layout = RtsGuardRosterLayout.layoutFor(rows.size());
        if (rows.size() != boundCellCount || layout.rowCount() != boundRowCount) {
            rebuild(rows);
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        applyRowVisuals(cmd, rows, layout);
        this.update(false, cmd);
    }

    public void refresh(
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull List<RtsGuardRosterSupport.GuardRow> rows
    ) {
        refresh(rows);
    }

    private static void appendWrappedRows(
        @Nonnull UICommandBuilder cmd,
        @Nonnull List<RtsGuardRosterSupport.GuardRow> rows,
        @Nonnull RtsGuardRosterLayout.Layout layout
    ) {
        int cols = layout.columnsPerRow();
        for (int row = 0; row < layout.rowCount(); row++) {
            cmd.append(GRID, "Aetherhaven/RtsGuardPortraitRow.ui");
            String cells = GRID + "[" + row + "] #PortraitRowCells";
            int start = row * cols;
            int end = Math.min(start + cols, rows.size());
            for (int i = start; i < end; i++) {
                cmd.append(cells, "Aetherhaven/RtsGuardPortraitCell.ui");
            }
        }
    }

    private static void applyGridLayout(@Nonnull UICommandBuilder cmd, @Nonnull RtsGuardRosterLayout.Layout layout) {
        Anchor grid = new Anchor();
        grid.setHeight(Value.of(layout.gridHeight()));
        cmd.setObject(GRID + ".Anchor", grid);
    }

    private static void applyRowVisuals(
        @Nonnull UICommandBuilder cmd,
        @Nonnull List<RtsGuardRosterSupport.GuardRow> rows,
        @Nonnull RtsGuardRosterLayout.Layout layout
    ) {
        int cols = layout.columnsPerRow();
        for (int i = 0; i < rows.size(); i++) {
            RtsGuardRosterSupport.GuardRow row = rows.get(i);
            int uiRow = i / cols;
            int uiCol = i % cols;
            String cell = GRID + "[" + uiRow + "] #PortraitRowCells[" + uiCol + "]";
            cmd.set(cell + " #PortraitButton #PortraitFrame #Portrait.AssetPath", row.portraitPath());
            cmd.set(cell + " #PortraitButton #SelectedBorder.Visible", row.selected());
            RtsGuardPortraitBarUi.apply(cmd, cell, row.healthFraction());
        }
    }
}
