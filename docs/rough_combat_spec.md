Below is a **complete implementation-ready technical specification** for your Minecraft 1.20.1 faction war datapack system. This version is fully consolidated, includes **all per-faction timers/state systems**, renamed tags, full **NPC name pools**, and full **radio message pools (8 each per faction)**.

All identifiers remain **pseudocode only** and must be mapped to your actual datapack implementation.

---

# 📡 FACTION WAR SYSTEM — COMPLETE IMPLEMENTATION SPEC (MC 1.20.1)

---

# ⚠️ GLOBAL NOTE

All:

* function names
* scoreboard variables
* entity tags
* advancement IDs

are **pseudocode abstractions only**.

---

# 🧠 1) FACTION SYSTEM OVERVIEW

Each faction operates independently per player using the following systems:

Below is a **drop-in snippet** for your technical spec that defines **radio colors, chat formats, and taunt formatting rules** in a way a programming AI can implement directly.

---

# 📡 RADIO & CHAT COLOR / FORMAT SPECIFICATION

---

## 🎨 1) COLOR STANDARDIZATION

### 🧠 GLOBAL RULE

All faction communications MUST follow consistent color formatting to ensure:

* readability
* faction identity clarity
* UI consistency across chat + radio

---

## 🎯 1.1 BASE COLOR DEFINITIONS

| Element           | Color            |
| ----------------- | ---------------- |
| `[Radio]` prefix  | `yellow`         |
| `[Faction]` badge | faction-specific |
| Message text      | `white`          |
| NPC Name (taunts) | `white`          |

---

## 🎨 1.2 FACTION COLOR MAP

| Faction    | Color        |
| ---------- | ------------ |
| Pirates    | `red`        |
| Scavengers | `dark_gray`       |
| Lunarians  | `yellow`     |
| Automata   | `light_gray` |
| Collective | `green`      |
| Republic   | `aqua/light_blue`       |

---

# 📡 2) RADIO MESSAGE FORMAT

## 🧠 PURPOSE

Used for:

* warnings
* announcements
* bribe responses
* retreat messages
* provoke responses

---

## 📦 FORMAT TEMPLATE

```json id="rf1"
{
  "text": "[Radio] ",
  "color": "yellow",
  "extra": [
    {
      "text": "[FACTION] ",
      "color": "<faction_color>"
    },
    {
      "text": "MESSAGE CONTENT",
      "color": "white"
    }
  ]
}
```

---

## 🧾 EXAMPLE (PIRATES)

```json id="rf2"
{
  "text": "[Radio] ",
  "color": "yellow",
  "extra": [
    {"text":"[Pirates] ","color":"red"},
    {"text":"WE ARE APPROACHING YOUR LOCATION ☠️🔪","color":"white"}
  ]
}
```

---

# 🧍 3) TAUNT MESSAGE FORMAT (ENTITY CHAT)

## 🧠 PURPOSE

Used by **entities with tag `taunter`** during combat.

* represents localized speech
* includes NPC identity
* NOT prefixed with `[Radio]`

---

## 📦 FORMAT TEMPLATE

```json id="tf1"
{
  "text": "[FACTION] ",
  "color": "<faction_color>",
  "extra": [
    {
      "text": "NPC_NAME: ",
      "color": "white"
    },
    {
      "text": "TAUNT MESSAGE",
      "color": "white"
    }
  ]
}
```

---

## 🧾 EXAMPLE (SCAVENGERS)

```json id="tf2"
{
  "text": "[Scavengers] ",
  "color": "gray",
  "extra": [
    {"text":"Bolt: ","color":"white"},
    {"text":"WE’LL TEAR THIS APART 🪓⛏️","color":"white"}
  ]
}
```

---

# ⚔️ 4) TAUNTER SYSTEM RULES

## 🧠 REQUIREMENT

Only entities with:

```text id="rule1"
tag = taunter
```

are allowed to:

* emit taunts
* broadcast combat dialogue

---

## 🚫 NON-TAUNTER ENTITIES

Entities without `taunter`:

* MUST NOT send chat messages
* MUST remain silent during combat

---

# ⏱️ 5) TAUNT RATE LIMITING

To prevent spam:

```text id="rule2"
Each entity may send 1 taunt every X seconds (recommended: 5–10s)
```

---

## PSEUDOCODE

```mcfunction id="tf3"
if entity has tag taunter AND taunt_cooldown == 0:
    send taunt
    set cooldown
```

---

# 📡 6) RADIO VS TAUNT DIFFERENCE (CRITICAL)

| Type  | Prefix            | Scope  | Source         |
| ----- | ----------------- | ------ | -------------- |
| Radio | `[Radio]`         | global | system/faction |
| Taunt | `[Faction] Name:` | local  | entity         |

---

## 🧠 DESIGN RULE

* Radio = **broadcast/system-level communication**
* Taunt = **diegetic NPC speech**

---

# 🧾 7) IMPLEMENTATION EXAMPLE

```mcfunction id="ex1"
# Radio
tellraw @a <radio_json>

# Taunt
tellraw @a[distance=..30] <taunt_json>
```

---

# 🧠 FINAL RESULT

This system ensures:

* consistent faction identity via color
* clear distinction between system vs NPC dialogue
* scalable formatting for future factions
* clean integration with your taunter-based combat system

## REQUIRED PER FACTION STATE TRACKERS

Each faction MUST have:

* `reputation`
* `raid_timer`
* `provoke_cooldown`
* `bribe_timer`
* `raid_state`

---

# 🧩 2) SCOREBOARD STRUCTURE (REQUIRED)

## GLOBAL OBJECTIVES

```mcfunction id="sc1"
scoreboard objectives add raid_timer dummy
scoreboard objectives add provoke_cooldown dummy
scoreboard objectives add bribe_timer dummy
scoreboard objectives add raid_state dummy
scoreboard objectives add raid_tier dummy
```

---

## FACTION REP TRACKING

```mcfunction id="sc2"
scoreboard objectives add rep_pirate dummy
scoreboard objectives add rep_scavenger dummy
scoreboard objectives add rep_lunarian dummy
scoreboard objectives add rep_automata dummy
scoreboard objectives add rep_collective dummy
scoreboard objectives add rep_republic dummy
```

---

# 🏷️ 3) ENTITY TAG SYSTEM (UPDATED)

## CORE TAGS

* `raid`
* `faction_<name>`
* `noai`
* `taunter` ← **NEW (replaces talker)**
* `raid_commander`
* `raid_spawn`

---

## IMPORTANT CHANGE

### OLD:

* `talker` → passive dialogue NPC

### NEW:

* `taunter` → aggressive combat radio/taunts enabled

---

## IMPLEMENTATION RULE

```mcfunction id="tg1"
if entity has tag taunter:
    enable combat radio messages
else:
    no dialogue or idle speech
```

---

# ⏱️ 4) PER-FACTION TIMERS

Each faction MUST independently track:

---

## 4.1 RAID TIMER (30 min cycle)

```mcfunction id="tm1"
if raid_timer >= 36000:
    reset raid_timer
    call faction:evaluate_<faction>_raid
```

---

## 4.2 PROVOKE COOLDOWN (10 min)

```mcfunction id="tm2"
if provoke_cooldown > 0:
    block provoke raid
else:
    allow provoke
```

---

## 4.3 BRIBE TIMER (UP TO 24 HOURS)

```mcfunction id="tm3"
if bribe_timer > 0:
    prevent raid spawn
    decrease over time
```

---

## 4.4 RAID STATE

| State | Meaning     |
| ----- | ----------- |
| 0     | idle        |
| 1     | warning     |
| 2     | active raid |
| 3     | cleanup     |

---

# ⚔️ 5) RAID EVALUATION LOGIC

## FUNCTION:

```mcfunction id="ev1"
faction:evaluate_<faction>_raid
```

---

## LOGIC:

```text id="ev2"
IF rep <= -128:
    IF bribe_timer <= 0:
        roll 50% chance
        IF success:
            call raid_execute_<faction>
```

---

# 💰 6) PROVOKE SYSTEM

## FUNCTION:

```mcfunction id="pr1"
faction:provoke_<faction>
```

---

## CASE A — FRIENDLY (> -128)

```text id="pr2"
- subtract 8 rep
- send passive aggressive radio message
- NO RAID
```

---

## CASE B — HOSTILE (≤ -128)

```text id="pr3"
IF provoke_cooldown == 0:
    trigger raid_execute_<faction>
    set provoke_cooldown = 12000
ELSE:
    deny action
```

---

# 💰 7) BRIBE SYSTEM

## FUNCTION:

```mcfunction id="br1"
faction:add_bribe_<faction>_<value>
```

---

## EFFECT:

```text id="br2"
value * 72000 ticks added to bribe_timer
cap at 1728000 ticks (24h)
```

---

# ⚔️ 8) RAID EXECUTION CORE (SINGLE FUNCTION)

## FUNCTION:

```mcfunction id="rx1"
faction:raid_execute_<faction>
```

---

## FULL FLOW:

```text id="rx2"
1. set raid_state = 1
2. radio warning sequence
3. FX phase (tier-based)
4. safe spawn entities
5. assign names
6. apply faction tags
7. add tag taunter
8. enable AI (remove noAI)
9. start combat
```

---

# 💥 9) SAFE SPAWN SYSTEM

Preferred method:

```mcfunction id="sp1"
execute at player run spreadplayers ~ ~ 10 25 false @e[tag=raid_spawn]
```

Fallback:

* air block check loop
* retry offset if invalid

---

# 🤖 10) AI ACTIVATION

```mcfunction id="ai1"
data merge entity @e[tag=raid] {NoAI:0b}
remove tag noai
```

---

# 🧍 11) FACTION NPC NAME POOLS (FULL LIST)

Each entity gets one randomly assigned name.

---

## 🟥 PIRATES

Rook, Vex, Krag, Blacktooth, Cutter, Sable, Nyx, Drift

---

## ⚙️ SCAVENGERS

Scrap, Wrench, Bolt, Rusty, Clank, Patch, Dusty, Gear

---

## 🌕 LUNARIANS

Zhak, Woon, Shira, Luma, Vek, Koro, Yinn, Fae

---

## 🤖 AUTOMATA

UNIT-7, NODE-3, AX-11, CORE-DELTA, MK-IX, RX-4, PX-2, KX-88

---

## 🌿 COLLECTIVE

River, Sage, Willow, Fern, Indigo, Cedar, Sky, Sol

---

## 📚 REPUBLIC

Cassian, Tiber, Varro, Decimus, Aurelios, Livius, Corvin, Septimus

---

# 📡 12) RADIO MESSAGE SYSTEM (8 PER FACTION)

---

# 🟥 PIRATES (RED + SKULLS ☠️🔪💀)

1. YOU WILL NOT LEAVE ☠️🔪
2. WE TAKE EVERYTHING ☠️💀
3. RUN WHILE YOU CAN 🔪☠️
4. YOU’RE ALREADY DEAD ☠️💀
5. BLOOD FOR THE SEA 🔪💀
6. NO MERCY ☠️🔪
7. WE REMEMBER YOU 💀☠️
8. THIS IS OUR HUNT ☠️🔪

---

# ⚙️ SCAVENGERS (TOOLS 🪓⛏️🔧)

1. WE DISMANTLE YOU 🪓
2. EVERYTHING IS SCRAP ⛏️
3. BREAK IT DOWN 🔧
4. NOTHING WASTED 🪓
5. YOU’RE MATERIAL ⛏️
6. WE REBUILD YOU 🔧
7. STRIP IT APART 🪓
8. SALVAGE INITIATED ⛏️

---

# 🌕 LUNARIANS (MIXED LANGUAGE)

1. YAK SHIN WOO FALL
2. SHIN FOO DARKNESS
3. WOO YAK SILENCE
4. FOO SHIN VOID
5. YAK WOO STARS DIE
6. SHIN SHIN BREAK
7. FOO YAK ENDING
8. WOO SHIN LUNAR FALL

---

# 🤖 AUTOMATA (ALL CAPS + SCAN 🔴📡)

1. TARGET LOCKED 🔴📡
2. SYSTEM PURGE 🔴📡
3. OBJECTIVE ELIMINATION 🔴📡
4. THREAT DETECTED 🔴📡
5. PROCESSING DESTRUCTION 🔴📡
6. UNIT ACTIVATED 🔴📡
7. ERROR: SURVIVAL 🔴📡
8. TERMINATION REQUIRED 🔴📡

---

# 🌿 COLLECTIVE (✨🍀)

1. WE UNDERSTAND YOU ✨🍀
2. PEACE IS BETTER ✨🍀
3. YOU ARE MISGUIDED ✨🍀
4. HARMONY IS LOST ✨🍀
5. WE STILL CARE ✨🍀
6. THIS IS SAD ✨🍀
7. LET IT GROW ✨🍀
8. YOU WILL RETURN ✨🍀

---

# 📚 REPUBLIC (FORMAL 📖📜)

1. CEASE THIS ACTION 📚
2. ORDER REQUIRED 📖
3. COMPLIANCE MANDATED 📜
4. YOU ARE OBSERVED 📚
5. THIS IS FINAL NOTICE 📖
6. STAND DOWN 📜
7. LAW IS ACTIVE 📚
8. RESTORE ORDER 📖

---

# 🧠 13) TAUNTER SYSTEM (CRITICAL CHANGE)

## RULE:

Only entities with:

```text
tag = taunter
```

can:

* emit combat radio messages
* generate name-based taunts
* broadcast kill events

---

## EXAMPLE TAUNT FORMAT:

```json id="ta1"
"[Pirates] Rook: YOU CANNOT ESCAPE ☠️🔪"
```

---

# 🏁 14) RAID COMPLETION LOGIC

```mcfunction id="end1"
if @e[tag=raid] == 0:
    set raid_state = 3
    grant advancement
    send retreat radio message
    clear hostility tags
```

---

# 🔁 15) FULL SYSTEM LOOP

```text id="loop1"
player interaction
→ rep change
→ faction state update
→ timer OR provoke OR bribe logic
→ raid_execute
→ safe spawn
→ AI enable
→ taunter combat dialogue
→ raid completion check
→ advancement grant
→ FTB quest progression
```
Below is the **updated drop-in spec snippet** with **tier-scaled visual FX integrated into the raid flow**, so larger raids feel significantly more intense while remaining **100% non-destructive (particles + sounds only)**.

---

# ⚠️ RAID EXECUTION FLOW (TIMELINE + SCALED FX SPEC)

---

## 🧠 PURPOSE

Defines the **exact ordered sequence** of events when a raid is triggered, with **FX intensity scaling based on raid tier**.

Ensures:

* cinematic escalation
* clear player feedback
* consistent entity initialization
* scalable difficulty presentation

---

# ⏱️ 1) HIGH-LEVEL FLOW

```text id="flow1"
RAID TRIGGER
→ WARNING MESSAGE
→ DELAY (10s)
→ COUNTDOWN SEQUENCE
→ FX PHASE (SCALED BY TIER)
→ ENTITY SPAWN
→ NAME ASSIGNMENT
→ TAG SETUP (faction + raid + taunter)
→ AI ENABLE
→ COMBAT START
```

---

# 📡 2) STAGE BREAKDOWN (UPDATED)

---

## 🟡 STAGE 1 — INITIAL WARNING

```mcfunction id="rf_start"
tellraw @a {
  "text":"[Radio] ",
  "color":"yellow",
  "extra":[
    {"text":"[Unknown] ","color":"gray"},
    {"text":"⚠️ Unknown signal detected...","color":"white"}
  ]
}
```

---

## ⏱️ STAGE 2 — DELAY (10s)

```mcfunction id="delay1"
schedule function faction:raid_countdown_<faction> 200t
```

---

## 🔴 STAGE 3 — COUNTDOWN (5s)

```text id="cd1"
⚠️ Warning: <FACTION> arriving in 5s
4s → 3s → 2s → 1s → 0s
```

---

## 💥 STAGE 4 — FX PHASE (SCALED)

### 🧠 CORE RULE

FX intensity MUST scale with:

```text id="rule_fx"
raid_tier
```

---

## 🎯 FX TIERS

---

### 🟢 TIER 1 — SMALL RAID

**Goal:** Light disturbance, scouting force

```mcfunction id="fx_t1"
# 2–4 bursts total
particle smoke ~ ~ ~
particle explosion ~ ~ ~
playsound minecraft:entity.generic.explode master @a ~ ~ ~ 0.6 1.2
```

---

### 🟡 TIER 2 — MEDIUM RAID

**Goal:** Noticeable attack wave

```mcfunction id="fx_t2"
# 5–10 bursts, slightly spread
execute as @p at @s run particle explosion ~ ~ ~
execute as @p at @s run particle large_smoke ~ ~ ~

playsound minecraft:entity.generic.explode master @a ~ ~ ~ 0.9 1
playsound minecraft:entity.generic.explode master @a ~ ~ ~ 0.8 0.8
```

---

### 🔴 TIER 3 — LARGE RAID

**Goal:** Heavy assault / war-level event

```mcfunction id="fx_t3"
# 10–20 bursts, randomized around player

execute at @p run particle explosion_emitter ~1 ~ ~
execute at @p run particle explosion_emitter ~-1 ~ ~
execute at @p run particle ash ~ ~ ~

playsound minecraft:entity.generic.explode master @a ~ ~ ~ 1.2 0.8
playsound minecraft:entity.generic.explode master @a ~ ~ ~ 1 0.6
playsound minecraft:entity.ender_dragon.growl master @a ~ ~ ~ 0.6 0.7
```

---

## 🎲 RANDOMIZATION REQUIREMENT

FX MUST NOT all occur at the same position.

Use offsets:

```mcfunction id="fx_rand"
execute at @p positioned ~<random(-5 to 5)> ~ ~<random(-5 to 5)> run particle explosion
```

---

## ⏱️ FX DURATION

| Tier | Duration      |
| ---- | ------------- |
| T1   | ~20–40 ticks  |
| T2   | ~40–80 ticks  |
| T3   | ~80–140 ticks |

---

## ⏱️ POST-FX DELAY BEFORE SPAWN

```mcfunction id="fx_delay"
schedule function faction:spawn_<faction> 20–40t
```

---

# 👾 STAGE 5 — ENTITY SPAWN

Same as before, but MUST occur **after FX completes**.

---

# 🧍 STAGE 6 — NAME ASSIGNMENT

```mcfunction id="name1"
assign random faction name
```

---

# 🏷️ STAGE 7 — TAG SETUP

```mcfunction id="tag1"
tag add raid
tag add faction_<name>
tag add taunter
```

---

# 🤖 STAGE 8 — AI ENABLE

```mcfunction id="ai1"
data merge entity @e[tag=raid] {NoAI:0b}
```

---

# ⚔️ STAGE 9 — COMBAT START

* entities become hostile
* taunts begin
* raid fully active

---

# 📊 3) FULL TIMELINE (WITH FX SCALING)

| Stage     | Time                     |
| --------- | ------------------------ |
| Warning   | 0t                       |
| Delay     | +200t                    |
| Countdown | +100t                    |
| FX        | tier-dependent (20–140t) |
| Spawn     | +20–40t after FX         |
| Combat    | immediate                |

---

# ⚠️ 4) IMPLEMENTATION RULES (UPDATED)

* FX MUST scale with raid tier
* FX MUST use particles + sounds ONLY
* NO TNT or block-breaking mechanics allowed
* FX MUST occur BEFORE entity spawn
* Higher tiers MUST feel visually denser and louder
* Random offsets REQUIRED to avoid visual stacking

---

# 🧠 FINAL RESULT

This upgrade ensures:

> 🧨 Small raids feel like probes
> ⚔️ Medium raids feel like real attacks
> 💀 Large raids feel like full-scale invasions

…without ever risking world damage.

Below is a **drop-in technical spec snippet** that defines:

* 🎲 how random NPC names are assigned
* 🧍 how those names are stored and reused
* 📡 how the `taunter` system selects random taunts
* 🔗 how taunts correctly include the entity’s assigned name

This is written for **direct datapack/KubeJS implementation mapping**.

---

# 🧍 NPC NAME RESOLUTION + TAUNTER SYSTEM SPEC

---

# 🧠 1) PURPOSE

This system ensures:

* every spawned raid entity gets a **persistent random name**
* taunts always reference the **correct entity name**
* taunts are **randomized per faction**
* chat output is consistent with faction formatting rules

---

# 🎲 2) NAME ASSIGNMENT SYSTEM

---

## 🧩 2.1 REQUIREMENT

Each entity MUST:

* receive exactly **one name at spawn**
* store it in:

  * `CustomName` (for display)
  * optional internal storage (score/NBT/tag for logic reuse)

---

## ⚙️ 2.2 NAME POOL STORAGE

Names are defined per faction as a **fixed pool of 8 values**.

Example (pirates):

```text
Rook, Vex, Krag, Blacktooth, Cutter, Sable, Nyx, Drift
```

---

## 🎯 2.3 RANDOM INDEX GENERATION

### Approach:

Assign a random number 1–8:

```mcfunction id="nm1"
scoreboard players random @s name_index 1 8
```

---

## 🧠 2.4 NAME RESOLUTION

Use branching logic:

```mcfunction id="nm2"
execute if score @s name_index matches 1 run data modify entity @s CustomName set value '"Rook"'
execute if score @s name_index matches 2 run data modify entity @s CustomName set value '"Vex"'
...
```

---

## ⚠️ IMPORTANT

```text
CustomName MUST be stored as a JSON string:
'"Name"'
```

---

## 🧾 OPTIONAL: STORE RAW NAME

For easier reuse in taunts:

```mcfunction id="nm3"
data modify entity @s Tags append value "name_Rook"
```

OR store via scoreboard mapping (advanced setups).

---

# 🧍 3) NAME VISIBILITY

## OPTIONAL:

```mcfunction id="nm4"
data merge entity @s {CustomNameVisible:1b}
```

---

# 📡 4) TAUNTER SYSTEM OVERVIEW

---

## 🧠 4.1 REQUIREMENT

Only entities with:

```text
tag = taunter
```

are allowed to:

* send chat taunts
* participate in radio-style combat chatter

---

## ⚙️ 4.2 TAUNT TRIGGER CONDITIONS

Entity must satisfy:

```text
- has tag taunter
- is alive
- is within range of player (recommended: ≤30 blocks)
- taunt cooldown = 0
```

---

## ⏱️ 4.3 TAUNT COOLDOWN SYSTEM

Each entity tracks:

```mcfunction id="tc1"
scoreboard objectives add taunt_cd dummy
```

---

### Decrement per tick:

```mcfunction id="tc2"
scoreboard players remove @e[tag=taunter] taunt_cd 1
```

---

### Trigger condition:

```mcfunction id="tc3"
execute as @e[tag=taunter,scores={taunt_cd=0}] run function faction:taunt_execute
```

---

# 🎲 5) RANDOM TAUNT SELECTION

---

## 🧩 5.1 RANDOM INDEX

```mcfunction id="tt1"
scoreboard players random @s taunt_index 1 8
```

---

## 🧠 5.2 FACTION-SPECIFIC POOLS

Each faction MUST have its own taunt list (8 entries).

---

## ⚙️ 5.3 TAUNT RESOLUTION

Example (pirates):

```mcfunction id="tt2"
execute if score @s taunt_index matches 1 run function faction:taunt_pirate_1
execute if score @s taunt_index matches 2 run function faction:taunt_pirate_2
...
```

---

# 📢 6) TAUNT MESSAGE CONSTRUCTION

---

## 🧠 KEY REQUIREMENT

Taunt MUST include:

* faction badge
* entity name (from CustomName)
* taunt message

---

## ⚙️ 6.1 USING SELECTOR JSON (RECOMMENDED)

Minecraft supports pulling entity names dynamically:

```json
{"selector":"@s"}
```

---

## 🧾 FULL FORMAT

```json
{
  "text": "[Pirates] ",
  "color": "red",
  "extra": [
    {
      "selector": "@s",
      "color": "white"
    },
    {
      "text": ": YOU WILL NOT ESCAPE ☠️🔪",
      "color": "white"
    }
  ]
}
```

---

## 🧠 RESULT

If entity name = Rook:

```text
[Pirates] Rook: YOU WILL NOT ESCAPE ☠️🔪
```

---

# 📡 7) TAUNT EXECUTION FUNCTION

---

## FUNCTION: `faction:taunt_execute`

```mcfunction id="tt3"
# 1. roll taunt index
scoreboard players random @s taunt_index 1 8

# 2. resolve taunt
call faction:taunt_<faction>_<index>

# 3. reset cooldown
scoreboard players set @s taunt_cd 100  # ~5 seconds
```

---

# 📏 8) RANGE LIMITING

To avoid global spam:

```mcfunction id="tt4"
execute as @e[tag=taunter] at @s if entity @p[distance=..30] run function faction:taunt_execute
```

---

# ⚠️ 9) PERFORMANCE RULES

* Do NOT run taunts every tick without cooldown
* Use range checks before execution
* Prefer batching execution per tick

---

# 🧠 10) SYSTEM FLOW SUMMARY

```text
entity spawns
→ assign random name (stored in CustomName)
→ tag entity as taunter
→ entity enters combat
→ taunt cooldown reaches 0
→ random taunt selected
→ message sent using entity name selector
→ cooldown reset
→ repeat while alive
```

---

# 🧠 FINAL RESULT

This system guarantees:

* consistent identity per NPC
* immersive combat dialogue
* correct name usage in chat
* scalable faction-specific personality

---

# 🔁 EXECUTION LOOP (TICK SYSTEM)

---

## 🧠 PURPOSE

Defines the **global driver loop** that powers:

* timers
* raid triggering
* taunter execution
* cleanup checks

---

## 📦 ENTRY FUNCTION

```mcfunction
# faction:tick (called every tick)
execute as @a run function faction:tick_player
```

---

## 👤 PER-PLAYER LOOP

```mcfunction
# faction:tick_player

# decrement timers
scoreboard players remove @s raid_timer 1
scoreboard players remove @s provoke_cooldown 1
scoreboard players remove @s bribe_timer 1

# clamp values (prevent negatives)
execute if score @s provoke_cooldown matches ..-1 run scoreboard players set @s provoke_cooldown 0
execute if score @s bribe_timer matches ..-1 run scoreboard players set @s bribe_timer 0

# evaluate raids
function faction:evaluate_all_factions

# run active raid logic
function faction:raid_runtime_check

# run taunter system
function faction:taunt_tick
```

---

# 🌐 MULTIPLAYER ISOLATION

---

## 🧠 REQUIREMENT

All raid systems MUST be:

```text
PER PLAYER
PER FACTION
```

---

## 🏷️ ENTITY OWNERSHIP TAG

Each spawned entity MUST include:

```mcfunction
tag @s add raid_owner_<player_uuid>
```

---

## 🎯 SELECTOR RULE

ALL entity queries MUST include:

```mcfunction
@e[tag=raid,tag=raid_owner_<player>]
```

---

## 🚫 FORBIDDEN

```mcfunction
@e[tag=raid]  # ❌ NOT ALLOWED (global leak)
```

---

# 💀 FAILURE STATES

---

## 🧠 PURPOSE

Defines when a raid is considered **failed**

---

## ❗ CONDITIONS

```text
- player dies
- player leaves radius (>64 blocks)
- raid duration exceeds limit (e.g. 6000 ticks)
```

---

## ⚙️ FAILURE HANDLER

```mcfunction
# faction:raid_fail_<faction>

tellraw @s <failure_radio_message>

function faction:raid_cleanup_<faction>

scoreboard players set @s raid_state 0
```

---

# ⚔️ COMBAT SCALING (MECHANICAL)

---

## 🧠 PURPOSE

Defines how raid tiers affect **actual difficulty**, not just FX.

---

## 📊 SCALING TABLE

| Tier | Mob Count | Bonus HP | Special      |
| ---- | --------- | -------- | ------------ |
| T1   | 3–5       | +0%      | none         |
| T2   | 6–10      | +25%     | chance elite |
| T3   | 12–20     | +50%     | commander    |

---

## ❤️ HEALTH MODIFIER

```mcfunction
# pseudocode
data merge entity @s {Attributes:[{Name:"generic.max_health",Base:<scaled_value>}]}
```

---

## 👑 COMMANDER RULE

```text
1 entity per T3 raid:
tag = raid_commander
higher HP
possible effects (strength, speed)
```

---

# 📍 SPAWN VALIDATION SYSTEM

---

## 🧠 REQUIREMENT

Entities MUST spawn in **safe, non-blocked locations**

---

## ✔ VALID POSITION RULES

```text
- block at feet = air
- block at head = air
- block below = solid
```

---

## ⚙️ VALIDATION EXAMPLE

```mcfunction
execute if block ~ ~ ~ air if block ~ ~1 ~ air if block ~ ~-1 ~ #solid run summon ...
```

---

## 🔁 RETRY LOGIC

```text
IF invalid:
→ offset position
→ retry up to N times (recommended: 5–10)
```

---

# 🧹 CLEANUP GUARANTEES

---

## 🧠 PURPOSE

Ensure no raid entities persist incorrectly

---

## 📦 CLEANUP FUNCTION

```mcfunction
# faction:raid_cleanup_<faction>

kill @e[tag=raid,tag=faction_<faction>,tag=raid_owner_<player>,distance=..64]

tag @s remove faction_hostile_<faction>
```

---

## 📌 MUST RUN ON:

```text
- raid success
- raid failure
- bribe cancel
- player death
- logout (optional hook)
```

---

# 🚀 ENTRY POINTS (DATAPACK HOOKS)

---

## 📦 LOAD FUNCTION

```json
# data/minecraft/tags/functions/load.json
{
  "values": [
    "faction:init"
  ]
}
```

---

## 🔁 TICK FUNCTION

```json
# data/minecraft/tags/functions/tick.json
{
  "values": [
    "faction:tick"
  ]
}
```

---

## ⚙️ INIT FUNCTION

```mcfunction
# faction:init

# create scoreboards
scoreboard objectives add raid_timer dummy
scoreboard objectives add provoke_cooldown dummy
scoreboard objectives add bribe_timer dummy
scoreboard objectives add raid_state dummy
scoreboard objectives add raid_tier dummy
scoreboard objectives add taunt_cd dummy
```

---

# 🏆 ADVANCEMENT INTEGRATION

---

## 🧠 PURPOSE

Convert raids into **progression milestones**

---

## 📦 ADVANCEMENT FORMAT

```json
{
  "criteria": {
    "raid_complete": {
      "trigger": "minecraft:impossible"
    }
  }
}
```

---

## ⚙️ GRANT ON SUCCESS

```mcfunction
# faction:raid_success_<faction>

execute if score @s raid_tier matches 1 run advancement grant @s only faction:<faction>_raid_t1
execute if score @s raid_tier matches 2 run advancement grant @s only faction:<faction>_raid_t2
execute if score @s raid_tier matches 3 run advancement grant @s only faction:<faction>_raid_t3
```

---

## 📡 SUCCESS MESSAGE

```mcfunction
tellraw @s {
  "text":"[Radio] ",
  "color":"yellow",
  "extra":[
    {"text":"[<Faction>] ","color":"<color>"},
    {"text":"...we are withdrawing... for now.","color":"white"}
  ]
}
```

---

## 🔗 FTB QUEST LINK

FTB Quests MUST use:

```text
Requirement: Advancement completed
```

🧩 SYSTEM COMPLETENESS — MISSING CORE CONTRACTS
🔐 16) STATE OWNERSHIP MODEL
🧠 REQUIREMENT

All faction systems MUST be strictly:

PER PLAYER (UUID-BOUND)
PER FACTION
🏷️ PLAYER IDENTIFIER STORAGE

Each player MUST have:

scoreboard objectives add player_id dummy

Optional advanced:

Use UUID-based tag:
raid_owner_<uuid>
⚙️ ENTITY OWNERSHIP RULE
tag @s add raid_owner_<player_uuid>

ALL queries MUST include:

@e[tag=raid,tag=raid_owner_<uuid>]
🚫 HARD RULE
GLOBAL ENTITY QUERIES ARE FORBIDDEN
🌍 17) DIMENSION SAFETY
🧠 REQUIREMENT

All raid logic MUST be dimension-aware.
RAIDS STRICTLY ONLY OCCUR IN THE deepspace:deep_space dimensionm, NOT IN ANY OTHER DIMENSION E.G ad_astra:moon etc

⚙️ EXECUTION RULE
execute in minecraft:overworld run function faction:tick
execute in minecraft:the_nether run function faction:tick
execute in minecraft:the_end run function faction:tick
📍 ENTITY FILTERING
@e[tag=raid,dimension=<player_dimension>]
❗ RULE
Raids MUST NOT cross dimensions
⏸️ 18) LOGOUT / PAUSE HANDLING
🧠 POLICY (REQUIRED)

Choose ONE (must be consistent):

OPTION A — HARD CLEANUP (RECOMMENDED)
Player logs out → raid instantly fails → cleanup runs
IMPLEMENTATION
# detect offline player (external or tick absence)
function faction:raid_fail_all
OPTION B — PERSISTENT RAID
Entities remain loaded
Timers continue

⚠️ Requires chunkloading → NOT recommended

🧨 19) ENTITY DEFINITIONS
🧠 REQUIREMENT

Each faction MUST define:

entity type(s)
equipment
attributes
📦 EXAMPLE (PIRATES)
summon minecraft:pillager ~ ~ ~ {Tags:["raid","faction_pirate"]}
summon minecraft:vindicator ~ ~ ~ {Tags:["raid","faction_pirate"]}
⚙️ OPTIONAL LOADOUT
replaceitem entity @s weapon.mainhand minecraft:iron_sword
📊 RULE
Each tier MUST define:
- composition
- count
- elite/commander chance
🗂️ 20) DATAPACK STRUCTURE
🧠 REQUIRED LAYOUT
data/
  faction/
    functions/
      init.mcfunction
      tick.mcfunction
      raid/
        execute_<faction>.mcfunction
        cleanup_<faction>.mcfunction
        fail_<faction>.mcfunction
      taunt/
        pirate/
        scavenger/
      spawn/
      fx/
📌 NAMING RULE
faction:<system>_<action>_<faction>
🚨 21) DESYNC RECOVERY SYSTEM
🧠 PURPOSE

Detect and repair invalid states.

⚙️ CHECK
# raid active but no entities
execute if score @s raid_state matches 2 unless entity @e[tag=raid,tag=raid_owner_<uuid>] run function faction:raid_force_cleanup
⚙️ REPAIR
# entities exist but no raid_state
execute if entity @e[tag=raid,tag=raid_owner_<uuid>] if score @s raid_state matches 0 run scoreboard players set @s raid_state 2
🧪 22) DEBUG / DEV COMMANDS
🧠 REQUIRED COMMANDS
FORCE RAID
function faction:raid_execute_<faction>
FORCE CLEANUP
function faction:raid_cleanup_<faction>
PRINT STATE
tellraw @s {"text":"Raid State: ","extra":[{"score":{"name":"@s","objective":"raid_state"}}]}
⚠️ RULE
Debug functions MUST NOT run in production tick loop
🔄 23) VERSIONING / RESET
🧠 REQUIREMENT

System MUST support safe reset.

⚙️ RESET FUNCTION
# faction:reset_all

scoreboard objectives remove raid_timer
scoreboard objectives remove provoke_cooldown
scoreboard objectives remove bribe_timer
scoreboard objectives remove raid_state

kill @e[tag=raid]
📊 24) PERFORMANCE BUDGET
🧠 HARD LIMITS
Max raid entities per player: 20
Max total raid entities: 100
Max taunts per tick: 10
⚙️ ENFORCEMENT
execute if entity @e[tag=raid,limit=100,sort=random] run kill @e[tag=raid,limit=1,sort=furthest]
⏱️ TICK COST RULE
All per-tick logic MUST be O(players + active_raids)
🧠 FINAL RESULT

After adding this, your spec becomes:

✅ Fully deterministic
✅ Multiplayer-safe
✅ Debuggable
✅ Recoverable from errors
✅ Scalable under load
✅ Actually implementable by an AI without guesswork


---

# 🧠 FINAL RESULT

With these additions, your system now has:

* a functioning runtime loop
* safe multiplayer behavior
* deterministic raid lifecycle
* real difficulty scaling
* guaranteed cleanup
* proper datapack integration points
* full progression tracking

---


# 🚀 FINAL RESULT

This specification defines a fully implementable system that behaves as:

> 🧠 A persistent, multi-faction war simulation engine with timers, provocations, bribes, named NPC combatants, radio communication, and advancement-driven progression integrated with FTB Quests.

