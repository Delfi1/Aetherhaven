package com.hexvane.aetherhaven.plotcreator;

import com.hexvane.aetherhaven.asset.AetherhavenAssetPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Paths under the plugin data directory ({@code mods/Hexvane_Aetherhaven}) for player authored buildings. */
public final class CustomBuildingsPaths {
    public static final String PREFABS_RELATIVE = "Server/Prefabs";
    public static final String ICONS_RELATIVE = "Common/Icons/ItemsGenerated";
    private static final String ICON_FILE_PREFIX = "Aetherhaven_Token_";

    private CustomBuildingsPaths() {}

    @Nonnull
    public static Path buildingsDirectory(@Nonnull Path dataDirectory) {
        return dataDirectory.resolve(AetherhavenAssetPaths.BUILDINGS);
    }

    @Nonnull
    public static Path prefabMaterialsDirectory(@Nonnull Path dataDirectory) {
        return dataDirectory.resolve(AetherhavenAssetPaths.PREFAB_MATERIALS);
    }

    @Nonnull
    public static Path prefabsDirectory(@Nonnull Path dataDirectory) {
        return dataDirectory.resolve(PREFABS_RELATIVE);
    }

    @Nonnull
    public static Path iconsDirectory(@Nonnull Path dataDirectory) {
        return dataDirectory.resolve(ICONS_RELATIVE);
    }

    @Nonnull
    public static Path iconFile(@Nonnull Path dataDirectory, @Nonnull String constructionId) {
        return iconsDirectory(dataDirectory).resolve(iconFileName(constructionId));
    }

    @Nonnull
    public static String iconFileName(@Nonnull String constructionId) {
        return ICON_FILE_PREFIX + constructionId.trim() + ".png";
    }

    @Nonnull
    public static String iconAssetPath(@Nonnull String constructionId) {
        return ICONS_RELATIVE.replace('\\', '/') + "/" + iconFileName(constructionId);
    }

    @Nonnull
    public static Path buildingFile(@Nonnull Path dataDirectory, @Nonnull String constructionId) {
        return buildingsDirectory(dataDirectory).resolve(constructionId + ".json");
    }

    @Nullable
    public static Path resolvePrefabFile(@Nonnull Path dataDirectory, @Nullable String prefabPathKey) {
        if (prefabPathKey == null || prefabPathKey.isBlank()) {
            return null;
        }
        String key = prefabPathKey.trim().replace('\\', '/');
        String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        if (!fileName.endsWith(".prefab.json")) {
            fileName = fileName + ".prefab.json";
        }
        Path candidate = prefabsDirectory(dataDirectory).resolve(fileName);
        return Files.isRegularFile(candidate) ? candidate : null;
    }

    public static boolean isUnderDataDirectory(@Nonnull Path dataDirectory, @Nonnull Path absoluteFile) {
        Path base = buildingsDirectory(dataDirectory).toAbsolutePath().normalize();
        Path file = absoluteFile.toAbsolutePath().normalize();
        return file.startsWith(base);
    }
}
