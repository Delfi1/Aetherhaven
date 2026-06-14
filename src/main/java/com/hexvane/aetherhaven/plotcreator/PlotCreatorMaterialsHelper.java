package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.construction.MaterialRequirement;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** Virtual chest inventory for the plot creator materials step (no world block). */
public final class PlotCreatorMaterialsHelper {
    /** Standard small chest slot count. */
    public static final short MATERIALS_CAPACITY = 27;

    private PlotCreatorMaterialsHelper() {}

    @Nonnull
    public static SimpleItemContainer ensureMaterialsContainer(@Nonnull PlotCreatorSession session) {
        SimpleItemContainer existing = session.getMaterialsContainer();
        if (existing != null) {
            return existing;
        }
        SimpleItemContainer created = new SimpleItemContainer(MATERIALS_CAPACITY);
        session.setMaterialsContainer(created);
        syncDraftIntoContainer(session);
        return created;
    }

    public static void syncDraftIntoContainer(@Nonnull PlotCreatorSession session) {
        SimpleItemContainer container = session.getMaterialsContainer();
        if (container == null) {
            return;
        }
        container.clear();
        PlotCreatorDraft draft = session.getDraft();
        for (MaterialRequirement req : draft.getMaterials()) {
            String itemId = req.getItemId();
            if (itemId == null || itemId.isBlank() || req.getResourceTypeId() != null) {
                continue;
            }
            container.addItemStack(new ItemStack(itemId.trim(), req.getCount()));
        }
    }

    public static void openMaterialsWindow(
        @Nonnull PlayerRef playerRef,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlotCreatorSession session
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        SimpleItemContainer container = ensureMaterialsContainer(session);
        ContainerWindow window = new ContainerWindow(container);
        player.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window);
    }

    public static void snapshotMaterials(@Nonnull PlotCreatorSession session) {
        SimpleItemContainer container = session.getMaterialsContainer();
        PlotCreatorDraft draft = session.getDraft();
        draft.getMaterials().clear();
        if (container == null) {
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        accumulateContainer(container, counts);
        for (var e : counts.entrySet()) {
            draft.getMaterials().add(MaterialRequirement.ofItem(e.getKey(), e.getValue()));
        }
    }

    private static void accumulateContainer(@Nonnull ItemContainer container, @Nonnull Map<String, Integer> counts) {
        for (short i = 0; i < container.getCapacity(); i++) {
            ItemStack stack = container.getItemStack(i);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            String id = stack.getItemId();
            if (id == null || id.isBlank()) {
                continue;
            }
            counts.merge(id, stack.getQuantity(), Integer::sum);
        }
    }

    /** Saves material counts to the draft and returns every item in the virtual chest to the player. */
    public static void snapshotAndReturnMaterials(
        @Nonnull PlotCreatorSession session,
        @Nonnull Player player,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store
    ) {
        snapshotMaterials(session);
        SimpleItemContainer container = session.getMaterialsContainer();
        if (container == null) {
            return;
        }
        for (short i = 0; i < container.getCapacity(); i++) {
            ItemStack stack = container.getItemStack(i);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            player.giveItem(stack, ref, store);
        }
        container.clear();
        player.getPageManager().setPage(ref, store, Page.None);
    }
}
