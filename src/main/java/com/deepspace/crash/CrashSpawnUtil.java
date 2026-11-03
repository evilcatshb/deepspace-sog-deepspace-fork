package com.deepspace.crash;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Safe spawn scanning for crash-site/Ad Astra arrival placement. */
public final class CrashSpawnUtil {
    private static final int SHIP_INTERIOR_TOP = 10;

    // -------------------------------------------------------------------------
    public static BlockPos findVerifiedSafeSpawn(ServerLevel level, CrashBubbleData bubble) {
        int cx     = bubble.shipCentreX();
        int cz     = bubble.shipCentreZ();
        int floorY = bubble.shipOriginY;

        for (int dy = SHIP_INTERIOR_TOP; dy >= 1; dy--) {
            int testY = floorY + dy;

            BlockPos centre = new BlockPos(cx, testY, cz);
            if (isSafeSpawn(level, centre)) return centre;

            for (int r = 1; r <= 6; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                        BlockPos candidate = new BlockPos(cx + dx, testY, cz + dz);
                        if (isSafeSpawn(level, candidate)) return candidate;
                    }
                }
            }
        }

        // Emergency fallback
        int emergencyFloorY = floorY + SHIP_INTERIOR_TOP + 2;
        BlockPos floor = new BlockPos(cx, emergencyFloorY, cz);
        BlockPos spawn = floor.above();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        DeepSpaceMod.LOGGER.warn(
            "[CrashSpawnUtil] Emergency spawn platform at {} for [{},{}]", floor, cx, cz);
        return spawn;
    }

    public static boolean isSafeSpawn(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() + 5) return false;
        if (!level.getBlockState(feet).isAir())          return false;
        if (!level.getBlockState(feet.above()).isAir())  return false;
        if (!level.getBlockState(feet.above(2)).isAir()) return false;
        BlockState floor = level.getBlockState(feet.below());
        return !floor.isAir();
    }
}
