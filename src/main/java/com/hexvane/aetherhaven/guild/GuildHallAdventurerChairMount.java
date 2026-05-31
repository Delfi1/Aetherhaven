package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hypixel.hytale.builtin.mounts.BlockMountAPI;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** Mounts guild hall display adventurers onto a chair/stool block under their spawn marker when present. */
public final class GuildHallAdventurerChairMount {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GuildHallAdventurerChairMount() {}

    /**
     * @return true when a block mount was applied
     */
    public static boolean tryMountChairBelowSpawn(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d spawnPosition
    ) {
        World world = store.getExternalData().getWorld();
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc == null) {
            return false;
        }
        Vector3d feet = tc.getPosition();
        Vector3i mountBlock = VillagerBlockUtil.findMountBlockNear(world, feet.x, feet.y, feet.z, spawnPosition);
        if (mountBlock == null) {
            return false;
        }
        if (!VillagerBlockUtil.canNpcMountBlockPoi(world, feet.x, feet.y, feet.z, mountBlock.x, mountBlock.y, mountBlock.z)) {
            return false;
        }
        try {
            Vector3d hit = new Vector3d(feet.x, feet.y + 0.5, feet.z);
            BlockMountAPI.BlockMountResult result =
                BlockMountAPI.mountOnBlock(npcRef, commandBuffer, mountBlock, hit);
            if (!(result instanceof BlockMountAPI.Mounted)) {
                return false;
            }
            NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
            if (npc != null) {
                npc.playAnimation(npcRef, AnimationSlot.Status, "Sit", store);
            }
            syncAnchorAfterMount(npcRef, store, commandBuffer);
            return true;
        } catch (RuntimeException ex) {
            LOGGER.at(Level.FINE).withCause(ex).log("Could not mount guild hall adventurer on chair");
            return false;
        }
    }

    public static boolean isBlockMounted(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> npcRef) {
        MountedComponent mounted = store.getComponent(npcRef, MountedComponent.getComponentType());
        return mounted != null && mounted.getControllerType() == MountController.BlockMount;
    }

    private static void syncAnchorAfterMount(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        GuildHallDisplayAnchor anchor = store.getComponent(npcRef, GuildHallDisplayAnchor.getComponentType());
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (anchor == null || tc == null) {
            return;
        }
        commandBuffer.putComponent(npcRef, GuildHallDisplayAnchor.getComponentType(), new GuildHallDisplayAnchor(tc.getPosition(), anchor.getYawRadians()));
    }
}
