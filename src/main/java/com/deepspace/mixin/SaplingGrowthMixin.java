package com.deepspace.mixin;

import com.deepspace.compat.WarpDriveBridge;
import com.deepspace.compat.WarpDriveCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts {@code SaplingBlock.randomTick()} to:
 *  1. Block tree growth in oxygen-only (no CO2) zones.
 *  2. Boost tree growth 3x in CO2 zones.
 *
 * WarpDrive's API is accessed exclusively via {@link WarpDriveBridge} (reflection),
 * so there is no compile-time dependency on the WarpDrive jar.
 *
 * ── Why a Mixin? ───────────────────────────────────────────────────────────
 * Forge's {@code BlockEvent.CropGrowEvent} is NOT fired by the autonomous
 * random-tick path in {@code SaplingBlock.randomTick()}.  Bonemeal on a
 * sapling IS caught by {@link WarpDriveCompat#onCropGrowPre} (through the
 * BonemealableBlock event chain); only the random-tick path needs this mixin.
 *
 * ── CO2 3x growth ──────────────────────────────────────────────────────────
 * After the natural tick succeeds in a CO2 zone, we call {@code randomTick}
 * 2 more times (3 total), guarded by {@link WarpDriveCompat#EXTRA_CO2_GROWTH_TICK}
 * to prevent recursion.  On the bonus calls the guard is set, so no further
 * bonus ticks are scheduled.
 *
 * ── O2-only growth block ───────────────────────────────────────────────────
 * If inside an active field with no CO2, cancel at HEAD before any growth
 * probability check runs.  Outside all zones, do not interfere.
 */
@Mixin(SaplingBlock.class)
public abstract class SaplingGrowthMixin {

    /**
     * Step 1: Block growth / Step 2: Allow + schedule bonus ticks.
     *
     * Runs at HEAD of randomTick.
     *  - Bonus-tick guard set  -> skip (already inside an extra tick).
     *  - Outside all active zones -> skip (don't interfere with vanilla).
     *  - CO2 present -> allow growth (return without cancelling).
     *  - O2-only (no CO2) -> cancel this tick entirely.
     */
    @Inject(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/server/level/ServerLevel;"
               + "Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/util/RandomSource;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void deepspace$checkCO2ForGrowth(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {

        // Already inside a bonus tick -- let it run normally (no recursion).
        if (WarpDriveCompat.EXTRA_CO2_GROWTH_TICK.get()) return;

        // Outside all active fields -> do not interfere.
        if (!WarpDriveBridge.hasAnyActiveOxygenField(level, pos)) return;

        // CO2 present -> photosynthesis possible, allow growth.
        if (WarpDriveBridge.hasCO2Coverage(level, pos)) return;

        // Inside an O2-only field with no CO2 -> block this tick.
        ci.cancel();
    }

    /**
     * After the natural tick completes in a CO2 zone, fire 2 bonus
     * {@code randomTick} calls for a total of 3 (3x growth rate).
     */
    @Inject(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/server/level/ServerLevel;"
               + "Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/util/RandomSource;)V",
        at = @At("RETURN")
    )
    private void deepspace$co2GrowthBoost(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {

        // Skip if we are already inside a bonus tick.
        if (WarpDriveCompat.EXTRA_CO2_GROWTH_TICK.get()) return;

        // Only boost in CO2 zones.
        if (!WarpDriveBridge.hasCO2Coverage(level, pos)) return;

        // The sapling might have already grown into a tree during this tick.
        WarpDriveCompat.EXTRA_CO2_GROWTH_TICK.set(true);
        try {
            // Bonus tick 1
            BlockState s1 = level.getBlockState(pos);
            if (s1.getBlock() instanceof SaplingBlock) {
                s1.randomTick(level, pos, random);
            }
            // Bonus tick 2
            BlockState s2 = level.getBlockState(pos);
            if (s2.getBlock() instanceof SaplingBlock) {
                s2.randomTick(level, pos, random);
            }
        } finally {
            WarpDriveCompat.EXTRA_CO2_GROWTH_TICK.set(false);
        }
    }
}
