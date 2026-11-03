package com.deepspace.entity.goal;

import com.deepspace.DSConstants;
import com.deepspace.entity.SpaceWhaleEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Makes a passive Space Whale wander slowly through space.
 *
 * MOD_6: Tamed whales wander within a 40-block radius of their owner so they
 * never fly away or despawn. Wild whales wander freely above the asteroid band.
 *
 * BUG_3: When the whale is on a lead it stands still — fighting the lead tension
 * with wander movement is what caused the rapid drift and lead snap.
 *
 * BUG_5: {@link #stop()} cancels navigation so the whale doesn't keep coasting
 * toward its old wander target after turning hostile.
 *
 * Speed: 75 % of the hostile approach speed defined in {@link SpaceWhaleChargeGoal}.
 */
public class SpaceWhaleWanderGoal extends Goal {

    private static final double FLIGHT_FLOOR = DSConstants.ASTEROID_BAND_Y_MAX + 15.0;
    private static final double FLIGHT_CEIL  = 240.0;

    /** Tamed whales wander within this radius of their owner (MOD_6). */
    private static final double TAMED_WANDER_RADIUS = 40.0;

    /** Wander speed = 75 % of hostile approach speed. */
    private static final double WANDER_SPEED = SpaceWhaleChargeGoal.APPROACH_SPEED * 0.75;

    /** Arrival threshold — stop when within 3 blocks of target. */
    private static final double ARRIVAL_DIST_SQ = 9.0;

    private final SpaceWhaleEntity whale;
    private double targetX, targetY, targetZ;
    private int cooldown = 0;

    public SpaceWhaleWanderGoal(SpaceWhaleEntity whale) {
        this.whale = whale;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (whale.isHostile()) return false;
        if (whale.isMigrating()) return false;
        if (whale.getTarget() != null) return false;
        // BUG_3: Never wander when on a lead — it fights the lead tension and
        // causes the rapid-drift / snap bug.
        if (whale.isLeashed()) return false;
        return --cooldown <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (whale.isHostile() || whale.isMigrating()) return false;
        if (whale.getTarget() != null) return false;
        // BUG_3: Abort immediately if the player attaches a lead mid-wander.
        if (whale.isLeashed()) return false;
        return whale.distanceToSqr(targetX, targetY, targetZ) > ARRIVAL_DIST_SQ;
    }

    @Override
    public void start() {
        cooldown = 100 + whale.getRandom().nextInt(100);

        // MOD_6: Tamed whales orbit near their owner. Wild whales wander freely.
        double centreX, centreY, centreZ;
        if (whale.isTame() && whale.getOwner() != null) {
            net.minecraft.world.entity.LivingEntity owner = whale.getOwner();
            centreX = owner.getX();
            centreY = owner.getY();
            centreZ = owner.getZ();
            double r = whale.getRandom().nextDouble() * TAMED_WANDER_RADIUS;
            double angle = whale.getRandom().nextDouble() * 2.0 * Math.PI;
            targetX = centreX + Math.cos(angle) * r;
            // Stay near owner's Y altitude (clamped so whale stays at a sensible height)
            targetY = centreY + (whale.getRandom().nextDouble() - 0.5) * 20.0;
            targetZ = centreZ + Math.sin(angle) * r;
        } else {
            // Wild wander: pick a random point in a 60-block radius at flight altitude
            double range = 60.0;
            targetX = whale.getX() + (whale.getRandom().nextDouble() - 0.5) * range * 2;
            targetY = FLIGHT_FLOOR + whale.getRandom().nextDouble() * (FLIGHT_CEIL - FLIGHT_FLOOR);
            targetZ = whale.getZ() + (whale.getRandom().nextDouble() - 0.5) * range * 2;
        }
    }

    @Override
    public void tick() {
        double dx = targetX - whale.getX();
        double dy = targetY - whale.getY();
        double dz = targetZ - whale.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1.0) return;

        // Drive velocity directly — FlyingPathNavigation silently fails in empty space.
        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;

        whale.setDeltaMovement(nx * WANDER_SPEED, ny * WANDER_SPEED * 0.5, nz * WANDER_SPEED);
        whale.setYRot((float) Math.toDegrees(Math.atan2(-nx, nz)));

        // Enforce altitude floor for wild whales only (tamed whales follow their owner's Y)
        if (!whale.isTame() && whale.getY() < FLIGHT_FLOOR) {
            Vec3 dm = whale.getDeltaMovement();
            whale.setDeltaMovement(dm.x, Math.max(dm.y, 0.12), dm.z);
        }
    }

    @Override
    public void stop() {
        // Cancel any residual navigation so the whale doesn't keep coasting
        // toward its old wander target after turning hostile (looked like fleeing).
        whale.getNavigation().stop();
    }
}
