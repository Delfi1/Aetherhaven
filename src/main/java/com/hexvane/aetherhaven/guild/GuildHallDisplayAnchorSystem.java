package com.hexvane.aetherhaven.guild;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkCharacterBinding;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import java.util.Set;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Keeps guild hall display adventurers at their spawn anchor facing and out of wander states. */
public final class GuildHallDisplayAnchorSystem extends EntityTickingSystem<EntityStore> {
    private static final double SNAP_DIST_SQ = 0.35 * 0.35;
    private static final float HEAD_YAW_EPS = 0.05F;

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
        return Query.and(
            GuildHallDisplayAnchor.getComponentType(),
            TownsfolkCharacterBinding.getComponentType(),
            NPCEntity.getComponentType(),
            TransformComponent.getComponentType()
        );
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        TownsfolkCharacterBinding binding = archetypeChunk.getComponent(index, TownsfolkCharacterBinding.getComponentType());
        GuildHallDisplayAnchor anchor = archetypeChunk.getComponent(index, GuildHallDisplayAnchor.getComponentType());
        NPCEntity npc = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
        TransformComponent tc = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (binding == null || anchor == null || npc == null || tc == null || npc.getRole() == null) {
            return;
        }
        if (!TownsfolkAssignmentKinds.isGuildHallAdventurer(binding.getAssignmentKind())) {
            return;
        }
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        boolean anchorChanged = false;

        if (!anchor.isChairMountFinished() && !GuildHallAdventurerChairMount.isBlockMounted(store, commandBuffer, ref)) {
            if (!GuildHallAdventurerChairMount.hasSeatNearSpawn(store, anchor)) {
                anchor.markChairMountFinished();
                anchorChanged = true;
            } else if (GuildHallAdventurerChairMount.tryMountChairBelowSpawn(ref, store, commandBuffer, anchor)) {
                anchor.markChairMountFinished();
                anchorChanged = true;
            } else {
                anchor.incrementChairMountAttempts();
                if (anchor.getChairMountAttempts() >= GuildHallDisplayAnchor.MAX_CHAIR_MOUNT_ATTEMPTS) {
                    GuildHallAdventurerChairMount.applySeatPoseFallback(ref, store, commandBuffer, anchor);
                    anchor.setSitFallbackApplied(true);
                    anchor.markChairMountFinished();
                }
                anchorChanged = true;
            }
        }

        anchorChanged |= applyDisplayStateIfNeeded(npc, ref, commandBuffer, anchor);

        if (anchorChanged) {
            commandBuffer.putComponent(ref, GuildHallDisplayAnchor.getComponentType(), anchor);
        }

        boolean inDialogue = isInInteractionDialogue(npc);
        if (GuildHallAdventurerChairMount.isBlockMounted(store, commandBuffer, ref) || anchor.isSitFallbackApplied()) {
            if (!inDialogue) {
                syncHeadYawIfNeeded(ref, store, commandBuffer, anchor.getYawRadians());
            }
            return;
        }

        Vector3d target = anchor.getPosition();
        Vector3d pos = tc.getPosition();
        double dx = pos.x - target.x;
        double dy = pos.y - target.y;
        double dz = pos.z - target.z;
        if (!inDialogue && dx * dx + dy * dy + dz * dz > SNAP_DIST_SQ) {
            Rotation3f rot = new Rotation3f(0.0F, anchor.getYawRadians(), 0.0F);
            commandBuffer.putComponent(ref, TransformComponent.getComponentType(), new TransformComponent(new Vector3d(target), rot));
        }
        if (!inDialogue) {
            syncHeadYawIfNeeded(ref, store, commandBuffer, anchor.getYawRadians());
        }
    }

    /** {@link StateSupport#getStateName()} is {@code State.subState}, not the bare state id. */
    private static boolean isInInteractionDialogue(@Nonnull NPCEntity npc) {
        if (npc.getRole() == null) {
            return false;
        }
        StateSupport stateSupport = npc.getRole().getStateSupport();
        int interactionState = stateSupport.getStateHelper().getStateIndex("$Interaction");
        return interactionState >= 0 && stateSupport.inState(interactionState);
    }

    /** @return true when anchor transient flags changed and should be persisted on the buffer */
    private static boolean applyDisplayStateIfNeeded(
        @Nonnull NPCEntity npc,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull GuildHallDisplayAnchor anchor
    ) {
        if (isInInteractionDialogue(npc)) {
            if (anchor.isDisplayStateApplied()) {
                anchor.setDisplayStateApplied(false);
                return true;
            }
            return false;
        }
        StateSupport stateSupport = npc.getRole().getStateSupport();
        int displayState = stateSupport.getStateHelper().getStateIndex(AetherhavenConstants.NPC_STATE_GUILD_HALL_DISPLAY);
        if (displayState >= 0 && stateSupport.inState(displayState)) {
            if (!anchor.isDisplayStateApplied()) {
                anchor.setDisplayStateApplied(true);
                return true;
            }
            return false;
        }
        if (anchor.isDisplayStateApplied()) {
            return false;
        }
        npc.getRole().getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_GUILD_HALL_DISPLAY, null, commandBuffer);
        anchor.setDisplayStateApplied(true);
        return true;
    }

    private static void syncHeadYawIfNeeded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        float yawRadians
    ) {
        HeadRotation head = store.getComponent(ref, HeadRotation.getComponentType());
        if (head == null) {
            return;
        }
        float current = head.getRotation().y();
        if (Math.abs(current - yawRadians) <= HEAD_YAW_EPS) {
            return;
        }
        head.teleportRotation(new Rotation3f(0.0F, yawRadians, 0.0F));
        commandBuffer.putComponent(ref, HeadRotation.getComponentType(), head);
    }
}
