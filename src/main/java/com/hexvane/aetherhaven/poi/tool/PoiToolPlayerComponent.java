package com.hexvane.aetherhaven.poi.tool;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** POI tool selection + transient debug label entity ids (labels are not persisted). */
public final class PoiToolPlayerComponent implements Component<EntityStore> {
    /** Serialized stand-in for "no selection" (codec cannot store null UUID). */
    @Nonnull
    private static final UUID NO_SELECTION = new UUID(0L, 0L);

    @Nonnull
    public static final BuilderCodec<PoiToolPlayerComponent> CODEC = BuilderCodec.builder(PoiToolPlayerComponent.class, PoiToolPlayerComponent::new)
        .append(
            new KeyedCodec<>("SelectedPoiId", Codec.UUID_BINARY),
            (c, u) -> c.selectedPoiId = u != null && !NO_SELECTION.equals(u) ? u : null,
            c -> c.selectedPoiId != null ? c.selectedPoiId : NO_SELECTION
        )
        .add()
        .append(
            new KeyedCodec<>("Mode", Codec.STRING),
            (c, s) -> c.mode = PoiToolMode.fromSerialized(s),
            c -> c.mode.name()
        )
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<EntityStore, PoiToolPlayerComponent> componentType;

    @Nullable
    private UUID selectedPoiId;
    @Nonnull
    private PoiToolMode mode = PoiToolMode.PoiEdit;
    /** Not serialized; cleared when tool is unequipped. */
    @Nonnull
    private final List<UUID> debugLabelEntityUuids = new ArrayList<>();
    /** Pending POI placement target block (not serialized). */
    @Nullable
    private Vector3i pendingPlacementBlock;
    @Nullable
    private UUID pendingTownId;
    @Nullable
    private UUID pendingPlotId;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(PoiToolPlayerComponent.class, "AetherhavenPoiTool", PoiToolPlayerComponent.CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, PoiToolPlayerComponent> getComponentType() {
        ComponentType<EntityStore, PoiToolPlayerComponent> t = componentType;
        if (t == null) {
            throw new IllegalStateException("PoiToolPlayerComponent not registered");
        }
        return t;
    }

    @Nullable
    public UUID getSelectedPoiId() {
        return selectedPoiId;
    }

    public void setSelectedPoiId(@Nullable UUID selectedPoiId) {
        this.selectedPoiId = selectedPoiId;
    }

    @Nonnull
    public PoiToolMode getMode() {
        return mode;
    }

    public void setMode(@Nonnull PoiToolMode mode) {
        this.mode = mode;
    }

    /** Cycles: POI edit -> POI placement -> POI remove -> adventurer spawn markers -> POI edit. */
    public void cycleMode() {
        this.mode =
            switch (mode) {
                case PoiEdit -> PoiToolMode.PoiPlacement;
                case PoiPlacement -> PoiToolMode.PoiRemove;
                case PoiRemove -> PoiToolMode.AdventurerSpawnMarker;
                case AdventurerSpawnMarker -> PoiToolMode.PoiEdit;
            };
    }

    @Nonnull
    public List<UUID> getDebugLabelEntityUuids() {
        return debugLabelEntityUuids;
    }

    public void clearDebugLabels() {
        debugLabelEntityUuids.clear();
    }

    public void setPendingPlacement(@Nullable Vector3i block, @Nullable UUID townId, @Nullable UUID plotId) {
        this.pendingPlacementBlock = block != null ? new Vector3i(block) : null;
        this.pendingTownId = townId;
        this.pendingPlotId = plotId;
    }

    @Nullable
    public Vector3i getPendingPlacementBlock() {
        return pendingPlacementBlock;
    }

    @Nullable
    public UUID getPendingTownId() {
        return pendingTownId;
    }

    @Nullable
    public UUID getPendingPlotId() {
        return pendingPlotId;
    }

    public void clearPendingPlacement() {
        pendingPlacementBlock = null;
        pendingTownId = null;
        pendingPlotId = null;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        PoiToolPlayerComponent c = new PoiToolPlayerComponent();
        c.selectedPoiId = this.selectedPoiId;
        c.mode = this.mode;
        return c;
    }
}
