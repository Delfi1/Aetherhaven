package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CommandPostBlock implements Component<ChunkStore> {
    @Nonnull
    public static final BuilderCodec<CommandPostBlock> CODEC = BuilderCodec.builder(CommandPostBlock.class, CommandPostBlock::new)
        .append(new KeyedCodec<>("TownId", Codec.STRING), (b, v) -> b.townId = v != null ? v : "", b -> b.townId)
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<ChunkStore, CommandPostBlock> componentType;

    private String townId = "";

    public static void register(@Nonnull ComponentRegistryProxy<ChunkStore> registry) {
        componentType = registry.registerComponent(CommandPostBlock.class, "AetherhavenCommandPost", CODEC);
    }

    @Nonnull
    public static ComponentType<ChunkStore, CommandPostBlock> getComponentType() {
        ComponentType<ChunkStore, CommandPostBlock> t = componentType;
        if (t == null) {
            throw new IllegalStateException("CommandPostBlock not registered");
        }
        return t;
    }

    public CommandPostBlock() {}

    public CommandPostBlock(@Nonnull String townId) {
        this.townId = townId != null ? townId : "";
    }

    @Nonnull
    public String getTownId() {
        return townId;
    }

    public void setTownId(@Nonnull String townId) {
        this.townId = townId != null ? townId : "";
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new CommandPostBlock(townId);
    }
}
