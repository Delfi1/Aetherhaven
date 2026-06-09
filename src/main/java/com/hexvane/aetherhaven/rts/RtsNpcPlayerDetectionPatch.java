package com.hexvane.aetherhaven.rts;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.support.PositionCache;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.function.BiPredicate;

/**
 * Extends {@link PositionCache#IS_VALID_PLAYER} so active RTS commanders are excluded from NPC
 * position caches — the same effect as creative {@code allowNPCDetection=false}, without changing game mode.
 */
public final class RtsNpcPlayerDetectionPatch {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile boolean installed;

    private RtsNpcPlayerDetectionPatch() {}

    public static void install() {
        if (installed) {
            return;
        }
        try {
            BiPredicate<Ref<EntityStore>, ComponentAccessor<EntityStore>> vanilla = PositionCache.IS_VALID_PLAYER;
            BiPredicate<Ref<EntityStore>, ComponentAccessor<EntityStore>> wrapped = (ref, accessor) -> {
                if (RtsCommanderNpcDetection.isHiddenFromNpcs(ref, accessor)) {
                    return false;
                }
                return vanilla.test(ref, accessor);
            };
            writeStaticFinalField(PositionCache.class, "IS_VALID_PLAYER", wrapped);
            installed = true;
        } catch (Throwable e) {
            LOGGER.atWarning().withCause(e).log(
                "Could not patch NPC player detection for RTS stealth; mobs may still target commanders."
            );
        }
    }

    private static void writeStaticFinalField(Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        VarHandle handle = lookup.findStaticVarHandle(owner, name, field.getType());
        handle.set(value);
    }
}
