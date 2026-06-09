package com.hexvane.aetherhaven.command;

import com.hexvane.aetherhaven.rts.RtsCommandService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Restores the hotbar saved from the player's last RTS command session. */
public final class AetherhavenRtsRecoverInventoryCommand extends AbstractPlayerCommand {
    private static final String P = "aetherhaven_rts.aetherhaven.rts";

    public AetherhavenRtsRecoverInventoryCommand() {
        super("rtsrecoverinventory", "Restore hotbar saved from your last guard command session");
        this.setPermissionGroups("hytale:Adventurer");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull PlayerRef pr,
        @Nonnull World world
    ) {
        RtsCommandService.RecoverSavedInventoryResult result =
            RtsCommandService.recoverSavedInventory(playerRef, store);
        switch (result) {
            case RESTORED -> context.sendMessage(Message.translation(P + ".recoverInventorySuccess"));
            case RESTORED_AND_EXITED -> context.sendMessage(Message.translation(P + ".recoverInventorySuccessExited"));
            case NONE_SAVED -> context.sendMessage(Message.translation(P + ".recoverInventoryNone"));
        }
    }
}
