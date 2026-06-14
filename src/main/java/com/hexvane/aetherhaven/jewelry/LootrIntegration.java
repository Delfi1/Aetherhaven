package com.hexvane.aetherhaven.jewelry;

import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Reflection bridge to [LootrHytale](https://github.com/LootrMinecraft/LootrHytale) when that mod is present. */
public final class LootrIntegration {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static volatile boolean attempted;
    private static volatile boolean available;
    @Nullable
    private static volatile ComponentType<ChunkStore, ?> lootComponentType;
    @Nullable
    private static volatile Field originalBlockField;

    private LootrIntegration() {}

  /**
   * Resolves Lootr types after all plugins have finished {@code setup()}. Call from {@link AetherhavenPlugin#start()}.
   */
    public static boolean tryInitialize() {
        if (attempted) {
            return available;
        }
        attempted = true;
        try {
            Class<?> lootrPluginClass;
            try {
                lootrPluginClass = Class.forName("noobanidus.mods.lootr.LootrPlugin");
            } catch (ClassNotFoundException ignored) {
                return false;
            }

            Object lootrPlugin = lootrPluginClass.getMethod("get").invoke(null);
            if (lootrPlugin == null) {
                LOGGER.atInfo().log("Lootr compatibility disabled: Lootr plugin instance is null.");
                return false;
            }
            Object typeObj = lootrPluginClass.getMethod("getLootContainerType").invoke(lootrPlugin);
            if (!(typeObj instanceof ComponentType<?, ?>)) {
                LOGGER.atWarning().log("Lootr compatibility disabled: Lootr loot component type not resolved.");
                return false;
            }
            @SuppressWarnings("unchecked")
            ComponentType<ChunkStore, ?> typed = (ComponentType<ChunkStore, ?>) typeObj;
            lootComponentType = typed;

            Class<?> lootrBlockClass = Class.forName("noobanidus.mods.lootr.block.ItemLootContainerBlock");
            Field original = lootrBlockClass.getDeclaredField("originalBlock");
            original.setAccessible(true);
            originalBlockField = original;

            available = true;
            LOGGER.atInfo().log("Lootr compatibility enabled.");
            return true;
        } catch (Throwable t) {
            LOGGER.atWarning().withCause(t).log("Lootr compatibility disabled: Lootr classes incompatible.");
            return false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    @Nullable
    public static ComponentType<ChunkStore, ?> getLootComponentType() {
        return lootComponentType;
    }

    @Nullable
    public static String readOriginalBlockId(@Nonnull Object lootrBlock) {
        Field field = originalBlockField;
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(lootrBlock);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (IllegalAccessException ignored) {
            // Fall through
        }
        return null;
    }

    /**
     * Lootr chests use a wrapper block id; eligibility should follow the original chest type when known.
     */
    @Nullable
    public static String resolveEligibleBlockTypeId(
        @Nonnull Object lootrBlock,
        @Nonnull Store<ChunkStore> chunkStore,
        @Nonnull BlockModule.BlockStateInfo state
    ) {
        String original = readOriginalBlockId(lootrBlock);
        if (original != null) {
            return original;
        }
        return LootChestBonusInjectSystem.resolveBlockTypeIdForState(chunkStore, state);
    }

    public static void registerIfAvailable(@Nonnull AetherhavenPlugin plugin) {
        if (!tryInitialize()) {
            return;
        }
        plugin.getEntityStoreRegistry().registerSystem(new LootrPerPlayerLootInjectPlayerSystem(plugin));
    }
}
