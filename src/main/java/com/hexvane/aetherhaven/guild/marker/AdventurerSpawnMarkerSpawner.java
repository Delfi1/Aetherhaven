package com.hexvane.aetherhaven.guild.marker;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabCopyableComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class AdventurerSpawnMarkerSpawner {
    private AdventurerSpawnMarkerSpawner() {}

    /**
     * Builds a marker entity holder for {@link com.hypixel.hytale.component.CommandBuffer#addEntity} (safe during
     * interaction ticks).
     */
    @Nullable
    public static Holder<EntityStore> createHolder(@Nonnull World world, @Nonnull Vector3d position) {
        return createHolder(world, position, 0.0F);
    }

    @Nullable
    public static Holder<EntityStore> createHolder(@Nonnull World world, @Nonnull Vector3d position, float yawRadians) {
        AdventurerSpawnMarkerEntity probe = new AdventurerSpawnMarkerEntity();
        if (!EntityModule.get().isKnown(probe)) {
            return null;
        }
        if (position.y() < -32.0) {
            return null;
        }
        Rotation3f rot = new Rotation3f(0.0F, yawRadians, 0.0F);

        AdventurerSpawnMarkerEntity entity = new AdventurerSpawnMarkerEntity();
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
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.addComponent(Invulnerable.getComponentType(), Invulnerable.INSTANCE);
        holder.ensureComponent(PrefabCopyableComponent.getComponentType());
        return holder;
    }
}
