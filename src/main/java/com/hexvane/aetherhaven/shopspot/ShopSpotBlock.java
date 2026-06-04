package com.hexvane.aetherhaven.shopspot;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShopSpotBlock implements Component<ChunkStore> {
    @Nonnull
    public static final BuilderCodec<ShopSpotBlock> CODEC = BuilderCodec.builder(ShopSpotBlock.class, ShopSpotBlock::new)
        .append(new KeyedCodec<>("SpotId", Codec.STRING), (s, v) -> s.spotId = v != null ? v : "", s -> s.spotId)
        .add()
        .append(new KeyedCodec<>("TownId", Codec.STRING), (s, v) -> s.townId = v != null ? v : "", s -> s.townId)
        .add()
        .append(new KeyedCodec<>("PlotId", Codec.STRING), (s, v) -> s.plotId = v != null ? v : "", s -> s.plotId)
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<ChunkStore, ShopSpotBlock> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<ChunkStore> registry) {
        componentType = registry.registerComponent(ShopSpotBlock.class, "AetherhavenShopSpotBlock", CODEC);
    }

    @Nonnull
    public static ComponentType<ChunkStore, ShopSpotBlock> getComponentType() {
        ComponentType<ChunkStore, ShopSpotBlock> t = componentType;
        if (t == null) {
            throw new IllegalStateException("ShopSpotBlock not registered");
        }
        return t;
    }

    private String spotId = "";
    private String townId = "";
    private String plotId = "";

    public ShopSpotBlock() {}

    public ShopSpotBlock(@Nonnull String spotId, @Nonnull String townId, @Nonnull String plotId) {
        this.spotId = spotId != null ? spotId : "";
        this.townId = townId != null ? townId : "";
        this.plotId = plotId != null ? plotId : "";
    }

    @Nonnull
    public String getSpotId() {
        return spotId;
    }

    @Nonnull
    public String getTownId() {
        return townId;
    }

    @Nonnull
    public String getPlotId() {
        return plotId;
    }

    public void setSpotId(@Nonnull String spotId) {
        this.spotId = spotId != null ? spotId : "";
    }

    public void setTownId(@Nonnull String townId) {
        this.townId = townId != null ? townId : "";
    }

    public void setPlotId(@Nonnull String plotId) {
        this.plotId = plotId != null ? plotId : "";
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ShopSpotBlock(spotId, townId, plotId);
    }
}
