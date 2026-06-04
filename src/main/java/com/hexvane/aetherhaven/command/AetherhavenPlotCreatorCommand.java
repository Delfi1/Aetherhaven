package com.hexvane.aetherhaven.command;

import com.hexvane.aetherhaven.plotcreator.PlotCreatorService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class AetherhavenPlotCreatorCommand extends AbstractCommandCollection {
    public AetherhavenPlotCreatorCommand() {
        super("plotcreator", "aetherhaven_commands_help.commands.aetherhaven.plotcreator.desc");
        this.addSubCommand(new StartCommand());
        this.addSubCommand(new CancelCommand());
        this.addSubCommand(new EditCommand());
    }

    private static final class StartCommand extends AbstractPlayerCommand {
        StartCommand() {
            super("start", "aetherhaven_commands_help.commands.aetherhaven.plotcreator.start.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            PlotCreatorService.startSession(playerRef, ref, store);
        }
    }

    private static final class EditCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> constructionIdArg =
            this.withRequiredArg(
                "constructionId",
                "aetherhaven_commands_help.commands.aetherhaven.plotcreator.edit.constructionId",
                ArgTypes.STRING
            );

        EditCommand() {
            super("edit", "aetherhaven_commands_help.commands.aetherhaven.plotcreator.edit.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            PlotCreatorService.startEditSession(playerRef, ref, store, constructionIdArg.get(context));
        }
    }

    private static final class CancelCommand extends AbstractPlayerCommand {
        CancelCommand() {
            super("cancel", "aetherhaven_commands_help.commands.aetherhaven.plotcreator.cancel.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            PlotCreatorService.cancelSession(playerRef, ref, store);
        }
    }
}
