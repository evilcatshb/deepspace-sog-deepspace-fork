package com.deepspace.compat;

import com.deepspace.DeepSpaceMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Curios API compat -- equips items into Curios slots via reflection.
 *
 * Confirmed API from curios-forge-5.14.1-1.20.1.jar bytecode:
 *
 *   // Entry point -- static method on CuriosApi
 *   CuriosApi.getCuriosInventory(LivingEntity)
 *     -> LazyOptional<ICuriosItemHandler>
 *
 *   // Equip directly -- no sub-handler needed
 *   ICuriosItemHandler.setEquippedCurio(String slotId, int index, ItemStack stack)
 *     -> void
 *
 * The "back" slot is provided by default in Curios 5.x (data/curios/curios/slots/back.json).
 * We register gtceu:electric_jetpack to it via data/curios/tags/items/back.json.
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    // Reflection state -- initialised once on first use
    private static volatile boolean ready  = false;
    private static volatile boolean failed = false;

    private static java.lang.reflect.Method mGetCuriosInventory; // CuriosApi.getCuriosInventory(LivingEntity)
    private static java.lang.reflect.Method mIFPresent;          // LazyOptional.ifPresent(Consumer)
    private static java.lang.reflect.Method mSetEquippedCurio;   // ICuriosItemHandler.setEquippedCurio(String,int,ItemStack)
    private static java.lang.reflect.Method mIsPresent;          // LazyOptional.isPresent()
    private static java.lang.reflect.Method mResolve;            // LazyOptional.resolve() -> Optional

    /**
     * Equip an item into Curios slot {@code slotId} at index 0.
     *
     * @param player  Player to equip on
     * @param item    Item to place in the slot
     * @param slotId  Curios slot identifier, e.g. "back"
     * @return true if equipped successfully, false if Curios unavailable or slot not found
     */
    public static boolean equipInSlot(ServerPlayer player, ItemStack item, String slotId) {
        if (!CURIOS_LOADED) return false;
        if (failed) return false;
        try {
            if (!ready) initReflection();
            if (failed) return false;

            // CuriosApi.getCuriosInventory(player) -> LazyOptional<ICuriosItemHandler>
            Object lazyOpt = mGetCuriosInventory.invoke(null, player);

            // lazyOpt.isPresent()
            boolean present = (boolean) mIsPresent.invoke(lazyOpt);
            if (!present) return false;

            // lazyOpt.resolve() -> Optional<ICuriosItemHandler>
            Object optional = mResolve.invoke(lazyOpt);
            // optional.isPresent()
            boolean optPresent = (boolean) optional.getClass()
                .getMethod("isPresent").invoke(optional);
            if (!optPresent) return false;

            // optional.get() -> ICuriosItemHandler
            Object handler = optional.getClass().getMethod("get").invoke(optional);

            // handler.setEquippedCurio(slotId, 0, item)
            mSetEquippedCurio.invoke(handler, slotId, 0, item);

            DeepSpaceMod.LOGGER.debug("[CuriosCompat] Equipped {} in '{}' slot for {}",
                item.getDescriptionId(), slotId, player.getName().getString());
            return true;

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[CuriosCompat] equipInSlot('{}') failed: {}", slotId, e.getMessage());
            return false;
        }
    }

    /**
     * Check whether the player's Curios slot {@code slotId} at index 0 is empty.
     */
    public static boolean isSlotEmpty(ServerPlayer player, String slotId) {
        if (!CURIOS_LOADED) return true; // treat as empty for inventory fallback logic
        if (failed) return true;
        try {
            if (!ready) initReflection();
            if (failed) return true;

            Object lazyOpt = mGetCuriosInventory.invoke(null, player);
            boolean present = (boolean) mIsPresent.invoke(lazyOpt);
            if (!present) return true;

            Object optional = mResolve.invoke(lazyOpt);
            boolean optPresent = (boolean) optional.getClass()
                .getMethod("isPresent").invoke(optional);
            if (!optPresent) return true;

            Object handler = optional.getClass().getMethod("get").invoke(optional);

            // handler.getStacksHandler(slotId) -> Optional<ICurioStacksHandler>
            java.lang.reflect.Method mGetStacksHandler =
                handler.getClass().getMethod("getStacksHandler", String.class);
            Object stacksOpt = mGetStacksHandler.invoke(handler, slotId);
            boolean stacksPresent = (boolean) stacksOpt.getClass()
                .getMethod("isPresent").invoke(stacksOpt);
            if (!stacksPresent) return true;

            Object stacksHandler = stacksOpt.getClass().getMethod("get").invoke(stacksOpt);

            // stacksHandler.getStacks() -> IDynamicStackHandler (IItemHandler)
            java.lang.reflect.Method mGetStacks =
                stacksHandler.getClass().getMethod("getStacks");
            Object stacks = mGetStacks.invoke(stacksHandler);

            // stacks.getStackInSlot(0) -> ItemStack
            java.lang.reflect.Method mGetStack =
                stacks.getClass().getMethod("getStackInSlot", int.class);
            Object stack = mGetStack.invoke(stacks, 0);
            if (stack instanceof ItemStack is) return is.isEmpty();
            return true;

        } catch (Exception e) {
            return true;
        }
    }

    private static void initReflection() {
        try {
            Class<?> curApi      = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Class<?> living      = net.minecraft.world.entity.LivingEntity.class;
            Class<?> lazyOptClass = Class.forName("net.minecraftforge.common.util.LazyOptional");
            Class<?> handlerClass = Class.forName(
                "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");

            // CuriosApi.getCuriosInventory(LivingEntity)
            mGetCuriosInventory = curApi.getMethod("getCuriosInventory", living);

            // LazyOptional.isPresent() and LazyOptional.resolve()
            mIsPresent = lazyOptClass.getMethod("isPresent");
            mResolve   = lazyOptClass.getMethod("resolve");

            // ICuriosItemHandler.setEquippedCurio(String, int, ItemStack)
            mSetEquippedCurio = handlerClass.getMethod(
                "setEquippedCurio", String.class, int.class, ItemStack.class);

            ready = true;
            DeepSpaceMod.LOGGER.info("[CuriosCompat] Curios 5.x API ready (back slot active).");

        } catch (ClassNotFoundException e) {
            failed = true;
            DeepSpaceMod.LOGGER.info("[CuriosCompat] Curios not on classpath -- jetpack -> inventory.");
        } catch (Exception e) {
            failed = true;
            DeepSpaceMod.LOGGER.warn("[CuriosCompat] Reflection init failed: {} -- jetpack -> inventory.",
                e.getMessage());
        }
    }
}
