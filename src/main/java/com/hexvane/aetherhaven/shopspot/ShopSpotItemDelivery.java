package com.hexvane.aetherhaven.shopspot;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Gives shop items to a player, spawning leftovers at the stall when inventory is full. */
public final class ShopSpotItemDelivery {

    public record Result(boolean delivered, boolean droppedOnGround) {
        public boolean succeeded() {
            return delivered || droppedOnGround;
        }
    }

    private ShopSpotItemDelivery() {}

    @Nonnull
    public static Result grantAtShop(
        @Nonnull Player player,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull ItemStack stack,
        @Nonnull Vector3i shopBlock
    ) {
        if (ItemStack.isEmpty(stack)) {
            return new Result(false, false);
        }
        Vector3d dropPos = new Vector3d(shopBlock.x + 0.5, shopBlock.y + 0.5, shopBlock.z + 0.5);
        ItemStackTransaction tx = player.giveItem(stack, playerRef, store);
        List<ItemStack> overflow = new ArrayList<>();
        boolean delivered = false;
        if (!tx.succeeded()) {
            overflow.add(stack);
        } else {
            delivered = true;
            ItemStack remainder = tx.getRemainder();
            if (!ItemStack.isEmpty(remainder)) {
                overflow.add(remainder);
            }
        }
        boolean dropped = false;
        if (!overflow.isEmpty()) {
            spawnItemDrops(store, playerRef, overflow, dropPos);
            dropped = true;
        }
        return new Result(delivered, dropped);
    }

    private static void spawnItemDrops(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull List<ItemStack> stacks,
        @Nonnull Vector3d dropPosition
    ) {
        if (stacks.isEmpty()) {
            return;
        }
        UUIDComponent playerUuid = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (playerUuid == null) {
            return;
        }
        UUID uuid = playerUuid.getUuid();
        store.forEachChunk(
            Query.and(Player.getComponentType(), UUIDComponent.getComponentType()),
            (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    UUIDComponent u = chunk.getComponent(i, UUIDComponent.getComponentType());
                    if (u == null || !uuid.equals(u.getUuid())) {
                        continue;
                    }
                    Holder<EntityStore>[] holders =
                        ItemComponent.generateItemDrops(commandBuffer, stacks, dropPosition, Rotation3f.ZERO);
                    if (holders.length > 0) {
                        commandBuffer.addEntities(holders, AddReason.SPAWN);
                    }
                    return;
                }
            }
        );
    }
}
