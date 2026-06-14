package com.hexvane.aetherhaven.command;

import com.hexvane.aetherhaven.rts.RtsCommandPlayerComponent;
import com.hexvane.aetherhaven.rts.debug.RtsBoxSelectDebug;
import com.hexvane.aetherhaven.rts.debug.RtsBoxSelectDebugOverlay;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Toggle world-space debug wireframe for RTS box selection. */
public final class AetherhavenRtsBoxDebugCommand extends AbstractCommandCollection {
    public AetherhavenRtsBoxDebugCommand() {
        super("rtsboxdebug", "Toggle RTS box-select world debug overlay");
        this.setPermissionGroups("hytale:Adventurer");
        this.addSubCommand(new OnCommand());
        this.addSubCommand(new OffCommand());
    }

    private static final class OnCommand extends AbstractPlayerCommand {
        OnCommand() {
            super("on", "Enable RTS box-select world debug overlay");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull PlayerRef pr,
            @Nonnull World world
        ) {
            setDebug(store, playerRef, pr, true);
            context.sendMessage(Message.raw("RTS box-select debug overlay enabled."));
        }
    }

    private static final class OffCommand extends AbstractPlayerCommand {
        OffCommand() {
            super("off", "Disable RTS box-select world debug overlay");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull PlayerRef pr,
            @Nonnull World world
        ) {
            setDebug(store, playerRef, pr, false);
            context.sendMessage(Message.raw("RTS box-select debug overlay disabled."));
        }
    }

    private static void setDebug(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef pr,
        boolean enabled
    ) {
        RtsBoxSelectDebug.setEnabled(pr.getUuid(), enabled);
        RtsCommandPlayerComponent session = store.getComponent(playerRef, RtsCommandPlayerComponent.getComponentType());
        if (session != null) {
            session.setBoxSelectDebug(enabled);
            store.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
        }
        if (!enabled) {
            RtsBoxSelectDebugOverlay.clear(pr);
        }
    }
}
