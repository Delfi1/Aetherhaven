package com.hexvane.aetherhaven.bard;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Active bard song performance on an NPC entity. */
public final class BardPerformanceComponent implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<BardPerformanceComponent> CODEC =
        BuilderCodec.builder(BardPerformanceComponent.class, BardPerformanceComponent::new)
            .append(new KeyedCodec<>("SongId", Codec.STRING), (c, v) -> c.songId = v != null ? v : "", c -> c.songId)
            .add()
            .append(
                new KeyedCodec<>("EndAtEpochMs", Codec.LONG),
                (c, v) -> c.endAtEpochMs = v,
                c -> c.endAtEpochMs
            )
            .add()
            .append(
                new KeyedCodec<>("LastParticleSpawnMs", Codec.LONG),
                (c, v) -> c.lastParticleSpawnMs = v,
                c -> c.lastParticleSpawnMs
            )
            .add()
            .append(
                new KeyedCodec<>("AmbienceFxIndex", Codec.INTEGER),
                (c, v) -> c.ambienceFxIndex = v,
                c -> c.ambienceFxIndex
            )
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, BardPerformanceComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType =
            registry.registerComponent(BardPerformanceComponent.class, "AetherhavenBardPerformance", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, BardPerformanceComponent> getComponentType() {
        ComponentType<EntityStore, BardPerformanceComponent> t = componentType;
        if (t == null) {
            throw new IllegalStateException("BardPerformanceComponent not registered");
        }
        return t;
    }

    @Nonnull
    private String songId = "";
    private long endAtEpochMs = 0L;
    private long lastParticleSpawnMs = 0L;
    private int ambienceFxIndex = 0;

    public BardPerformanceComponent() {}

    public BardPerformanceComponent(@Nonnull String songId, long endAtEpochMs, int ambienceFxIndex) {
        this.songId = songId;
        this.endAtEpochMs = endAtEpochMs;
        this.ambienceFxIndex = ambienceFxIndex;
    }

    @Nonnull
    public String getSongId() {
        return songId;
    }

    public long getEndAtEpochMs() {
        return endAtEpochMs;
    }

    public long getLastParticleSpawnMs() {
        return lastParticleSpawnMs;
    }

    public void setLastParticleSpawnMs(long lastParticleSpawnMs) {
        this.lastParticleSpawnMs = lastParticleSpawnMs;
    }

    public int getMusicContainerIndex() {
        return ambienceFxIndex;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        BardPerformanceComponent c = new BardPerformanceComponent(songId, endAtEpochMs, ambienceFxIndex);
        c.lastParticleSpawnMs = lastParticleSpawnMs;
        return c;
    }
}
