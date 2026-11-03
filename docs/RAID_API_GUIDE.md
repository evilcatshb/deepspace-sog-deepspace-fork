# Faction Raid System — API & Usage Guide

## Overview

The raid system exposes a set of `mcfunction` entry points that can be called from FTB Quests (via command rewards), operator commands, or other datapack functions. Every raid function runs **as the target player** (`execute as <player> run ...`).

All raid functions live under `factions:raid/` and `factions:taunt/`.

---

## Quick Reference

| Action | Command |
|---|---|
| Force a pirate raid | `execute as @p run function factions:raid/execute_pirate` |
| Provoke a faction | `execute as @p run function factions:raid/provoke_scav` |
| Bribe a faction (1hr) | `execute as @p run function factions:raid/bribe_luna_small` |
| Bribe a faction (6hr) | `execute as @p run function factions:raid/bribe_luna_large` |
| Cancel active raid | `execute as @p run function factions:raid/cleanup_pirate` |
| Cancel ALL raids | `execute as @p run function factions:raid/cleanup_all` |

Replace `@p` with a specific player selector or `@s` when running inside an `execute as` chain.



---

## Faction Short Names

Use these in all function paths:

| Faction | Short Name | Example Function |
|---|---|---|
| Pirates | `pirate` | `factions:raid/execute_pirate` |
| Scavengers | `scav` | `factions:raid/provoke_scav` |
| Lunarians | `luna` | `factions:raid/bribe_luna_small` |
| Automata | `auto` | `factions:raid/cleanup_auto` |
| Republic | `republic` | `factions:raid/execute_republic` |
| Collective | `collective` | `factions:raid/provoke_collective` |
| iMAX | `imax` | `factions:raid/bribe_imax_large` |

---

## FTB Quests Integration

### How Command Rewards Work

FTB Quests can run commands when a quest or task is completed. For raid functions, you need **Elevated Permission: True** so the command runs with operator privileges.

In the FTB Quests GUI:

1. Create or edit a quest
2. Add a **Command Reward**
3. Paste the command (without the leading `/`)
4. Enable **Elevated Permission** (the toggle/checkbox)

The placeholder `@p` in FTB Quests command rewards targets the player who completed the quest.

---

### Example: Provoke Quest

**Scenario:** Player accepts a quest to taunt a faction. Completing the quest provokes a raid from that faction.

**Quest Setup:**
- Task: Any (e.g. item submission, checkbox, etc.)
- Reward type: Command
- Command: see table below
- Elevated Permission: **True**

**What happens:**
1. If the player's rep with that faction is above -128: subtracts 8 rep, sends a threatening radio message, no raid.
2. If the player's rep is at or below -128 AND no cooldown/active raid: triggers a full raid with the 10-second warning sequence.
3. If provoke is on cooldown (10 min after last provoke-triggered raid): sends a "still regrouping" message, no raid.

### Provoke Commands (All Factions)

Copy-paste these directly into FTB Quests command rewards. All require **Elevated Permission: True**.

| Faction | FTB Quests Command |
|---|---|
| Pirates | `execute as @p run function factions:raid/provoke_pirate` |
| Scavengers | `execute as @p run function factions:raid/provoke_scav` |
| Lunarians | `execute as @p run function factions:raid/provoke_luna` |
| Automata | `execute as @p run function factions:raid/provoke_auto` |
| Republic | `execute as @p run function factions:raid/provoke_republic` |
| Collective | `execute as @p run function factions:raid/provoke_collective` |
| iMAX | `execute as @p run function factions:raid/provoke_imax` |

---

### Example: Bribe Quest (Item Turn-In)

**Scenario:** Player pays faction currency to buy temporary raid immunity from that faction.

**Quest Setup:**
- Task: Item submission (faction-appropriate currency — see suggested items below)
- Reward type: Command
- Command: see table below
- Elevated Permission: **True**

There are two bribe sizes per faction: **small** (1 hour immunity) and **large** (6 hours immunity). These are the only two tiers. If you need a different duration, see "Custom Bribe Durations" below.

Bribes stack: if the player already has 30 minutes of bribe time remaining and buys another 1-hour bribe, they'll have 1h30m total. The timer caps at 24 hours.

### Bribe Commands (All Factions, Both Sizes)

Copy-paste these directly into FTB Quests command rewards. All require **Elevated Permission: True**.

**Small Bribes (1 hour / 72,000 ticks):**

| Faction | FTB Quests Command | Suggested Item Cost |
|---|---|---|
| Pirates | `execute as @p run function factions:raid/bribe_pirate_small` | 16× `gtmutils:ancient_gold_coin` |
| Scavengers | `execute as @p run function factions:raid/bribe_scav_small` | 16× scavenger scrap |
| Lunarians | `execute as @p run function factions:raid/bribe_luna_small` | 16× lunarian coin |
| Automata | `execute as @p run function factions:raid/bribe_auto_small` | 16× automata cheque |
| Republic | `execute as @p run function factions:raid/bribe_republic_small` | 16× doge coin |
| Collective | `execute as @p run function factions:raid/bribe_collective_small` | 16× seeds bundle |
| iMAX | `execute as @p run function factions:raid/bribe_imax_small` | 16× iMAX currency |

**Large Bribes (6 hours / 432,000 ticks):**

| Faction | FTB Quests Command | Suggested Item Cost |
|---|---|---|
| Pirates | `/execute as @p run function factions:raid/bribe_pirate_large` | 64× `gtmutils:ancient_gold_coin` |
| Scavengers | `execute as @p run function factions:raid/bribe_scav_large` | 64× scavenger scrap |
| Lunarians | `execute as @p run function factions:raid/bribe_luna_large` | 64× lunarian coin |
| Automata | `execute as @p run function factions:raid/bribe_auto_large` | 64× automata cheque |
| Republic | `execute as @p run function factions:raid/bribe_republic_large` | 64× doge coin |
| Collective | `execute as @p run function factions:raid/bribe_collective_large` | 64× seeds bundle |
| iMAX | `execute as @p run function factions:raid/bribe_imax_large` | 64× iMAX currency |

### Custom Bribe Durations

If you need a bribe duration other than 1 hour or 6 hours, set the timer directly via a command reward instead of calling a bribe function:

```
# Example: 3 hours (216,000 ticks) of pirate immunity
execute as @p run scoreboard players add @s bribe_timer_pirate 216000
```

**Tick conversion:** 1 second = 20 ticks. Common durations:

| Duration | Ticks |
|---|---|
| 15 minutes | 18,000 |
| 30 minutes | 36,000 |
| 1 hour | 72,000 |
| 3 hours | 216,000 |
| 6 hours | 432,000 |
| 12 hours | 864,000 |
| 24 hours (max) | 1,728,000 |

If setting manually, cap it yourself: `execute if score @s bribe_timer_pirate matches 1728001.. run scoreboard players set @s bribe_timer_pirate 1728000`

---

### Example: Direct Raid Trigger (Boss Fight Quest)

**Scenario:** A quest line culminates in a direct faction raid as a boss encounter — no rep check, no chance roll, guaranteed trigger.

**Quest Setup:**
- Task: Reach a location / interact with an item / etc.
- Reward type: Command
- Command: `execute as @p run function factions:raid/execute_auto`
- Elevated Permission: **True**

`execute_<faction>` bypasses all gates (rep check, bribe timer, cooldown). It immediately starts the 10-second warning sequence followed by entity spawn. The raid tier is still calculated from the player's current rep with that faction.

### Direct Raid Commands (All Factions)

| Faction | FTB Quests Command |
|---|---|
| Pirates | `execute as @p run function factions:raid/execute_pirate` |
| Scavengers | `execute as @p run function factions:raid/execute_scav` |
| Lunarians | `execute as @p run function factions:raid/execute_luna` |
| Automata | `execute as @p run function factions:raid/execute_auto` |
| Republic | `execute as @p run function factions:raid/execute_republic` |
| Collective | `execute as @p run function factions:raid/execute_collective` |
| iMAX | `execute as @p run function factions:raid/execute_imax` |

**To force a specific tier**, set `raid_tier` before calling execute:

```
execute as @p run scoreboard players set @s raid_tier 3
execute as @p run function factions:raid/execute_auto
```

Note: `calc_tier_<faction>` runs inside `execute_<faction>` and will overwrite `raid_tier`. To force a tier, you need to set it AFTER execute starts but BEFORE spawn. The cleanest approach is to call `execute_<faction>` normally and then override the tier before the sequencer reaches 0:

```
execute as @p run function factions:raid/execute_auto
execute as @p run scoreboard players set @s raid_tier 3
```

This works because `execute_<faction>` only sets state to 1 (warning) and starts the 200-tick sequencer. The tier is re-read when `spawn_<faction>` dispatches to `spawn_<faction>_t1/t2/t3` at sequencer=0, but `calc_tier` already ran. Since `raid_tier` is set by `calc_tier` inside `execute`, you override it on the same tick immediately after.

---

### Example: Raid Completion as Quest Requirement

**Scenario:** A quest requires the player to survive a T2 pirate raid before progressing.

**Quest Setup:**
- Task type: **Advancement**
- Advancement: `factions:raids/pirate_raid_t2`

The raid system grants these advancements automatically when a raid succeeds:

| Advancement ID | Meaning |
|---|---|
| `factions:raids/<faction>_raid_t1` | Survived a Tier 1 raid |
| `factions:raids/<faction>_raid_t2` | Survived a Tier 2 raid |
| `factions:raids/<faction>_raid_t3` | Survived a Tier 3 raid |

These are `minecraft:impossible` trigger advancements — they can only be granted by the raid system's `success_<faction>` function, not by normal gameplay.

---

### Example: Set Rep to Make Raids Possible

If your quest line needs to push a player into raid-eligible territory:

```
execute as @p run scoreboard players set @s rep_pirate -256
```

**Rep thresholds for raids:**

| Rep Value | Effect |
|---|---|
| `<= -25` | Passive hostility (mobs attack on sight — existing system) |
| `<= -128` | Raid-eligible (30-min timer evaluations begin) |
| `<= -512` | Guaranteed T2+ raids |
| `<= -2048` | Guaranteed T3 raids |

---

## Debug Commands

### View Player Raid State

Print all raid-related scores for a specific faction:

```
execute as @p run tellraw @s [{"text":"=== PIRATE RAID STATE ===\n","color":"gold"},{"text":"raid_state: ","color":"gray"},{"score":{"name":"@s","objective":"raid_state_pirate"},"color":"white"},{"text":"\nraid_tier: "},{"score":{"name":"@s","objective":"raid_tier"}},{"text":"\nraid_duration: "},{"score":{"name":"@s","objective":"raid_duration"}},{"text":"\nraid_timer: "},{"score":{"name":"@s","objective":"raid_timer_pirate"}},{"text":"\nraid_seq: "},{"score":{"name":"@s","objective":"raid_seq"}},{"text":"\nprovoke_cd: "},{"score":{"name":"@s","objective":"provoke_cd_pirate"}},{"text":"\nbribe_timer: "},{"score":{"name":"@s","objective":"bribe_timer_pirate"}},{"text":"\nrep: "},{"score":{"name":"@s","objective":"rep_pirate"}}]
```

### View All Faction Raid States (Compact)

```
execute as @p run tellraw @s [{"text":"Raid States: ","color":"gold"},{"text":"PIR="},{"score":{"name":"@s","objective":"raid_state_pirate"}},{"text":" SCV="},{"score":{"name":"@s","objective":"raid_state_scav"}},{"text":" LUN="},{"score":{"name":"@s","objective":"raid_state_luna"}},{"text":" AUT="},{"score":{"name":"@s","objective":"raid_state_auto"}},{"text":" REP="},{"score":{"name":"@s","objective":"raid_state_republic"}},{"text":" COL="},{"score":{"name":"@s","objective":"raid_state_collective"}},{"text":" IMX="},{"score":{"name":"@s","objective":"raid_state_imax"}}]
```

### Raid State Values

| Value | Meaning |
|---|---|
| 0 | Idle — no active raid |
| 1 | Warning phase — countdown in progress |
| 2 | Active combat — entities spawned, fight happening |
| 3 | Cleanup — raid ending (transient, resets to 0) |

### Count Active Raid Entities

```
execute as @p at @s run tellraw @s [{"text":"Raid entities nearby: ","color":"gold"},{"text":"PIR="},{"selector":"@e[tag=raid_pirate,distance=..96]"},{"text":" | AUT="},{"selector":"@e[tag=raid_auto,distance=..96]"}]
```

Or just count them:

```
execute if entity @e[tag=raid_pirate] run say Pirate raid entities exist
execute if entity @e[tag=raid_entity] run say Some raid entities exist
execute unless entity @e[tag=raid_entity] run say No raid entities anywhere
```

### Force Instant Raid (Skip Warning Phase)

To bypass the 10-second warning sequence and spawn entities immediately:

```
execute as @p run function factions:raid/execute_pirate
execute as @p run scoreboard players set @s raid_seq 0
```

Setting `raid_seq` to 0 makes the runtime function trigger the spawn on the next tick.

### Force Raid Completion (Instant Win)

Kill all raid entities and trigger the success handler:

```
execute as @p run function factions:raid/success_pirate
```

This grants the advancement, sends the retreat message, and runs cleanup.

### Force Raid Failure

```
execute as @p run function factions:raid/fail_pirate
```

Sends the failure message and runs cleanup. No advancement is granted.

### Emergency Cleanup (Kill Everything)

If something goes wrong and raid entities are stuck:

```
# Kill ALL raid entities server-wide
kill @e[tag=raid_entity]

# Reset a specific player's raid state for all factions
execute as @p run function factions:raid/cleanup_all

# Nuclear option: kill + reset all players
kill @e[tag=raid_entity]
execute as @a run function factions:raid/cleanup_all
```

### Manipulate Timers

```
# Make pirate raid evaluation trigger next tick (resets 30-min timer to 0)
execute as @p run scoreboard players set @s raid_timer_pirate 0

# Clear provoke cooldown so player can provoke again immediately
execute as @p run scoreboard players set @s provoke_cd_pirate 0

# Clear bribe so raids can evaluate again
execute as @p run scoreboard players set @s bribe_timer_pirate 0

# Give player 10 minutes of bribe time (12000 ticks)
execute as @p run scoreboard players set @s bribe_timer_pirate 12000
```

### Inspect Taunters

```
# List all taunter entities nearby
execute as @p at @s run tellraw @s [{"text":"Taunters: ","color":"gold"},{"selector":"@e[tag=raid_taunter,distance=..64]"}]

# Check taunt cooldowns
execute as @e[tag=raid_taunter] run tellraw @a [{"selector":"@s"},{"text":" cd="},{"score":{"name":"@s","objective":"taunt_cd"}}]
```

### Check Dimension

Raids only tick in `deepspace:deep_space`. To verify a player is in the right dimension:

```
execute in deepspace:deep_space as @p run say I am in deep space
```

If nothing prints, the player is in the wrong dimension and raids won't evaluate.

---

## Scoreboard Reference

### Per-Player, Per-Faction Scores

| Objective Pattern | Range | Purpose |
|---|---|---|
| `raid_state_<faction>` | 0-3 | Current raid phase |
| `raid_timer_<faction>` | 0-36000 | Ticks until next evaluation |
| `provoke_cd_<faction>` | 0-12000 | Ticks until provoke allowed again |
| `bribe_timer_<faction>` | 0-1728000 | Ticks of raid immunity remaining |

### Per-Player Global Scores

| Objective | Purpose |
|---|---|
| `raid_tier` | Current raid tier (1, 2, or 3) — set during execute |
| `raid_duration` | Ticks since combat started (resets on cleanup) |
| `raid_mob_count` | Entities spawned for current raid |
| `raid_seq` | Warning phase sequencer countdown |

### Per-Entity Scores (Raid Mobs)

| Objective | Purpose |
|---|---|
| `taunt_cd` | Ticks until next taunt (decremented every 5 ticks) |
| `taunt_index` | Last rolled taunt message index (0-7) |

---

## Tag Reference

### Player Tags

| Tag | Meaning |
|---|---|
| `raid_active_<faction>` | Player has active combat with this faction |
| `raid_warning_<faction>` | Player is in warning/countdown phase |

### Entity Tags

| Tag | Meaning |
|---|---|
| `raid_entity` | Universal marker for any raid-spawned mob |
| `raid_<faction>` | Faction-specific raid marker (e.g. `raid_pirate`) |
| `raid_taunter` | Entity can emit combat taunts |
| `raid_commander` | T3 commander unit (buffed HP + effects) |
| `raid_new` | Temporary tag during spawn (removed same tick) |

---

## Function Reference (Complete)

### Raid Lifecycle

| Function | Context | Purpose |
|---|---|---|
| `factions:raid/execute_<f>` | `as <player>` | Start a raid (warning → countdown → spawn) |
| `factions:raid/runtime_<f>` | `as <player>` | Per-tick raid logic (auto-called by tick) |
| `factions:raid/spawn_<f>` | `as <player>` | Spawn raid entities (auto-called by runtime) |
| `factions:raid/success_<f>` | `as <player>` | Handle raid success (auto-called by runtime) |
| `factions:raid/fail_<f>` | `as <player>` | Handle raid failure (auto-called by runtime) |
| `factions:raid/cleanup_<f>` | `as <player>` | Kill entities + reset state |
| `factions:raid/cleanup_all` | `as <player>` | Cleanup all 7 factions |

### Player-Facing Actions

| Function | Context | Purpose |
|---|---|---|
| `factions:raid/provoke_<f>` | `as <player>` | Provoke a faction (rep gate + cooldown) |
| `factions:raid/bribe_<f>_small` | `as <player>` | 1-hour raid immunity |
| `factions:raid/bribe_<f>_large` | `as <player>` | 6-hour raid immunity |

### Internal (Auto-Called)

| Function | Purpose |
|---|---|
| `factions:raid/tick_player` | Per-player per-tick driver (timers, evaluation, runtime) |
| `factions:raid/evaluate_<f>` | 30-min cycle evaluation (rep + bribe + chance check) |
| `factions:raid/calc_tier_<f>` | Calculate raid tier from rep |
| `factions:raid/_evaluate_roll_<f>` | 50% chance roll helper |
| `factions:raid/_spawn_single_<f>` | Spawn 1 entity + apply raid tags |
| `factions:raid/spawn_<f>_t1` | T1 spawn composition |
| `factions:raid/spawn_<f>_t2` | T2 spawn composition |
| `factions:raid/spawn_<f>_t3` | T3 spawn composition + commander |
| `factions:raid/fx_<f>` | FX dispatcher (routes to tier FX) |
| `factions:raid/fx/tier1` | Light FX (shared) |
| `factions:raid/fx/tier2` | Medium FX (shared) |
| `factions:raid/fx/tier3` | Heavy FX (shared) |
| `factions:raid/name_<f>` | Random name assignment (runs as entity) |
| `factions:raid/fail_on_death` | Fail all active raids on player death |
| `factions:taunt/tick` | Taunt system driver (every 5 ticks) |
| `factions:taunt/fire_<f>` | Roll + send taunt (runs as entity) |

---

## Common Patterns for Quest Designers

### "Survive 3 pirate raids" quest chain

Create 3 sequential quests, each requiring the previous advancement:

- Quest 1: Task = advancement `factions:raids/pirate_raid_t1`
- Quest 2: Task = advancement `factions:raids/pirate_raid_t2`, depends on Quest 1
- Quest 3: Task = advancement `factions:raids/pirate_raid_t3`, depends on Quest 2

### "Choose your enemy" branching quest

Use command rewards to drop rep with the chosen faction:

- Branch A reward: `execute as @p run scoreboard players remove @s rep_pirate 256`
- Branch B reward: `execute as @p run scoreboard players remove @s rep_auto 256`

This makes the chosen faction raid-eligible while leaving the other neutral.

### "Defend the base" repeatable quest

Provoke is ideal for repeatable quests since it has a built-in 10-minute cooldown:

- Task: Submit 1× signal flare item
- Reward: `execute as @p run function factions:raid/provoke_republic`
- Set quest to repeatable with no cooldown (the provoke system handles its own)

### "Peace treaty" quest (stop raids)

Bribe + rep boost to exit raid territory:

```
execute as @p run function factions:raid/bribe_pirate_large
execute as @p run scoreboard players set @s rep_pirate -64
```

This gives 6 hours of immunity AND moves rep above the -128 raid threshold, effectively ending raids from that faction until the player antagonizes them again.

======================================================================

T1 — Pirate Skirmish → 4 mobs, triggered at rep ≤ -512 (or above, as the default)
T2 — Pirate Assault → 8 mobs, triggered at rep ≤ -512
T3 — Pirate Invasion → 14 mobs + commander, triggered at rep ≤ -2048