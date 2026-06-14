package com.hexvane.aetherhaven.patrol;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownRecord;
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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Prompt to name a patrol route before saving. */
public final class PatrolWandNameRoutePage extends AetherhavenInteractiveCustomUIPage<PatrolWandNameRoutePage.PageData> {
    private static final int MAX_NAME_LENGTH = 48;

    private boolean templateAppended;

    public PatrolWandNameRoutePage(@Nonnull PlayerRef playerRef) {
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
            commandBuilder.append("Aetherhaven/PatrolWandNameRoutePage.ui");
            templateAppended = true;
        }
        AetherhavenUiLocalization.applyPatrolWandNameRoutePage(commandBuilder);

        PatrolWandPlayerComponent st = store.getComponent(ref, PatrolWandPlayerComponent.getComponentType());
        if (st == null || st.getDraftNodes().size() < 2) {
            commandBuilder.set(
                "#Hint.TextSpans",
                Message.translation("aetherhaven_items.aetherhaven.patrolWand.needTwoNodes")
            );
            return;
        }
        commandBuilder.set(
            "#Hint.TextSpans",
            Message
                .translation("aetherhaven_items.aetherhaven.patrolWand.nameRouteHint")
                .param("n", String.valueOf(st.getDraftNodes().size()))
        );
        commandBuilder.set("#RouteNameInput.Value", defaultRouteName(store, ref, st));

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SaveRouteNameButton",
            new EventData().append("Action", "Save").append("@RouteName", "#RouteNameInput.Value"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelRouteNameButton",
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
            close();
            return;
        }
        if (!"Save".equalsIgnoreCase(data.action)) {
            return;
        }
        PatrolWandPlayerComponent st = store.getComponent(ref, PatrolWandPlayerComponent.getComponentType());
        if (st == null || st.getDraftNodes().size() < 2) {
            close();
            return;
        }
        String name = sanitizeRouteName(data.routeName, defaultRouteName(store, ref, st));
        UUID routeId = PatrolWandInteractions.commitSaveRoute(ref, store, name);
        if (routeId == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            PatrolWandInteractions.openAssignGuardPageFromStore(ref, store);
        }
    }

    @Nonnull
    private static String defaultRouteName(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PatrolWandPlayerComponent st
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        World world = store.getExternalData().getWorld();
        if (plugin == null) {
            return "Patrol";
        }
        TownRecord town = PatrolWandInteractions.resolveTownAtPlayerForUi(world, plugin, store, ref);
        if (town == null) {
            return "Patrol";
        }
        PatrolRouteRegistry reg = AetherhavenWorldRegistries.getOrCreatePatrolRouteRegistry(world, plugin);
        UUID editId = st.getEditingRouteId();
        if (editId != null) {
            PatrolRouteRecord existing = reg.get(editId);
            if (existing != null && existing.displayName != null && !existing.displayName.isBlank()) {
                return existing.displayName.trim();
            }
        }
        return reg.nextDisplayName(town.getTownId());
    }

    @Nonnull
    static String sanitizeRouteName(@Nullable String raw, @Nonnull String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            return trimmed.substring(0, MAX_NAME_LENGTH);
        }
        return trimmed;
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("@RouteName", Codec.STRING), (d, v) -> d.routeName = v, d -> d.routeName)
            .add()
            .build();

        @Nullable
        private String action;

        @Nullable
        private String routeName;
    }
}
