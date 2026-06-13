package com.hexvane.aetherhaven.bard;

import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tracks which bard music container index is currently forced for each player. */
public final class BardMusicProximityState implements Resource<EntityStore> {
    @Nullable
    private static volatile ResourceType<EntityStore, BardMusicProximityState> resourceType;

    private final Map<UUID, Integer> activeContainerIndex = new HashMap<>();

    public static void register(
        @Nonnull com.hypixel.hytale.component.ComponentRegistryProxy<EntityStore> registry
    ) {
        resourceType = registry.registerResource(BardMusicProximityState.class, BardMusicProximityState::new);
    }

    @Nonnull
    public static ResourceType<EntityStore, BardMusicProximityState> getResourceType() {
        ResourceType<EntityStore, BardMusicProximityState> t = resourceType;
        if (t == null) {
            throw new IllegalStateException("BardMusicProximityState not registered");
        }
        return t;
    }

    public boolean isListening(@Nonnull UUID playerId) {
        return activeContainerIndex.getOrDefault(playerId, 0) != 0;
    }

    public int getActiveContainerIndex(@Nonnull UUID playerId) {
        return activeContainerIndex.getOrDefault(playerId, 0);
    }

    public void setActive(@Nonnull UUID playerId, int musicContainerIndex) {
        if (musicContainerIndex == 0) {
            activeContainerIndex.remove(playerId);
        } else {
            activeContainerIndex.put(playerId, musicContainerIndex);
        }
    }

    public void clear(@Nonnull UUID playerId) {
        activeContainerIndex.remove(playerId);
    }

    @Override
    public Resource<EntityStore> clone() {
        BardMusicProximityState copy = new BardMusicProximityState();
        copy.activeContainerIndex.putAll(activeContainerIndex);
        return copy;
    }
}
