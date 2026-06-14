package com.hexvane.aetherhaven.npc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Throttles proximity checks and tracks active reputation-wave emotes on town NPCs. */
public final class NpcReputationWaveState implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<NpcReputationWaveState> CODEC =
        BuilderCodec.builder(NpcReputationWaveState.class, NpcReputationWaveState::new)
            .append(new KeyedCodec<>("NextCheckMs", Codec.LONG), (v, x) -> v.nextCheckMs = x != null ? x : 0L, v -> v.nextCheckMs)
            .add()
            .append(new KeyedCodec<>("WaveUntilMs", Codec.LONG), (v, x) -> v.waveUntilMs = x != null ? x : 0L, v -> v.waveUntilMs)
            .add()
            .append(new KeyedCodec<>("CooldownUntilMs", Codec.LONG), (v, x) -> v.cooldownUntilMs = x != null ? x : 0L, v -> v.cooldownUntilMs)
            .add()
            .append(new KeyedCodec<>("WaveTargetX", Codec.DOUBLE), (v, x) -> v.waveTargetX = x, v -> v.waveTargetX)
            .add()
            .append(new KeyedCodec<>("WaveTargetY", Codec.DOUBLE), (v, x) -> v.waveTargetY = x, v -> v.waveTargetY)
            .add()
            .append(new KeyedCodec<>("WaveTargetZ", Codec.DOUBLE), (v, x) -> v.waveTargetZ = x, v -> v.waveTargetZ)
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, NpcReputationWaveState> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(NpcReputationWaveState.class, "AetherhavenNpcReputationWaveState", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, NpcReputationWaveState> getComponentType() {
        ComponentType<EntityStore, NpcReputationWaveState> t = componentType;
        if (t == null) {
            throw new IllegalStateException("NpcReputationWaveState not registered");
        }
        return t;
    }

    private long nextCheckMs;
    private long waveUntilMs;
    private long cooldownUntilMs;
    @Nullable
    private Double waveTargetX;
    @Nullable
    private Double waveTargetY;
    @Nullable
    private Double waveTargetZ;

    public NpcReputationWaveState() {}

    @Nonnull
    public static NpcReputationWaveState fresh() {
        return new NpcReputationWaveState();
    }

    public long getNextCheckMs() {
        return nextCheckMs;
    }

    public void setNextCheckMs(long nextCheckMs) {
        this.nextCheckMs = nextCheckMs;
    }

    public long getWaveUntilMs() {
        return waveUntilMs;
    }

    public void setWaveUntilMs(long waveUntilMs) {
        this.waveUntilMs = waveUntilMs;
    }

    public long getCooldownUntilMs() {
        return cooldownUntilMs;
    }

    public void setCooldownUntilMs(long cooldownUntilMs) {
        this.cooldownUntilMs = cooldownUntilMs;
    }

    public boolean isWaveActive(long nowMs) {
        return waveUntilMs > nowMs;
    }

    public boolean isOnCooldown(long nowMs) {
        return cooldownUntilMs > nowMs;
    }

    public boolean hasWaveTarget() {
        return waveTargetX != null && waveTargetY != null && waveTargetZ != null;
    }

    @Nonnull
    public Vector3d waveTarget() {
        return new Vector3d(waveTargetX != null ? waveTargetX : 0.0, waveTargetY != null ? waveTargetY : 0.0, waveTargetZ != null ? waveTargetZ : 0.0);
    }

    public void setWaveTarget(@Nonnull Vector3d target) {
        waveTargetX = target.x;
        waveTargetY = target.y;
        waveTargetZ = target.z;
    }

    public void clearWaveTarget() {
        waveTargetX = null;
        waveTargetY = null;
        waveTargetZ = null;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        NpcReputationWaveState c = new NpcReputationWaveState();
        c.nextCheckMs = nextCheckMs;
        c.waveUntilMs = waveUntilMs;
        c.cooldownUntilMs = cooldownUntilMs;
        c.waveTargetX = waveTargetX;
        c.waveTargetY = waveTargetY;
        c.waveTargetZ = waveTargetZ;
        return c;
    }
}
