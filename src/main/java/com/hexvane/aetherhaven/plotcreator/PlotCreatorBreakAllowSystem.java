package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

/** Allows breaking Aetherhaven special blocks inside an active plot creator bounds session. */
public final class PlotCreatorBreakAllowSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private final AetherhavenPlugin plugin;

    public PlotCreatorBreakAllowSystem(@Nonnull AetherhavenPlugin plugin) {
        super(BreakBlockEvent.class);
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), UUIDComponent.getComponentType());
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull BreakBlockEvent event
    ) {
        if (event.isCancelled()) {
            return;
        }
        UUIDComponent uc = archetypeChunk.getComponent(index, UUIDComponent.getComponentType());
        if (uc == null) {
            return;
        }
        PlotCreatorSession session = PlotCreatorSessions.get(uc.getUuid());
        if (session == null) {
            return;
        }
        String blockId = event.getBlockType().getId();
        if (!PlotBuildingKindRequirements.isSpecialBlockType(blockId)) {
            return;
        }
        Vector3i pos = event.getTargetBlock();
        if (!session.getDraft().isInsideBounds(pos)) {
            return;
        }
        if (event.isCancelled()) {
            event.setCancelled(false);
        }
    }
}
