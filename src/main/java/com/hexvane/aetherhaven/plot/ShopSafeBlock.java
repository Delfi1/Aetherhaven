package com.hexvane.aetherhaven.plot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShopSafeBlock implements Component<ChunkStore> {
    @Nonnull
    public static final BuilderCodec<ShopSafeBlock> CODEC = BuilderCodec.builder(ShopSafeBlock.class, ShopSafeBlock::new)
        .append(new KeyedCodec<>("PlotId", Codec.STRING), (s, v) -> s.plotId = v != null ? v : "", s -> s.plotId)
        .add()
        .append(new KeyedCodec<>("TownId", Codec.STRING), (s, v) -> s.townId = v != null ? v : "", s -> s.townId)
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<ChunkStore, ShopSafeBlock> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<ChunkStore> registry) {
        componentType = registry.registerComponent(ShopSafeBlock.class, "AetherhavenShopSafeBlock", CODEC);
    }

    @Nonnull
    public static ComponentType<ChunkStore, ShopSafeBlock> getComponentType() {
        ComponentType<ChunkStore, ShopSafeBlock> t = componentType;
        if (t == null) {
            throw new IllegalStateException("ShopSafeBlock not registered");
        }
        return t;
    }

    private String plotId = "";
    private String townId = "";

    public ShopSafeBlock() {}

    public ShopSafeBlock(@Nonnull String plotId, @Nonnull String townId) {
        this.plotId = plotId != null ? plotId : "";
        this.townId = townId != null ? townId : "";
    }

    @Nonnull
    public String getPlotId() {
        return plotId;
    }

    @Nonnull
    public String getTownId() {
        return townId;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ShopSafeBlock(plotId, townId);
    }
}
