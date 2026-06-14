package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class RtsGuardHealthUtil {
    private RtsGuardHealthUtil() {}

    public static float healthFraction(@Nonnull Ref<EntityStore> guardRef, @Nonnull Store<EntityStore> store) {
        EntityStatMap map = store.getComponent(guardRef, EntityStatMap.getComponentType());
        if (map == null) {
            return 1f;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = map.get(healthIndex);
        if (health == null) {
            return 1f;
        }
        float max = health.getMax();
        if (max <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, health.get() / max));
    }
}
