# Entity Hostility Command Reference
**Factions Datapack — Minecraft Forge 1.20.1**

---

## Overview

The hostility system works on a **tag-then-sync** model. You **never** assign teams directly. Instead:

1. You add/remove **tags** on players or entities using the wrapper functions.
2. The tick system automatically syncs tags → teams every 2 ticks (players) or 5 ticks (mobs).
3. Teams control who mobs target. The whitelist system overrides all hostility.

**Rule:** Always use `factions:hostility/aggro/*` and `factions:hostility/whitelist/*` wrapper functions. Never call `team join` or `team leave` directly.

---

## Factions

| Faction | Mob tag | Aggro timer objective | Hostile team |
|---|---|---|---|
| `auto` | `auto` | `auto_aggro_timer` | `auto_hostile_players` |
| `scav` | `scav` | `scav_aggro_timer` | `scav_hostile_players` |
| `luna` | `luna` | `luna_aggro_timer` | `luna_hostile_players` |
| `republic` | `republic` | `republic_aggro_timer` | `republic_hostile_players` |
| `collective` | `collective` | `collective_aggro_timer` | `collective_hostile_players` |
| `pirate` | `pirate` | `pirate_aggro_timer` | `pirate_hostile_players` |
| `imax` | `imax` | `imax_aggro_timer` | `imax_hostile_players` |

---

## 1. Per-Faction Player Hostility Flags

### Enable hostility (permanent) — faction becomes hostile to a player

```
# Make scav faction permanently hostile to a specific named player
execute as SomePlayer run function factions:hostility/aggro/apply_scav_permanent

# Make multiple factions permanently hostile to nearest player
execute as @p run function factions:hostility/aggro/apply_scav_permanent
execute as @p run function factions:hostility/aggro/apply_pirate_permanent
execute as @p run function factions:hostility/aggro/apply_luna_permanent

# Make ALL factions permanently hostile to a specific player
execute as SomePlayer run function factions:hostility/aggro/apply_auto_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_scav_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_luna_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_republic_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_collective_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_pirate_permanent
execute as SomePlayer run function factions:hostility/aggro/apply_imax_permanent
```

### Disable hostility — remove faction hostility from a player

```
# Remove scav hostility from a specific player (clears both permanent and timed)
execute as SomePlayer run function factions:hostility/aggro/remove_scav

# Remove all faction hostility from nearest player
execute as @p run function factions:hostility/aggro/remove_auto
execute as @p run function factions:hostility/aggro/remove_scav
execute as @p run function factions:hostility/aggro/remove_luna
execute as @p run function factions:hostility/aggro/remove_republic
execute as @p run function factions:hostility/aggro/remove_collective
execute as @p run function factions:hostility/aggro/remove_pirate
execute as @p run function factions:hostility/aggro/remove_imax
```

> **Note:** `remove_*` clears both permanent and timed hostility. It zeros the timer and removes both `*_hostile_to_player` and `*_aggro_timed` tags. The player will be removed from the hostile team on the next Phase 2 sync (within 2 ticks).

---

## 2. Temporary Aggro with Custom Timers

Each faction has a timed-aggro wrapper that applies hostility for **600 ticks (30 seconds)** by default. You can override the timer immediately after calling the wrapper.

# 2.1 when debugging what tags the player has you can use this command to:
> LIST PLAYER TAGS
/tag @p list
> [dbg] get pirate agggro timer
/scoreboard players get @p pirate_aggro_timer
> [dbg] clear pirate tmp hostile
/execute as @p run function factions:hostility/aggro/remove_pirate
/execute as @p run function factions:hostility/aggro/remove_scav



### Apply timed aggro using the default duration (30 seconds)

```
# Make scav hostile to nearest player for 30 seconds
/execute as @p run function factions:hostility/aggro/apply_scav
# Make pirate hostile to nearest player for 30 seconds
/execute as @p run function factions:hostility/aggro/apply_pirate

# Make pirate hostile to a named player for 30 seconds
execute as SomePlayer run function factions:hostility/aggro/apply_pirate
```

### Apply timed aggro with a custom duration

The pattern is: **call the wrapper, then immediately override `<faction>_aggro_timer`** on the same player. Timer unit is **ticks** (20 ticks = 1 second).

```
# Make scav hostile to @p for 10 seconds (200 ticks)
execute as @p run function factions:hostility/aggro/apply_scav
execute as @p run scoreboard players set @s scav_aggro_timer 200

# Make luna hostile to @p for 5 minutes (6000 ticks)
execute as @p run function factions:hostility/aggro/apply_luna
execute as @p run scoreboard players set @s luna_aggro_timer 6000

# Make republic hostile to a specific player for 2 minutes (2400 ticks)
execute as SomePlayer run function factions:hostility/aggro/apply_republic
execute as SomePlayer run scoreboard players set @s republic_aggro_timer 2400

# Make ALL factions hostile to @p for 15 seconds (300 ticks)
execute as @p run function factions:hostility/aggro/apply_auto
execute as @p run scoreboard players set @s auto_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_scav
execute as @p run scoreboard players set @s scav_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_luna
execute as @p run scoreboard players set @s luna_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_republic
execute as @p run scoreboard players set @s republic_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_collective
execute as @p run scoreboard players set @s collective_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_pirate
execute as @p run scoreboard players set @s pirate_aggro_timer 300
execute as @p run function factions:hostility/aggro/apply_imax
execute as @p run scoreboard players set @s imax_aggro_timer 300
```

> **How timed expiry works:** Phase 1 (every tick) decrements each `*_aggro_timer`. When a timer reaches 0, if the player also has the `*_aggro_timed` companion tag (set by the wrapper), both the hostility tag and the timed marker are automatically removed. Permanent aggro (applied with `apply_*_permanent`) never sets `*_aggro_timed`, so it is never auto-removed by the timer system.

---

## 3. Whitelist Immunity (Combat Bypass)

The whitelist system grants complete combat immunity — whitelisted entities are moved to the `whitelist` team, which has no hostile relations with any faction team. This overrides ALL aggro tags. The whitelist is checked **first** in Phase 1, before any aggro sync.

### 3a. Permanent Whitelist (for NPCs, shopkeepers, quest givers)

Apply to any entity that should never be killed by faction mobs: shopkeepers, quest givers, traders, bosses you want to protect, etc.

```
# Whitelist the nearest entity (e.g. run near a shopkeeper)
execute as @e[type=minecraft:villager,tag=shopkeeper,limit=1] run function factions:hostility/whitelist/add

# Whitelist a custom NPC mob by tag (e.g. placed with a tag at spawn)
execute as @e[tag=quest_giver,limit=1] run function factions:hostility/whitelist/add

# Whitelist a specific player (admin, spectator, or protected user)
execute as SomePlayer run function factions:hostility/whitelist/add

# Whitelist ALL entities with a given tag at once
execute as @e[tag=protected_npc] run function factions:hostility/whitelist/add
```

### 3b. Timed Whitelist (temporary immunity, e.g. trade window, cutscene, safe zone)

Grants immunity for **600 ticks (30 seconds)** by default. Override the timer the same way as aggro.

```
# Give @p 30-second whitelist immunity (default)
execute as @p run function factions:hostility/whitelist/add_timed

# Give @p 10-second immunity (200 ticks) — custom override
execute as @p run function factions:hostility/whitelist/add_timed
execute as @p run scoreboard players set @s whitelist_timer 200

# Give a shopkeeper NPC 5-minute immunity during a trade event (6000 ticks)
execute as @e[tag=shopkeeper_npc,limit=1] run function factions:hostility/whitelist/add_timed
execute as @e[tag=shopkeeper_npc,limit=1] run scoreboard players set @s whitelist_timer 6000

# Give immunity to ALL players currently near a safe zone
execute as @a[x=100,y=64,z=200,distance=..20] run function factions:hostility/whitelist/add_timed
```

### 3c. Remove Whitelist Immunity

```
# Remove immunity from a player (they will re-enter normal combat on next sync)
execute as SomePlayer run function factions:hostility/whitelist/remove

# Remove immunity from a specific NPC
execute as @e[tag=quest_giver,limit=1] run function factions:hostility/whitelist/remove

# Remove immunity from all NPCs with a tag
execute as @e[tag=protected_npc] run function factions:hostility/whitelist/remove
```

> **Note:** After `whitelist/remove`, the entity's faction tags are unchanged. It will be re-assigned to its original faction team by the next mob sync cycle (Phase 3, within 5 ticks). There is no need to re-tag the mob's faction.

---

## 4. Combined Patterns

### Aggro + whitelist: trigger combat, then grant immunity after a delay (via function chain)

The cleanest way to do this is with two separate command blocks or functions — one to apply aggro, one to apply whitelist after a delay using a scoreboard timer of your own:

```
# Step 1: Apply timed aggro (e.g. player stole from scav)
execute as SomePlayer run function factions:hostility/aggro/apply_scav
execute as SomePlayer run scoreboard players set @s scav_aggro_timer 400

# Step 2 (called later, e.g. after a quest resolves): make the NPC immune
execute as @e[tag=scav_boss,limit=1] run function factions:hostility/whitelist/add
```

### Entity that starts hostile but must never be killed (e.g. boss NPC, bounty target)

```
# Apply permanent whitelist to the entity — no faction mob will target it
execute as @e[tag=bounty_target,limit=1] run function factions:hostility/whitelist/add

# When the quest resolves and the "kill" should be allowed:
execute as @e[tag=bounty_target,limit=1] run function factions:hostility/whitelist/remove
```

### Making a player hostile to one faction while whitelisted to another

```
# Player is hostile to scav but immune to luna (e.g. luna faction quest)
execute as SomePlayer run function factions:hostility/aggro/apply_scav_permanent
execute as SomePlayer run function factions:hostility/whitelist/add
# Note: whitelist overrides ALL faction combat, including scav.
# Whitelist is a global bypass — it cannot be per-faction.
# For per-faction protection without global immunity, use team-level settings
# via /team modify <team> friendlyFire or hostility matrix configuration.
```

---

## 5. Quick Reference: Timer Conversions

| Ticks | Seconds | Minutes |
|---|---|---|
| 20 | 1s | — |
| 100 | 5s | — |
| 200 | 10s | — |
| 400 | 20s | — |
| 600 | 30s | — (default) |
| 1200 | 60s | 1 min |
| 2400 | 120s | 2 min |
| 6000 | 300s | 5 min |
| 12000 | 600s | 10 min |
| 72000 | 3600s | 1 hour |

---

## 6. How the Sync Pipeline Works (Summary)

Understanding this prevents mistakes:

```
Your command (add tag)
    ↓
Phase 1 — every tick
  • Whitelist tag → whitelist team (immediate)
  • Timers decremented
  • Expired timed aggro/whitelist tags removed

Phase 2 — every 2 ticks
  • *_hostile_to_player tag → *_hostile_players team (player sync)
  • Reverse sync: players without tag leave hostile team

Phase 3 — every 5 ticks
  • Mob faction tag → faction team (near ≤96 blocks)
  • Reverse sync: mobs without tag leave faction team

Slow phase — every 10 ticks
  • Far mob sync (97–192 blocks)
```

**Max latency from tag change to behaviour change:**
- Player hostility: ~2 ticks (0.1s)
- Mob faction sync (near): ~5 ticks (0.25s)
- Mob faction sync (far): ~10 ticks (0.5s)
- Whitelist: ~1 tick (nearly instant — Phase 1 runs first)
