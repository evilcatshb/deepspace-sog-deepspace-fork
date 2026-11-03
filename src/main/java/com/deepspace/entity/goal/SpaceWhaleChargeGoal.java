package com.deepspace.entity.goal;

import com.deepspace.entity.SpaceWhaleEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Space Whale hostile goal — two sub-phases:
 *
 *   APPROACH  – while target is beyond charge range, fly toward the target at
 *               {@link #APPROACH_SPEED} blocks/tick (BUG_5: was missing; whale
 *               used to coast on its old wander momentum, appearing to flee).
 *
 *   CHARGE    – once 5–10 blocks away, accelerate hard toward target, deal
 *               14–16 damage on contact and knock the target ~6 blocks.
 *
 * {@link #APPROACH_SPEED} is also read by {@link SpaceWhaleWanderGoal} to derive
 * the passive wander speed (75 % of this value), keeping the two speeds consistent.
 */
public class SpaceWhaleChargeGoal extends Goal {

    // ── Tuning constants ─────────────────────────────────────────────────────
    private static final int    COOLDOWN_TICKS = 100;   // 5 s between charge attempts
    /** Approach speed (blocks/tick) when hostile and closing on target. */
    public  static final double APPROACH_SPEED = 0.4;
    /** Maximum range at which this goal will engage a target. */
    private static final double ENGAGE_RANGE   = 48.0;

    // ── Instance state ───────────────────────────────────────────────────────
    private final SpaceWhaleEntity whale;
    private final double chargeRange;   // inner trigger range (default 10 blocks)
    private final float  damageMin;
    private final float  damageMax;

    private int     cooldown        = 0;
    private boolean charging        = false;
    private boolean chargeHitLanded = false;
    private Vec3    chargeDirection = Vec3.ZERO;
    private int     chargeTicks     = 0;

    public SpaceWhaleChargeGoal(SpaceWhaleEntity whale, double chargeRange,
                                 float damageMin, float damageMax) {
        this.whale       = whale;
        this.chargeRange = chargeRange;
        this.damageMin   = damageMin;
        this.damageMax   = damageMax;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Goal lifecycle ────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (!whale.isHostile()) return false;
        LivingEntity target = whale.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (cooldown > 0) { cooldown--; return false; }
        // Engage if target is within follow range
        return whale.distanceToSqr(target) <= ENGAGE_RANGE * ENGAGE_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        if (!whale.isHostile()) return false;
        LivingEntity target = whale.getTarget();
        if (target == null || !target.isAlive()) return false;
        // During a charge: run until 20 ticks elapsed or hit already landed
        if (charging) return !chargeHitLanded && chargeTicks < 20;
        // During approach: keep going while target is in range
        return whale.distanceToSqr(target) <= ENGAGE_RANGE * ENGAGE_RANGE;
    }

    @Override
    public void start() {
        charging        = false;
        chargeHitLanded = false;
        chargeTicks     = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = whale.getTarget();
        if (target == null) return;

        double distSq = whale.distanceToSqr(target);

        if (charging) {
            // ── Charge phase ─────────────────────────────────────────────
            chargeTicks++;
            whale.setDeltaMovement(
                chargeDirection.x * 1.2,
                chargeDirection.y * 0.6,
                chargeDirection.z * 1.2);

            if (whale.distanceTo(target) < 4.5) {
                doChargeHit(target);
                chargeHitLanded = true; // canContinueToUse → false → stop() called next tick
            }

        } else if (distSq > 25.0 && distSq <= chargeRange * chargeRange) {
            // ── Initiate charge ─────────────────────────────────────────
            charging        = true;
            chargeHitLanded = false;
            chargeTicks     = 0;
            chargeDirection = target.position().subtract(whale.position()).normalize();
            whale.getLookControl().setLookAt(target, 30.0f, 30.0f);

        } else if (distSq > chargeRange * chargeRange) {
            // ── Approach phase (BUG_5 fix) ──────────────────────────────
            // Fly directly toward the target so the whale never appears to flee.
            Vec3 dir = target.position().subtract(whale.position()).normalize();
            whale.setDeltaMovement(
                dir.x * APPROACH_SPEED,
                dir.y * APPROACH_SPEED * 0.5,
                dir.z * APPROACH_SPEED);
            whale.setYRot((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
            whale.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
        // If target is within 5 blocks (distSq <= 25): TailSweepGoal handles it.
    }

    @Override
    public void stop() {
        charging        = false;
        chargeHitLanded = false;
        chargeTicks     = 0;
        cooldown        = COOLDOWN_TICKS;
        whale.setDeltaMovement(whale.getDeltaMovement().scale(0.3));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void doChargeHit(LivingEntity target) {
        float dmg = damageMin + whale.getRandom().nextFloat() * (damageMax - damageMin);
        target.hurt(whale.damageSources().mobAttack(whale), dmg);

        // ~6-block knockback along the charge direction
        target.setDeltaMovement(
            target.getDeltaMovement().add(
                chargeDirection.x * 1.5,
                0.5,
                chargeDirection.z * 1.5));
    }
}
