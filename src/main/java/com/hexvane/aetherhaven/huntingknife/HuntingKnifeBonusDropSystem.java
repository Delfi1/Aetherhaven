package com.hexvane.aetherhaven.huntingknife;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/**
 * When a player kills an NPC while holding the hunting knife, roll the creature's drop table again
 * and spawn any raw meat, hide, or feathers from that extra roll.
 */
public final class HuntingKnifeBonusDropSystem extends DeathSystems.OnDeathSystem {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> victimRef,
        @Nonnull DeathComponent death,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (store.getComponent(victimRef, Player.getComponentType()) != null) {
            return;
        }
        Damage info = death.getDeathInfo();
        if (info == null) {
            return;
        }
        Ref<EntityStore> killerRef = resolveKillerRef(info);
        if (killerRef == null || !killerRef.isValid()) {
            return;
        }
        if (store.getComponent(killerRef, Player.getComponentType()) == null) {
            return;
        }
        ItemStack weapon = InventoryComponent.getItemInHand(store, killerRef);
        if (weapon == null || !AetherhavenConstants.ITEM_HUNTING_KNIFE.equals(weapon.getItemId())) {
            return;
        }
        NPCEntity npc = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        Role role = npc.getRole();
        if (role == null) {
            return;
        }
        String dropListId = role.getDropListId();
        if (dropListId == null || dropListId.isBlank()) {
            return;
        }
        ItemModule itemModule = ItemModule.get();
        if (!itemModule.isEnabled()) {
            return;
        }
        List<ItemStack> rolled = itemModule.getRandomItemDrops(dropListId);
        List<ItemStack> bonus = new ArrayList<>(rolled.size());
        for (ItemStack stack : rolled) {
            if (stack != null && HuntingKnifeButcherLoot.isButcherLoot(stack.getItemId())) {
                bonus.add(stack);
            }
        }
        if (bonus.isEmpty()) {
            return;
        }
        TransformComponent transform = store.getComponent(victimRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        HeadRotation headRotation = store.getComponent(victimRef, HeadRotation.getComponentType());
        Vector3d dropPosition = new Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
        Holder<EntityStore>[] holders;
        if (headRotation != null) {
            holders = ItemComponent.generateItemDrops(
                store,
                bonus,
                dropPosition,
                new Rotation3f(headRotation.getRotation())
            );
        } else {
            holders = ItemComponent.generateItemDrops(store, bonus, dropPosition, Rotation3f.ZERO);
        }
        if (holders.length > 0) {
            commandBuffer.addEntities(holders, AddReason.SPAWN);
        }
    }

    @Nullable
    private static Ref<EntityStore> resolveKillerRef(@Nonnull Damage damage) {
        Damage.Source src = damage.getSource();
        if (src instanceof Damage.EntitySource es) {
            return es.getRef();
        }
        return null;
    }
}
