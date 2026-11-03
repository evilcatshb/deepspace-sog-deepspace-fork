package com.deepspace.entity.goal;

import com.deepspace.entity.SpaceWhaleEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Drives the Space Whale migration ritual in three phases:
 *
 *   APPROACH  – fly in from the south at 4× orbit speed
 *   CIRCLE    – orbit above the planet for 15 min real-time (18 000 ticks)
 *               at 1× orbit speed (one full revolution every 5 min / 6 000 ticks)
 *   RETREAT   – fly north at 4× orbit speed until clear of loaded chunks, then silently remove
 *
 * Each whale is assigned an initial angle offset (migrationAngle) by MigrationManager so that
 * N whales are spaced evenly around the circle at 2π/N apart, giving a halo-like formation.
 *
 * Speed hierarchy:
 *   orbit speed   = 0.45 blocks/tick  (1 rev / 5 min)
 *   travel speed  = 0.45 × 4 = 1.80  (approach / retreat)
 *
 * External API:
 *   {@link #skipToRetreat()} – called by /spacewhale migrate progress to immediately end circling
 */
public class MigrationCircleGoal extends Goal {

    // ── Geometry / timing constants ───────────────────────────────────────────
    /** Orbit radius: 16 chunks = 256 blocks from planet centre. */
    public static final double ORBIT_RADIUS   = 256.0;
    /** Angular velocity for 1 full revolution in 6 000 ticks (5 min). */
    private static final double ANG_VEL_ORBIT = (2.0 * Math.PI) / 6_000.0;
    /** Circling duration: 18 000 ticks = 15 min real-time. */
    public static final int CIRCLE_TICKS      = 18_000;
    /** Orbit speed block/tick (baseline). */
    private static final double ORBIT_SPEED   = 0.45;
    /** Travel speed (approach / retreat) = 4× orbit. */
    private static final double TRAVEL_SPEED  = ORBIT_SPEED * 4.0;
    /** Arrival tolerance squared (blocks²). */
    private static final double ARRIVE_SQ     = 64.0;
    /** How far north of the planet we fly before checking for despawn (2 chunks). */
    private static final double RETREAT_NORTH_CLEAR = 512.0;

    private final SpaceWhaleEntity whale;
    private Phase phase        = Phase.IDLE;
    private int   circleTicker = 0;

    public MigrationCircleGoal(SpaceWhaleEntity whale) {
        this.whale = whale;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Goal lifecycle ────────────────────────────────────────────────────────
    // MOD_6: isTame() guard ensures tamed whales are completely excluded from migrations.
    @Override public boolean canUse()           { return whale.isMigrating() && !whale.isHostile() && !whale.isTame(); }
    @Override public boolean canContinueToUse() { return whale.isMigrating() && !whale.isHostile() && !whale.isTame(); }

    @Override
    public void start() {
        phase        = Phase.APPROACH;
        circleTicker = 0;
    }

    @Override
    public void stop() {
        phase        = Phase.IDLE;
        circleTicker = 0;
    }

    @Override
    public void tick() {
        switch (phase) {
            case APPROACH -> tickApproach();
            case CIRCLE   -> tickCircle();
            case RETREAT  -> tickRetreat();
            default       -> { /* IDLE: migration goal is disabled */ }
        }
    }

    // ── Phase: APPROACH ───────────────────────────────────────────────────────
    private void tickApproach() {
        // Fly to the whale's own entry position on the orbit circle
        double ex = orbitX(whale.migrationAngle);
        double ez = orbitZ(whale.migrationAngle);
        double ey = whale.migrationFlightY;

        if (whale.distanceToSqr(ex, ey, ez) <= ARRIVE_SQ) {
            phase = Phase.CIRCLE;
            circleTicker = 0;
            return;
        }
        flyToward(ex, ey, ez, TRAVEL_SPEED);
    }

    // ── Phase: CIRCLE ─────────────────────────────────────────────────────────
    private void tickCircle() {
        // Advance around the circle at orbit speed
        whale.migrationAngle += ANG_VEL_ORBIT;
        if (whale.migrationAngle >= 2.0 * Math.PI) whale.migrationAngle -= 2.0 * Math.PI;

        flyToward(orbitX(whale.migrationAngle),
                  whale.migrationFlightY,
                  orbitZ(whale.migrationAngle),
                  ORBIT_SPEED);

        circleTicker++;
        if (circleTicker >= CIRCLE_TICKS) {
            phase = Phase.RETREAT;
        }
    }

    // ── Phase: RETREAT ─────────────────────────────────────────────────────────
    private void tickRetreat() {
        // Fly due north (−Z) at 4× speed
        double retreatTargetZ = whale.migrationCentreZ - 1024.0;
        flyToward(whale.getX(), whale.migrationFlightY, retreatTargetZ, TRAVEL_SPEED);

        double distNorth = whale.migrationCentreZ - whale.getZ();
        if (distNorth > RETREAT_NORTH_CLEAR) {
            // Only despawn if no player is within the whale's chunk
            boolean playerNear = !whale.level()
                .getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    whale.getBoundingBox().inflate(32.0))
                .isEmpty();
            if (!playerNear) {
                // Silent removal — no death event, no loot
                whale.setMigrating(false);
                whale.discard();
            }
        }
    }

    // ── External API ──────────────────────────────────────────────────────────
    /** Called by /spacewhale migrate progress to skip straight to the retreat phase. */
    public void skipToRetreat() {
        if (phase == Phase.CIRCLE || phase == Phase.APPROACH) {
            phase        = Phase.RETREAT;
            circleTicker = CIRCLE_TICKS;
        }
    }

    public Phase getPhase() { return phase; }
    public void  setPhase(Phase p) { this.phase = p; }

    // ── Geometry helpers ──────────────────────────────────────────────────────
    private double orbitX(double angle) {
        return whale.migrationCentreX + Math.cos(angle) * ORBIT_RADIUS;
    }
    private double orbitZ(double angle) {
        return whale.migrationCentreZ + Math.sin(angle) * ORBIT_RADIUS;
    }

    private void flyToward(double tx, double ty, double tz, double speed) {
        double dx = tx - whale.getX();
        double dy = ty - whale.getY();
        double dz = tz - whale.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return;
        whale.setDeltaMovement(dx / dist * speed, dy / dist * speed * 0.5, dz / dist * speed);
        whale.setYRot((float) (Math.toDegrees(Math.atan2(-dx / dist, dz / dist))));
    }

    public enum Phase { IDLE, APPROACH, CIRCLE, RETREAT }
}
