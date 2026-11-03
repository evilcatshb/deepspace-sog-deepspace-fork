package com.deepspace.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Performs a post-placement refresh pass after runtime structure imports.
 *
 * WHY THIS EXISTS:
 * ----------------
 * GTCEu pipes/cables compute visual connections using model data
 * rebuilds triggered by vanilla neighbor updates.
 *
 * StructureTemplate placement with flags=2 bypasses those updates
 * (and also skips the second-pass shape update when flags == 2),
 * leaving client models visually disconnected even though networks work.
 *
 * This class reproduces the necessary updates in two targeted tiers:
 *
 *   1. GT pipe/cable block entities: full updateNeighborsAt() so the
 *      pipe network recomputes connection topology.
 *
 *   2. Everything else: Block.updateFromNeighbourShapes() which updates
 *      shape-dependent visuals (fence/wall/stair connections, redstone
 *      lines, etc.) WITHOUT triggering neighborChanged().
 *
 * The previous implementation called updateNeighborsAt() on EVERY non-air
 * block.  This fired neighborChanged() cascades that could pop off
 * support-dependent blocks (carpet, torches, buttons, rails, etc.) when
 * the cascade disturbed their support -- the root cause of carpets
 * sometimes breaking on /importship.
 */
public final class StructurePlacementFixer {

    private StructurePlacementFixer() {}

    /** Cache for the GT pipe BE superclass; null means "not resolved yet". */
    private static Class<?> gtPipeBeClass;
    private static boolean gtPipeBeResolved = false;

    /**
     * Refresh all NON-AIR blocks inside a placed structure bounding box.
     *
     * MUST run on the server thread.
     *
     * Only non-air blocks are refreshed.  Iterating air blocks in a large
     * structure (e.g. a 12-chunk sphere) would produce millions of calls
     * on the main thread, freezing the server.
     *
     * Chunks are checked for load status before block access to avoid
     * accidentally force-loading unrelated chunks during the refresh pass.
     */
    public static void refreshArea(ServerLevel level, BlockPos min, BlockPos max) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {

                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                if (!level.hasChunk(chunkX, chunkZ)) continue;

                for (int y = min.getY(); y <= max.getY(); y++) {

                    pos.set(x, y, z);

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    BlockEntity be = level.getBlockEntity(pos);

                    if (isGtPipeBe(be)) {
                        // ── GT pipes/cables: full neighbor notification ──
                        // Required for the pipe network to recompute which
                        // faces are visually connected.
                        level.updateNeighborsAt(pos, state.getBlock());
                    } else {
                        // ── All other blocks: safe shape-only update ─────
                        // Updates this block's OWN state from its neighbours
                        // (fence connections, stair shapes, wall posts, etc.)
                        // WITHOUT calling neighborChanged() on adjacent
                        // blocks, so fragile blocks like carpet, torches,
                        // buttons, and rails are never popped off.
                        BlockState updated = Block.updateFromNeighbourShapes(
                                state, level, pos);
                        if (updated != state) {
                            // flags 2 = UPDATE_CLIENTS only, no neighbor
                            // notifications, preventing cascades.
                            level.setBlock(pos, updated, 2);
                        }
                    }

                    // Send client update packet (model refresh) for ALL blocks
                    level.sendBlockUpdated(pos,
                            level.getBlockState(pos),   // re-read: may have
                            level.getBlockState(pos),   // been shape-updated
                            3);

                    // Force BE dirty + sync
                    if (be != null) {
                        be.setChanged();
                        level.blockEntityChanged(pos);
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given block entity is a GTCEu pipe or cable.
     * Uses reflection so the mod compiles without a hard GT dependency.
     */
    private static boolean isGtPipeBe(BlockEntity be) {
        if (be == null) return false;
        if (!gtPipeBeResolved) {
            gtPipeBeResolved = true;
            try {
                gtPipeBeClass = Class.forName(
                        "com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity");
            } catch (ClassNotFoundException ignored) {
                gtPipeBeClass = null;  // GT not present
            }
        }
        return gtPipeBeClass != null && gtPipeBeClass.isInstance(be);
    }
}