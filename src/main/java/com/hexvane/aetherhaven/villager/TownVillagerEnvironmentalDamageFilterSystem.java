package com.hexvane.aetherhaven.villager;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Town-linked NPCs already use {@code Immunity_Environmental} on several roles, but that only resists the
 * {@code Environmental} damage cause — not fall, suffocation, drowning, or out-of-world kills from spawn misalignment
 * or portal platforms.
 */
public final class TownVillagerEnvironmentalDamageFilterSystem extends DamageEventSystem {
    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(TownVillagerBinding.getComponentType(), NPCEntity.getComponentType(), EntityStatMap.getComponentType());
    }

    @Override
    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Damage damage
    ) {
        DamageCause cause = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
        if (cause == null || !isBlockedEnvironmentalCause(cause.getId())) {
            return;
        }
        damage.setCancelled(true);
    }

    private static boolean isBlockedEnvironmentalCause(@Nullable String causeId) {
        if (causeId == null || causeId.isBlank()) {
            return false;
        }
        return switch (causeId) {
            case "Fall", "Suffocation", "Drowning", "OutOfWorld", "Environmental" -> true;
            default -> false;
        };
    }
}
