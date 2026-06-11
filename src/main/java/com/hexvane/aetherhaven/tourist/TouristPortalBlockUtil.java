package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

public final class TouristPortalBlockUtil {
    private static final int[][] STAND_OFFSETS_FRONT_FIRST = {
        {1, 2}, {0, 2}, {2, 2},
        {1, 3}, {0, 3}, {2, 3}, {1, 4}, {0, 4}, {2, 4},
        {1, 1}, {0, 1}, {2, 1},
    };

    private static final int[][] STAND_OFFSETS_ANY = {
        {1, 2}, {0, 2}, {2, 2},
        {1, 3}, {0, 3}, {2, 3},
        {1, 1}, {0, 1}, {2, 1},
        {1, 4}, {0, 4}, {2, 4},
    };

    private TouristPortalBlockUtil() {}

    public static boolean isTouristPortalBlock(@Nullable BlockType type) {
        return type != null && AetherhavenConstants.TOURIST_PORTAL_BLOCK_TYPE_ID.equals(type.getId());
    }

    @Nullable
    public static TouristPortalBlock getBlockComponent(@Nonnull World world, @Nonnull Vector3i pos) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) {
            return null;
        }
        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
        if (blockRef == null) {
            return null;
        }
        return blockRef.getStore().getComponent(blockRef, TouristPortalBlock.getComponentType());
    }

    public static boolean writeBlockComponent(@Nonnull World world, @Nonnull Vector3i pos, @Nonnull TouristPortalBlock block) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) {
            return false;
        }
        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
        if (blockRef == null) {
            return false;
        }
        Store<ChunkStore> cs = blockRef.getStore();
        cs.putComponent(blockRef, TouristPortalBlock.getComponentType(), copyBlock(block));
        return true;
    }

    @Nonnull
    private static TouristPortalBlock copyBlock(@Nonnull TouristPortalBlock block) {
        return new TouristPortalBlock(block.getPortalId(), block.getTownId(), block.getPlotId(), block.isConfigured());
    }

    public static void syncConfigToBlock(@Nonnull World world, @Nonnull Vector3i pos, @Nonnull TouristPortalRecord record) {
        TouristPortalBlock existing = getBlockComponent(world, pos);
        TouristPortalBlock block =
            existing != null
                ? copyBlock(existing)
                : new TouristPortalBlock(
                    record.getPortalId().toString(),
                    record.getTownId().toString(),
                    record.getPlotId().toString(),
                    false
                );
        block.applyRecord(record);
        if (!writeBlockComponent(world, pos, block)) {
            world.execute(() -> writeBlockComponent(world, pos, block));
        }
    }

    /** Center of the 2 wide platform at portal height for particles and burst VFX. */
    @Nonnull
    public static Vector3d portalEffectCenter(@Nonnull Vector3i blockPos) {
        return new Vector3d(blockPos.x + 1.0, blockPos.y + 2.5, blockPos.z + 0.5);
    }

    /** Feet position on open ground near the portal for NPC spawn. */
    @Nonnull
    public static Vector3d spawnFeetPosition(@Nonnull World world, @Nonnull Vector3i blockPos) {
        return spawnFeetPosition(world, blockPos, blockPos.hashCode());
    }

    /** Stand position in front of the portal for return / despawn approach. */
    @Nonnull
    public static Vector3d returnStandPosition(@Nonnull World world, @Nonnull Vector3i blockPos) {
        return resolveStandNearPortal(world, blockPos, true);
    }

    /** True when the NPC is close enough to the portal platform to despawn. */
    public static boolean isNearPortalDespawn(@Nonnull World world, @Nonnull Vector3i blockPos, @Nonnull Vector3d feet) {
        Vector3d stand = returnStandPosition(world, blockPos);
        double dx = feet.x - stand.x;
        double dz = feet.z - stand.z;
        if (dx * dx + dz * dz <= 6.25) {
            return true;
        }
        Vector3d center = portalEffectCenter(blockPos);
        dx = feet.x - center.x;
        dz = feet.z - center.z;
        return dx * dx + dz * dz <= 9.0;
    }

    @Nonnull
    private static Vector3d resolveStandNearPortal(
        @Nonnull World world,
        @Nonnull Vector3i blockPos,
        boolean preferInFront
    ) {
        int bx0 = blockPos.x;
        int bz0 = blockPos.z;
        int[][] order = preferInFront ? STAND_OFFSETS_FRONT_FIRST : STAND_OFFSETS_ANY;
        for (int feetY : standSearchHeights(blockPos.y, preferInFront)) {
            Vector3d stand = tryStandOffsets(world, bx0, bz0, feetY, order);
            if (stand != null) {
                return stand;
            }
        }
        return new Vector3d(blockPos.x + 1.5, blockPos.y + 1.0, blockPos.z + (preferInFront ? 2.5 : 1.5));
    }

    @Nonnull
    private static int[] standSearchHeights(int portalY, boolean preferInFront) {
        if (preferInFront) {
            return new int[] { portalY - 1, portalY + 1, portalY, portalY + 2, portalY - 2 };
        }
        return new int[] { portalY - 1, portalY, portalY + 1, portalY + 2, portalY - 2 };
    }

    /** Picks a validated stand tile; {@code salt} spreads concurrent spawns across nearby tiles. */
    @Nonnull
    public static Vector3d spawnFeetPosition(@Nonnull World world, @Nonnull Vector3i blockPos, long salt) {
        int bx0 = blockPos.x;
        int bz0 = blockPos.z;
        int[][] order = STAND_OFFSETS_ANY;
        java.util.ArrayList<Vector3d> candidates = new java.util.ArrayList<>();
        for (int feetY : standSearchHeights(blockPos.y, false)) {
            collectStandOffsets(world, bx0, bz0, feetY, order, candidates);
        }
        if (candidates.isEmpty()) {
            return new Vector3d(blockPos.x + 1.5, blockPos.y + 1.0, blockPos.z + 1.5);
        }
        int idx = (int) (Math.floorMod(salt, candidates.size()));
        return candidates.get(idx);
    }

    private static void collectStandOffsets(
        @Nonnull World world,
        int bx0,
        int bz0,
        int feetY,
        @Nonnull int[][] offsets,
        @Nonnull java.util.ArrayList<Vector3d> out
    ) {
        if (feetY < 1 || feetY >= 320) {
            return;
        }
        for (int[] off : offsets) {
            int bx = bx0 + off[0];
            int bz = bz0 + off[1];
            if (VillagerBlockUtil.isNpcStandColumn(world, bx, feetY, bz)) {
                out.add(new Vector3d(bx + 0.5, feetY, bz + 0.5));
            }
        }
    }

    @Nullable
    private static Vector3d tryStandOffsets(
        @Nonnull World world,
        int bx0,
        int bz0,
        int feetY,
        @Nonnull int[][] offsets
    ) {
        if (feetY < 1 || feetY >= 320) {
            return null;
        }
        for (int[] off : offsets) {
            int bx = bx0 + off[0];
            int bz = bz0 + off[1];
            if (VillagerBlockUtil.isNpcStandColumn(world, bx, feetY, bz)) {
                return new Vector3d(bx + 0.5, feetY, bz + 0.5);
            }
        }
        return null;
    }

    @Nullable
    public static UUID portalIdAt(@Nonnull World world, @Nonnull Vector3i pos) {
        TouristPortalBlock block = getBlockComponent(world, pos);
        if (block == null || block.getPortalId().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(block.getPortalId().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
