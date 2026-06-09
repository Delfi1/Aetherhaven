package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.protocol.packets.entities.SpawnModelParticles;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3f;

/** Spawns model-attached selection markers that follow an entity as it moves. */
public final class RtsModelMarkerUtil {
    private static final float HEAD_OFFSET_Y = 2.75f;

    private RtsModelMarkerUtil() {}

    public static boolean clearAttachedMarker(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        return sendModelParticles(entityRef, audience, accessor, new com.hypixel.hytale.protocol.ModelParticle[0]);
    }

    public static boolean spawnAttachedMarker(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull String particleSystemId,
        @Nonnull String targetNodeName,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull ComponentAccessor<EntityStore> accessor
    ) {
        ModelParticle modelParticle = new ModelParticle(
            particleSystemId,
            EntityPart.Self,
            targetNodeName,
            null,
            1.0f,
            new Vector3f(0.0f, HEAD_OFFSET_Y, 0.0f),
            null,
            false
        );
        return sendModelParticles(
            entityRef,
            audience,
            accessor,
            new com.hypixel.hytale.protocol.ModelParticle[] { modelParticle.toPacket() }
        );
    }

    private static boolean sendModelParticles(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull List<Ref<EntityStore>> audience,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull com.hypixel.hytale.protocol.ModelParticle[] particles
    ) {
        NetworkId networkId = accessor.getComponent(entityRef, NetworkId.getComponentType());
        if (networkId == null) {
            return false;
        }
        SpawnModelParticles packet = new SpawnModelParticles(networkId.getId(), particles);
        boolean sent = false;
        for (Ref<EntityStore> viewerRef : audience) {
            if (!viewerRef.isValid()) {
                continue;
            }
            PlayerRef pr = accessor.getComponent(viewerRef, PlayerRef.getComponentType());
            if (pr != null) {
                pr.getPacketHandler().write(packet);
                sent = true;
            }
        }
        return sent;
    }
}
