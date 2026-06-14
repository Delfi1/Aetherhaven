package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.questboard.QuestBoardSlotRecord;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class RaidHealthBarHud extends CustomUIHud {

    public RaidHealthBarHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, AetherhavenConstants.RAID_HEALTH_BAR_HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Aetherhaven/RaidHealthBarHud.ui");
    }

    public void refresh(@Nonnull Message title, @Nonnull QuestBoardSlotRecord slot) {
        UICommandBuilder b = new UICommandBuilder();
        b.set("#RaidTitle.TextSpans", title);
        RaidHealthBarUi.apply(b, slot.getRaidKillProgress(), slot.getRaidKillRequired());
        this.update(false, b);
    }
}
