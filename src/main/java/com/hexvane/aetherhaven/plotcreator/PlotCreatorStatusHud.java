package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/** In-world hints while the plot creator wizard is dismissed but the session is still active. */
public final class PlotCreatorStatusHud extends CustomUIHud {

    public PlotCreatorStatusHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, AetherhavenConstants.PLOT_CREATOR_HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/PlotCreatorStatusHud.ui");
    }

    public void refresh(@Nonnull PlotCreatorSession session) {
        UICommandBuilder b = new UICommandBuilder();
        String msg = "aetherhaven_plot_creator.aetherhaven.plotcreator";
        PlotCreatorStep step = session.getDraft().getStep();
        b.set("#PlotCreatorHudTitleText.TextSpans", Message.translation(msg + ".step." + step.name() + ".title"));
        b.set("#HintLine.TextSpans", Message.translation(msg + ".step." + step.name() + ".hint"));
        b.set("#HotkeyLine.TextSpans", Message.translation(msg + ".hud.hotkeys"));
        if (step == PlotCreatorStep.SUBSTEP) {
            PlotBuildingKindRequirements.SubstepRequirement sub = PlotCreatorService.currentSubstep(session.getDraft());
            if (sub != null) {
                b.set("#DetailLine.TextSpans", Message.translation(msg + ".substep." + sub.type().name()));
                b.set("#DetailLine.Visible", true);
            } else {
                b.set("#DetailLine.Visible", false);
            }
        } else if (step == PlotCreatorStep.MATERIALS) {
            b.set("#DetailLine.TextSpans", Message.translation(msg + ".step.MATERIALS.detail"));
            b.set("#DetailLine.Visible", true);
        } else if (step == PlotCreatorStep.KIND && session.getDraft().getKind() != null) {
            b.set(
                "#DetailLine.TextSpans",
                Message.translation(msg + ".hud.kindSelected").param("kind", session.getDraft().getKind().name())
            );
            b.set("#DetailLine.Visible", true);
        } else {
            b.set("#DetailLine.Visible", false);
        }
        this.update(false, b);
    }
}
