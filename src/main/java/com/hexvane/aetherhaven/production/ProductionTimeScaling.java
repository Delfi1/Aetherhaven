package com.hexvane.aetherhaven.production;

import com.hexvane.aetherhaven.config.AetherhavenPluginConfig;
import javax.annotation.Nonnull;

/** Applies {@link AetherhavenPluginConfig#getProductionTimeMultiplier()} to catalog tick intervals. */
public final class ProductionTimeScaling {
    private ProductionTimeScaling() {}

    /**
     * Effective entity ticks for one production unit after the server time multiplier ({@code ceil(catalogTicks * m)},
     * at least 1).
     */
    public static int effectiveTicks(int catalogTicks, double productionTimeMultiplier) {
        int base = Math.max(1, catalogTicks);
        double m = productionTimeMultiplier;
        if (Double.isNaN(m) || m <= 0.0) {
            m = 1.0;
        }
        double scaled = (double) base * m;
        if (scaled >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.ceil(scaled));
    }

    public static int effectiveTicks(@Nonnull AetherhavenPluginConfig config, int catalogTicks) {
        return effectiveTicks(catalogTicks, config.getProductionTimeMultiplier());
    }

    /**
     * Effective entity ticks after server config multiplier and workplace speed bonus ({@code workplaceSpeedMul} divides
     * ticks, e.g. 1.2 for 20% faster).
     */
    public static int effectiveTicksWithWorkplaceSpeed(
        @Nonnull AetherhavenPluginConfig config,
        int catalogTicks,
        double workplaceSpeedMul
    ) {
        double configMul = config.getProductionTimeMultiplier();
        if (Double.isNaN(configMul) || configMul <= 0.0) {
            configMul = 1.0;
        }
        double speed = workplaceSpeedMul;
        if (Double.isNaN(speed) || speed <= 0.0) {
            speed = 1.0;
        }
        return effectiveTicks(catalogTicks, configMul / speed);
    }
}
