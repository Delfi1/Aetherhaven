package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.CameraManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3i;

/**
 * Server-side screen-to-world pick for the RTS top-down camera ({@code LookAtPlane}).
 * Box select uses orthographic projection of the HUD rectangle onto the ground plane,
 * calibrated from client {@code targetBlock} hits when available.
 */
public final class RtsScreenPickUtil {
    /** Default vertical field of view when inferring ortho scale without calibration (degrees). */
    private static final float VERTICAL_FOV_DEG = 75f;
    private static final float ASPECT_RATIO = 16f / 9f;
    private static final float REF_WIDTH = 1920f;
    private static final float REF_HEIGHT = 1080f;
    /** Matches {@link com.hexvane.aetherhaven.rts.camera.TopDownCameraService} positionOffset Y. */
    private static final float CAMERA_EYE_OFFSET_Y = 3.0f;
    /** Hytale custom-camera mouse packets use NDC in roughly -1..1 (center = 0). */
    private static final float CAMERA_NDC_MAX = 1.01f;
    private static final float RAW_SCREEN_EPS = 0.004f;
    /** Approximate guard radius on the ground for footprint overlap in screen space. */
    private static final double GUARD_FOOTPRINT_RADIUS = 0.4;

    private RtsScreenPickUtil() {}

    public record GroundPick(double x, double y, double z) {}

    public record WorldAabb(double minX, double maxX, double minZ, double maxZ, double groundY) {}

    @Nullable
    public static Vector2fc latestCameraScreenPoint(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull com.hypixel.hytale.component.Store<EntityStore> store
    ) {
        CameraManager camera = store.getComponent(playerRef, CameraManager.getComponentType());
        if (camera == null) {
            return null;
        }
        Vector2dc raw = camera.getLastScreenPoint();
        if (raw == null) {
            return null;
        }
        float x = (float) raw.x();
        float y = (float) raw.y();
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            return null;
        }
        return new Vector2f(x, y);
    }

    public static boolean isUsableScreenPoint(@Nullable Vector2fc screen, @Nullable Vector3i targetBlock) {
        if (screen == null) {
            return false;
        }
        float x = screen.x();
        float y = screen.y();
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            return false;
        }
        return x != 0f || y != 0f || targetBlock != null;
    }

    @Nullable
    public static GroundPick resolve(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i clientBlock,
        @Nullable Vector2fc screenPoint
    ) {
        GroundPick fromBlock = fromClientBlock(clientBlock);
        if (fromBlock != null) {
            return fromBlock;
        }
        return pickFromScreen(session, screenPoint);
    }

    @Nullable
    public static GroundPick resolve(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i clientBlock,
        @Nullable Vector2dc screenPoint
    ) {
        GroundPick fromBlock = fromClientBlock(clientBlock);
        if (fromBlock != null) {
            return fromBlock;
        }
        return pickFromScreen(session, screenPoint);
    }

    @Nullable
    public static GroundPick resolve(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i clientBlock,
        @Nullable Vector2fc screenPoint
    ) {
        Vector3i block = clientBlock;
        if (block == null) {
            CameraManager camera = store.getComponent(playerRef, CameraManager.getComponentType());
            if (camera != null) {
                block = camera.getLastTargetBlock();
            }
        }
        Vector2fc screen = screenPoint;
        if (screen == null) {
            screen = latestCameraScreenPoint(playerRef, store);
        }
        return resolve(session, block, screen);
    }

    /**
     * Ground pick for RTS move orders and other single-point commands.
     * Uses orthographic screen projection (same as box select) instead of a stale {@code targetBlock}
     * from the command-post entry point.
     */
    @Nullable
    public static GroundPick resolveCommandGroundPick(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull com.hypixel.hytale.component.Store<EntityStore> store,
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        @Nullable Vector2fc screenPoint
    ) {
        Vector2fc screen = screenPoint;
        if (screen == null) {
            screen = latestCameraScreenPoint(playerRef, store);
        }
        if (screen != null && isUsableCommandScreenPoint(screen)) {
            float nx = cameraRawToNormalizedX(screen.x());
            float ny = cameraRawToNormalizedY(screen.y());
            GroundPick ortho = pickOrthographicGround(session, nx, ny);
            if (ortho != null) {
                double groundY = commandSurfaceY(session, targetBlock, ortho.y());
                return new GroundPick(ortho.x(), groundY, ortho.z());
            }
        }
        return fromClientBlockTop(targetBlock);
    }

    /** Top face of a clicked block (block min-Y + 1). */
    public static double blockTopY(int blockY) {
        return blockY + 1.0;
    }

    private static double commandSurfaceY(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector3i targetBlock,
        double fallback
    ) {
        if (targetBlock != null) {
            return blockTopY(targetBlock.y());
        }
        if (session.getBoxGroundY() != 0.0) {
            return session.getBoxGroundY() + 0.5;
        }
        return fallback;
    }

    @Nullable
    private static GroundPick fromClientBlockTop(@Nullable Vector3i clientBlock) {
        if (clientBlock == null) {
            return null;
        }
        return new GroundPick(
            clientBlock.x() + 0.5,
            blockTopY(clientBlock.y()),
            clientBlock.z() + 0.5
        );
    }

    public static boolean isUsableCommandScreenPoint(@Nullable Vector2fc screen) {
        if (screen == null) {
            return false;
        }
        float x = screen.x();
        float y = screen.y();
        return Float.isFinite(x) && Float.isFinite(y);
    }

    @Nullable
    private static GroundPick fromClientBlock(@Nullable Vector3i clientBlock) {
        if (clientBlock == null) {
            return null;
        }
        return new GroundPick(
            clientBlock.x() + 0.5,
            clientBlock.y() + 0.5,
            clientBlock.z() + 0.5
        );
    }

    @Nullable
    public static GroundPick pickFromScreen(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector2fc screenPoint
    ) {
        if (screenPoint == null) {
            return null;
        }
        return pickFromScreenCoords(session, screenPoint.x(), screenPoint.y());
    }

    @Nullable
    public static GroundPick pickFromScreen(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable Vector2dc screenPoint
    ) {
        if (screenPoint == null) {
            return null;
        }
        return pickFromScreenCoords(session, (float) screenPoint.x(), (float) screenPoint.y());
    }

    @Nullable
    public static GroundPick pickFromScreenCoords(
        @Nonnull RtsCommandPlayerComponent session,
        float screenX,
        float screenY
    ) {
        return pickFromNormalizedScreenCoords(
            session,
            cameraRawToNormalizedX(screenX),
            cameraRawToNormalizedY(screenY)
        );
    }

    @Nullable
    public static GroundPick pickFromNormalizedScreenCoords(
        @Nonnull RtsCommandPlayerComponent session,
        float normalizedX,
        float normalizedY
    ) {
        double groundY = groundPlaneY(session);
        Vector3d hit = pickGroundFromNormalized(
            normalizedX,
            normalizedY,
            session.getFocusX(),
            groundY,
            session.getFocusZ(),
            session.getDistance()
        );
        if (hit == null) {
            return null;
        }
        return new GroundPick(hit.x, hit.y, hit.z);
    }

    /** World X/Z column from the four HUD rectangle corners (orthographic ground projection). */
    @Nullable
    public static WorldAabb pickHudRectWorldColumn(@Nonnull RtsCommandPlayerComponent session) {
        float[] rect = hudDrawnSelectionRectNormalized(session);
        return pickOrthographicRectToWorldColumn(session, rect[0], rect[1], rect[2], rect[3]);
    }

    @Nullable
    public static WorldAabb pickOrthographicRectToWorldColumn(
        @Nonnull RtsCommandPlayerComponent session,
        float minNormX,
        float minNormY,
        float maxNormX,
        float maxNormY
    ) {
        GroundPick p00 = pickOrthographicGround(session, minNormX, minNormY);
        GroundPick p10 = pickOrthographicGround(session, maxNormX, minNormY);
        GroundPick p11 = pickOrthographicGround(session, maxNormX, maxNormY);
        GroundPick p01 = pickOrthographicGround(session, minNormX, maxNormY);
        return mergeSamples(session, p00, p10, p11, p01);
    }

    @Nullable
    public static GroundPick pickOrthographicGround(
        @Nonnull RtsCommandPlayerComponent session,
        float normalizedX,
        float normalizedY
    ) {
        float ndcX = (clampNormalized(normalizedX) - 0.5f) * 2f;
        float ndcY = (clampNormalized(normalizedY) - 0.5f) * 2f;
        double halfW = orthoHalfWidth(session);
        double halfH = orthoHalfHeight(session);
        double worldX = session.getFocusX() + ndcX * halfW;
        double worldZ = session.getFocusZ() + ndcY * halfH;
        return new GroundPick(worldX, groundPlaneY(session), worldZ);
    }

    /** Infer ortho scale from a client ground hit and matching normalized screen sample. */
    public static void calibrateOrthoFromSample(
        @Nonnull RtsCommandPlayerComponent session,
        float normalizedX,
        float normalizedY,
        double worldX,
        double worldZ
    ) {
        float ndcX = (clampNormalized(normalizedX) - 0.5f) * 2f;
        float ndcY = (clampNormalized(normalizedY) - 0.5f) * 2f;
        if (Math.abs(ndcX) > 0.08f) {
            session.setOrthoHalfWidth(Math.abs((worldX - session.getFocusX()) / ndcX));
        }
        if (Math.abs(ndcY) > 0.08f) {
            session.setOrthoHalfHeight(Math.abs((worldZ - session.getFocusZ()) / ndcY));
        }
    }

    public static double orthoHalfWidth(@Nonnull RtsCommandPlayerComponent session) {
        if (session.getOrthoHalfWidth() > 0.0) {
            return session.getOrthoHalfWidth();
        }
        return defaultOrthoHalfWidth(session.getDistance());
    }

    public static double orthoHalfHeight(@Nonnull RtsCommandPlayerComponent session) {
        if (session.getOrthoHalfHeight() > 0.0) {
            return session.getOrthoHalfHeight();
        }
        return defaultOrthoHalfHeight(session.getDistance());
    }

    public static double defaultOrthoHalfWidth(float cameraDistance) {
        return defaultOrthoHalfHeight(cameraDistance) * ASPECT_RATIO;
    }

    public static double defaultOrthoHalfHeight(float cameraDistance) {
        return cameraDistance * Math.tan(Math.toRadians(VERTICAL_FOV_DEG * 0.5));
    }

    /** @deprecated use {@link #pickHudRectWorldColumn} */
    @Deprecated
    public static WorldAabb pickSessionScreenRectToWorldAabb(@Nonnull RtsCommandPlayerComponent session) {
        return pickHudRectWorldColumn(session);
    }

    /**
     * Normalized 0..1 bounds of the drawn HUD overlay ({@code minX, minY, maxX, maxY}).
     * Mirrors {@link ui.RtsBoxSelectHud#refresh} pixel layout converted back to normalized space.
     */
    @Nonnull
    public static float[] hudDrawnSelectionRectNormalized(@Nonnull RtsCommandPlayerComponent session) {
        float x0 = session.getBoxScreenStartX();
        float y0 = session.getBoxScreenStartY();
        float x1 = session.getBoxScreenEndX();
        float y1 = session.getBoxScreenEndY();
        int inset = HUD_BOX_BORDER_INSET;
        int left = Math.max(0, toHudPixelX(Math.min(x0, x1)) - inset);
        int top = Math.max(0, toHudPixelY(Math.min(y0, y1)) - inset);
        int width = Math.max(2, toHudPixelX(Math.max(x0, x1)) - left + inset);
        int height = Math.max(2, toHudPixelY(Math.max(y0, y1)) - top + inset);
        return new float[] {
            left / REF_WIDTH,
            top / REF_HEIGHT,
            (left + width) / REF_WIDTH,
            (top + height) / REF_HEIGHT
        };
    }

    /** True when any part of the guard footprint projects inside the drawn HUD box. */
    public static boolean guardInHudSelectionRect(
        double worldX,
        double worldZ,
        @Nonnull RtsCommandPlayerComponent session
    ) {
        float[] rect = hudDrawnSelectionRectNormalized(session);
        return guardOverlapsScreenRect(worldX, worldZ, session, rect[0], rect[1], rect[2], rect[3]);
    }

    @Nullable
    public static WorldAabb pickScreenRectToWorldAabb(
        @Nonnull RtsCommandPlayerComponent session,
        float screenX0,
        float screenY0,
        float screenX1,
        float screenY1
    ) {
        float minSx = Math.min(screenX0, screenX1);
        float maxSx = Math.max(screenX0, screenX1);
        float minSy = Math.min(screenY0, screenY1);
        float maxSy = Math.max(screenY0, screenY1);
        return mergeSamples(
            session,
            pickFromNormalizedScreenCoords(session, minSx, minSy),
            pickFromNormalizedScreenCoords(session, maxSx, maxSy),
            pickFromNormalizedScreenCoords(session, minSx, maxSy),
            pickFromNormalizedScreenCoords(session, maxSx, minSy)
        );
    }

    @Nullable
    private static WorldAabb mergeSamples(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable GroundPick... picks
    ) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double groundY = groundPlaneY(session);
        for (GroundPick p : picks) {
            if (p == null) {
                continue;
            }
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
            groundY = p.y();
        }
        if (!Double.isFinite(minX)) {
            return null;
        }
        return new WorldAabb(minX, maxX, minZ, maxZ, groundY);
    }

    public static double groundPlaneY(@Nonnull RtsCommandPlayerComponent session) {
        if (session.getBoxGroundY() != 0.0) {
            return session.getBoxGroundY();
        }
        return session.getFocusY() - session.getDistance();
    }

    public static double viewHeightAboveGround(@Nonnull RtsCommandPlayerComponent session) {
        return session.getDistance() + CAMERA_EYE_OFFSET_Y;
    }

    private static void groundFrustumHalfExtents(float cameraDistance, @Nonnull double[] outHalfWidthHalfHeight) {
        double viewHeight = cameraDistance + CAMERA_EYE_OFFSET_Y;
        double halfTan = Math.tan(Math.toRadians(VERTICAL_FOV_DEG * 0.5));
        double halfHeight = viewHeight * halfTan;
        outHalfWidthHalfHeight[0] = halfHeight * ASPECT_RATIO;
        outHalfWidthHalfHeight[1] = halfHeight;
    }

    @Nullable
    public static Vector3d pickGroundPoint(
        float screenX,
        float screenY,
        double focusX,
        double planeY,
        double focusZ,
        float cameraDistance
    ) {
        return pickGroundFromNormalized(
            cameraRawToNormalizedX(screenX),
            cameraRawToNormalizedY(screenY),
            focusX,
            planeY,
            focusZ,
            cameraDistance
        );
    }

    @Nullable
    public static Vector3d pickGroundFromNormalized(
        float normalizedX,
        float normalizedY,
        double focusX,
        double groundPlaneY,
        double focusZ,
        float cameraDistance
    ) {
        float ndcX = (clampNormalized(normalizedX) - 0.5f) * 2f;
        float ndcY = (clampNormalized(normalizedY) - 0.5f) * 2f;

        double[] half = new double[2];
        groundFrustumHalfExtents(cameraDistance, half);

        double worldX = focusX + ndcX * half[0];
        double worldZ = focusZ + ndcY * half[1];
        return new Vector3d(worldX, groundPlaneY, worldZ);
    }

    public static float cameraRawToNormalizedX(float raw) {
        if (!Float.isFinite(raw)) {
            return 0.5f;
        }
        if (raw > 1.5f) {
            return clampNormalized(raw / REF_WIDTH);
        }
        if (raw >= -CAMERA_NDC_MAX && raw <= CAMERA_NDC_MAX) {
            return clampNormalized((raw + 1f) * 0.5f);
        }
        return clampNormalized(raw);
    }

    public static float cameraRawToNormalizedY(float raw) {
        if (!Float.isFinite(raw)) {
            return 0.5f;
        }
        if (raw > 1.5f) {
            return clampNormalized(raw / REF_HEIGHT);
        }
        if (raw >= -CAMERA_NDC_MAX && raw <= CAMERA_NDC_MAX) {
            return clampNormalized((raw + 1f) * 0.5f);
        }
        return clampNormalized(raw);
    }

    public static boolean rawScreenMoved(float rawX, float rawY, float lastRawX, float lastRawY) {
        return Math.abs(rawX - lastRawX) >= RAW_SCREEN_EPS || Math.abs(rawY - lastRawY) >= RAW_SCREEN_EPS;
    }

    public static final int HUD_BOX_BORDER_INSET = 12;
    private static final float HUD_X_NUDGE = -0.017f;

    public static int toHudPixelX(float normalized) {
        return Math.round(clampNormalized(normalized + HUD_X_NUDGE) * REF_WIDTH);
    }

    public static int toHudPixelY(float normalized) {
        return toPixelY(normalized);
    }

    public static int toPixelX(float normalized) {
        return Math.round(clampNormalized(normalized) * REF_WIDTH);
    }

    public static int toPixelY(float normalized) {
        return Math.round(clampNormalized(normalized) * REF_HEIGHT);
    }

    public static float worldToNormalizedScreenX(double worldX, @Nonnull RtsCommandPlayerComponent session) {
        double[] half = new double[2];
        groundFrustumHalfExtents(session.getDistance(), half);
        if (half[0] < 1e-6) {
            return 0.5f;
        }
        double ndcX = (worldX - session.getFocusX()) / half[0];
        return clampNormalized((float) (ndcX * 0.5 + 0.5));
    }

    public static float worldToNormalizedScreenY(double worldZ, @Nonnull RtsCommandPlayerComponent session) {
        double[] half = new double[2];
        groundFrustumHalfExtents(session.getDistance(), half);
        if (half[1] < 1e-6) {
            return 0.5f;
        }
        double ndcY = (worldZ - session.getFocusZ()) / half[1];
        return clampNormalized((float) (ndcY * 0.5 + 0.5));
    }

    public static float clampNormalized(float value) {
        if (value > 1.5f) {
            return clampNormalized(value / REF_WIDTH);
        }
        return Math.max(0f, Math.min(1f, value));
    }

    public static void applyScreenFromWorldPick(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable GroundPick pick
    ) {
        if (pick == null) {
            return;
        }
        float sx = worldToNormalizedScreenX(pick.x(), session);
        float sy = worldToNormalizedScreenY(pick.z(), session);
        session.setBoxScreenEnd(sx, sy);
    }

    public static void applyScreenStartFromWorldPick(
        @Nonnull RtsCommandPlayerComponent session,
        @Nullable GroundPick pick
    ) {
        if (pick == null) {
            return;
        }
        float sx = worldToNormalizedScreenX(pick.x(), session);
        float sy = worldToNormalizedScreenY(pick.z(), session);
        session.setBoxScreenStart(sx, sy);
        session.setBoxScreenEnd(sx, sy);
    }

    /**
     * Screen-space overlap using the same 75° FOV projection as corner raycasts.
     * Projects a small ground footprint so guards visibly touching the box are included.
     */
    public static boolean guardOverlapsScreenRect(
        double worldX,
        double worldZ,
        @Nonnull RtsCommandPlayerComponent session,
        float rectMinX,
        float rectMinY,
        float rectMaxX,
        float rectMaxY
    ) {
        double r = GUARD_FOOTPRINT_RADIUS;
        float sx0 = worldToNormalizedScreenX(worldX - r, session);
        float sx1 = worldToNormalizedScreenX(worldX + r, session);
        float sy0 = worldToNormalizedScreenY(worldZ - r, session);
        float sy1 = worldToNormalizedScreenY(worldZ + r, session);
        float centerSx = worldToNormalizedScreenX(worldX, session);
        float centerSy = worldToNormalizedScreenY(worldZ, session);

        float guardMinX = Math.min(centerSx, Math.min(sx0, sx1));
        float guardMaxX = Math.max(centerSx, Math.max(sx0, sx1));
        float guardMinY = Math.min(centerSy, Math.min(sy0, sy1));
        float guardMaxY = Math.max(centerSy, Math.max(sy0, sy1));

        return guardMaxX >= rectMinX && guardMinX <= rectMaxX
            && guardMaxY >= rectMinY && guardMinY <= rectMaxY;
    }
}
