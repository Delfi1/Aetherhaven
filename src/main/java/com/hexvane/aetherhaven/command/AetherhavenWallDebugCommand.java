package com.hexvane.aetherhaven.command;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.placement.WallPlacementDebug;
import com.hexvane.aetherhaven.placement.WallPlacementSession;
import com.hexvane.aetherhaven.placement.WallPlacementSessions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.GameMode;
import javax.annotation.Nonnull;

/** Toggle wall wand placement debug lines in chat and server log. */
public final class AetherhavenWallDebugCommand extends AbstractCommandCollection {
    public AetherhavenWallDebugCommand() {
        super("walldebug", "aetherhaven_commands_help.commands.aetherhaven.walldebug.desc");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new OnCommand());
        this.addSubCommand(new OffCommand());
        this.addSubCommand(new DumpCommand());
    }

    private static final class OnCommand extends AbstractPlayerCommand {
        OnCommand() {
            super("on", "aetherhaven_commands_help.commands.aetherhaven.walldebug.on.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            setEnabled(context, store, ref, playerRef, true);
        }
    }

    private static final class OffCommand extends AbstractPlayerCommand {
        OffCommand() {
            super("off", "aetherhaven_commands_help.commands.aetherhaven.walldebug.off.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            setEnabled(context, store, ref, playerRef, false);
        }
    }

    private static void setEnabled(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        boolean on
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null || !AetherhavenDebugUtil.requireDebug(plugin, playerRef)) {
            return;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return;
        }
        WallPlacementDebug.toggleForPlayer(uc.getUuid(), on);
        playerRef.sendMessage(
            Message.translation(on ? "aetherhaven_world_debug.aetherhaven.debug.wall.on" : "aetherhaven_world_debug.aetherhaven.debug.wall.off")
        );
    }

    private static final class DumpCommand extends AbstractPlayerCommand {
        DumpCommand() {
            super("dump", "aetherhaven_commands_help.commands.aetherhaven.walldebug.dump.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null || !AetherhavenDebugUtil.requireDebug(plugin, playerRef)) {
                return;
            }
            UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uc == null) {
                return;
            }
            WallPlacementSession session = WallPlacementSessions.get(uc.getUuid());
            if (session == null) {
                playerRef.sendMessage(Message.translation("aetherhaven_world_debug.aetherhaven.debug.wall.noSession"));
                return;
            }
            boolean wasOn = session.isDebugLogging();
            session.setDebugLogging(true);
            WallPlacementDebug.logState(playerRef, session, "dump");
            session.setDebugLogging(wasOn);
        }
    }
}
