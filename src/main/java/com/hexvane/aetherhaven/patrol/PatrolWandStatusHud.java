package com.hexvane.aetherhaven.patrol;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PatrolWandStatusHud extends CustomUIHud {

    public PatrolWandStatusHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, AetherhavenConstants.PATROL_WAND_HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/PatrolWandStatusHud.ui");
    }

    public void refresh(
        @Nonnull PatrolWandPlayerComponent st,
        @Nullable PatrolRouteRecord selectedRoute,
        @Nullable TownRecord town
    ) {
        UICommandBuilder b = new UICommandBuilder();
        b.set(
            "#PatrolWandHudTitleText.TextSpans",
            Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudTitle")
        );
        b.set(
            "#ModeName.TextSpans",
            Message.translation(
                st.getMode() == PatrolWandMode.Build
                    ? "aetherhaven_items.aetherhaven.patrolWand.hudNameBuild"
                    : "aetherhaven_items.aetherhaven.patrolWand.hudNameAssign"
            )
        );
        b.set(
            "#ModeHelp.TextSpans",
            Message.translation(
                st.getMode() == PatrolWandMode.Build
                    ? "aetherhaven_items.aetherhaven.patrolWand.hudHelpBuild"
                    : "aetherhaven_items.aetherhaven.patrolWand.hudHelpAssign"
            )
        );
        String routeName;
        if (selectedRoute != null) {
            routeName = selectedRoute.safeDisplayName();
        } else if (st.getEditingRouteId() == null) {
            routeName = "New route";
        } else {
            routeName = "Route";
        }
        b.set(
            "#RouteLine.TextSpans",
            Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudRoute").param("name", routeName)
        );
        b.set(
            "#NodesLine.TextSpans",
            Message
                .translation("aetherhaven_items.aetherhaven.patrolWand.hudNodes")
                .param("n", String.valueOf(st.getDraftNodes().size()))
        );
        boolean closedLoop =
            st.getMode() == PatrolWandMode.Build
                ? st.isDraftClosedLoop()
                : selectedRoute != null && selectedRoute.isClosedLoop();
        b.set(
            "#LoopLine.TextSpans",
            Message.translation(
                closedLoop
                    ? "aetherhaven_items.aetherhaven.patrolWand.hudLoopClosed"
                    : "aetherhaven_items.aetherhaven.patrolWand.hudLoopOpen"
            )
        );
        Message guardLine =
            selectedRoute != null && selectedRoute.getAssignedGuardUuidParsed() != null
                ? Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudGuardAssigned")
                : Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudGuardNone");
        b.set(
            "#GuardLine.TextSpans",
            Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudGuard").param("guard", guardLine)
        );
        b.set("#HintLine.TextSpans", Message.translation("aetherhaven_items.aetherhaven.patrolWand.hudHint"));
        this.update(false, b);
    }
}
