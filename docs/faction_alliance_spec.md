# Faction Alliance System — Technical Implementation Spec (Minecraft 1.20.1)

## Overview
This document defines a **drop-in datapack + FTB Quests integration system** for implementing a multi-faction alliance mechanic.

The system provides:

- Persistent faction reputation (`rep_<faction>` scoreboard)
- Binary alliance state (`allied_<faction>` tags)
- Idempotent +32 reputation bonus applied while allied
- Anti-exploit protection (no stacking, no repeat gains)
- FTB Quests integration via function calls and visibility conditions

---

# 1. Core Design Principles

## 1.1 State Separation
The system enforces strict separation of concerns:

| Layer | System | Purpose |
|------|--------|--------|
| Reputation | Scoreboard | Base faction opinion of player |
| Alliance | Tags | Player faction alignment state |
| Effects | MCFunctions | Apply/remove modifiers safely |
| UI | FTB Quests | Controls player interaction |

---

## 1.2 Key Rule
> Alliance modifies reputation ONCE at state transition only.

No ticking recalculation system is used.

---

# 2. Required Scoreboards

Create one scoreboard per faction:

```mcfunction
scoreboard objectives add rep_pirate dummy
scoreboard objectives add rep_collective dummy
scoreboard objectives add rep_republic dummy
scoreboard objectives add rep_luna dummy
scoreboard objectives add rep_auto dummy
```

---

# 3. Required Tags

Each faction has a binary alliance tag:

```mcfunction
tag @s add allied_pirates
```

List:

- `allied_pirates`
- `allied_collective`
- `allied_republic`
- `allied_lunarian`
- `allied_automata`

---

# 4. MCFunction API (Core System)

Each faction MUST implement two functions:

---

## 4.1 Form Alliance Function

**Path:**
```
data/faction/functions/form_<faction>_alliance.mcfunction
```

### Template:

```mcfunction
# Prevent double application
execute unless entity @s[tag=allied_<faction>] run scoreboard players add @s rep_<faction> 32

# Set alliance state
tag @s add allied_<faction>
```

---

## 4.2 Disband Alliance Function

**Path:**
```
data/faction/functions/disband_<faction>_alliance.mcfunction
```

### Template:

```mcfunction
# Only remove if currently allied
execute if entity @s[tag=allied_<faction>] run scoreboard players remove @s rep_<faction> 32

# Remove alliance state
tag @s remove allied_<faction>
```

---

# 5. FTB Quests Integration

## 5.1 Quest Structure Per Faction
Each faction requires 2 quests:

---

## 🟢 Form Alliance Quest

### Visibility Conditions:
- NOT `allied_<faction>`

### Requirements:
- `rep_<faction> > 0`

### Reward:
```mcfunction
function faction:form_<faction>_alliance
```

---

## 🔴 Disband Alliance Quest

### Visibility Conditions:
- `allied_<faction>`

### Requirements:
- none 

### Reward:
```mcfunction
function faction:disband_<faction>_alliance
```

---

# 6. Anti-Exploit Guarantees

## 6.1 No Infinite Farming
- Bonus applied ONLY when tag is added
- Re-running function has no effect

## 6.2 No Duplicate State
- Tag presence is the single truth source

## 6.3 No Quest Abuse
- Quest visibility prevents repeat form attempts

---

# 7. System Behavior Specification

## 7.1 Form Alliance Flow
1. Player completes quest
2. Function runs
3. System checks:
   - If NOT allied → apply +32 rep
   - Add alliance tag

Result:
- Player becomes allied
- Reputation increases once

---

## 7.2 Disband Alliance Flow
1. Player completes disband quest
2. Function runs
3. System checks:
   - If allied → remove +32 rep
   - Remove alliance tag

Result:
- Player loses alliance
- Reputation returns to base

---

# 8. FTB Quests Visibility Logic

## Form Quest Visibility
```
!has_tag(allied_<faction>)
```

## Disband Quest Visibility
```
has_tag(allied_<faction>)
```

---

# 9. File Structure (Datapack)

```
data/
  faction/
    functions/
      form_pirates_alliance.mcfunction
      disband_pirates_alliance.mcfunction
      form_collective_alliance.mcfunction
      disband_collective_alliance.mcfunction
      ...
```

---

# 10. Expansion Rules

To add a new faction:

1. Create scoreboard:
```
rep_<faction>
```

2. Create tag:
```
allied_<faction>
```

3. Duplicate two functions:
- form_<faction>_alliance.mcfunction
- disband_<faction>_alliance.mcfunction

4. Add 2 FTB Quests entries

---

## 11.2 Multi-Faction Diplomacy
System supports multiple simultaneous alliances by design.

---

# 12. Summary

This system provides:

- Deterministic faction alignment
- No score duplication exploits
- No recalculation overhead
- Fully FTB Quests compatible architecture
- Simple expansion model for unlimited factions

---

# End of Spec

