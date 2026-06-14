package com.hexvane.aetherhaven.map;

import com.hypixel.hytale.common.fastutil.HLongSet;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Reads {@link WorldMapTracker} loaded-chunk state for map chunks already sent to the client. */
public final class WorldMapTrackerCompat {

  @Nullable
  private static final Field LOADED_FIELD;

  @Nullable
  private static final Field LOADED_LOCK_FIELD;

  static {
    Field loaded = null;
    Field lock = null;
    try {
      loaded = WorldMapTracker.class.getDeclaredField("loaded");
      loaded.setAccessible(true);
      lock = WorldMapTracker.class.getDeclaredField("loadedLock");
      lock.setAccessible(true);
    } catch (ReflectiveOperationException ignored) {
      // Fall back to empty loaded set.
    }
    LOADED_FIELD = loaded;
    LOADED_LOCK_FIELD = lock;
  }

  private WorldMapTrackerCompat() {}

  /**
   * Returns a copy of map chunk indices already sent to this player's client, or empty if unavailable.
   */
  @Nonnull
  public static LongSet getLoadedChunks(@Nonnull Player player) {
    HLongSet loaded = getLoadedSet(player);
    if (loaded == null || loaded.isEmpty()) {
      return new LongOpenHashSet();
    }
    LongOpenHashSet copy = new LongOpenHashSet(loaded.size());
    for (long chunkIndex : loaded) {
      copy.add(chunkIndex);
    }
    return copy;
  }

  /** True when the tracker has already sent this map chunk to the player. */
  public static boolean hasChunkLoadedOnClient(@Nonnull Player player, long chunkIndex) {
    HLongSet loaded = getLoadedSet(player);
    return loaded != null && loaded.contains(chunkIndex);
  }

  @Nullable
  private static HLongSet getLoadedSet(@Nonnull Player player) {
    if (LOADED_FIELD == null || LOADED_LOCK_FIELD == null) {
      return null;
    }
    WorldMapTracker tracker = player.getWorldMapTracker();
    ReentrantReadWriteLock lock;
    try {
      lock = (ReentrantReadWriteLock) LOADED_LOCK_FIELD.get(tracker);
    } catch (IllegalAccessException e) {
      return null;
    }
    lock.readLock().lock();
    try {
      return (HLongSet) LOADED_FIELD.get(tracker);
    } catch (IllegalAccessException e) {
      return null;
    } finally {
      lock.readLock().unlock();
    }
  }
}
