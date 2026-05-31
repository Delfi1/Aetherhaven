package com.hexvane.aetherhaven.poi.marker;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabCopyableComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class PoiMarkerSpawner {
    private static final String MARKER_MODEL_ID = "NPC_Spawn_Marker";

    private PoiMarkerSpawner() {}

    @Nullable
    public static Holder<EntityStore> createHolder(
        @Nonnull World world,
        @Nonnull Vector3d position,
        float yawRadians,
        @Nonnull PoiMarkerDataComponent data
    ) {
        PoiMarkerEntity probe = new PoiMarkerEntity();
        if (!EntityModule.get().isKnown(probe)) {
            return null;
        }
        ModelAsset markerAsset = ModelAsset.getAssetMap().getAsset(MARKER_MODEL_ID);
        if (markerAsset == null) {
            return null;
        }
        if (position.y() < -32.0) {
            return null;
        }
        Model markerModel = Model.createUnitScaleModel(markerAsset);
        Rotation3f rot = new Rotation3f(0.0F, yawRadians, 0.0F);

        PoiMarkerEntity entity = new PoiMarkerEntity();
        entity.loadIntoWorld(world);
        if (!world.equals(entity.getWorld())) {
            return null;
        }
        if (entity.getReference() != null && entity.getReference().isValid()) {
            return null;
        }
        entity.unloadFromWorld();
        Holder<EntityStore> holder = entity.toHolder();
        HeadRotation headRotation = holder.ensureAndGetComponent(HeadRotation.getComponentType());
        headRotation.teleportRotation(rot);
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rot));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.putComponent(ModelComponent.getComponentType(), new ModelComponent(markerModel));
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.addComponent(Invulnerable.getComponentType(), Invulnerable.INSTANCE);
        holder.ensureComponent(PrefabCopyableComponent.getComponentType());
        holder.putComponent(PoiMarkerDataComponent.getComponentType(), data);
        Message label = Message.translation("aetherhaven_world_debug.aetherhaven.poiMarker.label");
        holder.putComponent(Nameplate.getComponentType(), new Nameplate("POI"));
        holder.putComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(label));
        return holder;
    }
}
