package com.hexvane.aetherhaven.world;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import javax.annotation.Nonnull;

/** Detects Hytale temporary instance worlds vs persistent player worlds. */
public final class PersistentWorldSupport {
    private PersistentWorldSupport() {}

    /**
     * Temporary instances (dungeons, portals, hub visits, etc.) are spawned with {@link InstanceWorldConfig}
     * and removed when their removal conditions are met. Permanent worlds do not carry that config.
     */
    public static boolean isTemporaryInstance(@Nonnull World world) {
        return InstanceWorldConfig.get(world.getWorldConfig()) != null;
    }

    public static boolean shouldPersistWorldData(@Nonnull World world) {
        return !isTemporaryInstance(world);
    }

    /** Used when only a world name is available (e.g. shutdown save). */
    public static boolean shouldPersistWorldDataByName(@Nonnull String worldName) {
        World world = Universe.get().getWorld(worldName);
        if (world != null) {
            return shouldPersistWorldData(world);
        }
        return !worldName.startsWith(InstancesPlugin.INSTANCE_PREFIX);
    }
}
