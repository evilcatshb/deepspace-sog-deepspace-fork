package com.deepspace.entity;

import com.deepspace.DeepSpaceMod;
import com.deepspace.init.DSDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forge event handler: injects faction-vs-faction targeting, faction-vs-player
 * targeting, and (where needed) melee attack goals into faction-tagged mobs
 * when they enter the Deep Space dimension.
 *
 * <h3>Why this is necessary</h3>
 * Vanilla mob AI targets are hardcoded per entity type (e.g. Pillager only
 * targets Player / AbstractVillager / IronGolem).  The faction system tags
 * mobs with e.g. {@code "pirate"} or {@code "luna"}, but that tag has no
 * meaning to the vanilla AI engine.  This injector bridges that gap by
 * adding {@link NearestAttackableTargetGoal}s whose predicates read entity
 * tags directly, making any mob aware of its enemies regardless of type.
 *
 * <h3>Hostility matrix (from spec)</h3>
 * <pre>
 *  pirate     → luna, republic, collective
 *  republic   → pirate
 *  collective → auto, pirate
 *  auto       → pirate
 *  scav, luna, imax → (neutral — no faction-vs-faction hostile targets)
 * </pre>
 *
 * <h3>Player hostility (all factions)</h3>
 * Every faction mob also receives a targeting goal that acquires
 * {@link ServerPlayer}s carrying the faction's
 * {@code <faction>_hostile_to_player} tag.  This tag is set by the factions
 * datapack when a player attacks a faction member (timed aggro) or when
 * reputation drops below the hostility threshold (permanent aggro).
 * <p>
 * The goal overrides {@code canContinueToUse()} to re-check the tag every
 * tick, ensuring the mob disengages cleanly when the aggro window expires
 * and the tag is removed.
 *
 * <h3>Melee attack fallback</h3>
 * {@code minecraft:pillager} only has {@link
 * net.minecraft.world.entity.ai.goal.RangedBowAttackGoal} as its attack goal.
 * That goal's {@code canUse()} returns {@code false} when the mob is not
 * holding a {@code BowItem} — so a pirate pillager given a nano_saber or
 * any non-bow weapon has a valid TARGET but can never actually SWING.
 * <p>
 * This injector adds a {@link MeleeAttackGoal} at priority 4 (below the bow
 * goal at 3) as a fallback.  When the mob does hold a bow the melee goal
 * will not win the goal selector competition; when it holds a melee weapon
 * the melee goal activates as normal.
 */
public final class FactionGoalInjector {

    // ── Hostility matrix (faction-vs-faction) ─────────────────────────────────

    /**
     * Maps own faction tag → ordered list of enemy faction tags.
     * Factions not present here are neutral (no faction-vs-faction targets).
     */
    private static final Map<String, List<String>> HOSTILE_MAP = Map.of(
        "pirate",     List.of("luna", "republic", "collective"),
        "republic",   List.of("pirate"),
        "collective", List.of("auto", "pirate"),
        "auto",       List.of("pirate")
    );

    // ── Player hostility (all factions) ───────────────────────────────────────

    /**
     * Maps every faction tag → the player-side hostility tag that the
     * factions datapack sets when the faction should attack that player.
     * ALL factions are listed here, including neutral ones.
     */
    private static final Map<String, String> PLAYER_HOSTILE_TAG_MAP = Map.of(
        "pirate",     "pirate_hostile_to_player",
        "republic",   "republic_hostile_to_player",
        "collective", "collective_hostile_to_player",
        "auto",       "auto_hostile_to_player",
        "scav",       "scav_hostile_to_player",
        "luna",       "luna_hostile_to_player",
        "imax",       "imax_hostile_to_player"
    );

    // ── Event listener ────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {

        // Server-side only, Deep Space only
        if (!DSDimensions.DEEP_SPACE.equals(event.getLevel().dimension())) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Schedule one tick after join so all other entity-join hooks (including
        // MobNavigationPatcher) have finished first.
        serverLevel.getServer().execute(() -> {
            if (!mob.isAlive()) return;

            Set<String> tags = mob.getTags();

            // ── Determine own faction ──────────────────────────────────────
            // Check ALL factions (player hostility applies to every faction,
            // not just those with faction-vs-faction enemies).
            String ownFaction = null;
            for (String factionTag : PLAYER_HOSTILE_TAG_MAP.keySet()) {
                if (tags.contains(factionTag)) {
                    ownFaction = factionTag;
                    break;
                }
            }

            if (ownFaction == null) return; // not a faction mob — nothing to inject

            // ── 1. Faction-vs-faction targeting (if applicable) ───────────
            List<String> enemyFactions = HOSTILE_MAP.get(ownFaction);
            if (enemyFactions != null && !enemyFactions.isEmpty()) {
                final List<String> targets = enemyFactions;
                mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                    mob,
                    LivingEntity.class,
                    10,
                    true,    // mustSee
                    false,   // mustReach
                    (LivingEntity candidate) -> {
                        if (candidate == mob) return false;
                        if (candidate.getTags().contains("whitelisted")) return false;
                        Set<String> candidateTags = candidate.getTags();
                        for (String enemyTag : targets) {
                            if (candidateTags.contains(enemyTag)) return true;
                        }
                        return false;
                    }
                ));
            }

            // ── 2. Player-hostile targeting (ALL factions) ────────────────
            //
            // Targets any ServerPlayer carrying the faction's hostile tag.
            // canContinueToUse() is overridden to re-check the tag every
            // tick so the mob disengages cleanly when timed aggro expires.
            final String hostileTag = PLAYER_HOSTILE_TAG_MAP.get(ownFaction);
            mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                mob,
                ServerPlayer.class,
                10,       // randomInterval — recheck every 10 ticks
                true,     // mustSee
                false,    // mustReach — keep target even if path fails
                (LivingEntity candidate) -> {
                    if (candidate.getTags().contains("whitelisted")) return false;
                    return candidate.getTags().contains(hostileTag);
                }
            ) {
                /**
                 * Re-checks the hostile tag every tick while the goal is
                 * active.  Without this override, canContinueToUse() only
                 * checks range and line-of-sight, meaning the mob would
                 * keep chasing a player indefinitely after the timed aggro
                 * window expires and the tag is removed.
                 */
                @Override
                public boolean canContinueToUse() {
                    LivingEntity target = this.mob.getTarget();
                    if (target instanceof ServerPlayer sp) {
                        if (!sp.getTags().contains(hostileTag)) {
                            return false;
                        }
                    }
                    return super.canContinueToUse();
                }
            });

            // ── 3. Melee attack fallback / Villager-subclass combat fix ──────
            //
            // TWO distinct cases need handling:
            //
            // A) Standard PathfinderMob (pillager, zombified piglin, etc.):
            //    Inject MeleeAttackGoal at priority 4 (below the ranged bow
            //    goal at 3) so melee-weapon-holding mobs can still attack.
            //
            // B) AbstractVillager subclass (Lunarian from Ad Astra):
            //    Villager's goal-selector includes VillagerTradeWithPlayerGoal
            //    and LookAtTradingPlayerGoal at priority 3, both of which claim
            //    the MOVE/LOOK flags whenever a player is in range — permanently
            //    blocking any goal at priority >= 4, including MeleeAttackGoal.
            //    Fix: inject LunarianCombatGoal at priority 2, which is above
            //    the trade goals (3) but below AvoidEntityGoal (1).
            //    VillagerPanicGoal (also priority 2) only activates when the
            //    mob was recently hurt, which doesn't occur from the tag-based
            //    aggro system, so there is no practical conflict.
            if (mob instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                // Villager-subclass: use priority-2 goal that beats trade goals
                mob.goalSelector.addGoal(2, new com.deepspace.entity.goal.LunarianCombatGoal(
                    mob,
                    hostileTag,
                    1.0   // speed — standard walk pace
                ));
                DeepSpaceMod.LOGGER.info(
                    "[FactionGoalInjector] Injected LunarianCombatGoal (prio 2) for {} " +
                    "(AbstractVillager subclass, hostile-tag: {})",
                    mob.getType().toShortString(), hostileTag);
            } else if (mob instanceof PathfinderMob pfMob) {
                // Standard case: MeleeAttackGoal below ranged-bow goal (prio 3)
                pfMob.goalSelector.addGoal(4, new MeleeAttackGoal(
                    pfMob,
                    1.0,   // speedModifier — standard walk speed
                    true   // followingTargetEvenIfNotSeen — pursue through walls/gaps
                ));
            }

            // ── 4. Shiphand crew: always attack any active raid entity ─────────
            //
            // Purchased crew (tagged "shiphand") must defend the ship against
            // raiders regardless of the raiding faction.  Raiders carry the
            // "raid_entity" tag (applied by _spawn_single_<faction>.mcfunction).
            //
            // Priority 1 — the highest targetSelector priority — ensures this
            // goal wins over faction-vs-faction targeting (prio 2) and player
            // hostility (prio 3), so a shiphand always breaks off a faction
            // skirmish to engage an incoming raider first.
            //
            // For AbstractVillager crew (Lunarians), LunarianCombatGoal.canUse()
            // adopts mob.getTarget() when it is already set by a targetSelector
            // goal — meaning this NearestAttackableTargetGoal feeds directly into
            // the existing combat goal with no extra goalSelector injection needed.
            if (tags.contains("shiphand")) {
                mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                    mob,
                    LivingEntity.class,
                    10,     // randomInterval — recheck every 10 ticks
                    true,   // mustSee
                    false,  // mustReach — keep target even through walls
                    (LivingEntity candidate) -> {
                        if (candidate == mob) return false;
                        if (candidate.getTags().contains("whitelisted")) return false;
                        return candidate.getTags().contains("raid_entity");
                    }
                ));
                DeepSpaceMod.LOGGER.info(
                    "[FactionGoalInjector] Injected shiphand raid-targeting goal (prio 1) for {} (faction: {})",
                    mob.getType().toShortString(), ownFaction);
            }

            // ── Logging ───────────────────────────────────────────────────
            boolean hasFactionEnemies = enemyFactions != null && !enemyFactions.isEmpty();
            if (mob instanceof net.minecraft.world.entity.npc.AbstractVillager) {
                DeepSpaceMod.LOGGER.info(
                    "[FactionGoalInjector] Injected {} + LunarianCombatGoal(prio 2) for {} " +
                    "(faction: {}, hostile-tag: {}{})",
                    hasFactionEnemies ? "faction-targeting" : "player-targeting",
                    mob.getType().toShortString(), ownFaction, hostileTag,
                    hasFactionEnemies ? ", faction-targets: " + enemyFactions : "");
            } else if (mob instanceof PathfinderMob) {
                DeepSpaceMod.LOGGER.info(
                    "[FactionGoalInjector] Injected {} for {} " +
                    "(faction: {}, player-hostile-tag: {}{})",
                    hasFactionEnemies ? "faction + player targeting + melee goals"
                                     : "player targeting + melee goals",
                    mob.getType().toShortString(), ownFaction, hostileTag,
                    hasFactionEnemies ? ", faction-targets: " + enemyFactions : "");
            } else {
                DeepSpaceMod.LOGGER.info(
                    "[FactionGoalInjector] Injected {} for {} " +
                    "(faction: {}, player-hostile-tag: {}{}) — " +
                    "not a PathfinderMob, skipping melee inject",
                    hasFactionEnemies ? "faction + player targeting goals"
                                     : "player targeting goal",
                    mob.getType().toShortString(), ownFaction, hostileTag,
                    hasFactionEnemies ? ", faction-targets: " + enemyFactions : "");
            }
        });
    }
}
