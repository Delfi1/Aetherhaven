package com.hexvane.aetherhaven.shopspot;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Ponder emote while NPCs browse shop listings before a purchase decision. */
public final class ShopSpotBrowseVisuals {
    /** Built-in emote from vanilla {@code EmotesInGame.json}. */
    public static final String PONDER_EMOTE_ID = "PonderDismissive";

    private ShopSpotBrowseVisuals() {}

    public static void beginPonder(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        playPonder(npcRef, commandBuffer);
    }

    public static void beginPonder(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store) {
        playPonder(npcRef, store);
    }

    public static void endPonder(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        stopPonder(npcRef, store, commandBuffer);
    }

    public static void endPonder(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store) {
        stopPonder(npcRef, store, null);
    }

    private static void playPonder(@Nonnull Ref<EntityStore> npcRef, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        AnimationUtils.playAnimation(npcRef, AnimationSlot.Emote, null, PONDER_EMOTE_ID, false, commandBuffer);
    }

    private static void playPonder(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store) {
        AnimationUtils.playAnimation(npcRef, AnimationSlot.Emote, null, PONDER_EMOTE_ID, false, store);
    }

    private static void stopPonder(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        if (commandBuffer != null) {
            AnimationUtils.stopAnimation(npcRef, AnimationSlot.Emote, commandBuffer);
        } else {
            AnimationUtils.stopAnimation(npcRef, AnimationSlot.Emote, store);
        }
        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc != null) {
            if (commandBuffer != null) {
                npc.playAnimation(npcRef, AnimationSlot.Emote, null, commandBuffer);
            } else {
                npc.playAnimation(npcRef, AnimationSlot.Emote, null, store);
            }
        }
    }
}
