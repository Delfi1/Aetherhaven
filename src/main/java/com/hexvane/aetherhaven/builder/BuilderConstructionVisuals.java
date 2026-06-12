package com.hexvane.aetherhaven.builder;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.equipment.VillagerEquipmentService;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Hammer equip and swing animation while the builder assists plot assembly. */
public final class BuilderConstructionVisuals {
    private static final String HAMMER_ITEM_ID = "Tool_Hammer_Iron";
    private static final String WORK_PROFILE_ID = "work_builder";

    private BuilderConstructionVisuals() {}

    public static void beginAssist(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull NPCEntity npc
    ) {
        VillagerEquipmentService.applyProfile(
            npcRef,
            store,
            commandBuffer,
            plugin.getEquipmentProfileCatalog(),
            WORK_PROFILE_ID
        );
    }

    public static void swingHammer(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull NPCEntity npc
    ) {
        Item item = Item.getAssetMap().getAsset(HAMMER_ITEM_ID);
        if (item == null) {
            return;
        }
        String pid = item.getPlayerAnimationsId();
        if (pid == null || pid.isBlank()) {
            return;
        }
        ItemPlayerAnimations ipa = ItemPlayerAnimations.getAssetMap().getAsset(pid);
        if (ipa != null) {
            AnimationUtils.playAnimation(npcRef, AnimationSlot.Action, ipa, "SwingLeft", store);
        } else {
            npc.playAnimation(npcRef, AnimationSlot.Action, "SwingLeft", store);
        }
        commandBuffer.putComponent(npcRef, NPCEntity.getComponentType(), npc);
    }

    public static void endAssist(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nullable NPCEntity npc
    ) {
        if (npc != null) {
            AnimationUtils.stopAnimation(npcRef, AnimationSlot.Action, store);
            AnimationUtils.stopAnimation(npcRef, AnimationSlot.Status, store);
            npc.playAnimation(npcRef, AnimationSlot.Action, null, store);
            npc.playAnimation(npcRef, AnimationSlot.Status, null, store);
            commandBuffer.putComponent(npcRef, NPCEntity.getComponentType(), npc);
        }
    }
}
