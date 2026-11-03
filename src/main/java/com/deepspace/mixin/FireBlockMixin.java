package com.deepspace.mixin;

import com.deepspace.compat.WarpDriveBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles fire behaviour inside WarpDrive gas zones.
 *
 * WarpDrive's API is accessed exclusively via {@link WarpDriveBridge} (reflection),
 * so there is no compile-time dependency on the WarpDrive jar.
 *
 * ── Why targets = "..." instead of value = FireBlock.class ─────────────────
 * The SRG mapping file only contains a randomTick entry under the DECLARING
 * class (Block), not under FireBlock (the override). When @Mixin uses
 * value = FireBlock.class, the AP tries to validate the method by looking up
 * "FireBlock/randomTick" in SRG — finds nothing — and warns "Cannot find
 * target method" even though the method exists at runtime.
 *
 * Switching to targets = "net.minecraft.world.level.block.FireBlock" (string)
 * causes the AP to skip compile-time class validation entirely. Mixin then
 * resolves the target at runtime where Forge 1.20.1 uses Mojang's official
 * names, so "randomTick" IS the correct runtime method name and the inject
 * applies correctly with zero warnings.
 *
 * ── Feature 2: Fire survives in any gas zone ───────────────────────────────
 * In deep_space (vacuum), Ad Astra extinguishes fire via its own FireBlock
 * mixin and via EnvironmentTickEvent. WarpDrive's AdAstraOxygenHooks already
 * cancels the environment tick for any gas zone (O2, air, CO2). This mixin
 * is a belt-and-suspenders second layer: if Ad Astra reaches randomTick via
 * any other path, we cancel the call and re-schedule so fire keeps ticking
 * through the scheduler instead.
 *
 * Priority 2000 (> Ad Astra's 1000) ensures our HEAD inject runs first.
 *
 * ── Feature 3: Fire spreads 3x in pure-O2 zone ───────────────────────────
 * After the vanilla fire tick in a pure-O2 zone (O2 present, no CO2), we
 * call randomTick 2 more times for 3 total spread events per cycle.
 * EXTRA_O2_FIRE_TICK guards against recursion on those bonus calls.
 */
@Mixin(value = FireBlock.class, priority = 2000)
public class FireBlockMixin {

    /**
     * Guard set during bonus fire-spread ticks (pure-O2 3x feature).
     * Prevents the RETURN inject from firing recursively on bonus calls.
     */
    private static final ThreadLocal<Boolean> EXTRA_O2_FIRE_TICK =
            ThreadLocal.withInitial(() -> false);

    // ── Feature 2: Prevent vacuum extinguishment in gas zones ───────────────

    /**
     * HIGH-priority HEAD inject -- runs before Ad Astra's vacuum suppressor.
     *
     * If any gas (O2, air, or CO2) covers this position:
     *   - Cancel so Ad Astra's removeBlock call never fires.
     *   - Re-schedule via scheduleTick so fire keeps ageing through the
     *     block scheduler, completely bypassing Ad Astra's intercept path.
     *
     * True vacuum (no gas) -> return without cancelling; Ad Astra suppresses
     * the fire normally.
     */
    @Inject(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/server/level/ServerLevel;"
               + "Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/util/RandomSource;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void deepspace$preserveFireInGasZone(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {

        // Inside a bonus tick -- already approved, skip gate.
        if (EXTRA_O2_FIRE_TICK.get()) return;

        // True vacuum -- let Ad Astra extinguish normally.
        if (!WarpDriveBridge.hasAnyActiveOxygenField(level, pos)) return;

        // Gas present: cancel and re-schedule so fire keeps ticking.
        ci.cancel();
        level.scheduleTick(pos, Blocks.FIRE, 1 + random.nextInt(5));
    }

    // ── Feature 3: Pure-O2 fire spreads 3x ───────────────────────────────────

    /**
     * RETURN inject: after the vanilla fire tick in a pure-O2 zone, fire
     * 2 bonus randomTick calls giving 3 total spread events per cycle.
     *
     * Conditions:
     *   - Not already inside a bonus tick (EXTRA_O2_FIRE_TICK guard).
     *   - Fire block still exists at pos (vanilla didn't extinguish it).
     *   - Pure-O2 zone: hasOxygen=true AND hasCarbonDioxide=false.
     */
    @Inject(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;"
               + "Lnet/minecraft/server/level/ServerLevel;"
               + "Lnet/minecraft/core/BlockPos;"
               + "Lnet/minecraft/util/RandomSource;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void deepspace$pureO2FireSpread(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {

        if (EXTRA_O2_FIRE_TICK.get()) return;
        if (!level.getBlockState(pos).is(Blocks.FIRE)) return;

        boolean hasO2  = WarpDriveBridge.hasOxygenCoverage(level, pos);
        boolean hasCO2 = WarpDriveBridge.hasCO2Coverage(level, pos);
        if (!hasO2 || hasCO2) return;

        EXTRA_O2_FIRE_TICK.set(true);
        try {
            if (level.getBlockState(pos).is(Blocks.FIRE)) {
                ((FireBlock) (Object) this).randomTick(
                        level.getBlockState(pos), level, pos, random);
            }
            if (level.getBlockState(pos).is(Blocks.FIRE)) {
                ((FireBlock) (Object) this).randomTick(
                        level.getBlockState(pos), level, pos, random);
            }
        } finally {
            EXTRA_O2_FIRE_TICK.set(false);
        }
    }
}
