package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class RtsHostileQuery {
    private static final String AGGRESSIVE_GROUP = "Aggressive";

    private RtsHostileQuery() {}

    public static boolean isAggressiveNpc(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            return false;
        }
        int roleIndex = npc.getRole().getRoleIndex();
        return roleIndex >= 0 && npcInAggressiveGroup(roleIndex);
    }

    @Nullable
    public static Ref<EntityStore> nearestHostile(
        @Nonnull Store<EntityStore> store,
        double centerX,
        double centerY,
        double centerZ,
        double radius
    ) {
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            store.getResource(EntityModule.get().getEntitySpatialResourceType());
        List<Ref<EntityStore>> hits = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(new Vector3d(centerX, centerY, centerZ), radius, hits);
        Ref<EntityStore> best = null;
        double bestSq = Double.MAX_VALUE;
        for (Ref<EntityStore> ref : hits) {
            if (!ref.isValid() || !isAggressiveNpc(ref, store)) {
                continue;
            }
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) {
                continue;
            }
            Vector3d p = tc.getPosition();
            double dx = p.x - centerX;
            double dy = p.y - centerY;
            double dz = p.z - centerZ;
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq <= radius * radius && sq < bestSq) {
                bestSq = sq;
                best = ref;
            }
        }
        return best;
    }

    @Nonnull
    public static List<Ref<EntityStore>> collectNpcRefsNear(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> centerRef,
        double radius
    ) {
        TransformComponent centerTc = store.getComponent(centerRef, TransformComponent.getComponentType());
        if (centerTc == null) {
            return List.of();
        }
        Vector3d center = centerTc.getPosition();
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            store.getResource(EntityModule.get().getEntitySpatialResourceType());
        List<Ref<EntityStore>> hits = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(center, radius, hits);
        List<Ref<EntityStore>> npcs = new ArrayList<>(hits.size());
        for (Ref<EntityStore> ref : hits) {
            if (ref.isValid() && !ref.equals(centerRef) && store.getComponent(ref, NPCEntity.getComponentType()) != null) {
                npcs.add(ref);
            }
        }
        return npcs;
    }

    public static double horizontalDistance(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Nullable
    public static UUID entityUuid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }

    public static void collectHostilesInBox(
        @Nonnull Store<EntityStore> store,
        double minX,
        double maxX,
        double minZ,
        double maxZ,
        @Nonnull List<Ref<EntityStore>> out
    ) {
        double cx = (minX + maxX) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        double radius = Math.max(Math.abs(maxX - minX), Math.abs(maxZ - minZ)) * 0.5 + 2.0;
        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
            store.getResource(EntityModule.get().getEntitySpatialResourceType());
        List<Ref<EntityStore>> hits = SpatialResource.getThreadLocalReferenceList();
        spatial.getSpatialStructure().collect(new Vector3d(cx, 0, cz), radius + ParticleUtil.DEFAULT_PARTICLE_DISTANCE, hits);
        hits.sort(Comparator.comparingInt(Ref::getIndex));
        for (Ref<EntityStore> ref : hits) {
            if (!ref.isValid() || !isAggressiveNpc(ref, store)) {
                continue;
            }
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) {
                continue;
            }
            Vector3d p = tc.getPosition();
            if (p.x >= minX && p.x <= maxX && p.z >= minZ && p.z <= maxZ) {
                out.add(ref);
            }
        }
    }

    private static boolean npcInAggressiveGroup(int roleIndex) {
        try {
            int g = NPCGroup.getAssetMap().getIndex(AGGRESSIVE_GROUP);
            if (g == Integer.MIN_VALUE) {
                return false;
            }
            return WorldSupport.hasTagInGroup(g, roleIndex);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
