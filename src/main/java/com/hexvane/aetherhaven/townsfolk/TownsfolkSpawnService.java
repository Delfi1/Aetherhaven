package com.hexvane.aetherhaven.townsfolk;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.guild.GuildHallDisplayAnchor;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.data.TownsfolkCharacterCatalog;
import com.hexvane.aetherhaven.townsfolk.data.TownsfolkCharacterDefinition;
import com.hexvane.aetherhaven.villager.AetherhavenVillagerHandle;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class TownsfolkSpawnService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TownsfolkSpawnService() {}

    public record SpawnedTownsfolk(
        @Nonnull String characterId,
        @Nonnull UUID entityUuid,
        @Nonnull List<String> personalityIds,
        @Nonnull String assignmentKind
    ) {}

    @Nonnull
    public static List<String> availableCharacterIds(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull String assignmentKind
    ) {
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        return pool.availableCharacterIds(plugin.getTownsfolkCharacterCatalog(), assignmentKind);
    }

    @Nonnull
    public static Optional<SpawnedTownsfolk> trySpawn(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull Vector3d position,
        @Nonnull String assignmentKind,
        @Nullable String preferredCharacterId,
        @Nonnull Random random
    ) {
        return trySpawn(world, plugin, town, store, position, assignmentKind, preferredCharacterId, random, new Rotation3f(0.0F, 0.0F, 0.0F), null);
    }

    @Nonnull
    public static Optional<SpawnedTownsfolk> trySpawn(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull Store<EntityStore> store,
        @Nonnull Vector3d position,
        @Nonnull String assignmentKind,
        @Nullable String preferredCharacterId,
        @Nonnull Random random,
        @Nonnull Rotation3f rotation,
        @Nullable Float displayAnchorYawRadians
    ) {
        String kind = assignmentKind.trim().toLowerCase();
        TownsfolkCharacterCatalog catalog = plugin.getTownsfolkCharacterCatalog();
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);

        String characterId;
        if (preferredCharacterId != null && !preferredCharacterId.isBlank()) {
            characterId = preferredCharacterId.trim();
            TownsfolkCharacterDefinition def = catalog.byId(characterId);
            if (def == null) {
                LOGGER.atWarning().log("Unknown townsfolk character id %s", characterId);
                return Optional.empty();
            }
            if (pool.isCheckedOut(characterId)) {
                LOGGER.atWarning().log("Townsfolk %s already checked out", characterId);
                return Optional.empty();
            }
            if (TownsfolkAssignmentKinds.isGuildHallAdventurer(kind)) {
                if (!def.supportsAssignment(TownsfolkAssignmentKinds.GUARD)) {
                    LOGGER.atWarning().log("Townsfolk %s is not guard eligible", characterId);
                    return Optional.empty();
                }
            } else if (!def.supportsAssignment(kind)) {
                LOGGER.atWarning().log("Townsfolk %s does not support assignment %s", characterId, kind);
                return Optional.empty();
            }
        } else if (TownsfolkAssignmentKinds.isGuildHallAdventurer(kind)) {
            characterId = pool.pickRandomGuardEligibleCharacterId(catalog, random);
            if (characterId == null) {
                LOGGER.atWarning().log("No guard eligible townsfolk for guild hall in world %s", world.getName());
                return Optional.empty();
            }
        } else {
            characterId = pool.pickRandomAvailableCharacterId(catalog, kind, random);
            if (characterId == null) {
                LOGGER.atWarning().log("No available townsfolk for assignment %s in world %s", kind, world.getName());
                return Optional.empty();
            }
        }

        TownsfolkCharacterDefinition character = catalog.byId(characterId);
        if (character == null) {
            return Optional.empty();
        }
        List<String> personalities = character.getPersonalityIds();

        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return Optional.empty();
        }
        var pair = npcPlugin.spawnNPC(store, AetherhavenConstants.NPC_TOWNSFOLK, null, position, rotation);
        if (pair == null) {
            LOGGER.atWarning().log("Failed to spawn townsfolk NPC %s for town %s", characterId, town.getTownId());
            return Optional.empty();
        }
        Ref<EntityStore> ref = pair.first();
        applyCharacterAppearance(ref, store, character.getModelAssetId(), character.getModelScale());

        String displayName = character.getDisplayName();
        if (displayName != null) {
            store.putComponent(ref, PersistentDisplayName.getComponentType(), new PersistentDisplayName(Message.raw(displayName)));
        }

        String handle = "Townsfolk_" + characterId + "_" + shortHex(town.getTownId());
        store.putComponent(ref, AetherhavenVillagerHandle.getComponentType(), new AetherhavenVillagerHandle(handle));
        store.putComponent(
            ref,
            TownVillagerBinding.getComponentType(),
            new TownVillagerBinding(town.getTownId(), TownVillagerBinding.KIND_TOWNSFOLK, null)
        );
        store.putComponent(
            ref,
            TownsfolkCharacterBinding.getComponentType(),
            new TownsfolkCharacterBinding(
                characterId,
                "",
                kind,
                character.getModelAssetId(),
                personalities
            )
        );

        if (TownsfolkAssignmentKinds.isGuildHallAdventurer(kind)) {
            float anchorYaw = displayAnchorYawRadians != null ? displayAnchorYawRadians : rotation.yaw();
            store.putComponent(ref, GuildHallDisplayAnchor.getComponentType(), new GuildHallDisplayAnchor(position, anchorYaw));
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null && npc.getRole() != null) {
                npc.getRole().getStateSupport().setState(ref, AetherhavenConstants.NPC_STATE_GUILD_HALL_DISPLAY, null, store);
            }
        }

        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            return Optional.empty();
        }
        UUID entityUuid = uuidComp.getUuid();
        pool.checkout(
            new TownsfolkPoolCheckoutRecord(
                characterId,
                town.getTownId().toString(),
                entityUuid.toString(),
                kind,
                ""
            )
        );
        TownsfolkPoolPersistence.save(world, plugin, pool);
        return Optional.of(new SpawnedTownsfolk(characterId, entityUuid, personalities, kind));
    }

    public static void release(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull String characterId) {
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        if (pool.release(characterId)) {
            TownsfolkPoolPersistence.save(world, plugin, pool);
        }
    }

    public static void releaseByEntity(@Nonnull World world, @Nonnull AetherhavenPlugin plugin, @Nonnull UUID entityUuid) {
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        TownsfolkPoolCheckoutRecord rec = pool.checkoutForEntity(entityUuid);
        if (rec != null) {
            pool.release(rec.getCharacterId());
            TownsfolkPoolPersistence.save(world, plugin, pool);
        }
    }

    /**
     * Despawns every townsfolk NPC in this world and clears all pool checkouts.
     *
     * @return number of entities removed from the world
     */
    public static int clearPoolAndDespawnAll(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        List<Ref<EntityStore>> refs = new ArrayList<>();
        store.forEachChunk(
            Query.and(TownsfolkCharacterBinding.getComponentType(), NPCEntity.getComponentType()),
            (archetypeChunk, commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
                    if (ref != null && ref.isValid()) {
                        refs.add(ref);
                    }
                }
            }
        );
        int despawned = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref.isValid()) {
                store.removeEntity(ref, RemoveReason.REMOVE);
                despawned++;
            }
        }
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        int checkouts = pool.clearAllCheckouts();
        TownsfolkPoolPersistence.save(world, plugin, pool);
        LOGGER.atInfo().log(
            "Cleared townsfolk pool in world %s: despawned %s entities, released %s checkouts",
            world.getName(),
            despawned,
            checkouts
        );
        return despawned;
    }

    public static void reconcileAfterWorldLoad(@Nonnull World world, @Nonnull AetherhavenPlugin plugin) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        if (store == null) {
            return;
        }
        TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
        List<String> toRelease = new ArrayList<>();
        for (TownsfolkPoolCheckoutRecord rec : pool.getCheckouts().values()) {
            UUID entityId;
            try {
                entityId = UUID.fromString(rec.getEntityUuid());
            } catch (IllegalArgumentException e) {
                toRelease.add(rec.getCharacterId());
                continue;
            }
            Ref<EntityStore> ref = store.getExternalData().getRefFromUUID(entityId);
            if (ref == null || !ref.isValid()) {
                toRelease.add(rec.getCharacterId());
            }
        }
        for (String id : toRelease) {
            pool.release(id);
        }
        if (!toRelease.isEmpty()) {
            TownsfolkPoolPersistence.save(world, plugin, pool);
            LOGGER.atInfo().log("Released %s townsfolk pool entries with missing entities in world %s", toRelease.size(), world.getName());
        }
    }

    @Nonnull
    private static String shortHex(@Nonnull UUID townId) {
        String hex = townId.toString().replace("-", "");
        return hex.length() >= 8 ? hex.substring(0, 8) : hex;
    }

    private static void applyCharacterAppearance(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull String modelAssetId,
        @Nullable Float modelScale
    ) {
        if (modelScale != null) {
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null) {
                npc.setInitialModelScale(modelScale);
            }
        }
        NPCEntity.setAppearance(ref, modelAssetId, store);
        if (modelScale != null) {
            ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
            if (modelComponent != null) {
                store.putComponent(
                    ref,
                    PersistentModel.getComponentType(),
                    new PersistentModel(modelComponent.getModel().toReference())
                );
            }
        }
    }
}
