package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Last opened tab in the Town Journal UI, persisted on the player entity. */
public final class PlayerTownJournalState implements Component<EntityStore> {
    public enum JournalTab {
        TOWN,
        GUIDE,
        QUESTS,
        SETTINGS;

        @Nonnull
        public static JournalTab fromPersisted(@Nullable String s) {
            if (s == null || s.isBlank()) {
                return QUESTS;
            }
            return switch (s.trim().toUpperCase()) {
                case "TOWN" -> TOWN;
                case "GUIDE" -> GUIDE;
                case "SETTINGS" -> SETTINGS;
                default -> QUESTS;
            };
        }

        @Nonnull
        public String persisted() {
            return name();
        }
    }

    public enum SettingsSubTab {
        PERSONAL,
        SERVER;

        @Nonnull
        public static SettingsSubTab fromPersisted(@Nullable String s) {
            if (s == null || s.isBlank()) {
                return PERSONAL;
            }
            return switch (s.trim().toUpperCase()) {
                case "SERVER" -> SERVER;
                default -> PERSONAL;
            };
        }

        @Nonnull
        public String persisted() {
            return name();
        }
    }

    @Nonnull
    public static final BuilderCodec<PlayerTownJournalState> CODEC =
        BuilderCodec.builder(PlayerTownJournalState.class, PlayerTownJournalState::new)
            .append(
                new KeyedCodec<>("LastTab", Codec.STRING),
                (c, v) -> c.lastTab = JournalTab.fromPersisted(v),
                c -> c.lastTab.persisted())
            .add()
            .append(
                new KeyedCodec<>("ShowTownBordersOnMap", Codec.BOOLEAN),
                (c, v) -> {
                    if (v != null) {
                        c.showTownBordersOnMap = v;
                    }
                },
                c -> c.showTownBordersOnMap)
            .add()
            .append(
                new KeyedCodec<>("LastSettingsSubTab", Codec.STRING),
                (c, v) -> c.lastSettingsSubTab = SettingsSubTab.fromPersisted(v),
                c -> c.lastSettingsSubTab.persisted())
            .add()
            .append(
                new KeyedCodec<>("RtsPickFovOverride", Codec.FLOAT),
                (c, v) -> {
                    if (v != null) {
                        c.rtsPickFovOverride = v;
                    }
                },
                c -> c.rtsPickFovOverride)
            .add()
            .append(
                new KeyedCodec<>("RtsPickAspectOverride", Codec.FLOAT),
                (c, v) -> {
                    if (v != null) {
                        c.rtsPickAspectOverride = v;
                    }
                },
                c -> c.rtsPickAspectOverride)
            .add()
            .build();

    @Nullable
    private static volatile ComponentType<EntityStore, PlayerTownJournalState> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType =
            registry.registerComponent(PlayerTownJournalState.class, "AetherhavenPlayerTownJournalState", PlayerTownJournalState.CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, PlayerTownJournalState> getComponentType() {
        ComponentType<EntityStore, PlayerTownJournalState> t = componentType;
        if (t == null) {
            throw new IllegalStateException("PlayerTownJournalState not registered");
        }
        return t;
    }

    @Nonnull
    private JournalTab lastTab = JournalTab.QUESTS;

    private boolean showTownBordersOnMap = true;

    @Nonnull
    private SettingsSubTab lastSettingsSubTab = SettingsSubTab.PERSONAL;

    /** {@code <= 0} means use {@link AetherhavenConstants} default. */
    private float rtsPickFovOverride;

    private float rtsPickAspectOverride;

    public PlayerTownJournalState() {}

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        PlayerTownJournalState c = new PlayerTownJournalState();
        c.lastTab = lastTab;
        c.showTownBordersOnMap = showTownBordersOnMap;
        c.lastSettingsSubTab = lastSettingsSubTab;
        c.rtsPickFovOverride = rtsPickFovOverride;
        c.rtsPickAspectOverride = rtsPickAspectOverride;
        return c;
    }

    @Nonnull
    public JournalTab getLastTab() {
        return lastTab;
    }

    public void setLastTab(@Nonnull JournalTab tab) {
        this.lastTab = tab;
    }

    public boolean isShowTownBordersOnMap() {
        return showTownBordersOnMap;
    }

    public void setShowTownBordersOnMap(boolean showTownBordersOnMap) {
        this.showTownBordersOnMap = showTownBordersOnMap;
    }

    @Nonnull
    public SettingsSubTab getLastSettingsSubTab() {
        return lastSettingsSubTab;
    }

    public void setLastSettingsSubTab(@Nonnull SettingsSubTab lastSettingsSubTab) {
        this.lastSettingsSubTab = lastSettingsSubTab;
    }

    public void clearRtsPickOverrides() {
        rtsPickFovOverride = 0f;
        rtsPickAspectOverride = 0f;
    }

    public void setRtsPickOverrides(float verticalFovDeg, float aspectRatio) {
        rtsPickFovOverride = verticalFovDeg;
        rtsPickAspectOverride = aspectRatio;
    }

    public float effectiveRtsPickVerticalFovDeg() {
        return rtsPickFovOverride > 0f ? rtsPickFovOverride : AetherhavenConstants.RTS_COMMAND_PICK_VERTICAL_FOV_DEG;
    }

    public float effectiveRtsPickAspectRatio() {
        return rtsPickAspectOverride > 0f ? rtsPickAspectOverride : AetherhavenConstants.RTS_COMMAND_PICK_ASPECT_RATIO;
    }
}
