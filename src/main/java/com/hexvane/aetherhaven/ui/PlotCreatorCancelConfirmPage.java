package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.plotcreator.PlotCreatorService;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlotCreatorCancelConfirmPage extends AetherhavenInteractiveCustomUIPage<PlotCreatorCancelConfirmPage.PageData> {
    private static final String MSG = "aetherhaven_plot_creator.aetherhaven.plotcreator";

    private boolean templateAppended;

    public PlotCreatorCancelConfirmPage(@Nonnull PlayerRef playerRef) {
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
            commandBuilder.append("Aetherhaven/PlotCreatorCancelConfirmPage.ui");
            templateAppended = true;
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelConfirmYes",
                EventData.of("Action", "Confirm"),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelConfirmNo",
                EventData.of("Action", "Back"),
                false
            );
        }
        commandBuilder.set("#CancelConfirmTitle.TextSpans", Message.translation(MSG + ".cancelConfirm.title"));
        commandBuilder.set("#CancelConfirmText.TextSpans", Message.translation(MSG + ".cancelConfirm.body"));
        commandBuilder.set("#CancelConfirmYes.TextSpans", Message.translation(MSG + ".cancelConfirm.yes"));
        commandBuilder.set("#CancelConfirmNo.TextSpans", Message.translation(MSG + ".cancelConfirm.no"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if ("Confirm".equals(data.action)) {
            PlotCreatorService.cancelSession(playerRef, ref, store);
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }
        player.getPageManager().setPage(ref, store, Page.None);
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .build();

        @Nullable
        private String action;
    }
}
