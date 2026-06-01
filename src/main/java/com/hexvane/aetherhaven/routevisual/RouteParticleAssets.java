package com.hexvane.aetherhaven.routevisual;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import javax.annotation.Nonnull;

/** Validates route particle assets once at startup. */
public final class RouteParticleAssets {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile boolean checked;

    private RouteParticleAssets() {}

    public static void validateOnce() {
        if (checked) {
            return;
        }
        checked = true;
        warnIfMissing(AetherhavenConstants.ROUTE_PARTICLE_TRAIL_ID);
        warnIfMissing(AetherhavenConstants.ROUTE_PARTICLE_NODE_ID);
        warnIfMissing(AetherhavenConstants.ROUTE_PARTICLE_TRAIL_SELECTED_ID);
        warnIfMissing(AetherhavenConstants.ROUTE_PARTICLE_NODE_SELECTED_ID);
    }

    private static void warnIfMissing(@Nonnull String id) {
        if (ParticleSystem.getAssetMap().getAsset(id) == null) {
            LOGGER.atWarning().log("Patrol route particle system not loaded: %s", id);
        }
    }
}
