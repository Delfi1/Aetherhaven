package com.hexvane.aetherhaven.villager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Builds NPC spawn models and re-applies {@link PersistentModel} to fix client texture glitches after rapid spawns. */
public final class NpcModelSpawnUtil {
    private NpcModelSpawnUtil() {}

    @Nullable
    public static Model buildScaledModel(@Nonnull String modelAssetId, @Nullable Float modelScale) {
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (asset == null) {
            return null;
        }
        float scale = modelScale != null && modelScale > 0f ? modelScale : asset.generateRandomScale();
        return Model.createScaledModel(asset, scale);
    }

    public static float resolveSpawnScale(@Nonnull String modelAssetId, @Nullable Float modelScale) {
        if (modelScale != null && modelScale > 0f) {
            return modelScale;
        }
        ModelAsset asset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        return asset != null ? asset.generateRandomScale() : 1.0f;
    }

    /**
     * Rebuilds {@link ModelComponent} from {@link PersistentModel}. Safe to call after spawn when the client may have
     * cached the wrong attachment textures from an intermediate appearance.
     */
    public static void resyncFromPersistentModel(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PersistentModel persistent = store.getComponent(ref, PersistentModel.getComponentType());
        if (persistent == null) {
            return;
        }
        Model fresh = persistent.getModelReference().toModel();
        if (fresh == null) {
            return;
        }
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(fresh));
    }
}
