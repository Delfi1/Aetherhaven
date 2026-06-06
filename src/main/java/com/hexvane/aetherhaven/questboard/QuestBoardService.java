package com.hexvane.aetherhaven.questboard;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.quest.data.QuestReward;
import com.hexvane.aetherhaven.questboard.data.QuestBoardFetchEntryJson;
import com.hexvane.aetherhaven.reputation.VillagerReputationService;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.ui.TownVillagerDirectory;
import com.hexvane.aetherhaven.ui.TownVillagerRow;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardService {
    public static final String JOURNAL_ROW_PREFIX = "board:";

    private static final Map<String, QuestBoardQuestTypeHandler> HANDLERS = new HashMap<>();

    static {
        register(new FetchQuestBoardHandler());
    }

    private QuestBoardService() {}

    public static void register(@Nonnull QuestBoardQuestTypeHandler handler) {
        HANDLERS.put(handler.typeId(), handler);
    }

    @Nullable
    public static QuestBoardQuestTypeHandler handlerFor(@Nullable String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return null;
        }
        return HANDLERS.get(typeId.trim());
    }

    @Nonnull
    public static String journalRowId(@Nonnull String instanceId) {
        return JOURNAL_ROW_PREFIX + instanceId.trim();
    }

    @Nullable
    public static String parseJournalInstanceId(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(JOURNAL_ROW_PREFIX)) {
            return null;
        }
        String id = rowId.substring(JOURNAL_ROW_PREFIX.length()).trim();
        return id.isEmpty() ? null : id;
    }

    public static boolean isBoardJournalRow(@Nullable String rowId) {
        return parseJournalInstanceId(rowId) != null;
    }

    public static void ensureBoardInitialized(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull Random rng
    ) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        Set<String> batchExclude = QuestBoardDrawPool.occupiedBoardEntryKeys(town);
        for (int i = 0; i < catalog.slotCount(); i++) {
            QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(i);
            if (slot.stateEnum() == QuestBoardSlotState.EMPTY) {
                if (generateOffer(town, store, catalog, i, rng, batchExclude)) {
                    trackSlotEntryKey(batchExclude, slot);
                }
            } else {
                trackSlotEntryKey(batchExclude, slot);
            }
        }
    }

    public static void refreshUnacceptedSlots(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull Random rng
    ) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        Set<String> batchExclude = new HashSet<>();
        for (QuestBoardSlotRecord slot : town.getQuestBoardSlots()) {
            if (slot.stateEnum() == QuestBoardSlotState.ACCEPTED) {
                trackSlotEntryKey(batchExclude, slot);
            }
        }
        for (int i = 0; i < catalog.slotCount(); i++) {
            QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(i);
            if (slot.stateEnum() == QuestBoardSlotState.ACCEPTED) {
                continue;
            }
            if (generateOffer(town, store, catalog, i, rng, batchExclude)) {
                trackSlotEntryKey(batchExclude, slot);
            }
        }
    }

    public static boolean generateOffer(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        int slotIndex,
        @Nonnull Random rng
    ) {
        Set<String> exclude = QuestBoardDrawPool.occupiedBoardEntryKeys(town);
        QuestBoardSlotRecord current = town.getQuestBoardSlots().get(slotIndex);
        if (current != null && current.stateEnum() != QuestBoardSlotState.EMPTY) {
            String role = current.getGiverRoleId();
            String entry = current.getConfigEntryId();
            if (role != null && entry != null) {
                exclude.remove(QuestBoardDrawPool.entryKey(role, entry));
            }
        }
        return generateOffer(town, store, catalog, slotIndex, rng, exclude);
    }

    public static boolean generateOffer(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        int slotIndex,
        @Nonnull Random rng,
        @Nonnull Set<String> excludeKeys
    ) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        if (slotIndex < 0 || slotIndex >= catalog.slotCount()) {
            return false;
        }
        QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(slotIndex);
        if (slot.stateEnum() == QuestBoardSlotState.ACCEPTED) {
            return false;
        }

        int townRankIndex = TownQuestBoardRank.rankIndex(TownQuestBoardRank.tierIdForXp(town.getQuestBoardRankXp(), catalog));
        List<String> eligibleKeys = buildEligibleEntryKeys(town, store, catalog, townRankIndex);
        String pickedKey = QuestBoardDrawPool.pickEntryKey(town, eligibleKeys, excludeKeys, rng);
        if (pickedKey == null) {
            slot.clearToEmpty();
            return false;
        }

        QuestBoardDrawPool.ParsedEntryKey parsed = QuestBoardDrawPool.parseEntryKey(pickedKey);
        if (parsed == null) {
            slot.clearToEmpty();
            return false;
        }
        QuestBoardFetchEntryJson entry = findFetchEntry(catalog, parsed.roleId(), parsed.entryId());
        if (entry == null) {
            slot.clearToEmpty();
            return false;
        }
        List<TownVillagerRow> givers = giversForRole(town, store, parsed.roleId());
        if (givers.isEmpty()) {
            slot.clearToEmpty();
            return false;
        }
        TownVillagerRow giver = givers.get(rng.nextInt(givers.size()));

        QuestBoardQuestTypeHandler handler = handlerFor(FetchQuestBoardHandler.TYPE_ID);
        if (handler == null) {
            slot.clearToEmpty();
            return false;
        }
        slot.clearToEmpty();
        boolean ok =
            handler.populateSlot(
                slot,
                town,
                store,
                giver.roleId(),
                giver.entityUuid().toString(),
                entry,
                catalog,
                rng
            );
        if (!ok) {
            slot.clearToEmpty();
        }
        return ok;
    }

    private static void trackSlotEntryKey(@Nonnull Set<String> excludeKeys, @Nonnull QuestBoardSlotRecord slot) {
        if (!slot.occupiesBoardSlot()) {
            return;
        }
        String role = slot.getGiverRoleId();
        String entry = slot.getConfigEntryId();
        if (role != null && !role.isBlank() && entry != null && !entry.isBlank()) {
            excludeKeys.add(QuestBoardDrawPool.entryKey(role, entry));
        }
    }

    @Nonnull
    private static List<String> buildEligibleEntryKeys(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        int townRankIndex
    ) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TownVillagerRow row : TownVillagerDirectory.listResidents(store, town)) {
            String roleId = row.roleId();
            for (QuestBoardFetchEntryJson entry : catalog.fetchEntriesForRole(roleId)) {
                if (!TownQuestBoardRank.townRankWithinWindow(townRankIndex, entry.minRank(), entry.maxRank())) {
                    continue;
                }
                if (entry.id() == null || entry.id().isBlank()) {
                    continue;
                }
                String key = QuestBoardDrawPool.entryKey(roleId, entry.id());
                if (seen.add(key)) {
                    out.add(key);
                }
            }
        }
        return out;
    }

    @Nullable
    private static QuestBoardFetchEntryJson findFetchEntry(
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull String roleId,
        @Nonnull String entryId
    ) {
        for (QuestBoardFetchEntryJson entry : catalog.fetchEntriesForRole(roleId)) {
            if (entryId.equalsIgnoreCase(entry.id())) {
                return entry;
            }
        }
        return null;
    }

    @Nonnull
    private static List<TownVillagerRow> giversForRole(
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull String roleId
    ) {
        List<TownVillagerRow> out = new ArrayList<>();
        for (TownVillagerRow row : TownVillagerDirectory.listResidents(store, town)) {
            if (roleId.equals(row.roleId())) {
                out.add(row);
            }
        }
        return out;
    }

    public static boolean acceptOffer(@Nonnull TownRecord town, @Nonnull UUID playerUuid, int slotIndex, @Nonnull QuestBoardCatalog catalog) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        if (slotIndex < 0 || slotIndex >= catalog.slotCount()) {
            return false;
        }
        if (!town.playerCanAcceptQuests(playerUuid)) {
            return false;
        }
        QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(slotIndex);
        if (slot.stateEnum() != QuestBoardSlotState.OFFER) {
            return false;
        }
        slot.setState(QuestBoardSlotState.ACCEPTED);
        slot.setAcceptedByPlayerUuid(playerUuid.toString());
        slot.setOnlineDaysElapsed(0);
        return true;
    }

    public static boolean abandonOffer(@Nonnull TownRecord town, @Nonnull UUID playerUuid, int slotIndex, @Nonnull QuestBoardCatalog catalog) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        if (slotIndex < 0 || slotIndex >= catalog.slotCount()) {
            return false;
        }
        if (!town.playerCanAbandonQuests(playerUuid)) {
            return false;
        }
        QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(slotIndex);
        if (slot.stateEnum() != QuestBoardSlotState.ACCEPTED) {
            return false;
        }
        slot.clearToEmpty();
        return true;
    }

    public static boolean abandonByInstanceId(
        @Nonnull TownRecord town,
        @Nonnull UUID playerUuid,
        @Nonnull String instanceId,
        @Nonnull QuestBoardCatalog catalog
    ) {
        town.ensureQuestBoardSlotCount(catalog.slotCount());
        for (int i = 0; i < town.getQuestBoardSlots().size(); i++) {
            QuestBoardSlotRecord slot = town.getQuestBoardSlots().get(i);
            if (instanceId.equals(slot.instanceIdOrEmpty()) && slot.isAccepted()) {
                if (!town.playerCanAbandonQuests(playerUuid)) {
                    return false;
                }
                slot.clearToEmpty();
                return true;
            }
        }
        return false;
    }

    public static int slotIndexForInstanceId(@Nonnull TownRecord town, @Nonnull String instanceId) {
        List<QuestBoardSlotRecord> slots = town.getQuestBoardSlots();
        for (int i = 0; i < slots.size(); i++) {
            if (instanceId.equals(slots.get(i).instanceIdOrEmpty())) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public static QuestBoardSlotRecord findAcceptedForGiver(@Nonnull TownRecord town, @Nonnull UUID giverEntityUuid) {
        return town.findAcceptedBoardQuestForGiver(giverEntityUuid);
    }

    public static boolean completeFetchQuest(
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID giverEntityUuid,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull Random rng
    ) {
        QuestBoardSlotRecord slot = town.findAcceptedBoardQuestForGiver(giverEntityUuid);
        if (slot == null || !FetchQuestBoardHandler.TYPE_ID.equals(slot.getQuestType())) {
            return false;
        }
        QuestBoardQuestTypeHandler handler = handlerFor(FetchQuestBoardHandler.TYPE_ID);
        if (handler == null || !handler.consumeRequiredItems(playerRef, store, slot)) {
            return false;
        }
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return false;
        }
        grantRewards(slot, player, playerRef, store);
        int oldXp = town.getQuestBoardRankXp();
        Message completedName = displayTitle(slot, town, store, catalog);
        town.addQuestBoardRankXp(slot.getRankXpReward());
        int newXp = town.getQuestBoardRankXp();
        String oldTier = TownQuestBoardRank.tierIdForXp(oldXp, catalog);
        String newTier = TownQuestBoardRank.tierIdForXp(newXp, catalog);

        slot.markCompleted();
        tm.updateTown(town);

        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.completedToast")
                    .param("name", completedName)
            );
            if (!oldTier.equalsIgnoreCase(newTier)) {
                pr.sendMessage(
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rankUpToast")
                        .param("rank", newTier)
                );
            }
        }
        return true;
    }

    private static void grantRewards(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull Player player,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store
    ) {
        for (QuestReward r : slot.rewardsOrEmpty()) {
            if (r.kind() == null) {
                continue;
            }
            if ("item".equalsIgnoreCase(r.kind().trim())) {
                String itemId = r.itemId();
                if (itemId != null && !itemId.isBlank()) {
                    player.giveItem(new ItemStack(itemId.trim(), Math.max(1, r.count())), playerRef, store);
                }
            }
        }
    }

    public static void failExpiredQuest(
        @Nonnull TownRecord town,
        @Nonnull TownManager tm,
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull Random rng,
        @Nullable PlayerRef notifyPlayer
    ) {
        if (!slot.isAccepted()) {
            return;
        }
        int slotIndex = slotIndexForInstanceId(town, slot.instanceIdOrEmpty());
        slot.clearToEmpty();
        if (slotIndex >= 0) {
            generateOffer(town, store, catalog, slotIndex, rng);
        }
        tm.updateTown(town);
        if (notifyPlayer != null) {
            notifyPlayer.sendMessage(Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.failedToast"));
        }
    }

    @Nonnull
    public static Message displayTitle(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    ) {
        QuestBoardQuestTypeHandler handler = handlerFor(slot.getQuestType());
        if (handler != null) {
            return handler.displayTitle(slot, town, store, catalog);
        }
        return Message.raw("");
    }

    @Nonnull
    public static Message displayDescription(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    ) {
        QuestBoardQuestTypeHandler handler = handlerFor(slot.getQuestType());
        if (handler != null) {
            return handler.displayDescription(slot, town, store, catalog);
        }
        return Message.raw("");
    }

    @Nonnull
    public static Message objectivesText(
        @Nonnull QuestBoardSlotRecord slot,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog
    ) {
        QuestBoardQuestTypeHandler handler = handlerFor(slot.getQuestType());
        if (handler != null) {
            return handler.objectivesText(slot, town, store, catalog);
        }
        return Message.raw("");
    }

    public static int daysRemaining(@Nonnull QuestBoardSlotRecord slot) {
        return Math.max(0, slot.getDaysLimit() - slot.getOnlineDaysElapsed());
    }

    @Nullable
    public static QuestReward firstItemReward(@Nonnull QuestBoardSlotRecord slot) {
        for (QuestReward r : slot.rewardsOrEmpty()) {
            if (r.kind() != null && "item".equalsIgnoreCase(r.kind().trim()) && r.itemId() != null && !r.itemId().isBlank()) {
                return r;
            }
        }
        return null;
    }
}
