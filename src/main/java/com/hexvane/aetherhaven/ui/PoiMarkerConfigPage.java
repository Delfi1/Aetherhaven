package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.poi.PoiInteractionKind;
import com.hexvane.aetherhaven.poi.marker.PoiMarkerPlacementService;
import com.hexvane.aetherhaven.poi.tool.PoiToolPlayerComponent;
import com.hexvane.aetherhaven.poi.tool.PoiToolVisualizationSystem;
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
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

public final class PoiMarkerConfigPage extends AetherhavenInteractiveCustomUIPage<PoiMarkerConfigPage.PageData> {
    private static final String MSG = "aetherhaven_world_debug.aetherhaven.poiMarkerConfig";

    private boolean templateAppended;
    private String preset = "Sleep";
    private int capacity = 1;
    private boolean mountOnUse = true;
    private String workAction = "None";

    public PoiMarkerConfigPage(@Nonnull PlayerRef playerRef) {
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
            commandBuilder.append("Aetherhaven/PoiMarkerConfigPage.ui");
            templateAppended = true;
            wireEvents(eventBuilder);
        }
        applyStaticLabels(commandBuilder);
        applyDynamicState(commandBuilder);
    }

    private void wireEvents(@Nonnull UIEventBuilder eventBuilder) {
        bindPreset(eventBuilder, "#PresetSleep", "Sleep");
        bindPreset(eventBuilder, "#PresetEat", "Eat");
        bindPreset(eventBuilder, "#PresetSit", "Sit");
        bindPreset(eventBuilder, "#PresetWork", "Work");
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#CapacitySlider",
            EventData.of("@Capacity", "#CapacitySlider.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#MountToggle",
            EventData.of("@MountOnUse", "#MountToggle.Value"),
            false
        );
        bindWork(eventBuilder, "#WorkNone", "None");
        bindWork(eventBuilder, "#WorkMine", "Mine");
        bindWork(eventBuilder, "#WorkChop", "Chop");
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn", EventData.of("Action", "Confirm"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn", EventData.of("Action", "Cancel"), false);
    }

    private static void bindPreset(@Nonnull UIEventBuilder eventBuilder, @Nonnull String selector, @Nonnull String preset) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "Preset:" + preset), false);
    }

    private static void bindWork(@Nonnull UIEventBuilder eventBuilder, @Nonnull String selector, @Nonnull String action) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "Work:" + action), false);
    }

    private void applyStaticLabels(@Nonnull UICommandBuilder b) {
        b.set("#PoiMarkerConfigTitle.TextSpans", Message.translation(MSG + ".title"));
        b.set("#NeedHeader.TextSpans", Message.translation(MSG + ".needHeader"));
        b.set("#PresetSleep.TextSpans", Message.translation(MSG + ".presetSleep"));
        b.set("#PresetEat.TextSpans", Message.translation(MSG + ".presetEat"));
        b.set("#PresetSit.TextSpans", Message.translation(MSG + ".presetSit"));
        b.set("#PresetWork.TextSpans", Message.translation(MSG + ".presetWork"));
        b.set("#CapacityLabel.TextSpans", Message.translation(MSG + ".capacity"));
        b.set("#MountToggleLabel.TextSpans", Message.translation(MSG + ".mountToggle"));
        b.set("#WorkActionLabel.TextSpans", Message.translation(MSG + ".workAction"));
        b.set("#WorkNone.TextSpans", Message.translation(MSG + ".workNone"));
        b.set("#WorkMine.TextSpans", Message.translation(MSG + ".workMine"));
        b.set("#WorkChop.TextSpans", Message.translation(MSG + ".workChop"));
        b.set("#ConfirmBtn.TextSpans", Message.translation(MSG + ".confirm"));
        b.set("#CancelBtn.TextSpans", Message.translation(MSG + ".cancel"));
    }

    private void applyDynamicState(@Nonnull UICommandBuilder b) {
        b.set("#CapacitySlider.Value", capacity);
        b.set("#MountToggle.Value", mountOnUse);
        boolean work = "Work".equals(preset);
        b.set("#WorkActionLabel.Visible", work);
        b.set("#WorkActionRow.Visible", work);
        b.set(
            "#PresetSummary.TextSpans",
            Message.translation(MSG + ".summary")
                .param("preset", preset)
                .param("kind", PoiMarkerPlacementService.kindForPreset(preset).name())
        );
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PageData data
    ) {
        if (data.capacity != null) {
            capacity = Math.max(1, Math.min(4, data.capacity.intValue()));
        }
        if (data.mountOnUse != null) {
            mountOnUse = data.mountOnUse;
        }
        if (data.action != null) {
            if (data.action.startsWith("Preset:")) {
                preset = data.action.substring("Preset:".length());
                PoiInteractionKind kind = PoiMarkerPlacementService.kindForPreset(preset);
                mountOnUse = kind == PoiInteractionKind.SIT || kind == PoiInteractionKind.SLEEP;
            } else if (data.action.startsWith("Work:")) {
                workAction = data.action.substring("Work:".length());
            } else if ("Confirm".equals(data.action)) {
                confirm(ref, store);
                return;
            } else if ("Cancel".equals(data.action)) {
                clearPending(ref, store);
                close();
                return;
            }
        }
        UICommandBuilder b = new UICommandBuilder();
        applyDynamicState(b);
        sendUpdate(b, null, false);
    }

    private void confirm(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        PoiToolPlayerComponent toolState = store.getComponent(ref, PoiToolPlayerComponent.getComponentType());
        if (plugin == null || toolState == null) {
            close();
            return;
        }
        Vector3i block = toolState.getPendingPlacementBlock();
        UUID townId = toolState.getPendingTownId();
        UUID plotId = toolState.getPendingPlotId();
        if (block == null || townId == null || plotId == null) {
            close();
            return;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.getTown(townId);
        if (town == null) {
            close();
            return;
        }
        Set<String> tags = new HashSet<>(PoiMarkerPlacementService.tagsForPreset(preset));
        PoiInteractionKind kind = PoiMarkerPlacementService.kindForPreset(preset);
        String equipment =
            kind == PoiInteractionKind.WORK_SURFACE ? PoiMarkerPlacementService.equipmentForWorkAction(workAction) : null;
        Ref<EntityStore> spawned =
            PoiMarkerPlacementService.placeMarkerFromPlayer(
                ref,
                world,
                plugin,
                town,
                plotId,
                block,
                tags,
                capacity,
                kind,
                mountOnUse,
                equipment,
                store
            );
        toolState.clearPendingPlacement();
        close();
        if (spawned != null && spawned.isValid()) {
            PoiToolVisualizationSystem.scheduleRefreshForPlayer(world, playerRef.getUuid(), plugin);
            NotificationUtil.sendNotification(
                playerRef.getPacketHandler(),
                Message.translation(MSG + ".placed"),
                NotificationStyle.Success
            );
        }
    }

    private void clearPending(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PoiToolPlayerComponent toolState = store.getComponent(ref, PoiToolPlayerComponent.getComponentType());
        if (toolState != null) {
            toolState.clearPendingPlacement();
        }
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC =
            BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(new KeyedCodec<>("@Capacity", Codec.DOUBLE), (d, v) -> d.capacity = v, d -> d.capacity)
                .add()
                .append(new KeyedCodec<>("@MountOnUse", Codec.BOOLEAN), (d, v) -> d.mountOnUse = v, d -> d.mountOnUse)
                .add()
                .build();

        @Nullable
        private String action;
        @Nullable
        private Double capacity;
        @Nullable
        private Boolean mountOnUse;
    }
}
