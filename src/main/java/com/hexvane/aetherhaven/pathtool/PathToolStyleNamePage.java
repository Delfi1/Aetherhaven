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

/** Name prompt before opening the chest for a new path style. */
public final class PathToolStyleNamePage extends AetherhavenInteractiveCustomUIPage<PathToolStyleNamePage.PageData> {
    private static final int MAX_NAME_LENGTH = 48;

    private boolean templateAppended;

    public PathToolStyleNamePage(@Nonnull PlayerRef playerRef) {
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
            commandBuilder.append("Aetherhaven/PathToolStyleNamePage.ui");
            templateAppended = true;
        }
        AetherhavenUiLocalization.applyPathToolStyleNamePage(commandBuilder);
        UUID playerId = playerUuid(ref, store);
        if (playerId == null) {
            return;
        }
        PathToolStyleSessions.Session session = PathToolStyleSessions.get(playerId);
        if (session == null) {
            commandBuilder.set(
                "#Hint.TextSpans",
                Message.translation("aetherhaven_items.aetherhaven.pathTool.styleEditNoSession")
            );
            return;
        }
        commandBuilder.set("#StyleNameInput.Value", session.styleName);
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ContinueButton",
            new EventData().append("Action", "Continue").append("@StyleName", "#StyleNameInput.Value"),
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
        if ("Cancel".equalsIgnoreCase(data.action)) {
            PathToolStyleUi.cancelAll(ref, store, playerRef);
            close();
            return;
        }
        if (!"Continue".equalsIgnoreCase(data.action)) {
            return;
        }
        UUID playerId = playerUuid(ref, store);
        if (playerId == null || PathToolStyleSessions.get(playerId) == null) {
            close();
            return;
        }
        String name = sanitizeName(data.styleName, "New path");
        PathToolStyleUi.continueFromNamePage(ref, store, playerRef, name);
    }

    @Nonnull
    private static String sanitizeName(@Nullable String raw, @Nonnull String fallback) {
        String s = raw != null ? raw.trim() : "";
        if (s.isEmpty()) {
            s = fallback;
        }
        if (s.length() > MAX_NAME_LENGTH) {
            s = s.substring(0, MAX_NAME_LENGTH);
        }
        return s;
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
            .append(new KeyedCodec<>("@StyleName", Codec.STRING), (d, v) -> d.styleName = v, d -> d.styleName)
            .add()
            .build();

        @Nullable
        String action;
        @Nullable
        String styleName;
    }
}
