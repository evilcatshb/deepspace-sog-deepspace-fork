package com.deepspace.crash;

import com.deepspace.DSConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finds the highest safe flat surface on a starter asteroid for ship placement.
 *
 * FIX (issue #7 -- no air gap between ship floor and asteroid top):
 *   findSurface() now returns the Y of the topmost solid block itself
 *   (not +1 above it). CrashedShipPlacer places the ship with its floor
 *   at exactly that Y, meaning the ship floor sits flush on the asteroid top.
 *
 * FIX (issue #2 -- player spawns inside ship):
 *   findSafeShipInterior() replaces findSafePlayerSpawn(). It scans upward
 *   from shipOriginY+1 (floor level) to find the first two-air-block gap
 *   inside the ship hull rather than searching above the asteroid exterior.
 */
public final class AsteroidSurfaceFinder {

    private static final int SHIP_W = 15;
    private static final int SHIP_D = 15;

    private AsteroidSurfaceFinder() {}

    /**
     * Find the topmost solid block on the asteroid at (centreX, centreZ).
     * Returns the Y of the topmost solid block (not +1).
     * CrashedShipPlacer places the ship floor at this Y so there is zero gap.
     */
    public static BlockPos findSurface(ServerLevel level, int centreX, int centreZ) {
        level.getChunk(centreX >> 4, centreZ >> 4);

        for (int y = DSConstants.ASTEROID_BAND_Y_MAX; y >= DSConstants.ASTEROID_BAND_Y_MIN; y--) {
            BlockPos candidate = new BlockPos(centreX, y, centreZ);
            if (isSolid(level, candidate) && isAir(level, candidate.above())) {
                // Found top surface -- scan for a flat placement spot
                BlockPos flat = findFlatPlacement(level, candidate);
                return flat != null ? flat : candidate;
            }
        }

        // No asteroid found -- return a reasonable default
        return new BlockPos(centreX, 64, centreZ);
    }

    /**
     * Find the best flat SHIP_WxSHIP_D landing zone near surfaceTop.
     * Returns the position of the topmost solid block at the best spot
     * (ship floor will be placed AT this Y).
     */
    private static BlockPos findFlatPlacement(ServerLevel level, BlockPos surfaceTop) {
        int baseX = surfaceTop.getX();
        int baseY = surfaceTop.getY();
        int baseZ = surfaceTop.getZ();

        int bestScore = -1;
        BlockPos bestPos = null;

        for (int dx = -(SHIP_W / 2); dx <= SHIP_W / 2; dx += 3) {
            for (int dz = -(SHIP_D / 2); dz <= SHIP_D / 2; dz += 3) {
                int ox = baseX + dx - SHIP_W / 2;
                int oz = baseZ + dz - SHIP_D / 2;
                int score = scorePlacement(level, ox, baseY, oz);
                if (score > bestScore) {
                    bestScore = score;
                    // Centre of footprint, at the surface Y
                    bestPos = new BlockPos(ox + SHIP_W / 2, baseY, oz + SHIP_D / 2);
                }
            }
        }

        int threshold = (SHIP_W * SHIP_D) * 4 / 10;
        return (bestScore >= threshold) ? bestPos : new BlockPos(baseX, baseY, baseZ);
    }

    private static int scorePlacement(ServerLevel level, int ox, int y, int oz) {
        int solidCount = 0;
        for (int dx = 0; dx < SHIP_W; dx += 2) {
            for (int dz = 0; dz < SHIP_D; dz += 2) {
                BlockPos below = new BlockPos(ox + dx, y, oz + dz);
                if (isSolid(level, below)) solidCount++;
                if (!isAir(level, below.above())) solidCount -= 2;
            }
        }
        return Math.max(0, solidCount);
    }

    /**
     * Find a safe standing position inside the crashed ship.
     * Scans upward from shipOriginY+1 (first block above ship floor).
     * Returns the first position with 2 air blocks (feet + head) and a solid block below.
     *
     * This replaces the old findSafePlayerSpawn which was scanning exterior asteroid surface.
     */
    public static BlockPos findSafeShipInterior(ServerLevel level, BlockPos shipOrigin) {
        // The ship floor is at shipOriginY (placed flush on asteroid).
        // The interior starts at shipOriginY + 1.
        for (int dy = 1; dy <= 12; dy++) {
            BlockPos feet = shipOrigin.above(dy);
            BlockPos head = feet.above();
            BlockPos ground = feet.below();

            if (isAir(level, feet) && isAir(level, head) && isSolid(level, ground)) {
                return feet;
            }
        }
        // Fallback: 2 above origin
        return shipOrigin.above(2);
    }

    /**
     * Legacy safe-surface helper retained for compatibility.
     * Finds a safe spot near the ship origin for respawn.
     */
    public static BlockPos findSafePlayerSpawn(ServerLevel level, BlockPos shipOrigin) {
        return findSafeShipInterior(level, shipOrigin);
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.isAir() && s.isSolidRender(level, pos);
    }

    private static boolean isAir(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }
}
