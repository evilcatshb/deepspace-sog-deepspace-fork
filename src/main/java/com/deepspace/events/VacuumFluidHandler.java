package com.deepspace.events;

import com.deepspace.DSConstants;
import com.deepspace.init.DSDimensions;
import earth.terrarium.adastra.api.systems.OxygenApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Oxygen-aware water / ice conversion in the Deep Space dimension.
 *
 * <ul>
 *   <li>Water in vacuum (no Ad Astra oxygen) freezes to ice after
 *       {@link DSConstants#VACUUM_FREEZE_TICKS} ticks.</li>
 *   <li>Ice in a sealed, oxygenated room melts back to a water source.</li>
 *   <li>Allows functional farms inside pressurised rooms.</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li><b>Fluid-place hook</b> -- when water appears (bucket, dispenser,
 *       flow) it is queued for a delayed freeze check.  At expiry the oxygen
 *       state is tested; if oxygen is present the freeze is cancelled.</li>
 *   <li><b>Random probe</b> -- every {@value #SCAN_INTERVAL_TICKS} ticks a
 *       batch of random positions near each player is sampled.  If an
 *       incorrect state is found (ice+oxygen or water+vacuum) it is added to
 *       a BFS conversion queue that spreads to neighbours, giving a fast
 *       visual "wave".</li>
 *   <li><b>BFS spread</b> -- up to {@value #MAX_CONVERSIONS_PER_TICK} blocks
 *       are converted each tick.  Each converted block enqueues its six
 *       neighbours for the same check, so a room-sized area converts in
 *       under a second once the first block is discovered.</li>
 * </ol>
 *
 * <p>Packed ice and blue ice are intentionally excluded -- they are crafted
 * blocks and should not participate in atmospheric conversion.</p>
 */
public class VacuumFluidHandler {

    // -- Tuning constants ----------------------------------------------------

    /** Ticks between random-probe scans. */
    private static final int SCAN_INTERVAL_TICKS = 10;

    /** Random positions sampled per player per scan. */
    private static final int PROBE_SAMPLES_PER_PLAYER = 80;

    /** Horizontal radius (blocks) around each player to probe. */
    private static final int PROBE_RADIUS_XZ = 48;

    /** Vertical radius (blocks) around each player to probe. */
    private static final int PROBE_RADIUS_Y = 24;

    /** Maximum block conversions (melt + freeze combined) per tick via BFS. */
    private static final int MAX_CONVERSIONS_PER_TICK = 64;

    // -- Queues --------------------------------------------------------------

    /** Newly-placed water blocks waiting for their freeze delay to expire. */
    private record PendingFreeze(BlockPos pos, long expiryTick) {}
    private final Queue<PendingFreeze> pendingFreezes = new ConcurrentLinkedQueue<>();

    /** Positions discovered by probe or BFS that need oxygen-based conversion. */
    private final Queue<BlockPos> spreadQueue = new ArrayDeque<>();

    /** Guards against re-enqueuing the same position during a single BFS wave. */
    private final Set<BlockPos> spreadVisited = new HashSet<>();

    // -- Event handlers ------------------------------------------------------

    /**
     * Intercepts water appearing via fluid-flow, bucket, or dispenser.
     * Queues a delayed freeze check rather than freezing immediately so the
     * oxygen system has time to update after a distributor is placed.
     */
    @SubscribeEvent
    public void onFluidPlaced(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(DSDimensions.DEEP_SPACE)) return;

        var fluid = event.getNewState().getFluidState().getType();
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            pendingFreezes.add(new PendingFreeze(
                event.getPos().immutable(),
                level.getGameTime() + DSConstants.VACUUM_FREEZE_TICKS));
        }
    }

    /**
     * Main tick driver.  Processes pending freezes, runs the periodic random
     * probe, and drains the BFS spread queue.
     */
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().equals(DSDimensions.DEEP_SPACE)) return;

        long now = level.getGameTime();

        // -- 1. Process delayed freezes (with oxygen check) ------------------
        pendingFreezes.removeIf(pf -> {
            if (now < pf.expiryTick()) return false;
            BlockPos pos = pf.pos();
            var fluid = level.getBlockState(pos).getFluidState().getType();
            if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
                if (!hasOxygen(level, pos)) {
                    level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                }
            }
            return true;
        });

        // -- 2. Periodic random probe ----------------------------------------
        if (now % SCAN_INTERVAL_TICKS == 0) {
            probeRandomBlocks(level);
        }

        // -- 3. Drain BFS spread queue ---------------------------------------
        processSpreadQueue(level);
    }

    // -- Random probe --------------------------------------------------------

    /**
     * Samples random positions near each online player and seeds the BFS
     * spread queue when an incorrect ice/water state is found.
     */
    private void probeRandomBlocks(ServerLevel level) {
        var players = level.players();
        if (players.isEmpty()) return;

        var rng = ThreadLocalRandom.current();

        for (var player : players) {
            int px = player.getBlockX();
            int py = player.getBlockY();
            int pz = player.getBlockZ();

            for (int i = 0; i < PROBE_SAMPLES_PER_PLAYER; i++) {
                int x = px + rng.nextInt(-PROBE_RADIUS_XZ, PROBE_RADIUS_XZ + 1);
                int y = py + rng.nextInt(-PROBE_RADIUS_Y,  PROBE_RADIUS_Y  + 1);
                int z = pz + rng.nextInt(-PROBE_RADIUS_XZ, PROBE_RADIUS_XZ + 1);

                BlockPos pos = new BlockPos(x, y, z);
                if (!level.isLoaded(pos)) continue;

                if (needsConversion(level, pos) && spreadVisited.add(pos.immutable())) {
                    spreadQueue.add(pos.immutable());
                }
            }
        }
    }

    // -- BFS spread ----------------------------------------------------------

    /**
     * Drains up to {@link #MAX_CONVERSIONS_PER_TICK} entries from the spread
     * queue.  Each converted block fans out to its six neighbours, so an
     * entire room converts in a rapid wave once the first block is discovered.
     */
    private void processSpreadQueue(ServerLevel level) {
        int converted = 0;

        while (!spreadQueue.isEmpty() && converted < MAX_CONVERSIONS_PER_TICK) {
            BlockPos pos = spreadQueue.poll();

            if (!level.isLoaded(pos)) continue;

            BlockState state = level.getBlockState(pos);
            boolean oxygen = hasOxygen(level, pos);

            if (isIce(state) && oxygen) {
                // Melt: ice -> water (room is oxygenated)
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                converted++;
                enqueueNeighbours(level, pos);

            } else if (isWater(state) && !oxygen) {
                // Freeze: water -> ice (vacuum, no oxygen)
                level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                converted++;
                enqueueNeighbours(level, pos);
            }
            // else: state already correct or block changed -- skip
        }

        // If the queue is fully drained, reset the visited set so future
        // probes can re-discover blocks whose oxygen state has changed.
        if (spreadQueue.isEmpty()) {
            spreadVisited.clear();
        }
    }

    /**
     * Enqueues the six cardinal neighbours of {@code centre} into the BFS
     * spread queue if they haven't been visited yet and need conversion.
     */
    private void enqueueNeighbours(ServerLevel level, BlockPos centre) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = centre.relative(dir);
            if (!level.isLoaded(neighbour)) continue;
            if (!spreadVisited.add(neighbour.immutable())) continue;

            if (needsConversion(level, neighbour)) {
                spreadQueue.add(neighbour.immutable());
            }
        }
    }

    // -- Helpers -------------------------------------------------------------

    /**
     * Returns {@code true} if the block at {@code pos} is in the wrong state
     * relative to its oxygen level (ice-with-oxygen or water-without-oxygen).
     */
    private static boolean needsConversion(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean oxygen = hasOxygen(level, pos);

        if (isIce(state)   && oxygen)  return true;   // should be water
        if (isWater(state) && !oxygen) return true;   // should be ice
        return false;
    }

    /**
     * Tests for regular ice or frosted ice.
     * Packed ice and blue ice are crafted blocks and intentionally excluded.
     */
    private static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
    }

    /** Tests for any water (source or flowing). */
    private static boolean isWater(BlockState state) {
        var fluid = state.getFluidState().getType();
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }

    /**
     * Queries the Ad Astra oxygen system for the given position.
     * Returns {@code false} on any error (e.g. Ad Astra not loaded).
     */
    private static boolean hasOxygen(ServerLevel level, BlockPos pos) {
        try {
            return OxygenApi.API.hasOxygen(level, pos);
        } catch (Exception e) {
            return false;
        }
    }
}
