package com.hexvane.aetherhaven.construction.assembly;

/** In-memory phase for an active {@link PlotAssemblyJob} while plot state is {@link com.hexvane.aetherhaven.town.PlotInstanceState#ASSEMBLING}. */
public enum PlotAssemblyPhase {
    /** Player clears obstructing world blocks in the prefab footprint (red markers + staff secondary). */
    CLEARING,
    /** Frontier growth: passive ticks and staff placement (green markers). */
    PLACING
}
