package com.hexvane.aetherhaven.command;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class AetherhavenCommand extends AbstractCommandCollection {
    public AetherhavenCommand() {
        super("aetherhaven", "aetherhaven_commands_root.commands.aetherhaven.root.desc");
        this.setPermissionGroups("hytale:Adventurer");
        this.addAliases("ah");
        this.addSubCommand(new AetherhavenStarterKitCommand());
        this.addSubCommand(new AetherhavenTownsCommand());
        this.addSubCommand(new AetherhavenReplaceCharterCommand());
        this.addSubCommand(new AetherhavenTownCommand());
        this.addSubCommand(new AetherhavenReloadCommand());
        this.addSubCommand(new ExportAvatarSkinCommand());
        this.addSubCommand(new DialogueCommand());
        this.addSubCommand(new AetherhavenPoiCommand());
        this.addSubCommand(new AetherhavenPlotsCommand());
        this.addSubCommand(new AetherhavenNeedsCommand());
        this.addSubCommand(new AetherhavenTaxCommand());
        this.addSubCommand(new AetherhavenQuestDebugCommand());
        this.addSubCommand(new AetherhavenAutonomyDebugCommand());
        this.addSubCommand(new AetherhavenReputationDebugCommand());
        this.addSubCommand(new AetherhavenVillagerCommand());
        this.addSubCommand(new AetherhavenGiftCommand());
        this.addSubCommand(new AetherhavenLootChestDebugCommand());
        this.addSubCommand(new AetherhavenPathCommand());
        this.addSubCommand(new AetherhavenFloatingGiftCommand());
        this.addSubCommand(new AetherhavenTimeCommand());
        this.addSubCommand(new AetherhavenDifficultyCommand());
        this.addSubCommand(new AetherhavenWallDebugCommand());
        this.addSubCommand(new AetherhavenRtsBoxDebugCommand());
        this.addSubCommand(new AetherhavenRtsRecoverInventoryCommand());
        this.addSubCommand(new AetherhavenTownsfolkCommand());
        this.addSubCommand(new AetherhavenGuildCommand());
        this.addSubCommand(new AetherhavenPlotTokenCommand());
        this.addSubCommand(new AetherhavenPlotCreatorCommand());
        this.addSubCommand(new AetherhavenTouristDebugCommand());
    }
}
