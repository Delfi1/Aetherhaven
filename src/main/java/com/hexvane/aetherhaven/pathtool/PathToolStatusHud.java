package com.hexvane.aetherhaven.pathtool;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.config.AetherhavenPluginConfig;
import com.hexvane.aetherhaven.ui.AetherhavenUiLocalization;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** In-world HUD overlay for path width and mode; shown while the path tool is held. */
public final class PathToolStatusHud extends CustomUIHud {

    public PathToolStatusHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, AetherhavenConstants.PATH_TOOL_HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/PathToolStatusHud.ui");
    }

    public void refresh(@Nonnull PathToolPlayerComponent st, @Nonnull AetherhavenPluginConfig cfg) {
        UICommandBuilder b = new UICommandBuilder();
        // Initial tree comes from {@link #show()} (append + clear). Use partial CustomHud updates here: sending
        // clear=true and re-appending every tick was hammering the client and has been linked to black/broken HUD
        // overlays on some GPUs (e.g. AMD) when holding the path tool idle.
        AetherhavenUiLocalization.applyPathToolStatusHudTitle(b, selector -> selector);
        b.set(
            "#ModeName.TextSpans",
            Message.translation(
                switch (st.getGizmoMode()) {
                    case Translate -> "aetherhaven_items.aetherhaven.pathTool.hudNameTranslate";
                    case Rotate -> "aetherhaven_items.aetherhaven.pathTool.hudNameRotate";
                    case Commit -> "aetherhaven_items.aetherhaven.pathTool.hudNameCommit";
                }
            )
        );
        b.set(
            "#ModeHelp.TextSpans",
            Message.translation(
                switch (st.getGizmoMode()) {
                    case Translate -> "aetherhaven_items.aetherhaven.pathTool.hudHelpTranslate";
                    case Rotate -> "aetherhaven_items.aetherhaven.pathTool.hudHelpRotate";
                    case Commit -> "aetherhaven_items.aetherhaven.pathTool.hudHelpCommit";
                }
            )
        );
        b.set(
            "#StyleLine.TextSpans",
            Message
                .translation("aetherhaven_items.aetherhaven.pathTool.hudStyle")
                .param("style", cfg.getPathToolStyleName(st.getPathStyleIndex()))
        );
        b.set(
            "#WidthLine.TextSpans",
            Message
                .translation("aetherhaven_items.aetherhaven.pathTool.hudWidth")
                .param("w", String.valueOf(st.getPathWidthBlocks()))
        );
        b.set(
            "#NodesLine.TextSpans",
            Message
                .translation("aetherhaven_items.aetherhaven.pathTool.hudNodes")
                .param("n", String.valueOf(st.getNodes().size()))
        );
        b.set("#HintLine.TextSpans", Message.translation("aetherhaven_items.aetherhaven.pathTool.hudHint"));
        this.update(false, b);
    }
}
