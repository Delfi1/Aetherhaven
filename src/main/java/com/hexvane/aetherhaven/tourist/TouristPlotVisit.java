package com.hexvane.aetherhaven.tourist;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** A tourist destination plot and the world position to path toward when entering it. */
public record TouristPlotVisit(
    @Nonnull UUID plotId,
    @Nonnull UUID destinationId,
    double entryX,
    double entryY,
    double entryZ
) {
    @Nonnull
    public static UUID destinationIdForPlot(@Nonnull UUID plotId) {
        return UUID.nameUUIDFromBytes(("aetherhaven-tourist-plot:" + plotId).getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    public static TouristPlotVisit of(@Nonnull UUID plotId, double entryX, double entryY, double entryZ) {
        return new TouristPlotVisit(plotId, destinationIdForPlot(plotId), entryX, entryY, entryZ);
    }

    public static boolean isPlotDestinationId(@Nullable UUID destinationId, @Nonnull UUID plotId) {
        return destinationId != null && destinationId.equals(destinationIdForPlot(plotId));
    }
}
