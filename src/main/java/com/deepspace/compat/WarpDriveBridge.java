package com.deepspace.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection bridge to WarpDrive's FieldProviderRegistry / InternalTankManager.
 *
 * DeepSpace cannot compile against the WarpDrive jar (no cross-dependency allowed
 * in either mod's libs/ directory). This class resolves all WarpDrive API calls
 * via reflection at runtime, falling back to {@code false} (treat as vacuum /
 * no gas) when WarpDrive is absent.
 *
 * All public methods are safe to call from any thread; the one-time initialisation
 * is double-checked-locked and idempotent.
 *
 * Bridged surface:
 * FieldProviderRegistry#hasAnyActiveOxygenField(ResourceKey, BlockPos, OxygenFuelGate)
 * FieldProviderRegistry#hasCO2Coverage(ResourceKey, BlockPos, OxygenFuelGate)
 * FieldProviderRegistry#hasOxygenCoverage(ResourceKey, BlockPos, OxygenFuelGate)
 *
 * The OxygenFuelGate argument is constructed internally via a JDK dynamic proxy
 * that delegates to InternalTankManager#hasFuel(String).
 */
public final class WarpDriveBridge {

    private WarpDriveBridge() {}

    // ── lazy init state ───────────────────────────────────────────────────────

    private static volatile boolean initialized = false;
    private static volatile boolean warpDrivePresent = false;

    /** com.warpdrive.field.FieldProviderRegistry singleton */
    private static Object registry;

    /**
     * Proxy implementing com.warpdrive.field.FieldProviderRegistry$OxygenFuelGate.
     * Delegates hasFuel(String) → InternalTankManager#hasFuel(String).
     */
    private static Object gate;

    private static Method mHasAny; // hasAnyActiveOxygenField(BlockPos, OxygenFuelGate)
    private static Method mHasCO2; // hasCO2Coverage(BlockPos, OxygenFuelGate)
    private static Method mHasO2; // hasOxygenCoverage(BlockPos, OxygenFuelGate)

    private static Method mHasAnyDim; // hasAnyActiveOxygenField(ResourceKey, BlockPos, OxygenFuelGate)
    private static Method mHasCO2Dim; // hasCO2Coverage(ResourceKey, BlockPos, OxygenFuelGate)
    private static Method mHasO2Dim; // hasOxygenCoverage(ResourceKey, BlockPos, OxygenFuelGate)

    // ── initialisation ────────────────────────────────────────────────────────

    private static void init() {
        if (initialized) return;
        synchronized (WarpDriveBridge.class) {
            if (initialized) return;
            try {
                warpDrivePresent = ModList.get().isLoaded("warpdrive");
                if (!warpDrivePresent) return;

                Class<?> regClass = Class.forName("com.warpdrive.field.FieldProviderRegistry");
                Class<?> tankClass = Class.forName("com.warpdrive.oxygen.InternalTankManager");
                // Inner interface: FieldProviderRegistry$OxygenFuelGate
                Class<?> gateIface = Class.forName(
                        "com.warpdrive.field.FieldProviderRegistry$OxygenFuelGate");

                registry = regClass.getMethod("get").invoke(null);
                Object tank = tankClass.getMethod("get").invoke(null);
                Method hasFuel = tankClass.getMethod("hasFuel", String.class);

                // Build an OxygenFuelGate proxy backed by InternalTankManager#hasFuel.
                gate = Proxy.newProxyInstance(
                        WarpDriveBridge.class.getClassLoader(),
                        new Class<?>[] { gateIface },
                        (proxy, method, args) -> hasFuel.invoke(tank, args));

                try {
                    mHasAnyDim = regClass.getMethod("hasAnyActiveOxygenField", ResourceKey.class, BlockPos.class, gateIface);
                    mHasCO2Dim = regClass.getMethod("hasCO2Coverage", ResourceKey.class, BlockPos.class, gateIface);
                    mHasO2Dim = regClass.getMethod("hasOxygenCoverage", ResourceKey.class, BlockPos.class, gateIface);
                } catch (Throwable ignored) {}

                mHasAny = regClass.getMethod("hasAnyActiveOxygenField", BlockPos.class, gateIface);
                mHasCO2 = regClass.getMethod("hasCO2Coverage", BlockPos.class, gateIface);
                mHasO2 = regClass.getMethod("hasOxygenCoverage", BlockPos.class, gateIface);

            } catch (Throwable t) {
                // WarpDrive missing or API changed — treat every position as vacuum.
                warpDrivePresent = false;
            } finally {
                initialized = true;
            }
        }
    }

    // ── public API ────────────────────────────────────────────────────────────

    public static boolean hasAnyActiveOxygenField(ResourceKey<Level> dimKey, BlockPos pos) {
        init();
        if (!warpDrivePresent) return false;
        if (mHasAnyDim != null && dimKey != null) {
            try { return (Boolean) mHasAnyDim.invoke(registry, dimKey, pos, gate); }
            catch (Throwable ignored) {}
        }
        return hasAnyActiveOxygenField(pos);
    }

    public static boolean hasAnyActiveOxygenField(Level level, BlockPos pos) {
        return hasAnyActiveOxygenField(level != null ? level.dimension() : null, pos);
    }

    public static boolean hasAnyActiveOxygenField(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level l) return hasAnyActiveOxygenField(l.dimension(), pos);
        return hasAnyActiveOxygenField((ResourceKey<Level>) null, pos);
    }

    public static boolean hasAnyActiveOxygenField(BlockPos pos) {
        init();
        if (!warpDrivePresent || mHasAny == null) return false;
        try { return (Boolean) mHasAny.invoke(registry, pos, gate); }
        catch (Throwable t) { return false; }
    }

    public static boolean hasCO2Coverage(ResourceKey<Level> dimKey, BlockPos pos) {
        init();
        if (!warpDrivePresent) return false;
        if (mHasCO2Dim != null && dimKey != null) {
            try { return (Boolean) mHasCO2Dim.invoke(registry, dimKey, pos, gate); }
            catch (Throwable ignored) {}
        }
        return hasCO2Coverage(pos);
    }

    public static boolean hasCO2Coverage(Level level, BlockPos pos) {
        return hasCO2Coverage(level != null ? level.dimension() : null, pos);
    }

    public static boolean hasCO2Coverage(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level l) return hasCO2Coverage(l.dimension(), pos);
        return hasCO2Coverage((ResourceKey<Level>) null, pos);
    }

    public static boolean hasCO2Coverage(BlockPos pos) {
        init();
        if (!warpDrivePresent || mHasCO2 == null) return false;
        try { return (Boolean) mHasCO2.invoke(registry, pos, gate); }
        catch (Throwable t) { return false; }
    }

    public static boolean hasOxygenCoverage(ResourceKey<Level> dimKey, BlockPos pos) {
        init();
        if (!warpDrivePresent) return false;
        if (mHasO2Dim != null && dimKey != null) {
            try { return (Boolean) mHasO2Dim.invoke(registry, dimKey, pos, gate); }
            catch (Throwable ignored) {}
        }
        return hasOxygenCoverage(pos);
    }

    public static boolean hasOxygenCoverage(Level level, BlockPos pos) {
        return hasOxygenCoverage(level != null ? level.dimension() : null, pos);
    }

    public static boolean hasOxygenCoverage(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level l) return hasOxygenCoverage(l.dimension(), pos);
        return hasOxygenCoverage((ResourceKey<Level>) null, pos);
    }

    public static boolean hasOxygenCoverage(BlockPos pos) {
        init();
        if (!warpDrivePresent || mHasO2 == null) return false;
        try { return (Boolean) mHasO2.invoke(registry, pos, gate); }
        catch (Throwable t) { return false; }
    }
}
