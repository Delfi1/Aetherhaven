package com.hexvane.aetherhaven.map;

import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Unpacks and repacks palette-based {@link MapImage} tiles for per-player border overlays. */
public final class TownMapImagePixels {
    public static final int MAP_CHUNK_BLOCK_SIZE = 32;

    private static final int MAX_PALETTE_4BIT = 16;
    private static final int MAX_PALETTE_8BIT = 256;
    private static final int MAX_PALETTE_12BIT = 4096;

    private TownMapImagePixels() {}

    public static boolean hasPixelData(@Nullable MapImage image) {
        if (image == null || image.width <= 0 || image.height <= 0) {
            return false;
        }
        int pixelCount = image.width * image.height;
        return pixelCount > 0
            && image.palette != null
            && image.palette.length > 0
            && image.packedIndices != null
            && image.packedIndices.length > 0
            && Byte.toUnsignedInt(image.bitsPerIndex) > 0;
    }

    @Nullable
    public static MapImage cloneImage(@Nullable MapImage source) {
        if (!hasPixelData(source)) {
            return null;
        }
        MapImage copy = new MapImage(source.width, source.height, null, (byte) 0, null);
        copy.palette = source.palette.clone();
        copy.bitsPerIndex = source.bitsPerIndex;
        copy.packedIndices = source.packedIndices.clone();
        return copy;
    }

    /**
     * Clones {@code base} and writes border colors at sparse pixel positions.
     * Falls back to full repack when the palette would exceed the current bit width.
     */
    @Nullable
    public static MapImage applySparsePixelColors(
        @Nonnull MapImage base,
        @Nonnull int[] pixelIndices,
        @Nonnull int[] colors
    ) {
        if (pixelIndices.length == 0) {
            return cloneImage(base);
        }
        if (pixelIndices.length != colors.length) {
            return null;
        }
        if (!hasPixelData(base)) {
            return null;
        }

        MapImage copy = cloneImage(base);
        if (copy == null) {
            return null;
        }

        int bits = Byte.toUnsignedInt(copy.bitsPerIndex);
        int pixelCount = copy.width * copy.height;
        int[] palette = copy.palette.clone();
        BitFieldArr indices = new BitFieldArr(bits, pixelCount);
        indices.set(copy.packedIndices);

        int paletteSize = palette.length;

        for (int i = 0; i < pixelIndices.length; i++) {
            int pixelIndex = pixelIndices[i];
            if (pixelIndex < 0 || pixelIndex >= pixelCount) {
                continue;
            }
            int color = colors[i];
            int paletteIndex = findPaletteIndex(palette, paletteSize, color);
            if (paletteIndex < 0) {
                if (!canAppendColor(paletteSize)) {
                    return applySparseViaFullRepack(base, pixelIndices, colors);
                }
                palette = Arrays.copyOf(palette, paletteSize + 1);
                palette[paletteSize] = color;
                paletteIndex = paletteSize;
                paletteSize++;
            }
            indices.set(pixelIndex, paletteIndex);
        }

        int requiredBits = calculateBitsRequired(Math.max(1, paletteSize));
        if (requiredBits > bits) {
            return applySparseViaFullRepack(base, pixelIndices, colors);
        }

        copy.palette = paletteSize == palette.length ? palette : Arrays.copyOf(palette, paletteSize);
        copy.bitsPerIndex = (byte) requiredBits;
        if (requiredBits != bits) {
            BitFieldArr resized = new BitFieldArr(requiredBits, pixelCount);
            for (int p = 0; p < pixelCount; p++) {
                resized.set(p, indices.get(p));
            }
            copy.packedIndices = resized.get();
        } else {
            copy.packedIndices = indices.get();
        }
        return copy;
    }

    @Nullable
    private static MapImage applySparseViaFullRepack(
        @Nonnull MapImage base,
        @Nonnull int[] pixelIndices,
        @Nonnull int[] colors
    ) {
        int[] pixels = unpackToArgb(base);
        if (pixels == null) {
            return null;
        }
        for (int i = 0; i < pixelIndices.length; i++) {
            int idx = pixelIndices[i];
            if (idx >= 0 && idx < pixels.length) {
                pixels[idx] = colors[i];
            }
        }
        MapImage copy = cloneImage(base);
        if (copy == null) {
            return null;
        }
        repackFromArgb(copy, pixels);
        return copy;
    }

    private static boolean canAppendColor(int paletteSize) {
        int next = paletteSize + 1;
        return next <= MAX_PALETTE_4BIT || next <= MAX_PALETTE_8BIT || next <= MAX_PALETTE_12BIT;
    }

    private static int findPaletteIndex(@Nonnull int[] palette, int paletteSize, int color) {
        for (int i = 0; i < paletteSize; i++) {
            if (palette[i] == color) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public static int[] unpackToArgb(@Nullable MapImage image) {
        if (!hasPixelData(image)) {
            return null;
        }
        int bits = Byte.toUnsignedInt(image.bitsPerIndex);
        int pixelCount = image.width * image.height;
        int[] palette = image.palette;

        BitFieldArr indices = new BitFieldArr(bits, pixelCount);
        indices.set(image.packedIndices);

        int[] out = new int[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            int paletteIndex = indices.get(i);
            out[i] = (paletteIndex >= 0 && paletteIndex < palette.length) ? palette[paletteIndex] : 0;
        }
        return out;
    }

    public static void repackFromArgb(@Nonnull MapImage target, @Nonnull int[] pixels) {
        MapImage encoded = fromRawPixels(target.width, target.height, pixels);
        target.palette = encoded.palette;
        target.bitsPerIndex = encoded.bitsPerIndex;
        target.packedIndices = encoded.packedIndices;
    }

    @Nonnull
    public static MapImage fromRawPixels(int width, int height, @Nonnull int[] pixels) {
        int pixelCount = width * height;
        Map<Integer, Integer> colorToIndex = new LinkedHashMap<>();
        for (int i = 0; i < pixelCount; i++) {
            colorToIndex.computeIfAbsent(pixels[i], ignored -> colorToIndex.size());
        }

        int[] palette = new int[colorToIndex.size()];
        for (Map.Entry<Integer, Integer> entry : colorToIndex.entrySet()) {
            palette[entry.getValue()] = entry.getKey();
        }

        int bitsPerIndex = calculateBitsRequired(Math.max(1, palette.length));
        BitFieldArr indices = new BitFieldArr(bitsPerIndex, pixelCount);
        for (int i = 0; i < pixelCount; i++) {
            indices.set(i, colorToIndex.get(pixels[i]));
        }

        return new MapImage(width, height, palette, (byte) bitsPerIndex, indices.get());
    }

    private static int calculateBitsRequired(int colorCount) {
        if (colorCount <= MAX_PALETTE_4BIT) {
            return 4;
        }
        if (colorCount <= MAX_PALETTE_8BIT) {
            return 8;
        }
        if (colorCount <= MAX_PALETTE_12BIT) {
            return 12;
        }
        return 16;
    }
}
