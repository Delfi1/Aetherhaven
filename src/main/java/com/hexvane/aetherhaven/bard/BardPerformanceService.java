package com.hexvane.aetherhaven.bard;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.bard.data.BardSongDefinition;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.RoleUtils;
import com.hypixel.hytale.protocol.AnimationSlot;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class BardPerformanceService {
  public static final String PARTICLE_SYSTEM_ID = "Aetherhaven_Bard_Notes";
  public static final String PERFORMANCE_ANIMATION_ID = "PlayLute";
  public static final String LUTE_ITEM_ID = "Aetherhaven_Lute";
  /** Spawn note particles at chest / lute height. */
  public static final double PARTICLE_SPAWN_Y_OFFSET = 1.15;
  /** Horizontal distance in front of the bard along facing. */
  public static final double PARTICLE_SPAWN_FORWARD_OFFSET = 0.72;

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private BardPerformanceService() {}

  public static boolean isPerforming(@Nonnull Store<EntityStore> store, @Nullable Ref<EntityStore> npcRef) {
    if (npcRef == null || !npcRef.isValid()) {
      return false;
    }
    return store.getComponent(npcRef, BardPerformanceComponent.getComponentType()) != null;
  }

  public static void startSong(
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull AetherhavenPlugin plugin,
      @Nonnull String songId
  ) {
    if (!npcRef.isValid()) {
      return;
    }
    BardSongDefinition song = plugin.getBardSongCatalog().byId(songId);
    if (song == null) {
      LOGGER.atWarning().log("Unknown bard song id %s", songId);
      return;
    }
    stopOnStore(store, commandBuffer, npcRef);
    int musicContainerIndex = BardEnvironmentMusic.resolveMusicContainerIndex(song);
    long endAt = System.currentTimeMillis() + song.getDurationSeconds() * 1000L;
    BardPerformanceComponent perf = new BardPerformanceComponent(song.getId(), endAt, musicContainerIndex);
    putPerformanceComponent(npcRef, commandBuffer, store, perf);
    applyPerformanceVisuals(npcRef, store, commandBuffer);
    spawnNoteParticles(npcRef, store, commandBuffer, perf);
  }

  /** Legacy entry for callers that only have a world reference. */
  public static void startSong(
      @Nonnull World world,
      @Nonnull AetherhavenPlugin plugin,
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull String songId
  ) {
    world.execute(() -> {
      Store<EntityStore> store = world.getEntityStore().getStore();
      if (store != null) {
        startSong(store, null, npcRef, plugin, songId);
      }
    });
  }

  public static void stop(@Nonnull World world, @Nonnull Ref<EntityStore> npcRef) {
    world.execute(() -> {
      Store<EntityStore> store = world.getEntityStore().getStore();
      if (store != null) {
        stopOnStore(store, null, npcRef);
      }
    });
  }

  public static void stopOnStore(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> npcRef) {
    stopOnStore(store, null, npcRef);
  }

  public static void stopOnStore(
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Ref<EntityStore> npcRef
  ) {
    if (!npcRef.isValid()) {
      return;
    }
    removePerformanceComponent(npcRef, commandBuffer, store);
    clearPerformanceVisuals(npcRef, store, commandBuffer);
    BardEnvironmentMusic.stopAllListeningPlayers(store, commandBuffer);
  }

  @Nonnull
  public static Vector3d particleSpawnPosition(@Nonnull TransformComponent tc) {
    Vector3d pos = tc.getPosition();
    float yaw = tc.getRotation().yaw();
    double forwardX = -Math.sin(yaw) * PARTICLE_SPAWN_FORWARD_OFFSET;
    double forwardZ = -Math.cos(yaw) * PARTICLE_SPAWN_FORWARD_OFFSET;
    return new Vector3d(pos.x + forwardX, pos.y + PARTICLE_SPAWN_Y_OFFSET, pos.z + forwardZ);
  }

  public static void spawnPerformanceNoteParticles(
      @Nonnull TransformComponent tc,
      @Nonnull Store<EntityStore> store
  ) {
    var rotation = tc.getRotation();
    ParticleUtil.spawnParticleEffect(
        PARTICLE_SYSTEM_ID,
        particleSpawnPosition(tc),
        rotation.yaw(),
        rotation.pitch(),
        rotation.roll(),
        1.0F,
        0.0F,
        store
    );
  }

  public static void maintainPerformanceVisuals(
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer
  ) {
    NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
    if (npc != null) {
      npc.playAnimation(npcRef, AnimationSlot.Status, PERFORMANCE_ANIMATION_ID, store);
      equipLuteInHand(npcRef, npc, store, commandBuffer);
    }
  }

  private static void spawnNoteParticles(
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer,
      @Nonnull BardPerformanceComponent perf
  ) {
    TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
    if (tc == null) {
      return;
    }
    spawnPerformanceNoteParticles(tc, store);
    perf.setLastParticleSpawnMs(System.currentTimeMillis());
    putPerformanceComponent(npcRef, commandBuffer, store, perf);
  }

  private static void putPerformanceComponent(
      @Nonnull Ref<EntityStore> npcRef,
      @Nullable CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Store<EntityStore> store,
      @Nonnull BardPerformanceComponent perf
  ) {
    if (commandBuffer != null) {
      commandBuffer.putComponent(npcRef, BardPerformanceComponent.getComponentType(), perf);
    } else {
      store.putComponent(npcRef, BardPerformanceComponent.getComponentType(), perf);
    }
  }

  private static void removePerformanceComponent(
      @Nonnull Ref<EntityStore> npcRef,
      @Nullable CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Store<EntityStore> store
  ) {
    ComponentType<EntityStore, BardPerformanceComponent> type = BardPerformanceComponent.getComponentType();
    if (commandBuffer != null) {
      commandBuffer.tryRemoveComponent(npcRef, type);
    } else {
      store.tryRemoveComponent(npcRef, type);
    }
  }

  private static void applyPerformanceVisuals(
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer
  ) {
    NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
    if (npc != null) {
      equipLuteInHand(npcRef, npc, store, commandBuffer);
      npc.playAnimation(npcRef, AnimationSlot.Status, PERFORMANCE_ANIMATION_ID, store);
    }
  }

  private static void equipLuteInHand(
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull NPCEntity npc,
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer
  ) {
    try {
      RoleUtils.setItemInHand(npcRef, npc, LUTE_ITEM_ID, commandBuffer != null ? commandBuffer : store);
    } catch (RuntimeException ex) {
      LOGGER.atWarning().withCause(ex).log("Could not equip lute on bard");
    }
  }

  private static void clearPerformanceVisuals(
      @Nonnull Ref<EntityStore> npcRef,
      @Nonnull Store<EntityStore> store,
      @Nullable CommandBuffer<EntityStore> commandBuffer
  ) {
    NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
    if (npc != null) {
      npc.playAnimation(npcRef, AnimationSlot.Status, null, store);
      try {
        RoleUtils.setItemInHand(npcRef, npc, null, commandBuffer != null ? commandBuffer : store);
      } catch (RuntimeException ex) {
        LOGGER.atWarning().withCause(ex).log("Could not clear lute from bard");
      }
    }
  }
}
