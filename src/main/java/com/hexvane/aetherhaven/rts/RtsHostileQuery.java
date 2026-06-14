package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.villager.TownVillagerBinding;
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
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3i;

public final class RtsHostileQuery {
    private static final String AGGRESSIVE_GROUP = "Aggressive";
    private static final String TOWNSFOLK_GROUP = "Aetherhaven_Townsfolk";
    private static final String RAID_GROUP = "Aetherhaven_Raid";

    /** Radius around a ground click to pick a hostile under the RTS cursor. */
    private static final double FOCUS_PICK_RADIUS = 6.0;
    /** Max normalized screen distance (0..1) to match cursor to a hostile icon. */
    private static final float HOSTILE_SCREEN_PICK_RADIUS = 0.035f;
    private static final float HOSTILE_SCREEN_PICK_RADIUS_SQ =
        HOSTILE_SCREEN_PICK_RADIUS * HOSTILE_SCREEN_PICK_RADIUS;

    private RtsHostileQuery() {}

    /**
     * Resolves the focus target for a command-sword click: direct entity hit first, then screen-space
     * pick (tracks camera pan), then nearest attackable NPC near the ground pick.
     */
    @Nullable
    public static Ref<EntityStore> resolveFocusTarget(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screen,
        @Nullable Ref<EntityStore> directTarget
    ) {
        if (directTarget != null && directTarget.isValid() && isGuardAttackableTarget(directTarget, store)) {
            return directTarget;
        }
        if (screen != null) {
            Ref<EntityStore> atScreen = pickAttackableAtScreen(store, session, screen.x(), screen.y());
            if (atScreen != null) {
                return atScreen;
            }
        }
        RtsScreenPickUtil.GroundPick pick = RtsScreenPickUtil.resolveCommandGroundPick(
            playerRef,
            store,
            session,
            targetBlock,
            screen
        );
        if (pick == null) {
            return null;
        }
        Ref<EntityStore> atPick = nearestAttackableTarget(store, pick.x(), pick.y(), pick.z(), FOCUS_PICK_RADIUS);
        if (atPick != null) {
            return atPick;
        }
        return nearestAttackableTarget(store, pick.x(), pick.y() + 2.0, pick.z(), FOCUS_PICK_RADIUS + 2.0);
    }

    @Nullable
    public static Ref<EntityStore> pickAttackableAtScreen(
        @Nonnull Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        float rawScreenX,
        float rawScreenY
    ) {
        float cursorNx = RtsScreenPickUtil.cameraRawToNormalizedX(rawScreenX);
        float cursorNy = RtsScreenPickUtil.cameraRawToNormalizedY(rawScreenY);
        double cx = session.getFocusX();
        double cz = session.getFocusZ();
        double viewRadius = Math.max(64.0, session.getSavedViewRadiusBlocks() + RtsScreenPickUtil.viewHeightAboveGround(session) + 16.0);
        List<Ref<EntityStore>> attackable = new ArrayList<>();
        collectAttackableInBox(store, cx - viewRadius, cx + viewRadius, cz - viewRadius, cz + viewRadius, attackable);

        Ref<EntityStore> best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Ref<EntityStore> ref : attackable) {
            if (!ref.isValid()) {
                continue;
            }
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) {
                continue;
            }
            Vector3d pos = tc.getPosition();
            float sx = RtsScreenPickUtil.worldToNormalizedScreenX(pos.x, session);
            float sy = RtsScreenPickUtil.worldToNormalizedScreenY(pos.z, session);
            double dx = sx - cursorNx;
            double dy = sy - cursorNy;
            double distSq = dx * dx + dy * dy;
            if (distSq <= HOSTILE_SCREEN_PICK_RADIUS_SQ && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = ref;
            }
        }
        return best;
    }

    /** True when the NPC role is in Hytale's Aggressive group (hostile creatures for auto-engage). */
    public static boolean isAggressiveNpc(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            return false;
        }
        int roleIndex = npc.getRole().getRoleIndex();
        return roleIndex >= 0 && npcInAggressiveGroup(roleIndex);
    }

    /**
     * True for any NPC guards may focus-fire except town villagers, tourists, guards, and other town staff.
     * Quest-board raid mobs ({@code Aetherhaven_Raid_*}) are always attackable even though their role id
     * matches the {@code Aetherhaven_*} townsfolk group wildcard.
     */
    public static boolean isGuardAttackableTarget(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (!ref.isValid()) {
            return false;
        }
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            return false;
        }
        if (store.getComponent(ref, TownVillagerBinding.getComponentType()) != null) {
            return false;
        }
        int roleIndex = npc.getRole().getRoleIndex();
        if (roleIndex >= 0 && npcInRaidGroup(roleIndex)) {
            return true;
        }
        return roleIndex < 0 || !npcInTownsfolkGroup(roleIndex);
    }

    /** True when {@code observerRef} has unobstructed line of sight to {@code targetRef}. */
    public static boolean hasLineOfSight(
        @Nonnull Ref<EntityStore> observerRef,
        @Nonnull Ref<EntityStore> targetRef,
        @Nonnull Store<EntityStore> store
    ) {
        if (!observerRef.isValid() || !targetRef.isValid()) {
            return false;
        }
        NPCEntity observer = store.getComponent(observerRef, NPCEntity.getComponentType());
        if (observer == null || observer.getRole() == null) {
            return false;
        }
        return observer.getRole().getPositionCache().hasLineOfSight(observerRef, targetRef, store);
    }

    @Nullable
    public static Ref<EntityStore> nearestHostile(
        @Nonnull Store<EntityStore> store,
        double centerX,
        double centerY,
        double centerZ,
        double radius
    ) {
        return nearestHostile(store, centerX, centerY, centerZ, radius, null);
    }

    @Nullable
    public static Ref<EntityStore> nearestHostile(
        @Nonnull Store<EntityStore> store,
        double centerX,
        double centerY,
        double centerZ,
        double radius,
        @Nullable Ref<EntityStore> observerRef
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
            if (observerRef != null && !hasLineOfSight(observerRef, ref, store)) {
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

    @Nullable
    public static Ref<EntityStore> nearestAttackableTarget(
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
            if (!ref.isValid() || !isGuardAttackableTarget(ref, store)) {
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
        collectAttackableInBox(store, minX, maxX, minZ, maxZ, out);
    }

    public static void collectAttackableInBox(
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
            if (!ref.isValid() || !isGuardAttackableTarget(ref, store)) {
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
        return npcInGroup(AGGRESSIVE_GROUP, roleIndex);
    }

    private static boolean npcInTownsfolkGroup(int roleIndex) {
        return npcInGroup(TOWNSFOLK_GROUP, roleIndex);
    }

    private static boolean npcInRaidGroup(int roleIndex) {
        return npcInGroup(RAID_GROUP, roleIndex);
    }

    private static boolean npcInGroup(@Nonnull String groupName, int roleIndex) {
        try {
            int g = NPCGroup.getAssetMap().getIndex(groupName);
            if (g == Integer.MIN_VALUE) {
                return false;
            }
            return WorldSupport.hasTagInGroup(g, roleIndex);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
