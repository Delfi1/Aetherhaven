package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.rts.camera.TopDownCameraService;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerInput;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * RTS commander: vanilla creative flight for WASD pan + top-down camera.
 * Does not strip locomotion, apply abs deltas, or snap the body each tick.
 */
public final class RtsCommanderCameraSystem {
    private static final float ZOOM_UNITS_PER_SEC = 28f;
    private static final double ZOOM_WISH_THRESHOLD = 0.05;

    private RtsCommanderCameraSystem() {}

    public static final class Follow extends EntityTickingSystem<EntityStore> {
        @SuppressWarnings("unused")
        private final AetherhavenPlugin plugin;

        public Follow(@Nonnull AetherhavenPlugin plugin) {
            this.plugin = plugin;
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(
                RtsCommandPlayerComponent.getComponentType(),
                Player.getComponentType(),
                TransformComponent.getComponentType(),
                PlayerRef.getComponentType()
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
            if (session == null || !session.isActive()) {
                return;
            }
            Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
            TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
            PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
            if (tc == null || pr == null) {
                return;
            }

            readModifiers(session, chunk, index, pr);
            applyZoomFromWish(session, dt, pr);
            RtsMovementSupport.ensureFlying(playerRef, store);

            var pos = tc.getPosition();
            session.trackFocus(pos.x, pos.y, pos.z);

            clampPlayerToTerritory(session, playerRef, tc, store, commandBuffer);

            if (session.isCameraDirty()) {
                TopDownCameraService.apply(pr, session.getDistance());
                session.clearCameraDirty();
                commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
            }
        }

        private void readModifiers(
            @Nonnull RtsCommandPlayerComponent session,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            @Nonnull PlayerRef pr
        ) {
            RtsClientMovementPacketAdapter.Snapshot inbound =
                RtsClientMovementPacketAdapter.poll(pr.getUuid());
            if (inbound != null && inbound.hasMovementStates) {
                session.setShiftModifierHeld(inbound.crouching);
                session.setSprintModifierHeld(inbound.sprinting);
            }
            PlayerInput input = chunk.getComponent(index, PlayerInput.getComponentType());
            if (input != null) {
                List<PlayerInput.InputUpdate> queue = input.getMovementUpdateQueue();
                double wishX = session.getPanWishX();
                double wishZ = session.getPanWishZ();
                for (PlayerInput.InputUpdate update : queue) {
                    if (update instanceof PlayerInput.SetMovementStates setStates) {
                        MovementStates states = setStates.movementStates();
                        session.setShiftModifierHeld(states.crouching || states.forcedCrouching);
                        session.setSprintModifierHeld(states.sprinting);
                    } else if (update instanceof PlayerInput.WishMovement wish) {
                        wishX = wish.getX();
                        wishZ = wish.getZ();
                    }
                }
                session.setPanWish(wishX, wishZ);
            }
            MovementStatesComponent liveStates =
                chunk.getComponent(index, MovementStatesComponent.getComponentType());
            if (liveStates != null) {
                MovementStates states = liveStates.getMovementStates();
                if (states.crouching || states.forcedCrouching) {
                    session.setShiftModifierHeld(true);
                }
                if (states.sprinting) {
                    session.setSprintModifierHeld(true);
                }
            }
        }

        private void applyZoomFromWish(
            @Nonnull RtsCommandPlayerComponent session,
            float dt,
            @Nonnull PlayerRef pr
        ) {
            if (!session.isShiftModifierHeld()) {
                return;
            }
            double wz = session.getPanWishZ();
            if (Math.abs(wz) < ZOOM_WISH_THRESHOLD) {
                return;
            }
            float before = session.getDistance();
            float dist = before - (float) (wz * ZOOM_UNITS_PER_SEC * dt);
            session.setDistance(TopDownCameraService.clampDistance(dist));
            if (Math.abs(session.getDistance() - before) >= 0.01f) {
                session.markCameraDirty();
                if (session.isBoxSelectActive()) {
                    session.clearOrthoCalibration();
                }
                RtsDiagnostics.zoomApplied(pr, before, session.getDistance(), wz);
            }
        }

        private void clampPlayerToTerritory(
            @Nonnull RtsCommandPlayerComponent session,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull TransformComponent tc,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(store.getExternalData().getWorld(), plugin);
            try {
                TownRecord town = tm.getTown(UUID.fromString(session.getTownId()));
                if (town == null) {
                    return;
                }
                int overlap = com.hexvane.aetherhaven.AetherhavenConstants.RTS_TERRITORY_OVERLAP_BLOCKS;
                int cx = town.getCharterX();
                int cz = town.getCharterZ();
                int r = town.getTerritoryChunkRadius() * 16 + overlap;
                double minX = cx - r;
                double maxX = cx + r;
                double minZ = cz - r;
                double maxZ = cz + r;
                double x = tc.getPosition().x;
                double z = tc.getPosition().z;
                double clampedX = Math.max(minX, Math.min(maxX, x));
                double clampedZ = Math.max(minZ, Math.min(maxZ, z));
                if (Math.abs(clampedX - x) > 0.01 || Math.abs(clampedZ - z) > 0.01) {
                    tc.getPosition().x = clampedX;
                    tc.getPosition().z = clampedZ;
                    commandBuffer.putComponent(playerRef, TransformComponent.getComponentType(), tc);
                    session.trackFocus(clampedX, tc.getPosition().y, clampedZ);
                    commandBuffer.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
