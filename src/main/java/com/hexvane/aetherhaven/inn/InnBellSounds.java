package com.hexvane.aetherhaven.inn;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

final class InnBellSounds {
    private static volatile int ringIndexResolved = Integer.MIN_VALUE;

    private InnBellSounds() {}

    static void playAt(@Nonnull Store<EntityStore> store, @Nonnull Vector3i block) {
        int idx = resolveRingIndex();
        if (idx < 0) {
            return;
        }
        double x = block.x + 0.5;
        double y = block.y + 0.5;
        double z = block.z + 0.5;
        SoundUtil.playSoundEvent3d(idx, SoundCategory.SFX, x, y, z, 1.0F, 1.0F, store);
    }

    private static int resolveRingIndex() {
        int local = ringIndexResolved;
        if (local != Integer.MIN_VALUE) {
            return local;
        }
        synchronized (InnBellSounds.class) {
            if (ringIndexResolved == Integer.MIN_VALUE) {
                int idx = SoundEvent.getAssetMap().getIndex(AetherhavenConstants.INN_BELL_RING_SOUND_EVENT_ID);
                ringIndexResolved = idx;
            }
            return ringIndexResolved;
        }
    }
}
