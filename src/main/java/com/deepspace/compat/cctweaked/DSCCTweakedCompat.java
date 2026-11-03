package com.deepspace.compat.cctweaked;

import com.deepspace.DeepSpaceMod;
import com.deepspace.blocks.BreederReactorBlockEntity;
import com.deepspace.blocks.ShipScannerBlockEntity;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

/**
 * CC:Tweaked integration for Deep Space.
 *
 * Registers:
 *   - {@code "ship_scanner"}    wrapping ShipScannerBlockEntity
 *   - {@code "breeder_reactor"} wrapping BreederReactorBlockEntity
 *
 * <p>Lua usage:
 * <pre>
 *   local scanner = peripheral.find("ship_scanner")
 *   local result  = scanner.getStructuresInRadius(2500)
 *
 *   local reactor = peripheral.find("breeder_reactor")
 *   reactor.setOutputPercent(50)   -- run at half power, fuel lasts 2x longer
 *   reactor.scram()                -- emergency stop, eject queued fuel
 * </pre>
 *
 * Called from {@link DeepSpaceMod#commonSetup} when CC:Tweaked is detected.
 */
public class DSCCTweakedCompat {

    public static void init() {
        try {
            ForgeComputerCraftAPI.registerPeripheralProvider(new ShipScannerProvider());
            DeepSpaceMod.LOGGER.info("[DeepSpace:CC] Peripheral 'ship_scanner' registered.");

            ForgeComputerCraftAPI.registerPeripheralProvider(new BreederReactorProvider());
            DeepSpaceMod.LOGGER.info("[DeepSpace:CC] Peripheral 'breeder_reactor' registered.");
        } catch (Throwable t) {
            DeepSpaceMod.LOGGER.error("[DeepSpace:CC] Failed to register peripheral: {}", t.getMessage());
        }
    }

    /** Peripheral provider -- wraps ShipScannerBlockEntity in ShipScannerPeripheral. */
    private static class ShipScannerProvider implements IPeripheralProvider {
        @Nonnull
        @Override
        public LazyOptional<IPeripheral> getPeripheral(
                @Nonnull Level world,
                @Nonnull BlockPos pos,
                @Nonnull Direction side) {
            if (world.getBlockEntity(pos) instanceof ShipScannerBlockEntity be) {
                return LazyOptional.of(() -> new ShipScannerPeripheral(be));
            }
            return LazyOptional.empty();
        }
    }

    /** Peripheral provider -- wraps BreederReactorBlockEntity in BreederReactorPeripheral. */
    private static class BreederReactorProvider implements IPeripheralProvider {
        @Nonnull
        @Override
        public LazyOptional<IPeripheral> getPeripheral(
                @Nonnull Level world,
                @Nonnull BlockPos pos,
                @Nonnull Direction side) {
            if (world.getBlockEntity(pos) instanceof BreederReactorBlockEntity be) {
                return LazyOptional.of(() -> new BreederReactorPeripheral(be));
            }
            return LazyOptional.empty();
        }
    }
}
