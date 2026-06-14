package com.hexvane.aetherhaven.rts;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.rts.camera.TopDownCameraService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Active RTS command session on a player. {@link #active} is cleared on exit and must not survive logout in command mode. */
public final class RtsCommandPlayerComponent implements Component<EntityStore> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type UUID_LIST = new TypeToken<List<String>>() {}.getType();

    @Nonnull
    public static final BuilderCodec<RtsCommandPlayerComponent> CODEC = BuilderCodec.builder(
            RtsCommandPlayerComponent.class,
            RtsCommandPlayerComponent::new
        )
        .append(new KeyedCodec<>("Active", Codec.BOOLEAN), (c, v) -> c.active = Boolean.TRUE.equals(v), c -> c.active)
        .add()
        .append(new KeyedCodec<>("TownId", Codec.STRING), (c, v) -> c.townId = v != null ? v : "", c -> c.townId)
        .add()
        .append(new KeyedCodec<>("FocusX", Codec.DOUBLE), (c, v) -> c.focusX = v != null ? v : 0, c -> c.focusX)
        .add()
        .append(new KeyedCodec<>("FocusY", Codec.DOUBLE), (c, v) -> c.focusY = v != null ? v : 0, c -> c.focusY)
        .add()
        .append(new KeyedCodec<>("FocusZ", Codec.DOUBLE), (c, v) -> c.focusZ = v != null ? v : 0, c -> c.focusZ)
        .add()
        .append(new KeyedCodec<>("PostX", Codec.DOUBLE), (c, v) -> c.postX = v != null ? v : 0, c -> c.postX)
        .add()
        .append(new KeyedCodec<>("PostY", Codec.DOUBLE), (c, v) -> c.postY = v != null ? v : 0, c -> c.postY)
        .add()
        .append(new KeyedCodec<>("PostZ", Codec.DOUBLE), (c, v) -> c.postZ = v != null ? v : 0, c -> c.postZ)
        .add()
        .append(
            new KeyedCodec<>("OrderMode", Codec.STRING),
            (c, s) -> c.orderMode = parseOrderMode(s),
            c -> c.orderMode.name()
        )
        .add()
        .append(
            new KeyedCodec<>("StanceMode", Codec.STRING),
            (c, s) -> c.stanceMode = parseStance(s),
            c -> c.stanceMode.name()
        )
        .add()
        .append(new KeyedCodec<>("SavedHotbar", Codec.STRING), (c, s) -> c.savedHotbarJson = s != null ? s : "", c -> c.savedHotbarJson)
        .add()
        .append(
            new KeyedCodec<>("SessionExitedSafely", Codec.BOOLEAN),
            (c, v) -> c.sessionExitedSafely = v == null || Boolean.TRUE.equals(v),
            c -> c.sessionExitedSafely
        )
        .add()
        .append(new KeyedCodec<>("Selection", Codec.STRING), (c, s) -> c.decodeSelection(s), c -> c.encodeSelection())
        .add()
        .append(new KeyedCodec<>("BoxActive", Codec.BOOLEAN), (c, v) -> c.boxSelectActive = Boolean.TRUE.equals(v), c -> c.boxSelectActive)
        .add()
        .append(new KeyedCodec<>("BoxStartX", Codec.DOUBLE), (c, v) -> c.boxStartX = v != null ? v : 0, c -> c.boxStartX)
        .add()
        .append(new KeyedCodec<>("BoxStartZ", Codec.DOUBLE), (c, v) -> c.boxStartZ = v != null ? v : 0, c -> c.boxStartZ)
        .add()
        .append(new KeyedCodec<>("BoxEndX", Codec.DOUBLE), (c, v) -> c.boxEndX = v != null ? v : 0, c -> c.boxEndX)
        .add()
        .append(new KeyedCodec<>("BoxEndZ", Codec.DOUBLE), (c, v) -> c.boxEndZ = v != null ? v : 0, c -> c.boxEndZ)
        .add()
        .append(new KeyedCodec<>("BoxGroundY", Codec.DOUBLE), (c, v) -> c.boxGroundY = v != null ? v : 0, c -> c.boxGroundY)
        .add()
        .append(new KeyedCodec<>("ExitX", Codec.DOUBLE), (c, v) -> c.exitX = v != null ? v : 0, c -> c.exitX)
        .add()
        .append(new KeyedCodec<>("ExitY", Codec.DOUBLE), (c, v) -> c.exitY = v != null ? v : 0, c -> c.exitY)
        .add()
        .append(new KeyedCodec<>("ExitZ", Codec.DOUBLE), (c, v) -> c.exitZ = v != null ? v : 0, c -> c.exitZ)
        .add()
        .append(new KeyedCodec<>("ExitSaved", Codec.BOOLEAN), (c, v) -> c.exitSaved = Boolean.TRUE.equals(v), c -> c.exitSaved)
        .add()
        .afterDecode(c -> {
            if (c.active) {
                c.active = false;
            }
            if (!c.active && c.hasRecoverableSavedHotbar() && c.sessionExitedSafely) {
                c.sessionExitedSafely = false;
            }
        })
        .build();

    @Nullable
    private static volatile ComponentType<EntityStore, RtsCommandPlayerComponent> componentType;

    private boolean active;
    @Nonnull
    private String townId = "";
    private double focusX;
    private double focusY;
    private double focusZ;
    private double postX;
    private double postY;
    private double postZ;
    /** Body position to restore when leaving command mode. */
    private double exitX;
    private double exitY;
    private double exitZ;
    private boolean exitSaved;
    @Nonnull
    private RtsOrderMode orderMode = RtsOrderMode.ATTACK_MOVE;
    @Nonnull
    private RtsCombatStance stanceMode = RtsCombatStance.DEFENSIVE;
    @Nonnull
    private String savedHotbarJson = "";
    /** False while in RTS or after an unclean exit; true after normal exit or recovery. */
    private boolean sessionExitedSafely = true;
    @Nonnull
    private final List<UUID> selectedGuardUuids = new ArrayList<>();
    private boolean boxSelectActive;
    private double boxStartX;
    private double boxStartZ;
    private double boxEndX;
    private double boxEndZ;
    private double boxGroundY;
    /** Normalized screen coords while box-dragging (0..1). */
    private float boxScreenStartX;
    private float boxScreenStartY;
    private float boxScreenEndX;
    private float boxScreenEndY;
    /** True once HUD drag start is anchored to a real camera screen sample. */
    private boolean boxScreenAnchorReady;
    /** True once box world endpoints came from a client {@code LookAtPlane} hit. */
    private boolean boxWorldAnchorReady;
    /** Width calibration multiplier relative to distance-based default; 0 = uncalibrated (1.0). */
    private double orthoHalfWidth;
    /** Height calibration multiplier relative to distance-based default; 0 = uncalibrated (1.0). */
    private double orthoHalfHeight;
    /**
     * Cached vertical extent for pick frustum: altitude above terrain under focus + camera rig offset.
     * Refreshed each tick in {@link RtsCommanderCameraSystem}; 0 = fall back to default spawn altitude.
     */
    private float pickViewHeight;
    /** Draw world-space selection column wireframe after box drags. */
    private boolean boxSelectDebug;
    /** Entity view radius before command mode; restored on exit. */
    private int savedViewRadiusBlocks;
    /** Per-tick relative displacement captured before locomotion is stripped. */
    private double panDeltaX;
    private double panDeltaZ;
    /** Guard UUID the commander camera follows until manual pan or box drag. */
    @Nullable
    private UUID cameraFollowGuardUuid;
    private double cameraFollowSnapCommanderX;
    private double cameraFollowSnapCommanderZ;
    private double cameraFollowSnapGuardX;
    private double cameraFollowSnapGuardZ;
    private boolean cameraFollowSnapReady;
    @Nullable
    private UUID lastRosterClickGuardUuid;
    private long lastRosterClickMs;
    /** Camera-aligned axes from absolute movement (forward ≈ +Z with top-down rig). */
    private double moveForward;
    private double moveStrafe;
    /** Previous client absolute sample for delta-based pan (not vs aerial body). */
    private double lastAbsX;
    private double lastAbsZ;
    private boolean hasLastAbs;
    /** Set when focus changes; gates Teleport to reduce rubberbanding. */
    private boolean commanderMoved;
    @Nonnull
    private RtsPickTuning pickTuning = RtsPickTuning.defaults();

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(
            RtsCommandPlayerComponent.class,
            "AetherhavenRtsCommandPlayer",
            CODEC
        );
    }

    @Nonnull
    public static ComponentType<EntityStore, RtsCommandPlayerComponent> getComponentType() {
        ComponentType<EntityStore, RtsCommandPlayerComponent> t = componentType;
        if (t == null) {
            throw new IllegalStateException("RtsCommandPlayerComponent not registered");
        }
        return t;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getPanDeltaX() {
        return panDeltaX;
    }

    public double getPanDeltaZ() {
        return panDeltaZ;
    }

    public void clearPanDelta() {
        this.panDeltaX = 0;
        this.panDeltaZ = 0;
        this.moveForward = 0;
        this.moveStrafe = 0;
    }

    public void clearLastAbsSample() {
        this.hasLastAbs = false;
    }

    public void setLastAbsSample(double x, double z) {
        this.lastAbsX = x;
        this.lastAbsZ = z;
        this.hasLastAbs = true;
    }

    public boolean hasLastAbsSample() {
        return hasLastAbs;
    }

    public double getLastAbsX() {
        return lastAbsX;
    }

    public double getLastAbsZ() {
        return lastAbsZ;
    }

    public void markCommanderMoved() {
        this.commanderMoved = true;
    }

    public boolean consumeCommanderMoved() {
        if (!commanderMoved) {
            return false;
        }
        commanderMoved = false;
        return true;
    }

    public void addPanDelta(double dx, double dz) {
        this.panDeltaX += dx;
        this.panDeltaZ += dz;
        this.moveStrafe += dx;
        this.moveForward += dz;
    }

    public double getMoveForward() {
        return moveForward;
    }

    public double getMoveStrafe() {
        return moveStrafe;
    }

    @Nonnull
    public RtsPickTuning getPickTuning() {
        return pickTuning;
    }

    public void setPickTuning(@Nonnull RtsPickTuning pickTuning) {
        this.pickTuning = pickTuning;
    }

    /** See {@link RtsScreenPickUtil#refreshPickViewHeight}. */
    public float getPickViewHeight() {
        if (pickViewHeight > 0f) {
            return pickViewHeight;
        }
        return TopDownCameraService.DEFAULT_DISTANCE + AetherhavenConstants.RTS_COMMAND_PICK_CAMERA_EYE_OFFSET_Y;
    }

    public void setPickViewHeight(float pickViewHeight) {
        this.pickViewHeight = pickViewHeight;
    }

    @Nonnull
    public String getTownId() {
        return townId;
    }

    public void setTownId(@Nonnull String townId) {
        this.townId = townId != null ? townId : "";
    }

    public double getFocusX() {
        return focusX;
    }

    public double getFocusY() {
        return focusY;
    }

    public double getFocusZ() {
        return focusZ;
    }

    public void setFocus(double x, double y, double z) {
        this.focusX = x;
        this.focusY = y;
        this.focusZ = z;
    }

    /** Updates click-pick focus from the flying commander body without re-sending the camera packet. */
    public void trackFocus(double x, double y, double z) {
        this.focusX = x;
        this.focusY = y;
        this.focusZ = z;
    }

    public double getPostX() {
        return postX;
    }

    public double getPostY() {
        return postY;
    }

    public double getPostZ() {
        return postZ;
    }

    public void setPostPosition(double x, double y, double z) {
        this.postX = x;
        this.postY = y;
        this.postZ = z;
    }

    public void setExitBody(double x, double y, double z) {
        this.exitX = x;
        this.exitY = y;
        this.exitZ = z;
        this.exitSaved = true;
    }

    public boolean hasExitBody() {
        return exitSaved;
    }

    public double getExitX() {
        return exitX;
    }

    public double getExitY() {
        return exitY;
    }

    public double getExitZ() {
        return exitZ;
    }

    @Nonnull
    public RtsOrderMode getOrderMode() {
        return orderMode;
    }

    public void setOrderMode(@Nonnull RtsOrderMode orderMode) {
        this.orderMode = orderMode;
    }

    public void cycleOrderMode() {
        this.orderMode = orderMode.next();
    }

    @Nonnull
    public RtsCombatStance getStanceMode() {
        return stanceMode;
    }

    public void setStanceMode(@Nonnull RtsCombatStance stanceMode) {
        this.stanceMode = stanceMode;
    }

    public void cycleStanceMode() {
        this.stanceMode = stanceMode.next();
    }

    @Nonnull
    public String getSavedHotbarJson() {
        return savedHotbarJson;
    }

    public void setSavedHotbarJson(@Nonnull String json) {
        this.savedHotbarJson = json != null ? json : "";
    }

    public boolean hasRecoverableSavedHotbar() {
        return savedHotbarJson != null && !savedHotbarJson.isBlank();
    }

    public boolean isSessionExitedSafely() {
        return sessionExitedSafely;
    }

    public void setSessionExitedSafely(boolean sessionExitedSafely) {
        this.sessionExitedSafely = sessionExitedSafely;
    }

    /** Saved hotbar left behind because RTS was not exited through normal cleanup. */
    public boolean needsUncleanSessionRecovery() {
        return !sessionExitedSafely && hasRecoverableSavedHotbar();
    }

    @Nonnull
    public List<UUID> getSelectedGuardUuids() {
        return selectedGuardUuids;
    }

    public void clearSelection() {
        selectedGuardUuids.clear();
    }

    @Nullable
    public UUID getCameraFollowGuardUuid() {
        return cameraFollowGuardUuid;
    }

    public void setCameraFollowGuardUuid(@Nullable UUID guardUuid) {
        this.cameraFollowGuardUuid = guardUuid;
        this.cameraFollowSnapReady = false;
    }

    public void clearCameraFollow() {
        this.cameraFollowGuardUuid = null;
        this.cameraFollowSnapReady = false;
    }

    public boolean isFollowingGuard() {
        return cameraFollowGuardUuid != null;
    }

    public void setCameraFollowSnap(double commanderX, double commanderZ, double guardX, double guardZ) {
        this.cameraFollowSnapCommanderX = commanderX;
        this.cameraFollowSnapCommanderZ = commanderZ;
        this.cameraFollowSnapGuardX = guardX;
        this.cameraFollowSnapGuardZ = guardZ;
        this.cameraFollowSnapReady = true;
    }

    public boolean hasCameraFollowSnap() {
        return cameraFollowSnapReady;
    }

    /** True when the commander moved independently of the followed guard since the last follow snap. */
    public boolean cameraFollowManualOverride(double commanderX, double commanderZ, double guardX, double guardZ) {
        if (!cameraFollowSnapReady) {
            return false;
        }
        double commanderDx = commanderX - cameraFollowSnapCommanderX;
        double commanderDz = commanderZ - cameraFollowSnapCommanderZ;
        double guardDx = guardX - cameraFollowSnapGuardX;
        double guardDz = guardZ - cameraFollowSnapGuardZ;
        double driftX = commanderDx - guardDx;
        double driftZ = commanderDz - guardDz;
        return Math.hypot(driftX, driftZ) > 0.75;
    }

    public void recordRosterClick(@Nonnull UUID guardUuid, long nowMs) {
        this.lastRosterClickGuardUuid = guardUuid;
        this.lastRosterClickMs = nowMs;
    }

    public void clearRosterClickTracking() {
        this.lastRosterClickGuardUuid = null;
        this.lastRosterClickMs = 0L;
    }

    public boolean matchesRosterDoubleClick(@Nonnull UUID guardUuid, long nowMs, long windowMs) {
        return lastRosterClickGuardUuid != null
            && lastRosterClickGuardUuid.equals(guardUuid)
            && nowMs - lastRosterClickMs <= windowMs;
    }

    public void setSelection(@Nonnull List<UUID> uuids) {
        selectedGuardUuids.clear();
        selectedGuardUuids.addAll(uuids);
    }

    public boolean isBoxSelectActive() {
        return boxSelectActive;
    }

    public void setBoxSelectActive(boolean boxSelectActive) {
        this.boxSelectActive = boxSelectActive;
    }

    public void setBoxStart(double x, double z) {
        this.boxStartX = x;
        this.boxStartZ = z;
    }

    public void setBoxEnd(double x, double z) {
        this.boxEndX = x;
        this.boxEndZ = z;
    }

    public double getBoxStartX() {
        return boxStartX;
    }

    public double getBoxStartZ() {
        return boxStartZ;
    }

    public double getBoxEndX() {
        return boxEndX;
    }

    public double getBoxEndZ() {
        return boxEndZ;
    }

    public double getBoxGroundY() {
        return boxGroundY;
    }

    public void setBoxGroundY(double boxGroundY) {
        this.boxGroundY = boxGroundY;
    }

    public float getBoxScreenStartX() {
        return boxScreenStartX;
    }

    public float getBoxScreenStartY() {
        return boxScreenStartY;
    }

    public float getBoxScreenEndX() {
        return boxScreenEndX;
    }

    public float getBoxScreenEndY() {
        return boxScreenEndY;
    }

    public boolean isBoxScreenAnchorReady() {
        return boxScreenAnchorReady;
    }

    public void setBoxScreenAnchorReady(boolean boxScreenAnchorReady) {
        this.boxScreenAnchorReady = boxScreenAnchorReady;
    }

    public boolean isBoxWorldAnchorReady() {
        return boxWorldAnchorReady;
    }

    public void setBoxWorldAnchorReady(boolean boxWorldAnchorReady) {
        this.boxWorldAnchorReady = boxWorldAnchorReady;
    }

    public double getOrthoHalfWidth() {
        return orthoHalfWidth;
    }

    public void setOrthoHalfWidth(double orthoHalfWidth) {
        this.orthoHalfWidth = orthoHalfWidth;
    }

    public double getOrthoHalfHeight() {
        return orthoHalfHeight;
    }

    public void setOrthoHalfHeight(double orthoHalfHeight) {
        this.orthoHalfHeight = orthoHalfHeight;
    }

    public boolean isBoxSelectDebug() {
        return boxSelectDebug;
    }

    public void setBoxSelectDebug(boolean boxSelectDebug) {
        this.boxSelectDebug = boxSelectDebug;
    }

    public void clearOrthoCalibration() {
        this.orthoHalfWidth = 0;
        this.orthoHalfHeight = 0;
    }

    public void setBoxScreenStart(float x, float y) {
        this.boxScreenStartX = x;
        this.boxScreenStartY = y;
    }

    public void setBoxScreenEnd(float x, float y) {
        this.boxScreenEndX = x;
        this.boxScreenEndY = y;
    }

    public void clearBoxScreen() {
        this.boxScreenStartX = 0;
        this.boxScreenStartY = 0;
        this.boxScreenEndX = 0;
        this.boxScreenEndY = 0;
        this.boxScreenAnchorReady = false;
        this.boxWorldAnchorReady = false;
    }

    public int getSavedViewRadiusBlocks() {
        return savedViewRadiusBlocks;
    }

    public void setSavedViewRadiusBlocks(int savedViewRadiusBlocks) {
        this.savedViewRadiusBlocks = savedViewRadiusBlocks;
    }

    @Nonnull
    private String encodeSelection() {
        List<String> ids = new ArrayList<>(selectedGuardUuids.size());
        for (UUID u : selectedGuardUuids) {
            ids.add(u.toString());
        }
        return GSON.toJson(ids);
    }

    private void decodeSelection(@Nullable String json) {
        selectedGuardUuids.clear();
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            List<String> raw = GSON.fromJson(json, UUID_LIST);
            if (raw == null) {
                return;
            }
            for (String s : raw) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                try {
                    selectedGuardUuids.add(UUID.fromString(s.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Nonnull
    private static RtsOrderMode parseOrderMode(@Nullable String s) {
        if (s == null) {
            return RtsOrderMode.ATTACK_MOVE;
        }
        try {
            return RtsOrderMode.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return RtsOrderMode.ATTACK_MOVE;
        }
    }

    @Nonnull
    private static RtsCombatStance parseStance(@Nullable String s) {
        if (s == null) {
            return RtsCombatStance.DEFENSIVE;
        }
        try {
            return RtsCombatStance.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return RtsCombatStance.DEFENSIVE;
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        RtsCommandPlayerComponent c = new RtsCommandPlayerComponent();
        c.active = active;
        c.townId = townId;
        c.focusX = focusX;
        c.focusY = focusY;
        c.focusZ = focusZ;
        c.postX = postX;
        c.postY = postY;
        c.postZ = postZ;
        c.exitX = exitX;
        c.exitY = exitY;
        c.exitZ = exitZ;
        c.exitSaved = exitSaved;
        c.orderMode = orderMode;
        c.stanceMode = stanceMode;
        c.savedHotbarJson = savedHotbarJson;
        c.sessionExitedSafely = sessionExitedSafely;
        c.selectedGuardUuids.addAll(selectedGuardUuids);
        c.boxSelectActive = boxSelectActive;
        c.boxStartX = boxStartX;
        c.boxStartZ = boxStartZ;
        c.boxEndX = boxEndX;
        c.boxEndZ = boxEndZ;
        c.boxGroundY = boxGroundY;
        c.boxScreenStartX = boxScreenStartX;
        c.boxScreenStartY = boxScreenStartY;
        c.boxScreenEndX = boxScreenEndX;
        c.boxScreenEndY = boxScreenEndY;
        c.boxScreenAnchorReady = boxScreenAnchorReady;
        c.boxWorldAnchorReady = boxWorldAnchorReady;
        c.orthoHalfWidth = orthoHalfWidth;
        c.orthoHalfHeight = orthoHalfHeight;
        c.boxSelectDebug = boxSelectDebug;
        c.pickTuning = pickTuning;
        c.pickViewHeight = pickViewHeight;
        c.cameraFollowGuardUuid = cameraFollowGuardUuid;
        c.cameraFollowSnapCommanderX = cameraFollowSnapCommanderX;
        c.cameraFollowSnapCommanderZ = cameraFollowSnapCommanderZ;
        c.cameraFollowSnapGuardX = cameraFollowSnapGuardX;
        c.cameraFollowSnapGuardZ = cameraFollowSnapGuardZ;
        c.cameraFollowSnapReady = cameraFollowSnapReady;
        c.lastRosterClickGuardUuid = lastRosterClickGuardUuid;
        c.lastRosterClickMs = lastRosterClickMs;
        return c;
    }
}
