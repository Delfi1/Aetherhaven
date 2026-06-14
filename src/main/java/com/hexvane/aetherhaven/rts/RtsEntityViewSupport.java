package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Expands entity view radius while commanding so guards below stay networked when zoomed out. */
public final class RtsEntityViewSupport {
    private RtsEntityViewSupport() {}

    public static void enter(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        EntityTrackerSystems.EntityViewer viewer =
            accessor.getComponent(playerRef, EntityTrackerSystems.EntityViewer.getComponentType());
        if (viewer == null) {
            return;
        }
        if (session.getSavedViewRadiusBlocks() <= 0) {
            session.setSavedViewRadiusBlocks(viewer.viewRadiusBlocks);
        }
        apply(session, viewer);
        accessor.putComponent(playerRef, RtsCommandPlayerComponent.getComponentType(), session);
    }

    public static void apply(
        @Nonnull RtsCommandPlayerComponent session,
        @Nonnull EntityTrackerSystems.EntityViewer viewer
    ) {
        int base = session.getSavedViewRadiusBlocks();
        if (base <= 0) {
            return;
        }
        viewer.viewRadiusBlocks = base + (int) Math.ceil(RtsScreenPickUtil.viewHeightAboveGround(session));
    }

    public static void restore(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        EntityTrackerSystems.EntityViewer viewer =
            accessor.getComponent(playerRef, EntityTrackerSystems.EntityViewer.getComponentType());
        if (viewer == null) {
            return;
        }
        int saved = session.getSavedViewRadiusBlocks();
        if (saved > 0) {
            viewer.viewRadiusBlocks = saved;
        }
    }
}
