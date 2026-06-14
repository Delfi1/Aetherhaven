package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.joml.Vector3i;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Keeps the plot creator step HUD visible for the whole session while a draft is active. */
public final class PlotCreatorPreviewSystem extends EntityTickingSystem<EntityStore> {
    private static final ConcurrentHashMap<UUID, PlotCreatorStep> LAST_STEP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_WIREFRAME_SIG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_SPAWN_MARKER_SIG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_SPAWN_MARKER_RESEND_MS = new ConcurrentHashMap<>();
    private static final long SPAWN_MARKER_RESEND_INTERVAL_MS = 2000L;

    @SuppressWarnings("unused")
    private final AetherhavenPlugin plugin;

    public PlotCreatorPreviewSystem(@Nonnull AetherhavenPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> chunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player == null) {
            return;
        }
        @Nullable
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr == null) {
            return;
        }
        UUID uuid = pr.getUuid();
        World world = store.getExternalData().getWorld();
        PlotCreatorSession session = PlotCreatorSessions.get(uuid);
        if (session == null) {
            if (PlotCreatorHudSupport.isActive(player)) {
                PlotCreatorHudSupport.removeHud(player, pr);
            }
            LAST_STEP.remove(uuid);
            LAST_WIREFRAME_SIG.remove(uuid);
            LAST_SPAWN_MARKER_SIG.remove(uuid);
            LAST_SPAWN_MARKER_RESEND_MS.remove(uuid);
            PlotCreatorService.clearPlotCreatorWireframe(pr, world);
            return;
        }
        PlotCreatorStep step = session.getDraft().getStep();
        if (step == PlotCreatorStep.DONE) {
            PlotCreatorHudSupport.removeHud(player, pr);
            LAST_STEP.remove(uuid);
            LAST_WIREFRAME_SIG.remove(uuid);
            LAST_SPAWN_MARKER_SIG.remove(uuid);
            LAST_SPAWN_MARKER_RESEND_MS.remove(uuid);
            PlotCreatorService.clearPlotCreatorWireframe(pr, world);
            return;
        }
        PlotCreatorStep prev = LAST_STEP.get(uuid);
        if (prev != step) {
            LAST_STEP.put(uuid, step);
            PlotCreatorHudSupport.obtainHud(player, pr).refresh(session);
        } else if (!PlotCreatorHudSupport.isActive(player)) {
            PlotCreatorHudSupport.obtainHud(player, pr).refresh(session);
        }
        long sig = wireframeSignature(session.getDraft());
        Long prevSig = LAST_WIREFRAME_SIG.get(uuid);
        if (prevSig == null || prevSig != sig) {
            LAST_WIREFRAME_SIG.put(uuid, sig);
            PlotCreatorService.refreshWireframe(session, pr);
        }
        long spawnSig = PlotCreatorSpawnMarkerOverlay.signature(session.getDraft());
        Long prevSpawnSig = LAST_SPAWN_MARKER_SIG.get(uuid);
        if (prevSpawnSig == null || prevSpawnSig != spawnSig) {
            LAST_SPAWN_MARKER_SIG.put(uuid, spawnSig);
            PlotCreatorService.refreshSpawnMarkers(session, pr);
            LAST_SPAWN_MARKER_RESEND_MS.put(uuid, System.currentTimeMillis());
        } else if (!session.getDraft().getAdventurerSpawns().isEmpty() || !session.getDraft().getVisitorSpawnLocals().isEmpty()) {
            long now = System.currentTimeMillis();
            Long lastResend = LAST_SPAWN_MARKER_RESEND_MS.get(uuid);
            if (lastResend == null || now - lastResend >= SPAWN_MARKER_RESEND_INTERVAL_MS) {
                LAST_SPAWN_MARKER_RESEND_MS.put(uuid, now);
                PlotCreatorService.refreshSpawnMarkers(session, pr);
            }
        }
    }

    private static long wireframeSignature(@Nonnull PlotCreatorDraft draft) {
        Vector3i a = draft.getCornerFirst();
        Vector3i b = draft.getCornerSecond();
        if (a == null || b == null) {
            return 0L;
        }
        Vector3i min = draft.boundsMin();
        Vector3i max = draft.boundsMax();
        long h = 17L;
        h = 31 * h + min.x;
        h = 31 * h + min.y;
        h = 31 * h + min.z;
        h = 31 * h + max.x;
        h = 31 * h + max.y;
        h = 31 * h + max.z;
        return h;
    }
}
