package com.deepspace.crash;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.compat.FTBChunksCompat;
import com.deepspace.util.PersonalSeedHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Allocates a personal crash site for a new player.
 *
 * Algorithm:
 *   Spiral outward from a centre chunk in steps of 128 chunks.
 *   For each candidate:
 *     - Reject if too close to another crash site (< 120 chunks)
 *     - Reject if any chunk in the bubble is FTBChunks-claimed
 *     - Reject if reserved by a pending allocation
 *   Accept first passing candidate.
 *
 * This allocator is a reusable crash-site allocation utility; callers must invoke it explicitly on the server thread.
 */
public final class CrashBubbleAllocator {

    /** Maximum spiral search radius in chunks before giving up. */
    private static final int MAX_SPIRAL_RADIUS = 2000;
    /** Spiral step size in chunks. */
    private static final int SPIRAL_STEP = 128;

    private CrashBubbleAllocator() {}

    /**
     * Find or create the crash bubble for this player.
     * Returns existing data immediately if the player has already spawned.
     *
     * Spiral search is centred on chunk [0,0] (world origin). Kept for
     * backward compatibility / Deep Space callers.
     *
     * @param level   The ServerLevel to allocate in (e.g. deepspace:deep_space)
     * @param player  The joining player
     * @return        The player's CrashBubbleData (newly allocated or retrieved)
     */
    @Nullable
    public static CrashBubbleData getOrAllocate(ServerLevel level, ServerPlayer player) {
        return getOrAllocate(level, player, 0, 0);
    }

    /**
     * Find or create the crash bubble for this player, spiralling outward
     * from an explicit centre chunk instead of world origin.
     *
     * Used by the Overworld first-spawn flow so a player's ship lands at (or
     * very near) the world's actual configured spawn point, rather than at
     * raw chunk [0,0] which may be some distance from the biome-searched
     * vanilla spawn position.
     *
     * @param level      The ServerLevel to allocate in
     * @param player     The joining player
     * @param centreCx   Chunk X to spiral outward from
     * @param centreCz   Chunk Z to spiral outward from
     * @return           The player's CrashBubbleData (newly allocated or retrieved)
     */
    @Nullable
    public static CrashBubbleData getOrAllocate(ServerLevel level, ServerPlayer player,
                                                 int centreCx, int centreCz) {
        DSWorldData worldData = DSWorldData.getOrCreate(level);
        UUID uuid = player.getUUID();

        // Fast path: already has a crash site
        if (worldData.hasBubble(uuid)) {
            return worldData.getBubble(uuid);
        }

        // Allocate a new one
        long worldSeed = level.getSeed();
        long personalSeed = PersonalSeedHelper.derive(worldSeed, uuid);

        // Spiral search for a safe position, offset from the given centre
        int cx = centreCx, cz = centreCz;
        boolean found = false;

        outer:
        for (int radius = 0; radius <= MAX_SPIRAL_RADIUS; radius += SPIRAL_STEP) {
            for (int[] candidate : spiralPoints(radius)) {
                int candidateCx = centreCx + candidate[0];
                int candidateCz = centreCz + candidate[1];

                // Safety check 1: not too close to another crash site
                if (worldData.isTooCloseToExistingBubble(candidateCx, candidateCz)) continue;

                // Safety check 2: bubble region not FTBChunks-claimed
                if (isBubbleRegionClaimed(level, candidateCx, candidateCz)) continue;

                cx = candidateCx;
                cz = candidateCz;
                found = true;
                break outer;
            }
        }

        if (!found) {
            DeepSpaceMod.LOGGER.error(
                "[CrashBubbleAllocator] Could not find safe spawn for player {} after {}+ chunk search",
                player.getName().getString(), MAX_SPIRAL_RADIUS);
            return null;
        }

        // Reserve the position immediately to prevent races with other concurrent joins
        worldData.reserveChunkCentre(cx, cz);

        // Ship origin: centre of the bubble asteroid, at Y=64 (placeholder --
        // CrashedShipPlacer overwrites this with the real placement Y).
        int shipOriginX = cx * 16 + 8;
        int shipOriginY = 64;
        int shipOriginZ = cz * 16 + 8;

        CrashBubbleData bubble = new CrashBubbleData(
            uuid, cx, cz, shipOriginX, shipOriginY, shipOriginZ, personalSeed);

        // Persist
        worldData.addBubble(bubble);
        worldData.releaseReservation(cx, cz);

        DeepSpaceMod.LOGGER.info(
            "[CrashBubbleAllocator] Allocated crash site for {} at chunk [{},{}] (seed={})",
            player.getName().getString(), cx, cz, personalSeed);

        return bubble;
    }

    /**
     * Check whether any chunk in the bubble region is claimed by FTBChunks.
     *
     * Two-pass check:
     *   Pass 1 -- Ship immediate footprint (5x5 chunks around centre):
     *     Checked at every-chunk resolution -- the ship structure writes blocks
     *     here and must never touch a claimed chunk.
     *   Pass 2 -- Wider bubble radius (for allocation breathing room):
     *     Sampled every 2 chunks for performance. Prevents allocating a crash
     *     site that immediately borders claimed land.
     */
    private static boolean isBubbleRegionClaimed(ServerLevel level, int centreCx, int centreCz) {
        // Pass 1: dense check of ship footprint area -- every chunk, no gaps
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (FTBChunksCompat.isChunkClaimed(level, centreCx + dx, centreCz + dz)) {
                    return true;
                }
            }
        }
        // Pass 2: wider bubble, every 2 chunks
        int r = DSConstants.BUBBLE_RADIUS_CHUNKS;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                if (FTBChunksCompat.isChunkClaimed(level, centreCx + dx, centreCz + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Enumerate candidate positions at a given spiral radius.
     * At radius 0, returns just (0,0).
     */
    private static int[][] spiralPoints(int radius) {
        if (radius == 0) return new int[][]{{0, 0}};

        // Walk the perimeter of a square ring at this radius
        int size = radius * 2 + 1;
        int count = size * 4 - 4;
        int[][] points = new int[count][2];
        int idx = 0;

        // Top row
        for (int x = -radius; x <= radius; x++) points[idx++] = new int[]{x, -radius};
        // Right col (skip corners)
        for (int z = -radius + 1; z <= radius; z++) points[idx++] = new int[]{radius, z};
        // Bottom row (skip corners)
        for (int x = radius - 1; x >= -radius; x--) points[idx++] = new int[]{x, radius};
        // Left col (skip top/bottom corners)
        for (int z = radius - 1; z > -radius; z--) points[idx++] = new int[]{-radius, z};

        return points;
    }
}
