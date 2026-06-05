package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * Registers plot-creator token thumbnails from the plugin data folder ({@code Common/Icons/ItemsGenerated}) so
 * clients receive them via {@link CommonAssetModule} (jar-bundled icons alone are not enough).
 */
public final class CustomBuildingIconAssetRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CustomBuildingIconAssetRegistry() {}

    public static void syncFromDataDirectory(@Nonnull AetherhavenPlugin plugin) {
        CommonAssetModule module = CommonAssetModule.get();
        if (module == null) {
            return;
        }
        String packId = new PluginIdentifier(plugin.getManifest()).toString();
        Path iconsDir = CustomBuildingsPaths.iconsDirectory(plugin.getDataDirectory());
        if (!Files.isDirectory(iconsDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(iconsDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .forEach(p -> registerIconFile(module, packId, p, false));
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to scan custom building icons at %s", iconsDir);
        }
    }

    public static void registerIconFile(@Nonnull AetherhavenPlugin plugin, @Nonnull Path iconFile) {
        CommonAssetModule module = CommonAssetModule.get();
        if (module == null || !Files.isRegularFile(iconFile)) {
            return;
        }
        String packId = new PluginIdentifier(plugin.getManifest()).toString();
        registerIconFile(module, packId, iconFile, true);
    }

    private static void registerIconFile(
        @Nonnull CommonAssetModule module,
        @Nonnull String packId,
        @Nonnull Path iconFile,
        boolean log
    ) {
        String assetName = "Icons/ItemsGenerated/" + iconFile.getFileName();
        try {
            byte[] bytes = Files.readAllBytes(iconFile);
            module.addCommonAsset(packId, new FileCommonAsset(iconFile, assetName, bytes), log);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to register custom building icon %s", iconFile);
        }
    }
}
