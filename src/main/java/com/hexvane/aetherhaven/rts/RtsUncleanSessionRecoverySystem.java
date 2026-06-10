package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import java.util.Set;
import javax.annotation.Nonnull;

/** Restores saved hotbar when a player's last RTS session did not exit cleanly. */
public final class RtsUncleanSessionRecoverySystem extends EntityTickingSystem<EntityStore> {
    private static final String AUTO_RECOVER_KEY = "aetherhaven_rts.aetherhaven.rts.recoverInventoryAuto";

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            Player.getComponentType(),
            PlayerRef.getComponentType(),
            RtsCommandPlayerComponent.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
        if (session == null || session.isActive() || !session.needsUncleanSessionRecovery()) {
            return;
        }
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        if (!RtsCommandService.recoverUncleanSession(playerRef, commandBuffer, pr)) {
            return;
        }
        NotificationUtil.sendNotification(
            pr.getPacketHandler(),
            Message.translation(AUTO_RECOVER_KEY),
            NotificationStyle.Warning
        );
    }
}
