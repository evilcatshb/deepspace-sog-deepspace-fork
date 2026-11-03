package com.deepspace.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Two-part system for controlling when faction mobs may target players.
 *
 * <h3>Part 1 — {@code setTarget} gate (PIRATE ONLY)</h3>
 * <p>Blocks any new targeting attempt at the source for pirate pillagers
 * when the player has {@code rep_pirate >= 0} and no forced aggro tag.
 * This gate uses the scoreboard rep value which is available immediately
 * on the same tick as the attack, unlike the advancement-driven
 * {@code *_hostile_to_player} tags which are applied at end-of-tick.</p>
 *
 * <p><b>Why only pirates:</b> The rep check ({@code rep_pirate >= 0}) is
 * a synchronous scoreboard read — it's valid the instant the mob is hurt.
 * Other factions rely on the {@code *_hostile_to_player} tag which is set
 * by an advancement reward function that fires at end-of-tick.  Gating
 * {@code setTarget} on a tag that doesn't exist yet would block the
 * initial hurt response entirely, leaving the mob unable to fight back.</p>
 *
 * <h3>Part 2 — periodic stale-target eviction (ALL FACTIONS)</h3>
 * <p>Once a mob has already acquired a player target, some vanilla goals
 * ({@code HurtByTargetGoal}) only check range and visibility in their
 * {@code canContinueToUse()} — they never re-run tag/rep conditions.
 * So when the forced aggro window expires and the hostile tag is removed,
 * the mob might keep chasing indefinitely.</p>
 *
 * <p>The fix: inject into {@code Mob.tick()} and every
 * {@value #STALE_TARGET_CHECK_INTERVAL} ticks check whether the current
 * target is still valid.  If not, call {@code setTarget(null)}.  This is
 * a safety net on top of the {@code AngerTime:1} trick in the datapack
 * (which triggers {@code stopBeingAngry() → setTarget(null)} via vanilla
 * code for NeutralMob types) and the {@code canContinueToUse()} override
 * in {@code FactionGoalInjector}.</p>
 *
 * <h3>Disengagement layers (all three must agree for clean stop)</h3>
 * <ol>
 *   <li><b>AngerTime:1</b> (datapack tick_phase1) — for NeutralMob types
 *       (zombified_piglin, iron_golem): vanilla decrements 1→0, calls
 *       {@code stopBeingAngry() → setTarget(null) + setLastHurtByMob(null)}.
 *       Fires 1 tick after timer expiry.</li>
 *   <li><b>canContinueToUse() override</b> (FactionGoalInjector) — the
 *       injected player-targeting goal re-checks the hostile tag every tick.
 *       Returns false immediately when tag is removed.</li>
 *   <li><b>Stale eviction</b> (this mixin, Part 2) — safety net that runs
 *       every 5 seconds for ALL faction types, catches edge cases like
 *       mobs beyond 96-block range or non-NeutralMob types.</li>
 * </ol>
 */
@Mixin(Mob.class)
public class PirateTargetMixin {

    /** How often (in ticks) each faction mob re-validates its current target. */
    private static final int STALE_TARGET_CHECK_INTERVAL = 100; // 5 seconds

    /**
     * Maps non-pirate faction tags to their player-side hostility tags.
     * Pirate is excluded because it uses a rep-based gate instead.
     */
    private static final Map<String, String> FACTION_HOSTILE_TAGS = Map.of(
        "scav",       "scav_hostile_to_player",
        "luna",       "luna_hostile_to_player",
        "auto",       "auto_hostile_to_player",
        "republic",   "republic_hostile_to_player",
        "collective", "collective_hostile_to_player",
        "imax",       "imax_hostile_to_player"
    );

    // -------------------------------------------------------------------------
    // Part 1: gate — block NEW targeting of good-rep players (PIRATE ONLY)
    // -------------------------------------------------------------------------

    /**
     * Cancels {@code setTarget(player)} for a pirate pillager when the player
     * does not have {@code pirate_hostile_to_player} and has {@code rep_pirate >= 0}.
     *
     * <p>This gate is pirate-specific because the rep score is available
     * synchronously on the same tick as the attack.  Other factions use
     * advancement-driven tags that aren't set until end-of-tick, so gating
     * them here would block the initial hurt response.</p>
     *
     * <p>Passing {@code null} (clearing a target) is always allowed.</p>
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void deepspace$blockPirateTargetingGoodRepPlayer(
            @Nullable LivingEntity target, CallbackInfo ci) {

        Mob self = (Mob) (Object) this;

        // Only pirate pillagers use the setTarget gate
        if (!(self instanceof Pillager)) return;
        if (!self.getTags().contains("pirate")) return;
        if (!(target instanceof ServerPlayer player)) return;

        // Authoritative tag present → datapack says targeting is valid
        if (player.getTags().contains("pirate_hostile_to_player")) return;

        if (deepspace$getPirateRep(self, player) >= 0) {
            ci.cancel();
        }
    }

    // -------------------------------------------------------------------------
    // Part 2: stale-target eviction — clear already-set targets that expired
    // -------------------------------------------------------------------------

    /**
     * Every {@value #STALE_TARGET_CHECK_INTERVAL} ticks, checks whether this
     * faction mob's current target is still a valid hostile.  If the player's
     * forced-aggro window has expired (tag gone, rep restored), the target is
     * cleared via {@code setTarget(null)}.
     *
     * <p>For NeutralMob types (zombified_piglin, iron_golem), the primary
     * disengage mechanism is the {@code AngerTime:1} trick in tick_phase1.
     * This eviction check is the safety net that catches edge cases.</p>
     *
     * <p>Entity {@code tickCount} values are naturally staggered across
     * mobs (each entity has its own lifetime counter), so the checks
     * spread out across ticks rather than all firing at once.</p>
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void deepspace$evictStaleFactionTarget(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        // Server-side only
        if (self.level().isClientSide()) return;

        // Stagger checks: only run once per STALE_TARGET_CHECK_INTERVAL ticks
        if (self.tickCount % STALE_TARGET_CHECK_INTERVAL != 0) return;

        LivingEntity currentTarget = self.getTarget();
        if (!(currentTarget instanceof ServerPlayer player)) return;

        Set<String> tags = self.getTags();
        Set<String> playerTags = player.getTags();

        // ── Pirate-specific eviction (rep check) ─────────────────────────
        if (tags.contains("pirate") && self instanceof Pillager) {
            if (playerTags.contains("pirate_hostile_to_player")) return;

            if (deepspace$getPirateRep(self, player) >= 0) {
                self.setTarget(null);
            }
            return;
        }

        // ── General faction eviction (tag check) ─────────────────────────
        for (Map.Entry<String, String> entry : FACTION_HOSTILE_TAGS.entrySet()) {
            if (tags.contains(entry.getKey())) {
                if (!playerTags.contains(entry.getValue())) {
                    self.setTarget(null);
                }
                return; // found the mob's faction, done
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared helper
    // -------------------------------------------------------------------------

    /**
     * Returns {@code rep_pirate} for the player, or {@code -1} if the
     * objective is missing or the player has no entry yet (safe hostile
     * fallback for uninitialized players).
     */
    private static int deepspace$getPirateRep(Mob mob, ServerPlayer player) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return -1;

        Scoreboard scoreboard = serverLevel.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective("rep_pirate");
        if (objective == null) return -1;

        if (!scoreboard.hasPlayerScore(player.getScoreboardName(), objective)) return -1;

        return scoreboard.getOrCreatePlayerScore(
                player.getScoreboardName(), objective).getScore();
    }
}
