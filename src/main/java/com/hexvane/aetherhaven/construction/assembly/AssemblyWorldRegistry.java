package com.hexvane.aetherhaven.construction.assembly;

import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** In-memory assembly jobs keyed by plot id (per world), each with incremental frontier state. */
public final class AssemblyWorldRegistry {
    private static final Map<String, ConcurrentHashMap<UUID, AssemblyEntry>> BY_WORLD = new ConcurrentHashMap<>();

    private record AssemblyEntry(@Nonnull PlotAssemblyJob job, @Nonnull PlotAssemblyFrontierRuntime runtime) {}

    private AssemblyWorldRegistry() {}

    /**
     * {@link IPrefabBuffer#release()} on cached prefab accessors is not idempotent (internal duplicate is nulled).
     * Overlapping cleanup (e.g. unload vs tick) or any double dispose must not crash the world thread.
     */
    /** Releases a cached prefab accessor; safe when already released or shared across jobs. */
    public static void releasePrefabBufferQuietly(@Nullable IPrefabBuffer buffer) {
        if (buffer == null) {
            return;
        }
        try {
            buffer.release();
        } catch (NullPointerException ignored) {
            // Already released
        }
    }

    private static void releaseJobBufferQuietly(@Nonnull IPrefabBuffer buffer) {
        releasePrefabBufferQuietly(buffer);
    }

    @Nonnull
    private static ConcurrentHashMap<UUID, AssemblyEntry> mapFor(@Nonnull World world) {
        return BY_WORLD.computeIfAbsent(world.getName(), n -> new ConcurrentHashMap<>());
    }

    public static void put(
        @Nonnull World world,
        @Nonnull UUID plotId,
        @Nonnull PlotAssemblyJob job,
        @Nonnull PlotAssemblyFrontierRuntime runtime
    ) {
        AssemblyEntry previous = mapFor(world).put(plotId, new AssemblyEntry(job, runtime));
        if (previous != null) {
            releaseJobBufferQuietly(previous.job().buffer());
        }
    }

    @Nullable
    public static PlotAssemblyJob get(@Nonnull World world, @Nonnull UUID plotId) {
        AssemblyEntry e = mapFor(world).get(plotId);
        return e != null ? e.job() : null;
    }

    @Nullable
    public static PlotAssemblyFrontierRuntime frontierRuntime(@Nonnull World world, @Nonnull UUID plotId) {
        AssemblyEntry e = mapFor(world).get(plotId);
        return e != null ? e.runtime() : null;
    }

    public static void remove(@Nonnull World world, @Nonnull UUID plotId) {
        AssemblyEntry e = mapFor(world).remove(plotId);
        if (e != null) {
            releaseJobBufferQuietly(e.job().buffer());
        }
    }

    @Nonnull
    public static Collection<PlotAssemblyJob> jobs(@Nonnull World world) {
        ArrayList<PlotAssemblyJob> out = new ArrayList<>(mapFor(world).size());
        for (AssemblyEntry e : mapFor(world).values()) {
            out.add(e.job());
        }
        return out;
    }

    public static void unloadWorld(@Nonnull String worldName) {
        ConcurrentHashMap<UUID, AssemblyEntry> m = BY_WORLD.remove(worldName);
        if (m != null) {
            for (AssemblyEntry e : m.values()) {
                releaseJobBufferQuietly(e.job().buffer());
            }
        }
    }
}
