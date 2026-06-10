package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Per-commander focus-fire target for distinct marker particles. */
public final class RtsFocusTargetVisuals {
    private static final Map<UUID, UUID> ACTIVE = new ConcurrentHashMap<>();

    private RtsFocusTargetVisuals() {}

    public static void register(@Nonnull UUID commanderId, @Nonnull UUID targetEntityId) {
        ACTIVE.put(commanderId, targetEntityId);
    }

    @Nullable
    public static UUID getActive(@Nonnull UUID commanderId) {
        return ACTIVE.get(commanderId);
    }

    public static void clear(@Nonnull UUID commanderId) {
        ACTIVE.remove(commanderId);
    }

    public static void clearCommander(@Nonnull Ref<EntityStore> commanderRef, @Nonnull Store<EntityStore> store) {
        UUIDComponent uc = store.getComponent(commanderRef, UUIDComponent.getComponentType());
        if (uc != null) {
            clear(uc.getUuid());
        }
    }
}
