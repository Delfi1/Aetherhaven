package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.shopspot.ShopLootFiles;
import com.hexvane.aetherhaven.shopspot.ShopSpotBlockUtil;
import com.hexvane.aetherhaven.shopspot.ShopSpotDailyRerollService;
import com.hexvane.aetherhaven.shopspot.ShopSpotDisplayService;
import com.hexvane.aetherhaven.shopspot.ShopSpotPersistence;
import com.hexvane.aetherhaven.shopspot.ShopSpotPlayerComponent;
import com.hexvane.aetherhaven.shopspot.ShopSpotRecord;
import com.hexvane.aetherhaven.shopspot.ShopSpotRegistry;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class ShopSpotConfigPage extends AetherhavenInteractiveCustomUIPage<ShopSpotConfigPage.PageData> {
    private static final String MSG = "aetherhaven_shop.aetherhaven.shop.config";

    private boolean templateAppended;
    private boolean playerControlled;
    private String lootTableId = "";

    public ShopSpotConfigPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("Aetherhaven/ShopSpotConfigPage.ui");
            templateAppended = true;
            wireEvents(eventBuilder);
        }
        applyLabels(commandBuilder);
        applyLootDropdown(commandBuilder);
        applyState(commandBuilder);
    }

    private void wireEvents(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#PlayerControlledToggle",
            EventData.of("@PlayerControlled", "#PlayerControlledToggle.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#LootTableDropdown",
            EventData.of("@LootTable", "#LootTableDropdown.Value"),
            false
        );
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn", EventData.of("Action", "Confirm"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn", EventData.of("Action", "Cancel"), false);
    }

    private void applyLabels(@Nonnull UICommandBuilder b) {
        b.set("#ShopSpotConfigTitle.TextSpans", Message.translation(MSG + ".title"));
        b.set("#IntroHint.TextSpans", Message.translation(MSG + ".intro"));
        b.set("#PlayerControlledLabel.TextSpans", Message.translation(MSG + ".playerControlled"));
        b.set("#NpcShopHeading.TextSpans", Message.translation(MSG + ".npcHeading"));
        b.set("#LootHeader.TextSpans", Message.translation(MSG + ".lootHeader"));
        b.set("#LootTableLabel.TextSpans", Message.translation(MSG + ".lootTableLabel"));
        b.set("#LootStockHint.TextSpans", Message.translation(MSG + ".lootStockHint"));
        b.set("#ConfirmBtn.TextSpans", Message.translation(MSG + ".confirm"));
        b.set("#CancelBtn.TextSpans", Message.translation(MSG + ".cancel"));
    }

    private void applyLootDropdown(@Nonnull UICommandBuilder b) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        ObjectArrayList<DropdownEntryInfo> entries = new ObjectArrayList<>();
        if (plugin != null) {
            List<String> ids = ShopLootFiles.listLootTableIds(plugin);
            for (String id : ids) {
                entries.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
            }
        }
        b.set("#LootTableDropdown.Entries", entries);
        b.set("#LootTableDropdown.Value", lootTableId);
    }

    private void applyState(@Nonnull UICommandBuilder b) {
        b.set("#PlayerControlledToggle.Value", playerControlled);
        b.set("#NpcShopPanel.Visible", !playerControlled);
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PageData data
    ) {
        if (data.playerControlled != null) {
            playerControlled = data.playerControlled;
        }
        if (data.lootTable != null && !data.lootTable.isBlank()) {
            lootTableId = data.lootTable.trim();
        }
        if (data.action != null) {
            if ("Confirm".equals(data.action)) {
                confirm(ref, store);
                return;
            }
            if ("Cancel".equals(data.action)) {
                cancel(ref, store);
                return;
            }
        }
        UICommandBuilder b = new UICommandBuilder();
        applyState(b);
        sendUpdate(b, null, false);
    }

    private void confirm(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        ShopSpotPlayerComponent st = store.getComponent(ref, ShopSpotPlayerComponent.getComponentType());
        if (plugin == null || st == null) {
            close();
            return;
        }
        UUID spotId = st.getPendingSpotId();
        Vector3i block = st.getPendingBlock();
        if (spotId == null || block == null) {
            close();
            return;
        }
        World world = store.getExternalData().getWorld();
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = registry.get(spotId);
        if (record == null) {
            close();
            return;
        }
        record.setPlayerControlled(playerControlled);
        if (!playerControlled) {
            record.setLootTableId(lootTableId.trim());
            long epochDay = Long.MIN_VALUE;
            WorldTimeResource wtr = store.getResource(WorldTimeResource.getResourceType());
            if (wtr != null) {
                epochDay = wtr.getGameDateTime().toLocalDate().toEpochDay();
            }
            ShopSpotDailyRerollService.initialRollIfNeeded(record, plugin, epochDay);
        } else {
            record.setItemId(null);
            record.setStock(0);
            record.setSellerUuid(null);
        }
        registry.put(record);
        ShopSpotBlockUtil.syncConfigToBlock(world, block, record);
        ShopSpotPersistence.save(world, plugin, registry);
        TownRecord town = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin).getTown(record.getTownId());
        if (town != null) {
            ShopSpotDisplayService.syncDisplay(world, store, plugin, registry, record, town);
        }
        st.clearPendingPlacement();
        close();
        NotificationUtil.sendNotification(
            playerRef.getPacketHandler(),
            Message.translation(MSG + ".saved"),
            NotificationStyle.Success
        );
    }

    private void cancel(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        ShopSpotPlayerComponent st = store.getComponent(ref, ShopSpotPlayerComponent.getComponentType());
        if (plugin == null || st == null) {
            close();
            return;
        }
        UUID spotId = st.getPendingSpotId();
        Vector3i block = st.getPendingBlock();
        if (spotId != null && block != null) {
            World world = store.getExternalData().getWorld();
            ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
            ShopSpotRecord record = registry.get(spotId);
            if (record != null) {
                ShopSpotDisplayService.removeDisplay(world, store, plugin, registry, record);
                registry.remove(spotId);
                ShopSpotPersistence.save(world, plugin, registry);
            }
            ShopSpotBlockUtil.breakBlock(world, block);
        }
        st.clearPendingPlacement();
        close();
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC =
            BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(new KeyedCodec<>("@PlayerControlled", Codec.BOOLEAN), (d, v) -> d.playerControlled = v, d -> d.playerControlled)
                .add()
                .append(new KeyedCodec<>("@LootTable", Codec.STRING), (d, v) -> d.lootTable = v, d -> d.lootTable)
                .add()
                .build();

        @Nullable
        private String action;
        @Nullable
        private Boolean playerControlled;
        @Nullable
        private String lootTable;
    }
}
