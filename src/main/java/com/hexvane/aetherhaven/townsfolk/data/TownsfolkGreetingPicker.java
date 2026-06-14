package com.hexvane.aetherhaven.townsfolk.data;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Picks a random townsfolk hub greeting from the personality pool on each conversation. */
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
        List<String> personalityIds = binding.getPersonalityIds();
        if (personalityIds.isEmpty()) {
            TownsfolkCharacterDefinition character =
                plugin.getTownsfolkCharacterCatalog().byId(binding.getCharacterId());
            if (character != null) {
                personalityIds = character.getPersonalityIds();
            }
        }
        TownsfolkPersonalityCatalog personalities = plugin.getTownsfolkPersonalityCatalog();
        List<String> greetingKeys = new ArrayList<>();
        for (String pid : personalityIds) {
            TownsfolkPersonalityDefinition personality = personalities.byId(pid);
            if (personality != null) {
                greetingKeys.addAll(personality.getDialogueGreetingLangKeys());
            }
        }
        if (greetingKeys.isEmpty()) {
            return null;
        }
        int idx = ThreadLocalRandom.current().nextInt(greetingKeys.size());
        return Message.translation(greetingKeys.get(idx));
    }
}
