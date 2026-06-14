package com.hexvane.aetherhaven.poi.marker;

import com.hexvane.aetherhaven.poi.PoiInteractionKind;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Serialized POI configuration on a prefab-saveable marker entity. */
public final class PoiMarkerDataComponent implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<PoiMarkerDataComponent> CODEC =
        BuilderCodec.builder(PoiMarkerDataComponent.class, PoiMarkerDataComponent::new)
            .append(new KeyedCodec<>("PoiRegistryId", Codec.UUID_BINARY), (c, u) -> c.poiRegistryId = u, c -> c.poiRegistryId)
            .add()
            .append(new KeyedCodec<>("TagsCsv", Codec.STRING), (c, s) -> c.tags = parseCsv(s), c -> String.join(",", c.tags))
            .add()
            .append(new KeyedCodec<>("Capacity", Codec.INTEGER), (c, v) -> c.capacity = Math.max(1, v), c -> c.capacity)
            .add()
            .append(new KeyedCodec<>("InteractionKind", Codec.STRING), (c, s) -> c.interactionKind = PoiInteractionKind.fromJson(s), c -> c.interactionKind.name())
            .add()
            .append(new KeyedCodec<>("MountOnUse", Codec.BOOLEAN), (c, v) -> c.mountOnUse = v, c -> c.mountOnUse)
            .add()
            .append(new KeyedCodec<>("EquipmentProfileId", Codec.STRING), (c, s) -> c.equipmentProfileId = blankToNull(s), c -> c.equipmentProfileId != null ? c.equipmentProfileId : "")
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, PoiMarkerDataComponent> componentType;

    @Nullable
    private UUID poiRegistryId;
    @Nonnull
    private Set<String> tags = Set.of();
    private int capacity = 1;
    @Nonnull
    private PoiInteractionKind interactionKind = PoiInteractionKind.NONE;
    private boolean mountOnUse = true;
    @Nullable
    private String equipmentProfileId;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(PoiMarkerDataComponent.class, "AetherhavenPoiMarkerData", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, PoiMarkerDataComponent> getComponentType() {
        ComponentType<EntityStore, PoiMarkerDataComponent> t = componentType;
        if (t == null) {
            throw new IllegalStateException("PoiMarkerDataComponent not registered");
        }
        return t;
    }

    public PoiMarkerDataComponent() {}

    public PoiMarkerDataComponent(
        @Nonnull UUID poiRegistryId,
        @Nonnull Set<String> tags,
        int capacity,
        @Nonnull PoiInteractionKind interactionKind,
        boolean mountOnUse,
        @Nullable String equipmentProfileId
    ) {
        this.poiRegistryId = poiRegistryId;
        this.tags = new HashSet<>(tags);
        this.capacity = Math.max(1, capacity);
        this.interactionKind = interactionKind;
        this.mountOnUse = mountOnUse;
        this.equipmentProfileId = blankToNull(equipmentProfileId);
    }

    @Nullable
    public UUID getPoiRegistryId() {
        return poiRegistryId;
    }

    public void setPoiRegistryId(@Nonnull UUID poiRegistryId) {
        this.poiRegistryId = poiRegistryId;
    }

    @Nonnull
    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public int getCapacity() {
        return capacity;
    }

    @Nonnull
    public PoiInteractionKind getInteractionKind() {
        return interactionKind;
    }

    public boolean isMountOnUse() {
        return mountOnUse;
    }

    @Nullable
    public String getEquipmentProfileId() {
        return equipmentProfileId;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PoiMarkerDataComponent(
            poiRegistryId != null ? poiRegistryId : new UUID(0L, 0L),
            tags,
            capacity,
            interactionKind,
            mountOnUse,
            equipmentProfileId
        );
    }

    @Nonnull
    private static Set<String> parseCsv(@Nullable String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim().toUpperCase());
            }
        }
        return Set.copyOf(out);
    }

    @Nullable
    private static String blankToNull(@Nullable String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
