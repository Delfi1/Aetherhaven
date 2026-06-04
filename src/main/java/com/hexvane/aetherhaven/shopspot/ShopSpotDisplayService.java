package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class ShopSpotDisplayService {
    private static final float DISPLAY_SCALE = 0.85f;
    private static final double Y_OFFSET = 0.22;

    private ShopSpotDisplayService() {}

    public static void syncDisplay(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRegistry registry,
        @Nonnull ShopSpotRecord record,
        @Nonnull TownRecord town
    ) {
        syncDisplay(world, store, null, plugin, registry, record, town);
    }

    public static void syncDisplay(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRegistry registry,
        @Nonnull ShopSpotRecord record,
        @Nonnull TownRecord town
    ) {
        // Look-at / interaction ticks pass a CommandBuffer; entity add/remove must run after the store finishes processing.
        if (commandBuffer != null) {
            world.execute(() -> {
                Store<EntityStore> deferred = world.getEntityStore().getStore();
                if (deferred != null) {
                    syncDisplay(world, deferred, null, plugin, registry, record, town);
                }
            });
            return;
        }
        EntityWriter writer = new EntityWriter(store, null);
        boolean open = ShopSpotOpenService.isOpen(record, town, world, store);
        if (!open || !record.hasStock()) {
            removeDisplay(world, store, commandBuffer, plugin, registry, record);
            return;
        }
        String itemId = record.getItemId();
        if (itemId == null) {
            removeDisplay(world, store, commandBuffer, plugin, registry, record);
            return;
        }
        if (!isSpotChunkLoaded(world, record)) {
            return;
        }
        Vector3d pos = blockCenter(record);
        pos.y += Y_OFFSET;
        String signature = ShopSpotJewelrySupport.listingDisplaySignature(itemId, record);
        UUID existing = record.getDisplayEntityUuid();
        if (existing != null) {
            Ref<EntityStore> ref = findEntityByUuid(store, existing);
            if (ref != null && ref.isValid() && signature.equals(record.getListingDisplaySignature())) {
                updateTransform(world, store, ref, pos, record);
                return;
            }
        }
        purgeOrphanDisplayEntities(world, store, registry, record);
        removeDisplay(world, store, commandBuffer, plugin, registry, record);
        Ref<EntityStore> spawned = spawnDisplay(writer, world, record, itemId, pos);
        if (spawned != null && spawned.isValid()) {
            record.setListingDisplaySignature(signature);
            UUIDComponent uc = store.getComponent(spawned, UUIDComponent.getComponentType());
            if (uc != null) {
                record.setDisplayEntityUuid(uc.getUuid());
            }
        }
    }

    public static void syncAllInWorld(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRegistry registry
    ) {
        for (ShopSpotRecord record : registry.allRecords()) {
            com.hexvane.aetherhaven.town.TownManager tm =
                com.hexvane.aetherhaven.town.AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
            TownRecord town = tm.getTown(record.getTownId());
            if (town != null) {
                syncDisplay(world, store, plugin, registry, record, town);
            }
        }
    }

    public static void removeDisplay(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRegistry registry,
        @Nonnull ShopSpotRecord record
    ) {
        removeDisplay(world, store, null, plugin, registry, record);
    }

    public static void removeDisplay(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nullable CommandBuffer<EntityStore> commandBuffer,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopSpotRegistry registry,
        @Nonnull ShopSpotRecord record
    ) {
        if (commandBuffer != null) {
            world.execute(() -> {
                Store<EntityStore> deferred = world.getEntityStore().getStore();
                if (deferred != null) {
                    removeDisplay(world, deferred, null, plugin, registry, record);
                }
            });
            return;
        }
        EntityWriter writer = new EntityWriter(store, null);
        purgeOrphanDisplayEntities(world, store, registry, record);
        UUID id = record.getDisplayEntityUuid();
        record.setDisplayEntityUuid(null);
        record.setListingDisplaySignature(null);
        if (id == null) {
            return;
        }
        Ref<EntityStore> ref = findEntityByUuid(store, id);
        if (ref != null && ref.isValid()) {
            writer.removeEntity(ref);
        }
    }

    /** Removes stale floating item props at this stall (e.g. after server restart). */
    public static void purgeOrphanDisplayEntities(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull ShopSpotRegistry registry,
        @Nonnull ShopSpotRecord record
    ) {
        if (!isSpotChunkLoaded(world, record)) {
            return;
        }
        double cx = record.getBlockX() + 0.5;
        double cy = record.getBlockY();
        double cz = record.getBlockZ() + 0.5;
        Set<UUID> protectedDisplayIds = collectDisplayEntityIds(registry);
        Set<UUID> toRemove = new LinkedHashSet<>();
        store.forEachChunk(
            Query.and(ItemComponent.getComponentType(), PreventPickup.getComponentType(), TransformComponent.getComponentType()),
            (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> ignored) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    if (!ref.isValid()) {
                        continue;
                    }
                    UUIDComponent uc = chunk.getComponent(i, UUIDComponent.getComponentType());
                    if (uc == null) {
                        continue;
                    }
                    if (protectedDisplayIds.contains(uc.getUuid())) {
                        continue;
                    }
                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null) {
                        continue;
                    }
                    var p = tc.getPosition();
                    if (Math.abs(p.x - cx) <= 1.25
                        && Math.abs(p.y - cy) <= 2.0
                        && Math.abs(p.z - cz) <= 1.25) {
                        toRemove.add(uc.getUuid());
                    }
                }
            }
        );
        if (toRemove.isEmpty()) {
            return;
        }
        EntityWriter writer = new EntityWriter(store, null);
        for (UUID id : toRemove) {
            Ref<EntityStore> ref = findEntityByUuid(store, id);
            if (ref != null && ref.isValid()) {
                writer.removeEntity(ref);
            }
        }
    }

    @Nonnull
    private static Set<UUID> collectDisplayEntityIds(@Nonnull ShopSpotRegistry registry) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (ShopSpotRecord r : registry.allRecords()) {
            UUID displayId = r.getDisplayEntityUuid();
            if (displayId != null) {
                ids.add(displayId);
            }
        }
        return ids;
    }

    /** Avoids spawning floating item props before the stall column is in memory (see vanilla {@code UpdateLocationSystems}). */
    static boolean isSpotChunkLoaded(@Nonnull World world, @Nonnull ShopSpotRecord record) {
        return world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(record.getBlockX(), record.getBlockZ())) != null;
    }

    @Nonnull
    private static Vector3d blockCenter(@Nonnull ShopSpotRecord record) {
        return new Vector3d(record.getBlockX() + 0.5, record.getBlockY(), record.getBlockZ() + 0.5);
    }

    private static void updateTransform(
        @Nonnull World world,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Vector3d pos,
        @Nonnull ShopSpotRecord record
    ) {
        Rotation3f rot = ShopSpotDisplayRotation.forRecord(world, record);
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) {
            tc.getPosition().set(pos);
            tc.getRotation().set(rot);
        }
        HeadRotation hr = store.getComponent(ref, HeadRotation.getComponentType());
        if (hr != null) {
            hr.getRotation().set(rot);
        }
    }

    @Nullable
    private static Ref<EntityStore> spawnDisplay(
        @Nonnull EntityWriter writer,
        @Nonnull World world,
        @Nonnull ShopSpotRecord record,
        @Nonnull String itemId,
        @Nonnull Vector3d pos
    ) {
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null) {
            return null;
        }
        Rotation3f rot = ShopSpotDisplayRotation.forRecord(world, record);
        Model model = resolveItemModel(item);
        Store<EntityStore> store = writer.store;
        Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rot));
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        ItemStack stack = ShopSpotJewelrySupport.buildDisplayStack(itemId, record);
        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(stack));
        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rot));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.ensureComponent(UUIDComponent.getComponentType());
        if (model != null) {
            String modelId = resolveItemModelId(item);
            if (modelId != null) {
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(
                    PersistentModel.getComponentType(),
                    new PersistentModel(new Model.ModelReference(modelId, DISPLAY_SCALE, null, true))
                );
            }
        }
        return writer.addEntity(holder);
    }

    @Nullable
    private static String resolveItemModelId(@Nonnull Item item) {
        String modelId = item.getModel();
        if (modelId == null && item.hasBlockType()) {
            BlockType blockType = BlockType.getAssetMap().getAsset(item.getId());
            if (blockType != null && blockType.getCustomModel() != null) {
                modelId = blockType.getCustomModel();
            }
        }
        return modelId;
    }

    @Nullable
    private static Model resolveItemModel(@Nonnull Item item) {
        String modelId = resolveItemModelId(item);
        if (modelId == null) {
            return null;
        }
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelId);
        return asset != null ? Model.createStaticScaledModel(asset, DISPLAY_SCALE) : null;
    }

    @Nullable
    private static Ref<EntityStore> findEntityByUuid(@Nonnull Store<EntityStore> store, @Nonnull UUID uuid) {
        Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(uuid);
        return ref != null && ref.isValid() ? ref : null;
    }

    private static final class EntityWriter {
        private final Store<EntityStore> store;
        @Nullable
        private final CommandBuffer<EntityStore> commandBuffer;

        private EntityWriter(@Nonnull Store<EntityStore> store, @Nullable CommandBuffer<EntityStore> commandBuffer) {
            this.store = store;
            this.commandBuffer = commandBuffer;
        }

        void removeEntity(@Nonnull Ref<EntityStore> ref) {
            if (!ref.isValid()) {
                return;
            }
            if (commandBuffer != null) {
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            } else {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        }

        @Nullable
        Ref<EntityStore> addEntity(@Nonnull Holder<EntityStore> holder) {
            if (commandBuffer != null) {
                return commandBuffer.addEntity(holder, AddReason.SPAWN);
            }
            return store.addEntity(holder, AddReason.SPAWN);
        }
    }
}
