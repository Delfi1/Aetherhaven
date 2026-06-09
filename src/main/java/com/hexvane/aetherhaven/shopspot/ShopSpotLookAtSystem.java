package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.joml.Vector3i;

public final class ShopSpotLookAtSystem extends EntityTickingSystem<EntityStore> {
    private static final double REACH = 5.0;
    private static final ConcurrentHashMap<UUID, Integer> LAST_SIG = new ConcurrentHashMap<>();

    @Nonnull
    private final Set<Dependency<EntityStore>> dependencies = RootDependency.firstSet();
    private final AetherhavenPlugin plugin;

    public ShopSpotLookAtSystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        PlayerRef pr = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null || pr == null) {
            return;
        }
        ShopSpotInteractionCleanup.healLegacyQuantityOverlay(playerRef, commandBuffer);

        World world = store.getExternalData().getWorld();
        ShopSpotPlayerComponent st = store.getComponent(playerRef, ShopSpotPlayerComponent.getComponentType());

        Vector3i block = TargetUtil.getTargetBlock(playerRef, REACH, store);
        ShopSpotRegistry registry = AetherhavenWorldRegistries.getOrCreateShopSpotRegistry(world, plugin);
        ShopSpotRecord record = null;
        if (block != null) {
            UUID spotId = ShopSpotBlockUtil.spotIdAt(world, block);
            if (spotId != null) {
                record = registry.get(spotId);
            }
            if (record == null) {
                record = registry.getAtBlock(block.x(), block.y(), block.z());
            }
        }
        if (record == null) {
            if (st != null) {
                st.setFocusedSpotId(null);
                commandBuffer.putComponent(playerRef, ShopSpotPlayerComponent.getComponentType(), st);
            }
            if (ShopSpotHudSupport.isActive(player)) {
                ShopSpotHudSupport.removeHud(player, pr);
            }
            LAST_SIG.remove(pr.getUuid());
            return;
        }

        if (st == null) {
            st = new ShopSpotPlayerComponent();
        }
        UUID pendingId = st.getPendingSpotId();
        if (pendingId != null && pendingId.equals(record.getSpotId())) {
            st.setFocusedSpotId(record.getSpotId());
            commandBuffer.putComponent(playerRef, ShopSpotPlayerComponent.getComponentType(), st);
            if (ShopSpotHudSupport.isActive(player)) {
                ShopSpotHudSupport.removeHud(player, pr);
            }
            LAST_SIG.remove(pr.getUuid());
            return;
        }

        st.setFocusedSpotId(record.getSpotId());
        commandBuffer.putComponent(playerRef, ShopSpotPlayerComponent.getComponentType(), st);
        TownRecord town = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin).getTown(record.getTownId());
        if (town == null) {
            return;
        }
        boolean gameDay = ShopSpotOpenService.isGameDay(store);
        boolean staffed = ShopSpotOpenService.hasStaffedWorkplace(record, town, store);
        int sig = st.hudSignature(record, gameDay, pr.getUuid(), town, staffed);
        Integer prev = LAST_SIG.get(pr.getUuid());
        if (prev != null && prev == sig && ShopSpotHudSupport.isActive(player)) {
            return;
        }
        LAST_SIG.put(pr.getUuid(), sig);
        ShopSpotStatusHud hud = ShopSpotHudSupport.obtainHud(player, pr);
        hud.refresh(world, record, town, gameDay, pr.getUuid(), plugin);
    }

    public static void invalidateSignature(@Nonnull UUID playerUuid) {
        LAST_SIG.remove(playerUuid);
    }
}
