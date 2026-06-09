package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.rts.ui.RtsCommandHudSupport;
import com.hexvane.aetherhaven.rts.ui.RtsCommandStatusHud;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import javax.annotation.Nonnull;

public final class RtsHudRefreshSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(RtsCommandPlayerComponent.getComponentType(), Player.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        RtsCommandPlayerComponent session = chunk.getComponent(index, RtsCommandPlayerComponent.getComponentType());
        if (session == null || !session.isActive()) {
            return;
        }
        Player player = chunk.getComponent(index, Player.getComponentType());
        PlayerRef pr = chunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null || pr == null) {
            return;
        }
        Ref<EntityStore> playerRef = chunk.getReferenceTo(index);
        ItemStack hand = InventoryComponent.getItemInHand(store, playerRef);
        RtsCommandStatusHud hud = RtsCommandHudSupport.obtainHud(player, pr);
        hud.refresh(session, RtsInteractions.toolHelpKey(hand));
    }
}
