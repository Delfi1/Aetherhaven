package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.plotcreator.icon.PlotCreatorIconExporter;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.saving.PrefabSaveContributor;
import com.hexvane.aetherhaven.shopspot.ShopSpotDisplayService;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.blocktype.component.BlockPhysics;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.LocalCachedChunkAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/**
 * Exports the plot creator bounds using the same pipeline as the built-in builder tools
 * ({@link BuilderToolsPlugin} {@code saveFromSelection} / {@link com.hypixel.hytale.builtin.buildertools.prefabeditor.saving.PrefabSaver}):
 * world chunk copy via {@link BlockSelection#copyFromAtWorld}, optional entities, {@link PrefabSaveContributor}s, then
 * {@link BlockSelection#relativize()} and {@link PrefabStore#savePrefab}.
 */
public final class PlotCreatorPrefabExporter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private PlotCreatorPrefabExporter() {}

    public static boolean export(
        @Nonnull World world,
        @Nonnull PlotCreatorDraft draft,
        @Nonnull Path outputFile,
        boolean overwrite
    ) {
        return export(world, draft, outputFile, overwrite, null);
    }

    public static boolean export(
        @Nonnull World world,
        @Nonnull PlotCreatorDraft draft,
        @Nonnull Path outputFile,
        boolean overwrite,
        @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        Vector3i min = draft.boundsMin();
        Vector3i max = draft.boundsMax();
        Vector3i anchor = draft.getPlotAnchor();
        if (anchor == null) {
            return false;
        }
        draft.setPrefabOriginMin(new Vector3i(min));
        PlotCreatorLocalCoords.recomputeAnchorOffset(draft);

        Store<EntityStore> entityStore = world.getEntityStore() != null ? world.getEntityStore().getStore() : null;
        if (entityStore != null && !draft.getAdventurerSpawns().isEmpty()) {
            if (commandBuffer != null) {
                PlotCreatorAdventurerMarkers.syncAll(world, commandBuffer, draft);
            } else {
                PlotCreatorAdventurerMarkers.syncAll(world, entityStore, draft);
            }
        }

        int xMin = min.x;
        int yMin = min.y;
        int zMin = min.z;
        int xMax = max.x;
        int yMax = max.y;
        int zMax = max.z;
        int width = xMax - xMin;
        int height = yMax - yMin;
        int depth = zMax - zMin;
        int halfWidth = width / 2;
        int halfDepth = depth / 2;

        LocalCachedChunkAccessor accessor =
            LocalCachedChunkAccessor.atWorldCoords(world, xMin + halfWidth, zMin + halfDepth, Math.max(width, depth));

        BlockSelection selection = new BlockSelection();
        selection.setPosition(xMin + halfWidth, yMin, zMin + halfDepth);
        selection.setSelectionArea(new Vector3i(xMin, yMin, zMin), new Vector3i(xMax, yMax, zMax));

        int editorBlock = BlockType.getAssetMap().getIndex("Editor_Block");
        boolean skipEditorBlock = editorBlock != Integer.MIN_VALUE;
        int editorBlockPrefabAir = BlockType.getAssetMap().getIndex("Editor_Empty");
        boolean includeEmpty = draft.isSaveEmptySpaces();

        int top = Math.max(yMin, yMax);
        int bottom = Math.min(yMin, yMax);
        int blockCount = 0;

        ChunkStore chunkStoreAccessor = world.getChunkStore();

        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                WorldChunk chunk = accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                if (chunk == null) {
                    continue;
                }
                Store<ChunkStore> chunkStore = chunk.getReference().getStore();
                int lastSection = -1;
                Ref<ChunkStore> sectionRef = null;
                BlockPhysics blockPhysics = null;

                for (int y = top; y >= bottom; y--) {
                    int block = chunk.getBlock(x, y, z);
                    if (lastSection != ChunkUtil.indexSection(y)) {
                        lastSection = ChunkUtil.indexSection(y);
                        sectionRef = chunkStoreAccessor.getChunkSectionReferenceAtBlock(x, y, z);
                        blockPhysics = sectionRef != null && sectionRef.isValid()
                            ? chunkStore.getComponent(sectionRef, BlockPhysics.getComponentType())
                            : null;
                    }

                    int fluid = 0;
                    if (sectionRef != null && sectionRef.isValid()) {
                        FluidSection fluidSection = chunkStore.getComponent(sectionRef, FluidSection.getComponentType());
                        if (fluidSection != null) {
                            fluid = fluidSection.getFluidId(x, y, z);
                        }
                    }

                    if ((block != 0 || fluid != 0 || includeEmpty) && (!skipEditorBlock || block != editorBlock)) {
                        if (block == editorBlockPrefabAir || (block == 0 && fluid == 0)) {
                            selection.addBlockAtWorldPos(x, y, z, 0, 0, 0, 0);
                        } else {
                            selection.copyFromAtWorld(x, y, z, chunk, blockPhysics);
                        }
                        blockCount++;
                    }
                }
            }
        }

        selection.setAnchorAtWorldPos(anchor.x, anchor.y, anchor.z);

        if (entityStore != null) {
            BuilderToolsPlugin.forEachCopyableInSelection(world, xMin, yMin, zMin, width, height, depth, e -> {
                if (ShopSpotDisplayService.isDisplayPropEntity(entityStore, e)) {
                    return;
                }
                Holder<EntityStore> holder = entityStore.copyEntity(e);
                selection.addEntityFromWorld(holder);
            });
        }

        BuilderToolsPlugin builderTools = BuilderToolsPlugin.get();
        if (builderTools != null) {
            List<PrefabSaveContributor> contributors = builderTools.getPrefabSaveContributors();
            Vector3i minCorner = new Vector3i(xMin, yMin, zMin);
            Vector3i maxCorner = new Vector3i(xMax, yMax, zMax);
            for (PrefabSaveContributor contributor : contributors) {
                contributor.contribute(selection, world, minCorner, maxCorner);
            }
        }

        if (blockCount == 0) {
            LOGGER.atWarning().log("Plot creator prefab export: no blocks in bounds");
            return false;
        }

        try {
            Files.createDirectories(outputFile.getParent());
            BlockSelection prefab = selection.relativize();
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin != null) {
                PlotCreatorIconExporter.tryExportIcon(prefab, draft.getConstructionId(), plugin.getDataDirectory());
            }
            PrefabStore.get().savePrefab(outputFile, prefab, overwrite);
            return true;
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to save prefab to %s", outputFile);
            return false;
        }
    }

    /**
     * Building JSON {@code prefabPath} and on-disk export name, e.g. {@code plot_my_shop.prefab.json}.
     * Derived from the construction id only (never re-sanitize an already suffixed name).
     */
    @Nullable
    public static String prefabPathKeyFromConstructionId(@Nullable String constructionId) {
        if (constructionId == null || constructionId.isBlank()) {
            return null;
        }
        String id = constructionId.trim().toLowerCase(java.util.Locale.ROOT);
        if (id.endsWith(".prefab.json")) {
            return id;
        }
        if (!id.matches("plot_[a-z0-9_]+")) {
            return null;
        }
        return id + ".prefab.json";
    }

    /** @deprecated use {@link #prefabPathKeyFromConstructionId} */
    @Deprecated
    @Nullable
    public static String sanitizePrefabFileName(@Nullable String raw) {
        return prefabPathKeyFromConstructionId(raw);
    }
}
