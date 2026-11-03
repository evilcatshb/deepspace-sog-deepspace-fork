package com.deepspace.entity.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * LunarianCombatGoal — a priority-2 combat goal for {@code Lunarian} entities
 * (and any other Villager subclass used as a faction mob).
 *
 * <h3>Mob targeting (raiders / shiphand defence)</h3>
 * When a {@link NearestAttackableTargetGoal} (injected by
 * {@code FactionGoalInjector}) has already set {@code mob.getTarget()} to a
 * non-player {@link LivingEntity} (e.g. a pirate raider tagged
 * {@code raid_entity}), {@code canUse()} adopts that target directly instead
 * of searching for a hostile player.  This means the same goal drives both
 * player-hostile combat and shiphand defence against raiders without needing
 * a separate goal class.
 *
 * <h3>Why this is needed</h3>
 * {@code Lunarian extends Villager}, inheriting the full Villager goal-selector
 * including:
 * <ul>
 *   <li>Priority 1: {@code AvoidEntityGoal} (flee from zombies etc.)</li>
 *   <li>Priority 2: {@code VillagerPanicGoal}
 *       (only activates when {@code getLastHurtByMob != null})</li>
 *   <li>Priority 3: {@code VillagerTradeWithPlayerGoal} — claims the
 *       {@link Flag#MOVE} flag whenever a player is in trade range</li>
 *   <li>Priority 3: {@code LookAtTradingPlayerGoal} — claims {@link Flag#LOOK}</li>
 * </ul>
 * The generic {@code MeleeAttackGoal} injected at priority 4 by
 * {@code FactionGoalInjector} is permanently blocked by the priority-3 trade
 * goals the moment a player gets close — exactly when combat should start.
 *
 * <h3>Fix</h3>
 * This goal runs at <b>priority 2</b>, below {@code AvoidEntityGoal} (priority 1)
 * but above the trade goals (priority 3).  It claims both MOVE and LOOK, so the
 * trade goals cannot start while a hostile player is nearby.
 *
 * {@code VillagerPanicGoal} (also priority 2) only activates when the Lunarian
 * was recently hurt ({@code getLastHurtByMob != null}), which doesn't occur from
 * the datapack's tag-based aggro system, so it doesn't compete in practice.
 *
 * <h3>Navigation and Brain coexistence</h3>
 * Villager's {@code customServerAiStep()} ticks the Brain (including
 * {@code MoveToTargetSink}) <em>before</em> {@code Mob.aiStep()} ticks the
 * goal-selector.  Our {@code tick()} call to {@code navigation.moveTo()} therefore
 * runs last in the update cycle and is never overridden by Brain locomotion in
 * the same tick.
 */
public class LunarianCombatGoal extends Goal {

    /** Attack reach squared: ~2 blocks (matches vanilla melee range for villager bbox). */
    private static final double ATTACK_REACH_SQ = 6.0;
    /** Stop pursuing target beyond this distance (squared). */
    private static final double MAX_PURSUIT_SQ = 48.0 * 48.0;
    /** Ticks between melee swings (20 = once per second). */
    private static final int SWING_COOLDOWN = 20;

    private final Mob mob;
    /** Tag that must be present on the player for this mob to attack them. */
    private final String hostilePlayerTag;
    /** Navigation speed multiplier. */
    private final double speedMod;

    /**
     * Current combat target — either a hostile {@link ServerPlayer} or any
     * {@link LivingEntity} that a {@code NearestAttackableTargetGoal} has
     * placed on {@code mob.getTarget()} (e.g. a raider tagged
     * {@code raid_entity}).
     */
    private LivingEntity target;
    private int swingCooldown;

    public LunarianCombatGoal(Mob mob, String hostilePlayerTag, double speedMod) {
        this.mob         = mob;
        this.hostilePlayerTag = hostilePlayerTag;
        this.speedMod    = speedMod;
        // Claim MOVE + LOOK so trade goals (also MOVE + LOOK) cannot start
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // If a NearestAttackableTargetGoal (injected by FactionGoalInjector) has
        // already acquired a mob target (e.g. a pirate raider tagged raid_entity),
        // adopt it directly.  This is how shiphand crew engage raiders: the
        // priority-1 shiphand targeting goal sets mob.getTarget(); we pick it up here.
        LivingEntity mobTarget = mob.getTarget();
        if (mobTarget != null && mobTarget.isAlive()
                && !mobTarget.getTags().contains("whitelisted")) {
            target = mobTarget;
            return true;
        }
        // Fallback: search for a hostile player (standard faction hostility).
        target = findNearestHostilePlayer();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (mob.distanceToSqr(target) >= MAX_PURSUIT_SQ) return false;
        if (target instanceof ServerPlayer sp) {
            // Player target: re-check hostile tag every tick (timed-aggro may expire)
            return !sp.isSpectator() && sp.getTags().contains(hostilePlayerTag);
        }
        // Mob target (raider): continue as long as alive, in range, and not whitelisted
        return !target.getTags().contains("whitelisted");
    }

    @Override
    public void start() {
        mob.setTarget(target);
        mob.getLookControl().setLookAt(target, 30f, 30f);
        swingCooldown = 0;
    }

    @Override
    public void tick() {
        if (target == null) return;

        // Re-sync mob.target every tick so other systems see our choice and
        // don't race to clear it between ticks.
        mob.setTarget(target);

        // Keep looking at target
        mob.getLookControl().setLookAt(target, 30f, 30f);

        // Drive navigation directly.  This runs after Brain.tick() in the
        // entity update cycle, so our path is never overwritten this tick.
        mob.getNavigation().moveTo(target, speedMod);

        // Melee attack when in range
        if (--swingCooldown <= 0) {
            swingCooldown = SWING_COOLDOWN;
            if (mob.distanceToSqr(target) <= ATTACK_REACH_SQ) {
                mob.swing(InteractionHand.MAIN_HAND);
                // doHurtTarget() crashes if the mob lacks ATTACK_DAMAGE attribute
                // (Lunarian inherits Villager which never registers it).
                // Fall back to a direct hurt() with a fixed 3 HP base damage.
                if (mob.getAttributes().hasAttribute(
                        net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                    mob.doHurtTarget(target);
                } else {
                    target.hurt(mob.damageSources().mobAttack(mob), 3.0f);
                }
            }
        }
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        mob.getNavigation().stop();
        target       = null;
        swingCooldown = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerPlayer findNearestHostilePlayer() {
        if (!(mob.level() instanceof ServerLevel sl)) return null;
        ServerPlayer best    = null;
        double       bestDist = MAX_PURSUIT_SQ;
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.getTags().contains("whitelisted"))  continue;
            if (!p.getTags().contains(hostilePlayerTag)) continue;
            double d = mob.distanceToSqr(p);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }
}
