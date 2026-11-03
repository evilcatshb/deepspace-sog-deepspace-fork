package com.deepspace.entity.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Walk node evaluator for mobs in the Deep Space dimension.
 *
 * <p>Climbable blocks (ladders, scaffolding) are typed as {@link BlockPathTypes#OPEN}
 * so they are treated as traversable space.  Ladder descent is not handled by
 * vanilla pathfinding and will be addressed later with a custom pathfinding library.</p>
 */
public final class SpaceAwareNodeEvaluator extends WalkNodeEvaluator {

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
        if (level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.CLIMBABLE)) {
            return BlockPathTypes.OPEN;
        }
        return super.getBlockPathType(level, x, y, z);
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        if (level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.CLIMBABLE)) {
            return BlockPathTypes.OPEN;
        }
        return super.getBlockPathType(level, x, y, z, mob);
    }
}
