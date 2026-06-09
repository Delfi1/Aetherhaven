package com.hexvane.aetherhaven.questboard;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Tags quest-board raid mobs so only their deaths count toward raid progress. */
public final class RaidQuestMobBinding implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<RaidQuestMobBinding> CODEC =
        BuilderCodec.builder(RaidQuestMobBinding.class, RaidQuestMobBinding::new)
            .append(new KeyedCodec<>("TownId", Codec.STRING), (b, v) -> b.townId = v != null ? v : "", b -> b.townId)
            .add()
            .append(
                new KeyedCodec<>("BoardInstanceId", Codec.STRING),
                (b, v) -> b.boardInstanceId = v != null ? v : "",
                b -> b.boardInstanceId
            )
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, RaidQuestMobBinding> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(RaidQuestMobBinding.class, "AetherhavenRaidQuestMobBinding", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, RaidQuestMobBinding> getComponentType() {
        ComponentType<EntityStore, RaidQuestMobBinding> t = componentType;
        if (t == null) {
            throw new IllegalStateException("RaidQuestMobBinding not registered");
        }
        return t;
    }

    private String townId = "";
    private String boardInstanceId = "";
    private double marchLeashX;
    private double marchLeashY;
    private double marchLeashZ;
    private long nextMarchAdvanceEpochMs;
    private boolean marchInitialized;

    public RaidQuestMobBinding() {}

    public RaidQuestMobBinding(@Nonnull UUID townId, @Nonnull String boardInstanceId) {
        this.townId = townId.toString();
        this.boardInstanceId = boardInstanceId.trim();
    }

    @Nonnull
    public UUID getTownId() {
        return UUID.fromString(townId);
    }

    @Nonnull
    public String getBoardInstanceId() {
        return boardInstanceId;
    }

    public boolean isMarchInitialized() {
        return marchInitialized;
    }

    public void setMarchInitialized(boolean marchInitialized) {
        this.marchInitialized = marchInitialized;
    }

    public long getNextMarchAdvanceEpochMs() {
        return nextMarchAdvanceEpochMs;
    }

    public void setNextMarchAdvanceEpochMs(long nextMarchAdvanceEpochMs) {
        this.nextMarchAdvanceEpochMs = nextMarchAdvanceEpochMs;
    }

    @Nonnull
    public Vector3d getMarchLeash() {
        return new Vector3d(marchLeashX, marchLeashY, marchLeashZ);
    }

    public void setMarchLeash(@Nonnull Vector3d leash) {
        this.marchLeashX = leash.x;
        this.marchLeashY = leash.y;
        this.marchLeashZ = leash.z;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        RaidQuestMobBinding copy = new RaidQuestMobBinding(getTownId(), boardInstanceId);
        copy.marchLeashX = marchLeashX;
        copy.marchLeashY = marchLeashY;
        copy.marchLeashZ = marchLeashZ;
        copy.nextMarchAdvanceEpochMs = nextMarchAdvanceEpochMs;
        copy.marchInitialized = marchInitialized;
        return copy;
    }
}
