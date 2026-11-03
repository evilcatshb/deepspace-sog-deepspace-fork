package com.deepspace.entity.navigation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Ground-only navigation for the four faction mob types inside Deep Space.
 *
 * <p>Uses {@link SpaceAwareNodeEvaluator} in place of the vanilla
 * {@link net.minecraft.world.level.pathfinder.WalkNodeEvaluator} so that
 * climbable blocks (ladders, scaffolding) are properly typed as
 * {@link net.minecraft.world.level.pathfinder.BlockPathTypes#CLIMBABLE},
 * enabling both ascending and descending ladder paths.</p>
 *
 * <p>{@code canOpenDoors} is {@code false} so mobs never trigger airlock
 * doors.</p>
 *
 * <p>{@link #canUpdatePath()} always returns {@code true} because vanilla
 * {@link GroundPathNavigation} gates recalculation on
 * {@code mob.isOnGround()}, which is unreliable in Deep Space's low-gravity
 * environment where mobs settle very slowly.</p>
 */
public final class DeepSpaceNavigation extends GroundPathNavigation {

    public DeepSpaceNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        SpaceAwareNodeEvaluator eval = new SpaceAwareNodeEvaluator();
        eval.setCanOpenDoors(false);
        eval.setCanPassDoors(true);
        this.nodeEvaluator = eval;
        return new PathFinder(eval, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
    }
}
