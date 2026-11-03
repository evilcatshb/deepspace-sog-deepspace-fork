# Faction Hostility & Aggro System Specification (Minecraft 1.20.1)

## Overview

This document defines the **core faction hostility, player aggro, and global immunity system** for the modpack.
This system is **authoritative and non-negotiable** and must be implemented exactly as specified.

The system is built on four layers:

1. **Tags (Source of Truth)**
2. **Teams (AI Layer Only)**
3. **Tick-Based Sync System**
4. **Global Whitelist Override System (Highest Priority Immunity Layer)**

It supports:

* Faction vs faction combat (matrix-driven)
* Per-faction player hostility flags
* Temporary aggro with timers
* Temporary and permanent whitelist immunity
* Fully automated syncing (no manual team assignment required)

---

# 1. DESIGN PRINCIPLES

### 1.1 Source of Truth

* **ALL logic originates from tags**
* Teams must NEVER be manually assigned outside the sync system

---

### 1.2 Global Override Priority

System priority order (highest → lowest):

1. **Whitelist System (absolute immunity override)**
2. **Player Hostility Tags**
3. **Faction Hostility Matrix**
4. **Neutral Behavior**

---

### 1.3 Player Handling

* Players MUST NEVER be assigned to faction teams
* Players ONLY use:

  * hostility tags
  * timers
  * whitelist override (optional immunity state)

---

### 1.4 Mob Handling

* Mobs are assigned faction identity via tags
* Teams are automatically synced from tags
* Whitelist overrides ALL mob behavior regardless of faction

---

# 2. FACTION DEFINITIONS

## 2.1 Entity Faction Tags

All mobs MUST use one of the following tags:

```
scav
luna
auto
imax
pirate
republic
collective
```

---

## 2.2 Player Hostility Tags

Each faction has a corresponding player hostility tag:

```
scav_hostile_to_player
luna_hostile_to_player
auto_hostile_to_player
republic_hostile_to_player
collective_hostile_to_player
pirate_hostile_to_player
imax_hostile_to_player
whitelisted
```

---

# 3. TEAM DEFINITIONS (AI LAYER ONLY)

## 3.1 Faction Teams (Mobs Only)

```
scav
luna
auto
imax
pirate
republic
collective
```

---

## 3.2 Player Hostility Teams (Per Faction)

```
scav_hostile_players
luna_hostile_players
auto_hostile_players
imax_hostile_players
pirate_hostile_players
republic_hostile_players
collective_hostile_players
```

---

## 3.3 Whitelist Team

```
whitelist
```

* MUST be fully neutral
* MUST NOT be part of any faction matrix

---

# 4. FACTION HOSTILITY MATRIX

## 4.1 Rules

* ALL factions are **neutral by default**
* ONLY explicitly defined hostility applies
* Entities with `whitelisted` tag are excluded from ALL matrix behavior

---

## 4.2 Matrix Definition

Pirates hostile to:

* Republic
* Lunarians
* Collective

Pirates NOT hostile to:

* Scavengers

Republic hostile to:

* Pirates

Collective hostile to:

* Automata
* Pirates

Automata hostile to:

* Pirates

Automata NOT hostile to:

* Collective (should flee if possible)

Scavengers NOT hostile to:

* Pirates

---

## 4.3 Team Configuration Requirements

Pirates:

```
hostileTeams:
- republic
- luna
- collective
```

Republic:

```
hostileTeams:
- pirate
```

Collective:

```
hostileTeams:
- auto
- pirate
```

Automata:

```
hostileTeams:
- pirate
```

Scavengers:

```
(none)
```

Lunarians:

```
(no hostility defined unless extended later)
```

IMAX:

```
(no hostility defined)
```

---

## 4.4 FACTION HOSTILITY MATRIX — IMPLEMENTATION DETAILS (REQUIRED)

### 4.4.1 Design Rules

* Teams are the ONLY mechanism used for faction vs faction hostility
* Hostility is NOT assumed to be bidirectional
* All factions are neutral by default
* Whitelisted entities are excluded from ALL matrix behavior

---

### 4.4.2 Initialization Function

```
/function factions:init_matrix
```

Must:

* Clear previous hostility
* Reapply full matrix exactly

---

### 4.4.3 Reset All Hostility

```
team modify scav hostileTeams []
team modify luna hostileTeams []
team modify auto hostileTeams []
team modify imax hostileTeams []
team modify pirate hostileTeams []
team modify republic hostileTeams []
team modify collective hostileTeams []
```

---

### 4.4.4 Apply Hostility Matrix

Pirates:

```
team modify pirate hostileTeams add republic
team modify pirate hostileTeams add luna
team modify pirate hostileTeams add collective
```

Republic:

```
team modify republic hostileTeams add pirate
```

Collective:

```
team modify collective hostileTeams add auto
team modify collective hostileTeams add pirate
```

Automata:

```
team modify auto hostileTeams add pirate
```

Scavengers:

```
# No hostile teams
```

Lunarians:

```
# No hostile teams defined
```

IMAX:

```
# No hostile teams defined
```

---

### 4.4.5 Player Hostility Integration (CRITICAL)

```
team modify scav hostileTeams add scav_hostile_players
team modify luna hostileTeams add luna_hostile_players
team modify auto hostileTeams add auto_hostile_players
team modify imax hostileTeams add imax_hostile_players
team modify pirate hostileTeams add pirate_hostile_players
team modify republic hostileTeams add republic_hostile_players
team modify collective hostileTeams add collective_hostile_players
```

---

### 4.4.6 Final Combined Example (Automata)

```
team modify auto hostileTeams add pirate
team modify auto hostileTeams add auto_hostile_players
```

---

### 4.4.7 Execution Requirements

* Runs on datapack load
* Runs on server start
* Runs after manual modification
* MUST NOT run every tick

---

### 4.4.8 Validation Requirements

Pirates attack:

* Republic
* Lunarians
* Collective

Republic attacks:

* Pirates

Collective attacks:

* Automata
* Pirates

Automata attacks:

* Pirates ONLY

Scavengers:

* No factions

Lunarians:

* No factions

IMAX:

* No factions

---

### 4.4.9 Failure Conditions

* Any faction attacks undefined faction
* Missing matrix hostility
* Implicit hostility exists
* Whitelisted entities are attacked

---

# 5. PLAYER AGGRO SYSTEM

When a player has a tag:

```
<FACTION>_hostile_to_player
```

They are added to corresponding hostile team.

---

## Example

```
tag @p add auto_hostile_to_player
```

---

## Removal

```
tag @p remove auto_hostile_to_player
```

---

# 6. TEMPORARY AGGRO SYSTEM (TIMERS)

## Scoreboards

```
auto_aggro_timer
scav_aggro_timer
luna_aggro_timer
republic_aggro_timer
collective_aggro_timer
pirate_aggro_timer
imax_aggro_timer
```

---

## Apply

```
tag @p add auto_hostile_to_player
scoreboard players set @p auto_aggro_timer 600
```

---

## Tick down

```
scoreboard players remove @a[scores={auto_aggro_timer=1..}] auto_aggro_timer 1
```

---

## Remove

```
execute as @a[scores={auto_aggro_timer=0}] run tag @s remove auto_hostile_to_player
```

---

# 7. SYNC SYSTEM

Whitelist runs FIRST.

## Mob sync

```
team join auto @e[tag=auto,tag=!whitelisted]
team join scav @e[tag=scav,tag=!whitelisted]
team join luna @e[tag=luna,tag=!whitelisted]
team join imax @e[tag=imax,tag=!whitelisted]
team join pirate @e[tag=pirate,tag=!whitelisted]
team join republic @e[tag=republic,tag=!whitelisted]
team join collective @e[tag=collective,tag=!whitelisted]
```

---

## Player sync

(all include tag=!whitelisted)

```
team join auto_hostile_players @a[tag=auto_hostile_to_player,tag=!whitelisted]
team join scav_hostile_players @a[tag=scav_hostile_to_player,tag=!whitelisted]
team join luna_hostile_players @a[tag=luna_hostile_to_player,tag=!whitelisted]
team join republic_hostile_players @a[tag=republic_hostile_to_player,tag=!whitelisted]
team join collective_hostile_players @a[tag=collective_hostile_to_player,tag=!whitelisted]
team join pirate_hostile_players @a[tag=pirate_hostile_to_player,tag=!whitelisted]
team join imax_hostile_players @a[tag=imax_hostile_to_player,tag=!whitelisted]
```

---

# 8. TEAM HOSTILITY CONFIGURATION

```
team modify auto hostileTeams add auto_hostile_players
team modify scav hostileTeams add scav_hostile_players
team modify luna hostileTeams add luna_hostile_players
team modify republic hostileTeams add republic_hostile_players
team modify collective hostileTeams add collective_hostile_players
team modify pirate hostileTeams add pirate_hostile_players
team modify imax hostileTeams add imax_hostile_players
```

---

# 9. USER COMMAND INTERFACE

```
tag @p add auto_hostile_to_player
```

```
tag @p remove auto_hostile_to_player
```

```
tag @p add pirate_hostile_to_player
scoreboard players set @p pirate_aggro_timer 1200
```

---

# 10. GLOBAL WHITELIST SYSTEM

## Rules

* Highest priority override
* Immune to all factions
* Preserves all faction tags

---

## Tag

```
whitelisted
```

---

## Team

```
team add whitelist
team modify whitelist hostileTeams []
```

---

## Timer

```
scoreboard objectives add whitelist_timer dummy
```

Apply:

```
tag @p add whitelisted
scoreboard players set @p whitelist_timer 600
```

Tick:

```
scoreboard players remove @e[scores={whitelist_timer=1..}] whitelist_timer 1
```

Remove:

```
execute as @e[scores={whitelist_timer=0}] run tag @s remove whitelisted
```

---

## Sync

```
team join whitelist @e[tag=whitelisted]
team leave scav @e[tag=whitelisted]
team leave luna @e[tag=whitelisted]
team leave auto @e[tag=whitelisted]
team leave imax @e[tag=whitelisted]
team leave pirate @e[tag=whitelisted]
team leave republic @e[tag=whitelisted]
team leave collective @e[tag=whitelisted]
```

---

## Behavior

* Fully immune to all factions
* Removed from all combat systems
* Rejoins correctly when removed

---

## Failure Conditions

* Whitelisted entities are attacked
* Whitelist ignored by sync system
* Whitelist does not override aggro

---

# 11. Performance Optimization / Scaling Considerations

This section defines required optimizations to ensure the faction system remains stable and performant at **1000+ entities** (including mobs and players) without introducing tick lag or command overload.

---

## 11.1 Core Performance Principles

1. **Never perform full-world scans every tick**
2. **Minimize selector usage with broad entity ranges**
3. **Separate logic into phased execution**
4. **Whitelist check must be cheapest possible operation**
5. **Avoid redundant team reassignment**
6. **Prefer tag checks over team queries where possible**

---

## 11.2 Tick Phase Splitting (CRITICAL)

Split the system into **staggered tick phases** instead of running all logic every tick.

### Recommended structure:

```
tick.mcfunction
```

---

### Phase 1 (Every Tick - Lightweight Only)

* Whitelist enforcement
* Timer countdowns

```mcfunction
# WHITELIST FIRST (CHEAP FILTER)
team join whitelist @e[tag=whitelisted,limit=200]

# TIMER SYSTEMS
scoreboard players remove @a[scores={auto_aggro_timer=1..}] auto_aggro_timer 1
scoreboard players remove @a[scores={scav_aggro_timer=1..}] scav_aggro_timer 1
```

---

### Phase 2 (Every 2–5 Ticks - Medium Cost)

* Player hostility sync
* Hostility team assignment

```mcfunction
team join auto_hostile_players @a[tag=auto_hostile_to_player,tag=!whitelisted]
team join scav_hostile_players @a[tag=scav_hostile_to_player,tag=!whitelisted]
```

---

### Phase 3 (Every 5–10 Ticks - Heavy Operations)

* Mob faction syncing
* Cleanup operations

```mcfunction
team join auto @e[tag=auto,tag=!whitelisted,limit=300]
team join scav @e[tag=scav,tag=!whitelisted,limit=300]
```

---

## 11.3 Selector Optimization Rules

### REQUIRED RULES:

### 1. Always use `limit`

Prevent full entity scans:

```mcfunction
@e[tag=auto,limit=200]
```

---

### 2. Always use tag-first filtering

GOOD:

```mcfunction
@e[tag=auto,tag=!whitelisted]
```

BAD:

```mcfunction
@e[type=zombie]
```

---

### 3. Avoid repeated selectors in same tick

BAD:

```mcfunction
team join auto @e[tag=auto]
team leave auto @e[tag=auto]
```

GOOD:

```mcfunction
# only join OR leave based on state logic
```

---

## 11.4 Whitelist Optimization (CRITICAL PATH)

Whitelist must be **O(1) behavior equivalent** in practice.

### RULES:

* Always check `tag=!whitelisted` first
* Never run expensive logic before whitelist exclusion
* Whitelist entities should be skipped immediately

Example:

```mcfunction
@e[tag=auto,tag=!whitelisted]
```

---

## 11.5 Reduce Team Rewrites (IMPORTANT)

Do NOT reassign teams unnecessarily.

### BAD (causes lag):

```mcfunction
team join auto @e[tag=auto]
```

### BETTER:

Only assign when needed OR rely on periodic sync (5–10 ticks)

---

## 11.6 Chunk-Based Processing (ADVANCED OPTIMIZATION)

For very large worlds:

Split processing by distance:

```mcfunction
@e[tag=auto,distance=..96]
@e[tag=auto,distance=97..192]
```

Recommended:

* Near player: every tick
* Mid range: every 5 ticks
* Far range: every 10–20 ticks

---

## 11.7 Aggro System Optimization

### Replace per-player checks with team-based targeting

Instead of:

```mcfunction
@e[tag=auto,nbt={...}]
```

Use:

```mcfunction
@e[team=auto_hostile_players]
```

This reduces evaluation cost significantly.

---

## 11.8 Timer System Optimization

All timers MUST:

* Use scoreboard objectives (not tags)
* Use batch decrement operations

GOOD:

```mcfunction
scoreboard players remove @a[scores={auto_aggro_timer=1..}] auto_aggro_timer 1
```

BAD:

```mcfunction
execute as @a run scoreboard players remove @s ...
```

---

## 11.9 System Load Budget (Target)

| System Component | Budget         |
| ---------------- | -------------- |
| Whitelist checks | Ultra low      |
| Timer systems    | Low            |
| Player sync      | Medium         |
| Mob sync         | Medium-high    |
| Matrix init      | Rare (startup) |

---

## 11.10 Failure Conditions

System is considered unstable if:

* TPS drops below 18 with 1000+ entities
* Whitelist checks are delayed
* Sync runs every tick for all entities
* No chunk or distance filtering is used

---

## 11.11 Final Requirement

This system MUST:

* Scale linearly, not exponentially
* Prioritize whitelist filtering above all operations
* Avoid full world entity scans on tick
* Use phased execution for all heavy logic


# 12. FINAL REQUIREMENT

All faction combat logic MUST derive from this system.

Whitelist override takes absolute precedence over all systems.

# 13. Implementation Checklist (Ordered Build Plan)

This checklist describes the **exact implementation order a programmer should follow** to build the system correctly and avoid architectural rework.

Each step references the relevant **spec chapters**.

---

# PHASE 0 — FOUNDATION SETUP

## [ ] 13.1 Create Core Data Structures (Chapter 2, 3)

- [ ] Define all entity faction tags (2.1)
- [ ] Define all player hostility tags (2.2)
- [ ] Create `whitelisted` tag (2.2, 10)
- [ ] Create faction teams (3.1)
- [ ] Create player hostility teams (3.2)
- [ ] Create whitelist team (3.3)

---

# PHASE 1 — FACTION MATRIX CORE

## [ ] 13.2 Initialize Matrix Function (Chapter 4.4.2)

- [ ] Create `/function factions:init_matrix`
- [ ] Ensure function is idempotent (safe to re-run)

---

## [ ] 13.3 Reset Hostility State (Chapter 4.4.3)

- [ ] Clear all `hostileTeams` for all faction teams
- [ ] Ensure deterministic reset before re-application

---

## [ ] 13.4 Apply Faction Hostility Matrix (Chapter 4.4.4)

- [ ] Implement Pirates → Republic / Luna / Collective hostility
- [ ] Ensure Pirates do NOT attack Scavengers
- [ ] Implement Republic → Pirates hostility
- [ ] Implement Collective → Automata + Pirates hostility
- [ ] Implement Automata → Pirates hostility only
- [ ] Ensure Scavengers = neutral
- [ ] Ensure Lunarians = neutral
- [ ] Ensure IMAX = neutral

---

## [ ] 13.5 Apply Player Hostility Integration (Chapter 4.4.5)

- [ ] Add each `*_hostile_players` team to corresponding faction hostility lists
- [ ] Ensure this is applied AFTER matrix reset

---

## [ ] 13.6 Validate Matrix Correctness (Chapter 4.4.8)

- [ ] Confirm all faction vs faction interactions match spec exactly
- [ ] Confirm no implicit hostility exists
- [ ] Confirm whitelist exclusion behavior is respected

---

# PHASE 2 — TICK SYNC SYSTEM

## [ ] 13.7 Build Tick Execution Framework (Chapter 7)

- [ ] Create main tick function entrypoint
- [ ] Ensure execution order control is enforced

---

## [ ] 13.8 Implement Whitelist Priority Layer (Chapter 7 + 10)

- [ ] Whitelist processing runs FIRST in tick
- [ ] Assign `whitelisted` entities to whitelist team
- [ ] Remove whitelisted entities from ALL other teams
- [ ] Ensure whitelist immediately blocks all further logic

---

## [ ] 13.9 Mob → Team Sync (Chapter 7.2)

- [ ] Sync faction tags → faction teams
- [ ] Exclude all `tag=whitelisted` entities
- [ ] Prevent duplicate team assignment spam

---

## [ ] 13.10 Player → Hostility Sync (Chapter 7.4)

- [ ] Sync player hostility tags → hostility teams
- [ ] Exclude whitelist-tagged players
- [ ] Ensure players are NEVER assigned faction teams

---

# PHASE 3 — PLAYER AGGRO SYSTEM

## [ ] 13.11 Implement Hostility Tag Logic (Chapter 5)

- [ ] Add `<faction>_hostile_to_player` tag handling
- [ ] Ensure tag → team sync works correctly

---

## [ ] 13.12 Validate Player Aggro Behavior (Chapter 5.1–5.3)

- [ ] Verify mobs attack tagged players correctly
- [ ] Verify removal stops aggression immediately
- [ ] Verify whitelist overrides player hostility

---

# PHASE 4 — TEMPORARY AGGRO SYSTEM

## [ ] 13.13 Create Scoreboard Timer Objectives (Chapter 6.1)

- [ ] Create all faction aggro timers

---

## [ ] 13.14 Implement Timer Application Logic (Chapter 6.2)

- [ ] Apply hostility tag + timer simultaneously

---

## [ ] 13.15 Implement Tick Countdown System (Chapter 6.3)

- [ ] Reduce all timers every tick (batched operation)

---

## [ ] 13.16 Implement Auto-Removal System (Chapter 6.4)

- [ ] Remove hostility tags when timer reaches 0

---

## [ ] 13.17 Validate Timer + Whitelist Interaction

- [ ] Ensure whitelist overrides active timers
- [ ] Ensure timers cannot reapply aggression during immunity

---

# PHASE 5 — FACTION TEAM HOSTILITY CONFIGURATION

## [ ] 13.18 Apply Hostility Team Mapping (Chapter 8)

- [ ] Link all faction teams → correct hostile player teams
- [ ] Ensure correct target routing per faction

---

# PHASE 6 — COMMAND INTERFACE SYSTEM

## [ ] 13.19 Implement Tag Commands (Chapter 9)

- [ ] Add commands for setting hostility tags
- [ ] Add commands for removing hostility tags
- [ ] Add commands for timed aggression triggers

---

## [ ] 13.20 Validate Command Flow

- [ ] Ensure commands correctly update teams
- [ ] Ensure whitelist overrides all command effects

---

# PHASE 7 — GLOBAL WHITELIST SYSTEM

## [ ] 13.21 Implement Whitelist Tag System (Chapter 10)

- [ ] Implement `whitelisted` tag application
- [ ] Ensure persistence across systems

---

## [ ] 13.22 Implement Whitelist Team (Chapter 10.3)

- [ ] Create whitelist team
- [ ] Ensure `hostileTeams = []` always

---

## [ ] 13.23 Implement Whitelist Timer System (Chapter 10.5)

- [ ] Create whitelist timer objective
- [ ] Apply timed immunity logic
- [ ] Ensure expiration correctly removes whitelist

---

## [ ] 13.24 Enforce Whitelist Priority Sync (Chapter 10.6)

- [ ] Whitelist processing runs BEFORE all systems
- [ ] Remove entity from all faction and hostility teams
- [ ] Ensure absolute immunity enforcement

---

## [ ] 13.25 Validate Whitelist Immunity (Chapter 10.11)

- [ ] No faction can target whitelisted entities
- [ ] No timer or tag can override whitelist
- [ ] No entity remains in combat state while whitelisted

---

# PHASE 8 — PERFORMANCE & SCALING SYSTEM

## [ ] 13.26 Implement Tick Phase Splitting (Chapter 11.2)

- [ ] Phase 1: whitelist + timers
- [ ] Phase 2: player sync
- [ ] Phase 3: mob sync

---

## [ ] 13.27 Apply Selector Optimization Rules (Chapter 11.3)

- [ ] Always use `limit`
- [ ] Always filter `tag=!whitelisted`
- [ ] Prevent redundant selectors

---

## [ ] 13.28 Implement Whitelist Fast Path (Chapter 11.4)

- [ ] Ensure whitelist filtering is the first check in all systems

---

## [ ] 13.29 Reduce Team Rewrite Frequency (Chapter 11.5)

- [ ] Avoid unnecessary `/team join`
- [ ] Only sync when state changes or interval passes

---

## [ ] 13.30 Add Chunk / Distance Scaling (Chapter 11.6)

- [ ] Split entity processing by distance tiers
- [ ] Reduce far-range processing frequency

---

## [ ] 13.31 Optimize Aggro System (Chapter 11.7)

- [ ] Replace NBT logic with team-based targeting

---

## [ ] 13.32 Optimize Timer System (Chapter 11.8)

- [ ] Ensure batch scoreboard operations only

---

## [ ] 13.33 Validate Performance Targets (Chapter 11.9–11.10)

- [ ] Stable performance at 1000+ entities
- [ ] No TPS degradation under load
- [ ] No full-world scans per tick

---

# PHASE 9 — FINAL SYSTEM VALIDATION

## [ ] 13.34 Full Integration Test (Chapter 12)

- [ ] All faction relationships match matrix exactly
- [ ] Player hostility works as expected
- [ ] Timer system works correctly
- [ ] Whitelist overrides ALL systems

---

## [ ] 13.35 Failure Condition Verification

- [ ] No undefined faction aggression exists
- [ ] No implicit hostility exists
- [ ] Whitelist cannot be bypassed under any condition

---

# FINAL DELIVERY REQUIREMENT

System is ONLY considered complete when:

- [ ] All chapters implemented in correct order
- [ ] Whitelist is highest-priority system in execution order
- [ ] No manual team assignment exists outside sync system
- [ ] System scales cleanly to 1000+ entities without TPS degradation