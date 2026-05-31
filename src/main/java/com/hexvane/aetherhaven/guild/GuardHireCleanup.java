package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.autonomy.VillagerBlockUtil;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Clears guild hall display state when an adventurer is hired as a guard. */
public final class GuardHireCleanup {
    private GuardHireCleanup() {}

    public static void prepareForGuardDuty(
        @Nonnull Ref<EntityStore> npcRef,
        @Nonnull Store<EntityStore> store,
        @Nullable PlotInstance guildPlot
    ) {
        store.tryRemoveComponent(npcRef, GuildHallDisplayAnchor.getComponentType());
        store.tryRemoveComponent(npcRef, MountedComponent.getComponentType());

        NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npc != null) {
            npc.playAnimation(npcRef, AnimationSlot.Status, null, store);
            Vector3d leash = resolvePatrolLeashPoint(store, npcRef, guildPlot);
            npc.setLeashPoint(leash);
        }

        standOffChairIfNeeded(npcRef, store);
    }

    @Nonnull
    private static Vector3d resolvePatrolLeashPoint(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> npcRef,
        @Nullable PlotInstance guildPlot
    ) {
        if (guildPlot != null) {
            PlotFootprintRecord fp = guildPlot.toFootprint();
            double cx = (fp.getMinX() + fp.getMaxX()) * 0.5 + 0.5;
            double cz = (fp.getMinZ() + fp.getMaxZ()) * 0.5 + 0.5;
            World world = store.getExternalData().getWorld();
            int standY = VillagerBlockUtil.findStandY(world, (int) Math.floor(cx), (int) Math.floor(cz), guildPlot.getSignY() + 3);
            double y = standY != Integer.MIN_VALUE ? standY + 0.02 : guildPlot.getSignY() + 0.02;
            return new Vector3d(cx, y, cz);
        }
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc != null) {
            return new Vector3d(tc.getPosition());
        }
        return new Vector3d();
    }

    private static void standOffChairIfNeeded(@Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store) {
        TransformComponent tc = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (tc == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3d pos = tc.getPosition();
        int bx = (int) Math.floor(pos.x);
        int bz = (int) Math.floor(pos.z);
        int standY = VillagerBlockUtil.findStandY(world, bx, bz, (int) Math.floor(pos.y) + 2);
        if (standY == Integer.MIN_VALUE) {
            return;
        }
        pos.y = standY + 0.02;
        store.putComponent(npcRef, TransformComponent.getComponentType(), tc);
    }
}
