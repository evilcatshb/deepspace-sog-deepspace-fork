package com.deepspace.compat;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * DeepSpace integration with WarpDrive's Large Oxygen Distributor.
 *
 * All WarpDrive API calls are made via {@link WarpDriveBridge} (reflection),
 * so there is no compile-time dependency on the WarpDrive jar.  DeepSpace and
 * WarpDrive can be loaded independently; when WarpDrive is absent every bridge
 * call returns false and these features are silently disabled.
 *
 * ── Feature 1: Plant growth gating + CO2 3x growth boost ───────────────────
 * Plants, saplings, and trees require CO2 for photosynthesis:
 *   - Pure oxygen (circuit-0): no CO2  -> plants DO NOT grow.
 *   - Atmospheric air (circuit-1): CO2 present -> plants grow normally.
 *   - Pure CO2 (circuit-2): CO2 rich -> plants grow 3x FASTER.
 *
 * "3x faster" is implemented by triggering 2 additional {@code randomTick}
 * calls after every natural growth event (CropGrowEvent.Post) inside a CO2
 * zone.  Combined with the 1 natural tick = 3 total per game-tick cycle.
 * A ThreadLocal guard prevents infinite recursion between the extra ticks.
 *
 * Sapling random-tick growth (not covered by CropGrowEvent) is handled by
 * {@link com.deepspace.mixin.SaplingGrowthMixin}.
 *
 * ── Feature 2: Fire in gas zones (any gas) ─────────────────────────────────
 * Fire extinguishment in vacuum is handled in WarpDrive's AdAstraOxygenHooks
 * (any-gas EnvironmentTickEvent) and FireBlockMixin.  No additional event
 * handler here.
 *
 * ── Feature 3: Pure-oxygen fire damage 1.5x ────────────────────────────────
 * When an entity takes fire damage inside a pure-O2 zone (hasOxygen=true,
 * hasCarbonDioxide=false), the damage amount is multiplied by 1.5x.
 */
public final class WarpDriveCompat {

    private static boolean registered = false;

    /**
     * Guard against recursive extra-tick calls in onCropGrowPost.
     * Set to true while we are injecting bonus ticks so that the Post handler
     * on the bonus ticks is a no-op (preventing infinite recursion).
     * Also read by {@link com.deepspace.mixin.SaplingGrowthMixin}.
     */
    public static final ThreadLocal<Boolean> EXTRA_CO2_GROWTH_TICK =
            ThreadLocal.withInitial(() -> false);

    private WarpDriveCompat() {}

    /**
     * Registers all Forge event bus listeners.
     * Must be called during {@code FMLCommonSetupEvent.enqueueWork}.
     * No-op if called more than once.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(WarpDriveCompat.class);
        DeepSpaceMod.LOGGER.info(
            "[DeepSpace/WarpDriveCompat] Plant-growth CO2 gate, CO2 3x boost, "
          + "and pure-O2 fire damage 1.5x registered.");
    }

    // ── Feature 1a: Deny growth in O2-only zone (no CO2) ─────────────────────

    /**
     * Cancels crop/plant growth when inside a WarpDrive oxygen field that
     * has NO carbon dioxide (pure O2 mode).
     *
     * Fires for: CropBlock, StemBlock, BambooSaplingBlock, BambooStalkBlock,
     * and any BonemealableBlock (including SaplingBlock via bonemeal).
     */
    @SubscribeEvent
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (event.getLevel().isClientSide()) return;
        BlockPos pos = event.getPos();

        // Only intervene inside an active field zone.
        if (!WarpDriveBridge.hasAnyActiveOxygenField(event.getLevel(), pos)) return;

        // If CO2 is present, photosynthesis is possible -> allow.
        if (WarpDriveBridge.hasCO2Coverage(event.getLevel(), pos)) return;

        // Inside a field with O2 only, no CO2 -> deny.
        event.setResult(Event.Result.DENY);
    }

    // ── Feature 1b: CO2-zone 3x growth boost ──────────────────────────────────

    /**
     * After a successful crop growth in a CO2 zone, fires 2 extra
     * {@code randomTick} calls on the same block -- making total ticks per
     * natural random-tick cycle = 3 (i.e. 3x growth rate).
     *
     * Guard: {@link #EXTRA_CO2_GROWTH_TICK} is set during the bonus ticks so
     * this handler is a no-op on the recursive calls, preventing runaway growth.
     */
    @SubscribeEvent
    public static void onCropGrowPost(BlockEvent.CropGrowEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        if (EXTRA_CO2_GROWTH_TICK.get()) return; // we are already in a bonus tick

        BlockPos pos = event.getPos();

        // Only boost inside a CO2-containing zone.
        if (!WarpDriveBridge.hasCO2Coverage(event.getLevel(), pos)) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        RandomSource random = level.getRandom();

        EXTRA_CO2_GROWTH_TICK.set(true);
        try {
            // Bonus tick 1
            BlockState state1 = level.getBlockState(pos);
            if (!state1.isAir()) {
                state1.randomTick(level, pos, random);
            }
            // Bonus tick 2
            BlockState state2 = level.getBlockState(pos);
            if (!state2.isAir()) {
                state2.randomTick(level, pos, random);
            }
        } finally {
            EXTRA_CO2_GROWTH_TICK.set(false);
        }
    }

    // ── Feature 3: Pure-O2 fire damage 1.5x ──────────────────────────────────

    /**
     * Multiplies fire damage by 1.5 when the entity is inside a pure-oxygen
     * zone (hasOxygen=true, hasCarbonDioxide=false).
     *
     * Oxygen-enriched atmospheres burn hotter -- fire damage arrives faster.
     * Applied only to server-side living entities to avoid double-application.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;

        BlockPos pos = event.getEntity().blockPosition();

        // Pure-oxygen zone: hasOxygen=true AND hasCarbonDioxide=false.
        boolean hasO2  = WarpDriveBridge.hasOxygenCoverage(event.getEntity().level(), pos);
        boolean hasCO2 = WarpDriveBridge.hasCO2Coverage(event.getEntity().level(), pos);

        if (hasO2 && !hasCO2) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }
}
