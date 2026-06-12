package com.hexvane.aetherhaven.rescue;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Particle and sound when a rescue NPC vanishes after dialogue. */
public final class RescueVillagerDespawnEffects {
    private RescueVillagerDespawnEffects() {}

    public static void playAtNpc(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull String particleSystemId,
        @Nonnull String soundEventId
    ) {
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        Vector3d pos = tc.getPosition();
        Vector3d vfxPos = new Vector3d(pos.x, pos.y + 1.0, pos.z);
        ParticleUtil.spawnParticleEffect(particleSystemId, vfxPos, store);
        int sfx = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (sfx != 0) {
            SoundUtil.playSoundEvent3d(null, sfx, vfxPos, store);
        }
    }
}
