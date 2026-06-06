package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.quest.data.QuestReward;
import com.hexvane.aetherhaven.questboard.QuestBoardCatalog;
import com.hexvane.aetherhaven.questboard.QuestBoardDrawPool;
import com.hexvane.aetherhaven.questboard.QuestBoardGiverDisplay;
import com.hexvane.aetherhaven.questboard.QuestBoardItemRequirement;
import com.hexvane.aetherhaven.questboard.QuestBoardService;
import com.hexvane.aetherhaven.questboard.QuestBoardSlotRecord;
import com.hexvane.aetherhaven.questboard.QuestBoardSlotState;
import com.hexvane.aetherhaven.questboard.TownQuestBoardRank;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardPage extends AetherhavenInteractiveCustomUIPage<QuestBoardPage.PageData> {
    private static final String CARD_ROW = "#QuestBoardRoot #Content #CardRowScroll #CardRow";
    private static final int MAX_ITEMS = 6;

    /**
     * {@code append(ui)} must run only once per page instance; repeating it on every {@link #sendUpdate} duplicates the
     * whole tree and breaks card selectors.
     */
    private boolean templateAppended;

    public QuestBoardPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("Aetherhaven/QuestBoardPage.ui");
            templateAppended = true;
        }
        AetherhavenUiLocalization.applyQuestBoardPage(commandBuilder);
        populateContent(ref, commandBuilder, eventBuilder, store);
    }

    private void populateContent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        World world = store.getExternalData().getWorld();
        if (plugin == null || uc == null) {
            showBlocked(commandBuilder, Message.translation("aetherhaven_common.aetherhaven.common.pluginNotLoaded"));
            return;
        }

        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null) {
            showBlocked(commandBuilder, Message.translation("aetherhaven_ui_shell.aetherhaven.ui.questJournal.needTown"));
            return;
        }
        if (!town.playerHasQuestPermission(uc.getUuid())) {
            showBlocked(commandBuilder, Message.translation("aetherhaven_ui_shell.aetherhaven.ui.questJournal.noPermission"));
            return;
        }

        QuestBoardCatalog catalog = plugin.getQuestBoardCatalog();
        Random rng = new Random(town.getTownId().hashCode() ^ System.nanoTime());
        QuestBoardService.ensureBoardInitialized(town, store, catalog, rng);
        tm.updateTown(town);

        commandBuilder.set("#BlockedMessage.Visible", false);
        commandBuilder.set("#RankBar.Visible", true);
        commandBuilder.set("#CardRowScroll.Visible", true);

        applyRankBar(commandBuilder, town, catalog);
        applyCards(commandBuilder, eventBuilder, town, store, catalog, uc.getUuid());
    }

    private static void showBlocked(@Nonnull UICommandBuilder commandBuilder, @Nonnull Message msg) {
        commandBuilder.set("#BlockedMessage.Visible", true);
        commandBuilder.set("#BlockedMessage.TextSpans", msg);
        commandBuilder.set("#RankBar.Visible", false);
        commandBuilder.set("#CardRowScroll.Visible", false);
    }

    private static void applyRankBar(@Nonnull UICommandBuilder cmd, @Nonnull TownRecord town, @Nonnull QuestBoardCatalog catalog) {
        int xp = town.getQuestBoardRankXp();
        String tier = TownQuestBoardRank.tierIdForXp(xp, catalog);
        cmd.set("#TownRankIcon.AssetPath", TownQuestBoardRank.iconPathForRank(tier, catalog));
        cmd.set(
            "#TownRankLabel.TextSpans",
            Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.townRankLabel")
        );
        QuestBoardRankBarUi.apply(cmd, xp, catalog);
    }

    private static void applyCards(
        @Nonnull UICommandBuilder cmd,
        @Nonnull UIEventBuilder evt,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull QuestBoardCatalog catalog,
        @Nonnull UUID playerUuid
    ) {
        cmd.clear(CARD_ROW);
        List<QuestBoardSlotRecord> slots = town.getQuestBoardSlots();
        int n = Math.min(slots.size(), catalog.slotCount());
        for (int i = 0; i < n; i++) {
            QuestBoardSlotRecord slot = slots.get(i);
            cmd.append(CARD_ROW, "Aetherhaven/QuestBoardCard.ui");
            String card = CARD_ROW + "[" + i + "]";
            QuestBoardSlotState state = slot.stateEnum();

            if (state == QuestBoardSlotState.EMPTY) {
                cmd.set(card + " #EmptyHint.Visible", true);
                cmd.set(card + " #CardInner.Visible", false);
                cmd.set(
                    card + " #EmptyHint.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.emptySlot")
                );
                continue;
            }

            cmd.set(card + " #EmptyHint.Visible", false);
            cmd.set(card + " #CardInner.Visible", true);

            if (state == QuestBoardSlotState.COMPLETED) {
                cmd.set(card + " #CardBody.Visible", false);
                cmd.set(card + " #CardFooter.Visible", false);
                cmd.set(card + " #CompletedOnly.Visible", true);
                cmd.set(
                    card + " #CompletedBanner.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.completedBanner")
                        .insert(Message.raw("\n"))
                        .insert(Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.completedRefreshHint"))
                );
                continue;
            }

            cmd.set(card + " #CompletedOnly.Visible", false);
            cmd.set(card + " #CardBody.Visible", true);
            cmd.set(card + " #CardFooter.Visible", true);
            cmd.set(card + " #CardHeader.Visible", true);
            cmd.set(card + " #QuestDescription.Visible", true);
            cmd.set(card + " #RequestedLabel.Visible", true);
            cmd.set(card + " #ItemRow.Visible", true);
            cmd.set(card + " #RewardLabelHeader.Visible", true);
            cmd.set(card + " #DaysLeft.Visible", true);
            cmd.set(card + " #ActionButton.Visible", true);
            cmd.set(
                card + " #RequestedLabel.TextSpans",
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.requestedLabel")
            );
            cmd.set(
                card + " #RewardLabelHeader.TextSpans",
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rewardLabel")
            );

            String questRank = slot.getQuestRank() != null ? slot.getQuestRank() : "E";
            cmd.set(card + " #QuestRankIcon.AssetPath", TownQuestBoardRank.iconPathForRank(questRank, catalog));
            cmd.set(card + " #Portrait.AssetPath", QuestBoardGiverDisplay.portraitPath(slot, store));
            cmd.set(card + " #QuestTitle.TextSpans", QuestBoardService.displayTitle(slot, town, store, catalog));
            cmd.set(card + " #QuestDescription.TextSpans", QuestBoardService.displayDescription(slot, town, store, catalog));

            applyItemRow(cmd, card, slot);
            applyRewardRow(cmd, card, slot);

            if (state == QuestBoardSlotState.ACCEPTED) {
                cmd.set(card + " #DaysLeft.Visible", true);
                int days = QuestBoardService.daysRemaining(slot);
                cmd.set(
                    card + " #DaysLeft.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.daysLeft").param("days", String.valueOf(days))
                );
                cmd.set(
                    card + " #ActionButton.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.abandonButton")
                );
                if (town.playerCanAbandonQuests(playerUuid)) {
                    evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        card + " #ActionButton",
                        new EventData().append("Action", "AbandonSlot").append("SlotIndex", String.valueOf(i)),
                        false
                    );
                } else {
                    cmd.set(card + " #ActionButton.Disabled", true);
                }
            } else {
                cmd.set(card + " #DaysLeft.Visible", true);
                cmd.set(
                    card + " #DaysLeft.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.daysLimit")
                        .param("days", String.valueOf(slot.getDaysLimit()))
                );
                cmd.set(
                    card + " #ActionButton.TextSpans",
                    Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.acceptButton")
                );
                cmd.set(card + " #ActionButton.Disabled", false);
                if (town.playerCanAcceptQuests(playerUuid)) {
                    evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        card + " #ActionButton",
                        new EventData().append("Action", "AcceptSlot").append("SlotIndex", String.valueOf(i)),
                        false
                    );
                } else {
                    cmd.set(card + " #ActionButton.Disabled", true);
                }
            }
        }
    }

    private static void applyItemRow(@Nonnull UICommandBuilder cmd, @Nonnull String card, @Nonnull QuestBoardSlotRecord slot) {
        cmd.clear(card + " #ItemRow");
        List<QuestBoardItemRequirement> items = slot.requiredItemsOrEmpty();
        int count = Math.min(items.size(), MAX_ITEMS);
        for (int j = 0; j < count; j++) {
            QuestBoardItemRequirement req = items.get(j);
            cmd.append(card + " #ItemRow", "Aetherhaven/QuestBoardItemSlot.ui");
            String itemSel = card + " #ItemRow[" + j + "]";
            cmd.set(itemSel + " #ItemIcon.ItemId", req.itemIdOrEmpty());
            cmd.set(itemSel + " #ItemCount.TextSpans", Message.raw("x" + req.count()));
        }
    }

    private static void applyRewardRow(@Nonnull UICommandBuilder cmd, @Nonnull String card, @Nonnull QuestBoardSlotRecord slot) {
        QuestReward rw = QuestBoardService.firstItemReward(slot);
        if (rw == null || rw.itemId() == null || rw.itemId().isBlank()) {
            cmd.set(card + " #RewardRow.Visible", false);
            return;
        }
        cmd.set(card + " #RewardRow.Visible", true);
        cmd.set(card + " #RewardSlot.ItemId", rw.itemId().trim());
        Item asset = Item.getAssetMap().getAsset(rw.itemId().trim());
        if (asset != null && asset.getTranslationKey() != null && !asset.getTranslationKey().isBlank()) {
            cmd.set(
                card + " #RewardLabel.TextSpans",
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rewardLine")
                    .param("count", String.valueOf(Math.max(1, rw.count())))
                    .param("item", Message.translation(asset.getTranslationKey()))
            );
        } else {
            cmd.set(
                card + " #RewardLabel.TextSpans",
                Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.rewardLine")
                    .param("count", String.valueOf(Math.max(1, rw.count())))
                    .param("item", rw.itemId().trim())
            );
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null || data.action.isBlank()) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (plugin == null || uc == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null) {
            return;
        }
        QuestBoardCatalog catalog = plugin.getQuestBoardCatalog();
        int slotIndex = parseSlotIndex(data.slotIndex);

        if ("AcceptSlot".equalsIgnoreCase(data.action)) {
            if (QuestBoardService.acceptOffer(town, uc.getUuid(), slotIndex, catalog)) {
                tm.updateTown(town);
                PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                if (pr != null) {
                    pr.sendMessage(Message.translation("aetherhaven_ui_quest_board.aetherhaven.ui.questBoard.acceptedToast"));
                }
            }
        } else if ("AbandonSlot".equalsIgnoreCase(data.action)) {
            if (QuestBoardService.abandonOffer(town, uc.getUuid(), slotIndex, catalog)) {
                Random rng = new Random(town.getTownId().hashCode() ^ slotIndex);
                Set<String> exclude = QuestBoardDrawPool.occupiedBoardEntryKeys(town);
                QuestBoardService.generateOffer(town, store, catalog, slotIndex, rng, exclude);
                tm.updateTown(town);
            }
        } else {
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        populateContent(ref, cmd, evt, store);
        sendUpdate(cmd, evt, false);
    }

    private static int parseSlotIndex(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC =
            BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(new KeyedCodec<>("SlotIndex", Codec.STRING), (d, v) -> d.slotIndex = v, d -> d.slotIndex)
                .add()
                .build();

        @Nullable
        private String action;
        @Nullable
        private String slotIndex;
    }
}
