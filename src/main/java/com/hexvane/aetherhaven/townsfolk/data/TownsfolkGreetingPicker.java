package com.hexvane.aetherhaven.townsfolk.data;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TownsfolkGreetingPicker {
    private TownsfolkGreetingPicker() {}

    @Nullable
    public static Message pickMessage(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull UUID playerUuid,
        @Nonnull UUID npcEntityUuid
    ) {
        TownsfolkCharacterBinding binding = store.getComponent(npcRef, TownsfolkCharacterBinding.getComponentType());
        if (binding == null) {
            return null;
        }
        TownsfolkPersonalityCatalog personalities = plugin.getTownsfolkPersonalityCatalog();
        TownsfolkPersonalityDefinition personality = personalities.byId(binding.getActivePersonalityId());
        if (personality == null) {
            return null;
        }
        List<String> keys = personality.getDialogueGreetingLangKeys();
        if (keys.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(playerUuid.hashCode() ^ npcEntityUuid.hashCode(), keys.size());
        return Message.translation(keys.get(idx));
    }
}
