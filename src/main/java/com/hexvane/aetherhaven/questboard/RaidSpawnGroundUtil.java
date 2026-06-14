package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

final class RaidSpawnGroundUtil {
    private static final int MAX_ABOVE_CHARTER = 6;
    private static final int MAX_BELOW_CHARTER = 22;

    private RaidSpawnGroundUtil() {}

    @Nullable
    static Vector3d findSpawnPosition(@Nonnull World world, int bx, int bz, int charterY) {
        int startY = Math.min(319, charterY + 10);
        int minY = Math.max(1, charterY - MAX_BELOW_CHARTER);
        for (int feetY = startY; feetY >= minY; feetY--) {
            if (feetY > charterY + MAX_ABOVE_CHARTER) {
                continue;
            }
            if (!isSafeRaidStand(world, bx, feetY, bz)) {
                continue;
            }
            return new Vector3d(bx + 0.5, feetY, bz + 0.5);
        }
        return null;
    }

    static boolean isSafeRaidStand(@Nonnull World world, int bx, int feetY, int bz) {
        if (!VillagerBlockUtil.isNpcStandColumn(world, bx, feetY, bz)) {
            return false;
        }
        if (hasFluid(world, bx, feetY, bz) || hasFluid(world, bx, feetY + 1, bz)) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (hasFluid(world, bx + dx, feetY, bz + dz) || hasFluid(world, bx + dx, feetY + 1, bz + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasFluid(@Nonnull World world, int x, int y, int z) {
        if (y < 0 || y >= 320) {
            return false;
        }
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null || !chunk.getReference().isValid()) {
            return true;
        }
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) {
            return true;
        }
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return true;
        }
        FluidSection fluidSection = chunkStore.getStore().getComponent(sectionRef, FluidSection.getComponentType());
        return fluidSection != null && fluidSection.getFluidId(x, y, z) != 0;
    }
}
