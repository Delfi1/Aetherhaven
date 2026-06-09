package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Prefab-saveable shop spot marker; runtime IDs plus template configuration for prefab authoring. */
public final class ShopSpotBlock implements Component<ChunkStore> {
    @Nonnull
    public static final BuilderCodec<ShopSpotBlock> CODEC = BuilderCodec.builder(ShopSpotBlock.class, ShopSpotBlock::new)
        .append(new KeyedCodec<>("SpotId", Codec.STRING), (s, v) -> s.spotId = v != null ? v : "", s -> s.spotId)
        .add()
        .append(new KeyedCodec<>("TownId", Codec.STRING), (s, v) -> s.townId = v != null ? v : "", s -> s.townId)
        .add()
        .append(new KeyedCodec<>("PlotId", Codec.STRING), (s, v) -> s.plotId = v != null ? v : "", s -> s.plotId)
        .add()
        .append(new KeyedCodec<>("PlayerControlled", Codec.BOOLEAN), (s, v) -> s.playerControlled = v, s -> s.playerControlled)
        .add()
        .append(new KeyedCodec<>("LootTableId", Codec.STRING), (s, v) -> s.lootTableId = v != null ? v : "", s -> s.lootTableId)
        .add()
        .append(new KeyedCodec<>("Configured", Codec.BOOLEAN), (s, v) -> s.configured = v, s -> s.configured)
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
    private boolean playerControlled;
    private String lootTableId = "";
    private boolean configured;

    public ShopSpotBlock() {}

    public ShopSpotBlock(
        @Nonnull String spotId,
        @Nonnull String townId,
        @Nonnull String plotId,
        boolean playerControlled,
        @Nonnull String lootTableId,
        boolean configured
    ) {
        this.spotId = spotId != null ? spotId : "";
        this.townId = townId != null ? townId : "";
        this.plotId = plotId != null ? plotId : "";
        this.playerControlled = playerControlled;
        this.lootTableId = lootTableId != null ? lootTableId : "";
        this.configured = configured;
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

    public boolean isPlayerControlled() {
        return playerControlled;
    }

    @Nonnull
    public String getLootTableId() {
        return lootTableId != null ? lootTableId : "";
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isTemplatePlacement() {
        return townId == null || townId.isBlank() || plotId == null || plotId.isBlank();
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

    public void setPlayerControlled(boolean playerControlled) {
        this.playerControlled = playerControlled;
    }

    public void setLootTableId(@Nonnull String lootTableId) {
        this.lootTableId = lootTableId != null ? lootTableId : "";
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public void applyRecord(@Nonnull ShopSpotRecord record) {
        spotId = record.getSpotId().toString();
        townId = record.getTownId().toString();
        plotId = record.getPlotId().toString();
        playerControlled = record.isPlayerControlled();
        lootTableId = record.getLootTableId();
        configured = true;
    }

    public void applyToRecord(@Nonnull ShopSpotRecord record) {
        record.setPlayerControlled(playerControlled);
        if (!playerControlled) {
            String table = lootTableId != null && !lootTableId.isBlank() ? lootTableId.trim() : AetherhavenConstants.SHOP_LOOT_TABLE_GIFTS;
            record.setLootTableId(table);
        } else {
            record.setItemId(null);
            record.setStock(0);
            record.setSellerUuid(null);
        }
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ShopSpotBlock(spotId, townId, plotId, playerControlled, lootTableId, configured);
    }
}
