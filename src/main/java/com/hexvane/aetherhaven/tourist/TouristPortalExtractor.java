package com.hexvane.aetherhaven.tourist;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.PlotFootprintRecord;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/** Activates prefab embedded tourist portal blocks when a plot build completes. */
public final class TouristPortalExtractor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private TouristPortalExtractor() {}

    public static void registerForCompletedBuild(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nonnull UUID plotId,
        @Nonnull PlotInstance plot
    ) {
        PlotFootprintRecord fp = plot.toFootprint();
        TouristPortalRegistry registry = AetherhavenWorldRegistries.getOrCreateTouristPortalRegistry(world, plugin);
        int activated = 0;

        for (int x = fp.getMinX(); x <= fp.getMaxX(); x++) {
            for (int y = fp.getMinY(); y <= fp.getMaxY(); y++) {
                for (int z = fp.getMinZ(); z <= fp.getMaxZ(); z++) {
                    if (!AetherhavenConstants.TOURIST_PORTAL_BLOCK_TYPE_ID.equals(world.getBlockType(x, y, z).getId())) {
                        continue;
                    }
                    Vector3i pos = new Vector3i(x, y, z);
                    TouristPortalBlock blockComp = TouristPortalBlockUtil.getBlockComponent(world, pos);
                    if (blockComp != null && blockComp.isConfigured() && !blockComp.isTemplatePlacement()) {
                        TouristPortalRecord bound = registry.getAtBlock(x, y, z);
                        if (bound != null
                            && town.getTownId().equals(bound.getTownId())
                            && plotId.equals(bound.getPlotId())) {
                            continue;
                        }
                    }
                    activated += activatePortal(world, plugin, registry, town, plotId, pos, blockComp);
                }
            }
        }

        if (activated > 0) {
            TouristPortalPersistence.save(world, plugin, registry);
            LOGGER.atInfo().log("Activated %s tourist portal(s) for plot %s in town %s", activated, plotId, town.getTownId());
        }
    }

    private static int activatePortal(
        @Nonnull World world,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull TouristPortalRegistry registry,
        @Nonnull TownRecord town,
        @Nonnull UUID plotId,
        @Nonnull Vector3i pos,
        @Nullable TouristPortalBlock blockComp
    ) {
        TouristPortalRecord existing = registry.getAtBlock(pos.x, pos.y, pos.z);
        if (existing != null) {
            registry.remove(existing.getPortalId());
        }

        UUID portalId = UUID.randomUUID();
        if (blockComp != null && !blockComp.getPortalId().isBlank()) {
            try {
                portalId = UUID.fromString(blockComp.getPortalId().trim());
            } catch (IllegalArgumentException ignored) {
                // use fresh id
            }
        }

        TouristPortalRecord record = new TouristPortalRecord();
        record.setPortalId(portalId);
        record.setWorldName(world.getName());
        record.setBlockPosition(pos);
        record.setTownId(town.getTownId());
        record.setPlotId(plotId);

        registry.put(record);
        TouristPortalBlockUtil.syncConfigToBlock(world, pos, record);
        return 1;
    }
}
