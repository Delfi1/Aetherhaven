package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

final class RtsMousePacketUtil {
    private RtsMousePacketUtil() {}

    @Nullable
    static Vector3i targetBlock(@Nonnull MouseInteraction packet) {
        if (packet.worldInteraction == null || packet.worldInteraction.blockPosition == null) {
            return null;
        }
        BlockPosition bp = packet.worldInteraction.blockPosition;
        return new Vector3i(bp.x, bp.y, bp.z);
    }

    @Nullable
    static Ref<EntityStore> targetEntityRef(@Nonnull MouseInteraction packet, @Nonnull Store<EntityStore> store) {
        if (packet.worldInteraction == null || packet.worldInteraction.entityId < 0) {
            return null;
        }
        return store.getExternalData().getRefFromNetworkId(packet.worldInteraction.entityId);
    }
}
