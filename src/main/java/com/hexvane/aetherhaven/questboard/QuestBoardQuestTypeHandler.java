package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.questboard.data.QuestBoardFetchEntryJson;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface QuestBoardQuestTypeHandler {
    @Nonnull
    String typeId();

    boolean populateSlot(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull String giverRoleId,
        @Nonnull String giverEntityUuid,
        @Nonnull QuestBoardFetchEntryJson entry,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull Random rng
    );

    @Nonnull
    Message displayTitle(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    );

    @Nonnull
    Message displayDescription(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    );

    @Nonnull
    Message objectivesText(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    );

    boolean hasRequiredItems(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull QuestBoardSlotRecord slot);

    boolean consumeRequiredItems(@Nonnull Ref<EntityStore> playerRef, @Nonnull Store<EntityStore> store, @Nonnull QuestBoardSlotRecord slot);
}
