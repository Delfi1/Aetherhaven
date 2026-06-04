package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shop prices ship in the mod jar at {@value #DEFAULT_RESOURCE}. The data folder may hold a small
 * {@value #SHOP_PRICES_FILE_NAME} with {@code catalogRevision} and per-item overrides only. A legacy full copy of the
 * catalog (from older builds) is ignored in favor of the bundled file.
 */
public final class ShopPriceFiles {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String SHOP_PRICES_FILE_NAME = "shop_prices.json";
    private static final String DEFAULT_RESOURCE = "/defaults/shop_prices.json";
    /** Bump when the bundled defaults/shop_prices.json changes in a breaking way. */
    public static final int BUNDLED_CATALOG_REVISION = 1;
    /** Legacy installs copied the full catalog; small files are treated as intentional overrides. */
    private static final int LEGACY_OVERRIDE_MAX_ENTRIES = 64;
    private static final String OVERRIDE_TEMPLATE =
        """
        {
          "catalogRevision": %d,
          "prices": {}
        }
        """.formatted(BUNDLED_CATALOG_REVISION);

    private ShopPriceFiles() {}

    @Nonnull
    public static Path pricesPath(@Nonnull AetherhavenPlugin plugin) {
        return plugin.getDataDirectory().resolve(SHOP_PRICES_FILE_NAME);
    }

    @Nonnull
    public static String readDefaultJson() throws IOException {
        try (InputStream in = ShopPriceFiles.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                return "{\"catalogRevision\":%d,\"defaultGoldPrice\":5,\"prices\":{}}"
                    .formatted(BUNDLED_CATALOG_REVISION);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void ensureDefaultPricesFile(@Nonnull AetherhavenPlugin plugin) {
        Path path = pricesPath(plugin);
        if (Files.isRegularFile(path)) {
            return;
        }
        try {
            Files.createDirectories(plugin.getDataDirectory());
            Files.writeString(path, OVERRIDE_TEMPLATE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to write default %s", SHOP_PRICES_FILE_NAME);
        }
    }

    @Nonnull
    public static ShopPriceCatalog loadCatalog(@Nonnull AetherhavenPlugin plugin) {
        try {
            ShopPriceCatalog bundled = ShopPriceCatalog.parseJson(readDefaultJson());
            Path path = pricesPath(plugin);
            if (!Files.isRegularFile(path)) {
                logLoadedCatalog(plugin, bundled, null, 0);
                return bundled;
            }
            String dataJson = Files.readString(path, StandardCharsets.UTF_8);
            ShopPriceCatalog data = ShopPriceCatalog.parseJson(dataJson);
            if (data.getCatalogRevision() >= BUNDLED_CATALOG_REVISION) {
                ShopPriceCatalog merged = bundled.withOverrides(data);
                logLoadedCatalog(plugin, merged, path, data.getExplicitPriceCount());
                return merged;
            }
            if (data.getExplicitPriceCount() > 0 && data.getExplicitPriceCount() <= LEGACY_OVERRIDE_MAX_ENTRIES) {
                ShopPriceCatalog merged = bundled.withOverrides(data);
                LOGGER
                    .atInfo()
                    .log(
                        "Loaded shop prices: bundled catalog with %d override(s) from legacy %s (add catalogRevision: %d to that file to silence this).",
                        data.getExplicitPriceCount(),
                        path,
                        BUNDLED_CATALOG_REVISION
                    );
                logLoadedCatalog(plugin, merged, path, data.getExplicitPriceCount());
                return merged;
            }
            backupLegacyCatalog(path);
            logLoadedCatalog(plugin, bundled, path, 0);
            return bundled;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to load shop price catalog; using empty catalog");
            try {
                return ShopPriceCatalog.parseJson(readDefaultJson());
            } catch (IOException e2) {
                return ShopPriceCatalog.empty();
            }
        }
    }

    private static void backupLegacyCatalog(@Nonnull Path path) {
        Path backup = path.resolveSibling(SHOP_PRICES_FILE_NAME + ".bak");
        try {
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER
                .atWarning()
                .log(
                    "Ignored outdated full %s at %s (replaced by bundled catalog). Backed up to %s. Use a small override file with catalogRevision: %d for custom prices.",
                    SHOP_PRICES_FILE_NAME,
                    path,
                    backup,
                    BUNDLED_CATALOG_REVISION
                );
        } catch (IOException e) {
            LOGGER
                .atWarning()
                .withCause(e)
                .log(
                    "Outdated %s at %s is still in use; delete it or set catalogRevision: %d. Using bundled catalog for this session.",
                    SHOP_PRICES_FILE_NAME,
                    path,
                    BUNDLED_CATALOG_REVISION
                );
        }
    }

    private static void logLoadedCatalog(
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull ShopPriceCatalog catalog,
        @Nullable Path overridePath,
        int overrideCount
    ) {
        if (overridePath == null) {
            LOGGER
                .atInfo()
                .log(
                    "Loaded shop prices: %d entries from bundled catalog (no %s in %s).",
                    catalog.getExplicitPriceCount(),
                    SHOP_PRICES_FILE_NAME,
                    plugin.getDataDirectory()
                );
        } else {
            LOGGER
                .atInfo()
                .log(
                    "Loaded shop prices: %d entries (%d override(s) from %s, bundled defaults).",
                    catalog.getExplicitPriceCount(),
                    overrideCount,
                    overridePath
                );
        }
    }
}
