package com.hexvane.aetherhaven.plotcreator.icon;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/** Averages RGB from block texture PNGs registered in {@link CommonAssetRegistry}. */
public final class BlockTextureColorSampler {
    private static final int UNRESOLVED = Integer.MIN_VALUE;
    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private BlockTextureColorSampler() {}

    public static int averageArgb(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return UNRESOLVED;
        }
        String key = "full:" + texturePath.trim();
        return CACHE.computeIfAbsent(key, k -> loadAverageArgb(texturePath.trim(), 0f, 0f, 1f, 1f));
    }

    /** Hay/straw roof atlases store golden thatch in the upper rows and wood trim lower (see Slope_Hay.blockymodel). */
    public static int averageRoofTopArgb(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return UNRESOLVED;
        }
        String key = "roofTop:" + texturePath.trim();
        return CACHE.computeIfAbsent(key, k -> loadAverageArgb(texturePath.trim(), 0f, 0f, 1f, 0.38f));
    }

    /** Wood trim / underside band on hay roof texture atlases. */
    public static int averageRoofSideArgb(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return UNRESOLVED;
        }
        String key = "roofSide:" + texturePath.trim();
        return CACHE.computeIfAbsent(key, k -> loadAverageArgb(texturePath.trim(), 0f, 0.3f, 1f, 0.7f));
    }

    public static boolean isResolved(int argb) {
        return argb != UNRESOLVED;
    }

    public static boolean isHayRoofAtlas(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return false;
        }
        String path = texturePath.replace('\\', '/');
        return path.contains("Slope_Hay_Textures/")
            || path.contains("Flat_Hay_Textures/")
            || path.contains("/Roofs/") && path.contains("Hay");
    }

    private static int loadAverageArgb(
        @Nonnull String texturePath,
        float regionX,
        float regionY,
        float regionW,
        float regionH
    ) {
        try {
            CommonAsset asset = CommonAssetRegistry.getByName(texturePath);
            if (asset == null) {
                return UNRESOLVED;
            }
            byte[] bytes = asset.getBlob().join();
            if (bytes == null || bytes.length == 0) {
                return UNRESOLVED;
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return UNRESOLVED;
            }
            return averageOpaquePixels(image, regionX, regionY, regionW, regionH);
        } catch (Exception ignored) {
            return UNRESOLVED;
        }
    }

    private static int averageOpaquePixels(
        @Nonnull BufferedImage image,
        float regionX,
        float regionY,
        float regionW,
        float regionH
    ) {
        int startX = Math.max(0, Math.round(image.getWidth() * regionX));
        int startY = Math.max(0, Math.round(image.getHeight() * regionY));
        int endX = Math.min(image.getWidth(), Math.round(image.getWidth() * (regionX + regionW)));
        int endY = Math.min(image.getHeight(), Math.round(image.getHeight() * (regionY + regionH)));
        if (endX <= startX || endY <= startY) {
            return UNRESOLVED;
        }
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        long count = 0;
        int step = Math.max(1, Math.min(endX - startX, endY - startY) / 12);
        for (int y = startY; y < endY; y += step) {
            for (int x = startX; x < endX; x += step) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 32) {
                    continue;
                }
                sumR += (argb >> 16) & 0xFF;
                sumG += (argb >> 8) & 0xFF;
                sumB += argb & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return UNRESOLVED;
        }
        int r = (int) (sumR / count);
        int g = (int) (sumG / count);
        int b = (int) (sumB / count);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
