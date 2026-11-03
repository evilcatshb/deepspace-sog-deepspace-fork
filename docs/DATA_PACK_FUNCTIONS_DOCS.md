# Factions Datapack — New Functions Documentation

## Summary of Added Functions

| Category | Count | Path Pattern |
|----------|-------|-------------|
| First Contact Ship Spawn | 6 factions × 3 files | `spawn/spawn_first_contact_ship_<faction>` |
| Reset Reputation | 7 factions | `rep/reset/reset_<faction>` |
| Defect / Betrayal | 7 factions | `defect/defect_<faction>` |
| Alliance (Form + Disband) | 7 factions × 2 | `alliance/form_<faction>_alliance` / `disband_<faction>_alliance` |
| Praise / Compliment | 7 factions × 2 + tick | `praise/praise_<faction>` + `praise/_apply_<faction>` |

---

## 1. First Contact Ship Spawn

Spawns a faction's first-contact trading ship near the player with a dramatic 3-line chat sequence (5s delays).

**Factions:** scav, luna, auto, pirate, republic, collective

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
function factions:spawn/spawn_first_contact_ship_scav
```

**What happens:**
1. `⚠️ Unknown signal detected...` (red, bold — immediate)
2. `⚠️ Scanning...` (red, bold — 5s later)
3. `⚠️ Vessel exiting warp...` (red, bold — 10s later)
4. Ship spawns via `/importship shop_first_contact_<faction>.nbt @p`

---

## 2. Reset Reputation

Sets a single faction's reputation to 0. Bypasses the faction matrix — only the target faction is affected.

**Factions:** scav, luna, auto, imax, pirate, republic, collective

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
execute as @p run function factions:rep/reset/reset_pirate
```

---

## 3. Defect / Betrayal

Player betrays a faction. Sets that faction's rep to **-128** and gives **+128** to all opposing/rival factions (per the hostility matrix).

**Opposing faction map:**

| Defect From | Opposing Factions Getting +128 |
|-------------|-------------------------------|
| scav | *(none — neutral faction)* |
| luna | pirate |
| auto | pirate, collective |
| imax | *(none — neutral faction)* |
| pirate | republic, luna, collective, auto |
| republic | pirate |
| collective | auto, pirate |

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
execute as @p run function factions:defect/defect_pirate
```

---

## 4. Alliance (Form / Disband)

Per the `faction_alliance_spec.md`, each faction has two functions:

### Form Alliance
- **Prerequisite:** `rep_<faction> > 0` (check via FTB Quests visibility condition)
- **Visibility:** Player does NOT have tag `allied_<tag>`
- Applies a one-time **+32 reputation bonus** (idempotent — no stacking)
- Adds the alliance tag

**Alliance tags per faction:**

| Faction | Tag |
|---------|-----|
| scav | `allied_scav` |
| luna | `allied_lunarian` |
| auto | `allied_automata` |
| imax | `allied_imax` |
| pirate | `allied_pirates` |
| republic | `allied_republic` |
| collective | `allied_collective` |

**FTB Quests — Form Alliance Quest:**
- Visibility: `!has_tag(allied_pirates)` (for pirate example)
- Requirement: `rep_pirate > 0`
- Reward Command (Elevated):
```
execute as @p run function factions:alliance/form_pirate_alliance
```

### Disband Alliance
- **Visibility:** Player HAS the alliance tag
- Removes the **+32 bonus** and clears the alliance tag

**FTB Quests — Disband Alliance Quest:**
- Visibility: `has_tag(allied_pirates)`
- No requirements
- Reward Command (Elevated):
```
execute as @p run function factions:alliance/disband_pirate_alliance
```

---

## 5. Praise / Compliment

Gives **+1 reputation** to a faction, capped at **32**. Has a **15-minute cooldown** per faction per player.

**Three possible outcomes per call:**

| Condition | Result |
|-----------|--------|
| Cooldown active (praised within 15min) | Throttle message, no rep change |
| Rep already ≥ 32 | Cap-reached message, no rep change |
| Rep < 32 and no cooldown | +1 rep applied, 15min cooldown starts, accepted message |

**FTB Quests Setup:**
- Reward type: **Command**
- Run with Elevated Permission: **True**
- Command:
```
execute as @p run function factions:praise/praise_pirate
```

**Cooldown system:**
- Uses per-player scoreboards `praise_cd_<faction>` (value = seconds remaining)
- Decremented once per second via `praise/_tick_cooldowns` (called every 20 ticks from `tick.mcfunction`)
- 900 seconds = 15 minutes

---

## Quick Reference — All New Function Paths

```
# First Contact (6 factions)
factions:spawn/spawn_first_contact_ship_scav
factions:spawn/spawn_first_contact_ship_luna
factions:spawn/spawn_first_contact_ship_auto
factions:spawn/spawn_first_contact_ship_pirate
factions:spawn/spawn_first_contact_ship_republic
factions:spawn/spawn_first_contact_ship_collective

# Reset Rep (7 factions)
factions:rep/reset/reset_scav
factions:rep/reset/reset_luna
factions:rep/reset/reset_auto
factions:rep/reset/reset_imax
factions:rep/reset/reset_pirate
factions:rep/reset/reset_republic
factions:rep/reset/reset_collective

/execute as @p run function factions:rep/reset/reset_scav
/execute as @p run function factions:rep/reset/reset_luna
/execute as @p run function factions:rep/reset/reset_auto
/execute as @p run function factions:rep/reset/reset_imax
/execute as @p run function factions:rep/reset/reset_pirate
/execute as @p run function factions:rep/reset/reset_republic
/execute as @p run function factions:rep/reset/reset_collective

# Defect (7 factions)
factions:defect/defect_scav
factions:defect/defect_luna
factions:defect/defect_auto
factions:defect/defect_imax
factions:defect/defect_pirate
factions:defect/defect_republic
factions:defect/defect_collective

# Alliance — Form (7 factions)
factions:alliance/form_scav_alliance
factions:alliance/form_luna_alliance
factions:alliance/form_auto_alliance
factions:alliance/form_imax_alliance
factions:alliance/form_pirate_alliance
factions:alliance/form_republic_alliance
factions:alliance/form_collective_alliance

# Alliance — Disband (7 factions)
factions:alliance/disband_scav_alliance
factions:alliance/disband_luna_alliance
factions:alliance/disband_auto_alliance
factions:alliance/disband_imax_alliance
factions:alliance/disband_pirate_alliance
factions:alliance/disband_republic_alliance
factions:alliance/disband_collective_alliance

# Praise (7 factions)
factions:praise/praise_scav
factions:praise/praise_luna
factions:praise/praise_auto
factions:praise/praise_imax
factions:praise/praise_pirate
factions:praise/praise_republic
factions:praise/praise_collective
```

All functions are designed to be called from FTB Quests with `Run with Elevated Permission: True` using the pattern `execute as @p run function factions:<path>`.
