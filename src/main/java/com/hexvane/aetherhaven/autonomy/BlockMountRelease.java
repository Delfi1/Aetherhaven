package com.hexvane.aetherhaven.autonomy;

import com.hypixel.hytale.builtin.mounts.BlockMountComponent;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Clears {@link BlockMountComponent} seated-entity refs before {@link MountedComponent} is dropped or the entity is
 * removed. Vanilla {@code MountSystems.RemoveMounted} can miss block cleanup when {@code MountedComponent} was already
 * queued for removal, leaving invalid refs that crash chunk save.
 */
public final class BlockMountRelease {
    private BlockMountRelease() {}

    public static void release(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        MountedComponent mounted = store.getComponent(entityRef, MountedComponent.getComponentType());
        if (mounted != null && mounted.getControllerType() == MountController.BlockMount) {
            clearBlockSeat(entityRef, mounted);
        }
        if (commandBuffer != null) {
            commandBuffer.tryRemoveComponent(entityRef, MountedComponent.getComponentType());
        } else {
            store.tryRemoveComponent(entityRef, MountedComponent.getComponentType());
        }
    }

    private static void clearBlockSeat(@Nonnull Ref<EntityStore> entityRef, @Nonnull MountedComponent mounted) {
        Ref<ChunkStore> blockRef = mounted.getMountedToBlock();
        if (blockRef == null || !blockRef.isValid()) {
            return;
        }
        Store<ChunkStore> chunkStore = blockRef.getStore();
        BlockMountComponent seat = chunkStore.getComponent(blockRef, BlockMountComponent.getComponentType());
        if (seat == null) {
            return;
        }
        seat.removeSeatedEntity(entityRef);
        if (seat.isDead()) {
            chunkStore.removeComponent(blockRef, BlockMountComponent.getComponentType());
        }
    }
}
