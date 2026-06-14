package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** Draws a ground-hugging selection rectangle with world particles while box-dragging. */
public final class RtsBoxSelectVisuals {
    private static final double EDGE_SPACING = 0.75;
    /** Lift above the clicked ground plane so dots do not clip into terrain. */
    private static final double GROUND_LIFT = 1.05;

    private RtsBoxSelectVisuals() {}

    public static void spawnOutline(
        @Nonnull Store<EntityStore> store,
        @Nonnull List<Ref<EntityStore>> audience,
        double minX,
        double maxX,
        double minZ,
        double maxZ,
        double groundY
    ) {
        if (maxX - minX < 0.05 && maxZ - minZ < 0.05) {
            spawnDot(store, audience, minX, groundY, minZ);
            return;
        }
        double y = groundY + GROUND_LIFT;
        spawnEdge(store, audience, minX, maxX, minZ, minZ, y);
        spawnEdge(store, audience, minX, maxX, maxZ, maxZ, y);
        spawnEdge(store, audience, minX, minX, minZ, maxZ, y);
        spawnEdge(store, audience, maxX, maxX, minZ, maxZ, y);
    }

    private static void spawnEdge(
        @Nonnull Store<EntityStore> store,
        @Nonnull List<Ref<EntityStore>> audience,
        double x0,
        double x1,
        double z0,
        double z1,
        double y
    ) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.05) {
            spawnDot(store, audience, x0, y, z0);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(len / EDGE_SPACING));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            spawnDot(store, audience, x0 + dx * t, y, z0 + dz * t);
        }
    }

    private static void spawnDot(
        @Nonnull Store<EntityStore> store,
        @Nonnull List<Ref<EntityStore>> audience,
        double x,
        double y,
        double z
    ) {
        ParticleUtil.spawnParticleEffect(
            AetherhavenConstants.RTS_SELECT_BOX_DOT_PARTICLE,
            new Vector3d(x, y, z),
            audience,
            store
        );
    }
}
