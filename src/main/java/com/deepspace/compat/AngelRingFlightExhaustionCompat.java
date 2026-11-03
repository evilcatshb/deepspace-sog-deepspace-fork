package com.deepspace.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Optional Angel Ring / Curios lookup for the flight-exhaustion compatibility mixin.
 * Uses reflection for Curios so this helper remains safe if Curios is absent.
 * Any missing mod, registry item, capability, or API method fails closed.
 */
public final class AngelRingFlightExhaustionCompat {
    private static final ResourceLocation ANGEL_RING_ID = new ResourceLocation("angelring", "angel_ring");

    private static volatile boolean initialized;
    private static volatile boolean unavailable;
    private static Method getCuriosInventory;
    private static Method lazyOptionalResolve;
    private static Method isEquipped;

    private AngelRingFlightExhaustionCompat() {}

    public static boolean isAngelRingEquipped(LivingEntity entity) {
        if (entity == null || !ModList.get().isLoaded("angelring") || !ModList.get().isLoaded("curios")) {
            return false;
        }

        Item ring = ForgeRegistries.ITEMS.getValue(ANGEL_RING_ID);
        if (ring == null || ring == Items.AIR) {
            return false;
        }

        if (!initialize()) {
            return false;
        }

        try {
            Object lazy = getCuriosInventory.invoke(null, entity);
            if (lazy == null) return false;
            @SuppressWarnings("unchecked")
            Optional<Object> resolved = (Optional<Object>) lazyOptionalResolve.invoke(lazy);
            if (resolved == null || resolved.isEmpty()) return false;
            return Boolean.TRUE.equals(isEquipped.invoke(resolved.get(), ring));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    /** Returns true only when Tough As Nails is present and its thirst system is enabled. */
    public static boolean isThirstIntegrationAvailable() {
        if (!ModList.get().isLoaded("toughasnails")) return false;
        try {
            Class<?> helper = Class.forName("toughasnails.api.thirst.ThirstHelper");
            Method enabled = helper.getMethod("isThirstEnabled");
            return Boolean.TRUE.equals(enabled.invoke(null));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }
    private static boolean initialize() {
        if (initialized) return !unavailable;
        synchronized (AngelRingFlightExhaustionCompat.class) {
            if (initialized) return !unavailable;
            try {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> curiosHandler = Class.forName(
                    "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                Class<?> lazyOptional = Class.forName("net.minecraftforge.common.util.LazyOptional");
                getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                lazyOptionalResolve = lazyOptional.getMethod("resolve");
                isEquipped = curiosHandler.getMethod("isEquipped", Item.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                unavailable = true;
            } finally {
                initialized = true;
            }
        }
        return !unavailable;
    }
}
