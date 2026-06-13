package com.hexvane.aetherhaven.pathtool;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.config.PathToolStyleDefinition;
import com.hexvane.aetherhaven.ui.AetherhavenInteractiveCustomUIPage;
import com.hexvane.aetherhaven.ui.AetherhavenUiLocalization;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Scrollable list of path styles with create, edit, and cancel. */
public final class PathToolStyleListPage extends AetherhavenInteractiveCustomUIPage<PathToolStyleListPage.PageData> {
    private static final String ROWS = "#ListScroll #Rows";

    private boolean templateAppended;

    public PathToolStyleListPage(@Nonnull PlayerRef playerRef) {
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
            commandBuilder.append("Aetherhaven/PathToolStyleListPage.ui");
            AetherhavenUiLocalization.applyPathToolStyleListPage(commandBuilder);
            templateAppended = true;
        }
        UUID playerId = playerUuid(ref, store);
        if (playerId == null) {
            return;
        }
        PathToolStyleSessions.Session session = PathToolStyleSessions.getOrCreate(playerId);
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        commandBuilder.clear(ROWS);
        if (plugin == null) {
            commandBuilder.set("#Hint.TextSpans", Message.translation("aetherhaven_common.aetherhaven.common.pluginNotLoaded"));
            return;
        }
        List<PathToolStyleDefinition> styles = plugin.getConfig().get().getPathToolStyleDefinitions();
        if (session.listSelectedIndex >= styles.size()) {
            session.listSelectedIndex = Math.max(0, styles.size() - 1);
        }
        if (styles.isEmpty()) {
            commandBuilder.set(
                "#Hint.TextSpans",
                Message.translation("aetherhaven_items.aetherhaven.pathTool.styleListEmpty")
            );
        } else {
            PathToolStyleDefinition selected = styles.get(session.listSelectedIndex);
            commandBuilder.set(
                "#Hint.TextSpans",
                Message
                    .translation("aetherhaven_items.aetherhaven.pathTool.styleListSelected")
                    .param("name", selected.getName())
            );
        }
        for (int i = 0; i < styles.size(); i++) {
            commandBuilder.append(ROWS, "Aetherhaven/PathToolStyleListRow.ui");
            String row = ROWS + "[" + i + "]";
            boolean selected = i == session.listSelectedIndex;
            commandBuilder.set(row + " #SelectHilite.Visible", selected);
            commandBuilder.set(row + " #SelectedMark.Visible", selected);
            if (selected) {
                commandBuilder.set(
                    row + " #SelectedMark.TextSpans",
                    Message.translation("aetherhaven_items.aetherhaven.pathTool.styleListRowSelected")
                );
            }
            commandBuilder.set(
                row + " #Select #StyleName.TextSpans",
                Message.raw(styles.get(i).getName())
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                row + " #Select",
                new EventData().append("Action", "Select").append("Index", Integer.toString(i)),
                false
            );
        }
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CreateButton",
            new EventData().append("Action", "Create"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#EditButton",
            new EventData().append("Action", "Edit"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            new EventData().append("Action", "Cancel"),
            false
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) {
            return;
        }
        UUID playerId = playerUuid(ref, store);
        if (playerId == null) {
            close();
            return;
        }
        PathToolStyleSessions.Session session = PathToolStyleSessions.getOrCreate(playerId);
        switch (data.action) {
            case "Cancel" -> {
                PathToolStyleUi.cancelAll(ref, store, playerRef);
                close();
            }
            case "Select" -> {
                int idx = parseIndex(data.indexRaw);
                if (idx >= 0) {
                    session.listSelectedIndex = idx;
                    refreshList(ref, store);
                }
            }
            case "Create" -> PathToolStyleUi.beginCreate(ref, store, playerRef);
            case "Edit" -> {
                AetherhavenPlugin plugin = AetherhavenPlugin.get();
                if (plugin == null) {
                    return;
                }
                int n = plugin.getConfig().get().getPathToolStyleDefinitions().size();
                if (n <= 0 || session.listSelectedIndex < 0 || session.listSelectedIndex >= n) {
                    playerRef.sendMessage(Message.translation("aetherhaven_items.aetherhaven.pathTool.styleListPickOne"));
                    return;
                }
                PathToolStyleUi.beginEdit(ref, store, playerRef, session.listSelectedIndex);
            }
            default -> {}
        }
    }

    @Override
    protected void rebuild() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null) {
            refreshList(ref, ref.getStore());
        }
    }

    private void refreshList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder ev = new UIEventBuilder();
        build(ref, cmd, ev, store);
        sendUpdate(cmd, ev, false);
    }

    private static int parseIndex(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Nullable
    private static UUID playerUuid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("Index", Codec.STRING), (d, v) -> d.indexRaw = v, d -> d.indexRaw)
            .add()
            .build();

        @Nullable
        String action;
        @Nullable
        String indexRaw;
    }
}
