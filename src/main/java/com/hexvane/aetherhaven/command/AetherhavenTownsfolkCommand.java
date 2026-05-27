package com.hexvane.aetherhaven.command;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkAssignmentKinds;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolCheckoutRecord;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolPersistence;
import com.hexvane.aetherhaven.townsfolk.TownsfolkPoolState;
import com.hexvane.aetherhaven.townsfolk.TownsfolkSpawnService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public final class AetherhavenTownsfolkCommand extends AbstractCommandCollection {
    public AetherhavenTownsfolkCommand() {
        super("townsfolk", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.desc");
        this.setPermissionGroups("hytale:WorldEditor");
        this.addSubCommand(new SpawnSubCommand());
        this.addSubCommand(new ReleaseSubCommand());
        this.addSubCommand(new ListSubCommand());
    }

    @Nullable
    private static TownRecord townForPlayer(
        @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull World world
    ) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return null;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return null;
        }
        return AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin).findTownForPlayerInWorld(uc.getUuid());
    }

    private static final class SpawnSubCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> characterIdArg =
            this.withOptionalArg("characterId", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.characterId.desc", ArgTypes.STRING);
        private final OptionalArg<String> assignmentArg =
            this.withOptionalArg("assignmentKind", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.assignmentKind.desc", ArgTypes.STRING);

        SpawnSubCommand() {
            super("spawn", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.spawn.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null) {
                ctx.sendMessage(Message.translation("aetherhaven_common.aetherhaven.common.pluginNotLoaded"));
                return;
            }
            TownRecord town = townForPlayer(store, ref, world);
            if (town == null) {
                ctx.sendMessage(Message.translation("aetherhaven_ui_shell.aetherhaven.ui.questJournal.needTown"));
                return;
            }
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d pos = new Vector3d(transform.getPosition());
            String assignment = assignmentArg.provided(ctx) ? assignmentArg.get(ctx) : TownsfolkAssignmentKinds.IDLE;
            String characterId = characterIdArg.provided(ctx) ? characterIdArg.get(ctx) : null;
            var spawned =
                TownsfolkSpawnService.trySpawn(
                    world,
                    plugin,
                    town,
                    store,
                    pos,
                    assignment,
                    characterId,
                    new Random()
                );
            if (spawned.isEmpty()) {
                ctx.sendMessage(Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.spawn.failed"));
                return;
            }
            TownsfolkSpawnService.SpawnedTownsfolk s = spawned.get();
            ctx.sendMessage(
                Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.spawn.ok")
                    .param("id", s.characterId())
                    .param("assignment", s.assignmentKind())
            );
        }
    }

    private static final class ReleaseSubCommand extends AbstractPlayerCommand {
        private final com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg<String> characterIdArg =
            this.withRequiredArg("characterId", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.characterId.desc", ArgTypes.STRING);
        private final FlagArg despawnFlag = this.withFlagArg("despawn", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.despawn.desc");

        ReleaseSubCommand() {
            super("release", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.release.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null) {
                return;
            }
            String characterId = characterIdArg.get(ctx);
            TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
            TownsfolkPoolCheckoutRecord rec = pool.checkoutForCharacter(characterId);
            if (rec == null) {
                ctx.sendMessage(Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.release.notCheckedOut"));
                return;
            }
            if (despawnFlag.provided(ctx)) {
                try {
                    java.util.UUID entityId = java.util.UUID.fromString(rec.getEntityUuid());
                    Ref<EntityStore> npcRef = store.getExternalData().getRefFromUUID(entityId);
                    if (npcRef != null && npcRef.isValid()) {
                        store.removeEntity(npcRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
                    }
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
            TownsfolkSpawnService.release(world, plugin, characterId);
            ctx.sendMessage(
                Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.release.ok").param("id", characterId)
            );
        }
    }

    private static final class ListSubCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> assignmentArg =
            this.withOptionalArg("assignmentKind", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.assignmentKind.desc", ArgTypes.STRING);

        ListSubCommand() {
            super("list", "aetherhaven_commands_help.commands.aetherhaven.townsfolk.list.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null) {
                return;
            }
            String filter = assignmentArg.provided(ctx) ? assignmentArg.get(ctx) : null;
            TownsfolkPoolState pool = TownsfolkPoolPersistence.getOrLoad(world, plugin);
            if (filter != null && !filter.isBlank()) {
                List<String> available = TownsfolkSpawnService.availableCharacterIds(world, plugin, filter);
                ctx.sendMessage(
                    Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.list.available")
                        .param("assignment", filter)
                        .param("ids", String.join(", ", available))
                );
            } else {
                List<String> available = TownsfolkSpawnService.availableCharacterIds(world, plugin, TownsfolkAssignmentKinds.IDLE);
                ctx.sendMessage(
                    Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.list.availableIdle")
                        .param("ids", String.join(", ", available))
                );
            }
            StringBuilder checked = new StringBuilder();
            for (TownsfolkPoolCheckoutRecord rec : pool.getCheckouts().values()) {
                if (filter != null && !filter.isBlank() && !filter.equalsIgnoreCase(rec.getAssignmentKind())) {
                    continue;
                }
                if (!checked.isEmpty()) {
                    checked.append(", ");
                }
                checked.append(rec.getCharacterId()).append('@').append(rec.getAssignmentKind());
            }
            ctx.sendMessage(
                Message.translation("aetherhaven_commands_help.commands.aetherhaven.townsfolk.list.inUse")
                    .param("ids", checked.isEmpty() ? "(none)" : checked.toString())
            );
        }
    }
}
