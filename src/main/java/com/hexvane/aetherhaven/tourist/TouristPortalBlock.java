package com.hexvane.aetherhaven.tourist;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Prefab saveable tourist portal marker; runtime ids assigned when the plot completes. */
public final class TouristPortalBlock implements Component<ChunkStore> {
    @Nonnull
    public static final BuilderCodec<TouristPortalBlock> CODEC =
        BuilderCodec.builder(TouristPortalBlock.class, TouristPortalBlock::new)
            .append(new KeyedCodec<>("PortalId", Codec.STRING), (s, v) -> s.portalId = v != null ? v : "", s -> s.portalId)
            .add()
            .append(new KeyedCodec<>("TownId", Codec.STRING), (s, v) -> s.townId = v != null ? v : "", s -> s.townId)
            .add()
            .append(new KeyedCodec<>("PlotId", Codec.STRING), (s, v) -> s.plotId = v != null ? v : "", s -> s.plotId)
            .add()
            .append(new KeyedCodec<>("Configured", Codec.BOOLEAN), (s, v) -> s.configured = v, s -> s.configured)
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<ChunkStore, TouristPortalBlock> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<ChunkStore> registry) {
        componentType = registry.registerComponent(TouristPortalBlock.class, "AetherhavenTouristPortalBlock", CODEC);
    }

    @Nonnull
    public static ComponentType<ChunkStore, TouristPortalBlock> getComponentType() {
        ComponentType<ChunkStore, TouristPortalBlock> t = componentType;
        if (t == null) {
            throw new IllegalStateException("TouristPortalBlock not registered");
        }
        return t;
    }

    private String portalId = "";
    private String townId = "";
    private String plotId = "";
    private boolean configured;

    public TouristPortalBlock() {}

    public TouristPortalBlock(
        @Nonnull String portalId,
        @Nonnull String townId,
        @Nonnull String plotId,
        boolean configured
    ) {
        this.portalId = portalId != null ? portalId : "";
        this.townId = townId != null ? townId : "";
        this.plotId = plotId != null ? plotId : "";
        this.configured = configured;
    }

    @Nonnull
    public String getPortalId() {
        return portalId;
    }

    @Nonnull
    public String getTownId() {
        return townId;
    }

    @Nonnull
    public String getPlotId() {
        return plotId;
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isTemplatePlacement() {
        return townId == null || townId.isBlank() || plotId == null || plotId.isBlank();
    }

    public void applyRecord(@Nonnull TouristPortalRecord record) {
        portalId = record.getPortalId().toString();
        townId = record.getTownId().toString();
        plotId = record.getPlotId().toString();
        configured = true;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new TouristPortalBlock(portalId, townId, plotId, configured);
    }
}
