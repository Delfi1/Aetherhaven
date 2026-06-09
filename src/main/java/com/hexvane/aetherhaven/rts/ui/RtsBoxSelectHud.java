package com.hexvane.aetherhaven.rts.ui;

import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hexvane.aetherhaven.rts.RtsDiagnostics;
import com.hexvane.aetherhaven.rts.RtsScreenPickUtil;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** Screen-space selection rectangle while box-dragging guards. */
public final class RtsBoxSelectHud extends CustomUIHud {

    private static final float MIN_VISIBLE_DRAG = 0.012f;
    private boolean shown;
    private Boolean lastVisible;

    public RtsBoxSelectHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, com.hexvane.aetherhaven.AetherhavenConstants.RTS_BOX_SELECT_HUD_KEY, 1);
    }

    public void ensureShown() {
        if (!shown) {
            this.show();
            shown = true;
            RtsDiagnostics.boxHudLifecycle(getPlayerRef(), "show-hud");
        }
    }

    public void hideSelection() {
        if (!shown) {
            return;
        }
        UICommandBuilder b = new UICommandBuilder();
        b.set("#SelectionBox.Visible", false);
        this.update(false, b);
        lastVisible = false;
        logVisible(false, 0, 0, 0, 0, "hide");
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/RtsBoxSelectOverlay.ui");
        RtsDiagnostics.boxHudLifecycle(getPlayerRef(), "build-ui");
    }

    public void refresh(@Nonnull RtsCommandPlayerComponent session) {
        UICommandBuilder b = new UICommandBuilder();
        if (!session.isBoxSelectActive() || !session.isBoxScreenAnchorReady()) {
            b.set("#SelectionBox.Visible", false);
            this.update(false, b);
            logVisible(false, 0, 0, 0, 0, session.isBoxSelectActive() ? "await-screen" : "inactive");
            return;
        }

        float x0 = session.getBoxScreenStartX();
        float y0 = session.getBoxScreenStartY();
        float x1 = session.getBoxScreenEndX();
        float y1 = session.getBoxScreenEndY();
        float dx = x1 - x0;
        float dy = y1 - y0;
        if (dx * dx + dy * dy < MIN_VISIBLE_DRAG * MIN_VISIBLE_DRAG) {
            b.set("#SelectionBox.Visible", false);
            this.update(false, b);
            logVisible(false, 0, 0, 0, 0, "too-small");
            return;
        }

        int inset = RtsScreenPickUtil.HUD_BOX_BORDER_INSET;
        int left = Math.max(0, RtsScreenPickUtil.toHudPixelX(Math.min(x0, x1)) - inset);
        int top = Math.max(0, RtsScreenPickUtil.toHudPixelY(Math.min(y0, y1)) - inset);
        int width = Math.max(2, RtsScreenPickUtil.toHudPixelX(Math.max(x0, x1)) - left + inset);
        int height = Math.max(2, RtsScreenPickUtil.toHudPixelY(Math.max(y0, y1)) - top + inset);

        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        b.setObject("#SelectionBox.Anchor", anchor);
        b.set("#SelectionBox.Visible", true);
        this.update(false, b);
        logVisible(true, left, top, width, height, "draw");
    }

    private void logVisible(
        boolean visible,
        int left,
        int top,
        int width,
        int height,
        @Nonnull String reason
    ) {
        if (lastVisible != null && lastVisible == visible && !visible) {
            return;
        }
        if (lastVisible != null && lastVisible == visible && visible) {
            RtsDiagnostics.boxHudRefresh(getPlayerRef(), visible, left, top, width, height, reason, true);
            return;
        }
        lastVisible = visible;
        RtsDiagnostics.boxHudRefresh(getPlayerRef(), visible, left, top, width, height, reason, false);
    }
}
