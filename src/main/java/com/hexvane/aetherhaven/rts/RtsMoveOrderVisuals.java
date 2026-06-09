package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Ground move-destination marker (ring + rising motes) until guards arrive. */
public final class RtsMoveOrderVisuals {
    /** Tiny lift so the flat ring clears z-fighting; rise spawner adds its own offset. */
    private static final double SURFACE_LIFT = 0.02;
    private static final Map<UUID, ActiveDestination> ACTIVE = new ConcurrentHashMap<>();

    private RtsMoveOrderVisuals() {}

    public record ActiveDestination(
        double x,
        double y,
        double z,
        @Nonnull Set<UUID> guardIds,
        long lastSpawnWorldTick
    ) {
        ActiveDestination withSpawnTick(long worldTick) {
            return new ActiveDestination(x, y, z, guardIds, worldTick);
        }
    }

    public static void register(
        @Nonnull UUID commanderId,
        double x,
        double y,
        double z,
        @Nonnull Collection<UUID> guardIds,
        long worldTick
    ) {
        ACTIVE.put(
            commanderId,
            new ActiveDestination(x, y, z, new HashSet<>(guardIds), worldTick)
        );
    }

    public static void spawn(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> commanderRef,
        double x,
        double y,
        double z,
        @Nonnull Collection<UUID> guardIds,
        long worldTick
    ) {
        UUID commanderId = commanderUuid(commanderRef, store);
        if (commanderId == null) {
            return;
        }
        register(commanderId, x, y, z, guardIds, worldTick);
        spawnEffect(store, commanderRef, x, y, z);
    }

    public static void refreshSpawn(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> commanderRef,
        @Nonnull ActiveDestination destination,
        long worldTick
    ) {
        UUID commanderId = commanderUuid(commanderRef, store);
        if (commanderId == null) {
            return;
        }
        spawnEffect(store, commanderRef, destination.x(), destination.y(), destination.z());
        ACTIVE.put(commanderId, destination.withSpawnTick(worldTick));
    }

    @Nullable
    public static ActiveDestination getActive(@Nonnull UUID commanderId) {
        return ACTIVE.get(commanderId);
    }

    public static void clear(@Nonnull UUID commanderId) {
        ACTIVE.remove(commanderId);
    }

    public static void clearCommander(@Nonnull Ref<EntityStore> commanderRef, @Nonnull Store<EntityStore> store) {
        UUID commanderId = commanderUuid(commanderRef, store);
        if (commanderId != null) {
            clear(commanderId);
        }
    }

    private static void spawnEffect(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> commanderRef,
        double x,
        double y,
        double z
    ) {
        double markerY = y + SURFACE_LIFT;
        ParticleUtil.spawnParticleEffect(
            AetherhavenConstants.RTS_MOVE_ORDER_MARKER_PARTICLE,
            new Vector3d(x, markerY, z),
            List.of(commanderRef),
            store
        );
    }

    @Nullable
    private static UUID commanderUuid(@Nonnull Ref<EntityStore> commanderRef, @Nonnull Store<EntityStore> store) {
        var uc = store.getComponent(commanderRef, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }
}

