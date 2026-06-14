package com.hexvane.aetherhaven.patrol;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Runtime guard patrol progress (waypoint index, pause, route rotation). */
public final class GuardPatrolState implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<GuardPatrolState> CODEC = BuilderCodec.builder(
            GuardPatrolState.class,
            GuardPatrolState::new
        )
        .append(
            new KeyedCodec<>("ActiveRouteId", Codec.UUID_BINARY),
            (c, u) -> c.activeRouteId = u,
            c -> c.activeRouteId
        )
        .add()
        .append(
            new KeyedCodec<>("NodeIndex", Codec.INTEGER),
            (c, i) -> c.nodeIndex = i != null && i >= 0 ? i : 0,
            c -> c.nodeIndex
        )
        .add()
        .append(
            new KeyedCodec<>("PauseUntilMs", Codec.LONG),
            (c, v) -> c.pauseUntilMs = v != null ? Math.max(0L, v) : 0L,
            c -> c.pauseUntilMs
        )
        .add()
        .append(
            new KeyedCodec<>("RouteSlot", Codec.INTEGER),
            (c, i) -> c.routeSlot = i != null && i >= 0 ? i : 0,
            c -> c.routeSlot
        )
        .add()
        .append(
            new KeyedCodec<>("Forward", Codec.BOOLEAN),
            (c, v) -> c.forward = v == null || v,
            c -> c.forward
        )
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<EntityStore, GuardPatrolState> componentType;

    @Nullable
    private UUID activeRouteId;
    private int nodeIndex;
    private long pauseUntilMs;
    private int routeSlot;
    private boolean forward = true;

    @Nonnull
    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(
            GuardPatrolState.class,
            "AetherhavenGuardPatrolState",
            GuardPatrolState.CODEC
        );
    }

    @Nonnull
    public static ComponentType<EntityStore, GuardPatrolState> getComponentType() {
        ComponentType<EntityStore, GuardPatrolState> t = componentType;
        if (t == null) {
            throw new IllegalStateException("GuardPatrolState not registered");
        }
        return t;
    }

    @Nullable
    public UUID getActiveRouteId() {
        return activeRouteId;
    }

    public void setActiveRouteId(@Nullable UUID activeRouteId) {
        this.activeRouteId = activeRouteId;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = Math.max(0, nodeIndex);
    }

    public long getPauseUntilMs() {
        return pauseUntilMs;
    }

    public void setPauseUntilMs(long pauseUntilMs) {
        this.pauseUntilMs = Math.max(0L, pauseUntilMs);
    }

    public int getRouteSlot() {
        return routeSlot;
    }

    public void setRouteSlot(int routeSlot) {
        this.routeSlot = Math.max(0, routeSlot);
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void resetProgress() {
        nodeIndex = 0;
        pauseUntilMs = 0L;
        forward = true;
    }

    public static void resetForGuard(
        @Nonnull Ref<EntityStore> guardRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull UUID routeId
    ) {
        GuardPatrolState st = commandBuffer.getComponent(guardRef, GuardPatrolState.getComponentType());
        if (st == null) {
            st = new GuardPatrolState();
            commandBuffer.addComponent(guardRef, GuardPatrolState.getComponentType(), st);
        }
        st.setActiveRouteId(routeId);
        st.resetProgress();
        commandBuffer.putComponent(guardRef, GuardPatrolState.getComponentType(), st);
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        GuardPatrolState c = new GuardPatrolState();
        c.activeRouteId = this.activeRouteId;
        c.nodeIndex = this.nodeIndex;
        c.pauseUntilMs = this.pauseUntilMs;
        c.routeSlot = this.routeSlot;
        c.forward = this.forward;
        return c;
    }
}
