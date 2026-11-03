package com.deepspace.entity.goal;

import com.deepspace.entity.SpaceWhaleEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Space Whale tail sweep: hits target and all nearby entities within 3 blocks of target.
 * Range: 5-6 blocks. Damage: 12-15. Knockback within 3-block radius of target.
 */
public class SpaceWhaleTailSweepGoal extends Goal {

    private static final float ATTACK_RANGE   = 6.0f;
    private static final float SPLASH_RADIUS  = 3.0f;
    private static final float DAMAGE_MIN     = 12.0f;
    private static final float DAMAGE_MAX     = 15.0f;
    private static final int   COOLDOWN_TICKS = 60;   // 3 seconds

    private final SpaceWhaleEntity whale;
    private int cooldown = 0;

    public SpaceWhaleTailSweepGoal(SpaceWhaleEntity whale) {
        this.whale = whale;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!whale.isHostile()) return false;
        LivingEntity target = whale.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (cooldown > 0) { cooldown--; return false; }
        return whale.distanceToSqr(target) <= ATTACK_RANGE * ATTACK_RANGE;
    }

    @Override
    public boolean canContinueToUse() { return false; } // One-shot per canUse

    @Override
    public void start() {
        LivingEntity target = whale.getTarget();
        if (target == null) return;

        cooldown = COOLDOWN_TICKS;

        float dmg = DAMAGE_MIN + whale.getRandom().nextFloat() * (DAMAGE_MAX - DAMAGE_MIN);
        target.hurt(whale.damageSources().mobAttack(whale), dmg);

        // Knockback the primary target
        double kbX = target.getX() - whale.getX();
        double kbZ = target.getZ() - whale.getZ();
        double dist = Math.sqrt(kbX * kbX + kbZ * kbZ);
        if (dist > 0.0) {
            target.setDeltaMovement(
                target.getDeltaMovement().add(kbX / dist * 2.0, 0.4, kbZ / dist * 2.0));
        }

        // AOE: hit all entities within SPLASH_RADIUS of target (except the whale itself)
        List<LivingEntity> nearby = whale.level().getEntitiesOfClass(
            LivingEntity.class,
            new AABB(target.getX() - SPLASH_RADIUS, target.getY() - SPLASH_RADIUS,
                     target.getZ() - SPLASH_RADIUS,
                     target.getX() + SPLASH_RADIUS, target.getY() + SPLASH_RADIUS,
                     target.getZ() + SPLASH_RADIUS));
        for (LivingEntity near : nearby) {
            if (near == whale || near == target) continue;
            float nearDmg = DAMAGE_MIN + whale.getRandom().nextFloat() * (DAMAGE_MAX - DAMAGE_MIN);
            near.hurt(whale.damageSources().mobAttack(whale), nearDmg * 0.7f);
        }
    }
}
