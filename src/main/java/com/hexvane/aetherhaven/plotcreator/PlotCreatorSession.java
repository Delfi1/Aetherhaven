package com.hexvane.aetherhaven.plotcreator;

import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlotCreatorSession {
    @Nonnull
    private final UUID playerUuid;
    @Nonnull
    private final World world;
    @Nonnull
    private final PlotCreatorDraft draft = new PlotCreatorDraft();
    @Nullable
    private SimpleItemContainer materialsContainer;
    /** Substep index → item id → quantity granted for placement (revoked when stepping back). */
    @Nonnull
    private final Map<Integer, Map<String, Integer>> substepGrants = new HashMap<>();

    public PlotCreatorSession(@Nonnull UUID playerUuid, @Nonnull World world) {
        this.playerUuid = playerUuid;
        this.world = world;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @Nonnull
    public World getWorld() {
        return world;
    }

    @Nonnull
    public PlotCreatorDraft getDraft() {
        return draft;
    }

    @Nullable
    public SimpleItemContainer getMaterialsContainer() {
        return materialsContainer;
    }

    public void setMaterialsContainer(@Nullable SimpleItemContainer materialsContainer) {
        this.materialsContainer = materialsContainer;
    }

    @Nonnull
    public Map<Integer, Map<String, Integer>> getSubstepGrants() {
        return substepGrants;
    }
}
