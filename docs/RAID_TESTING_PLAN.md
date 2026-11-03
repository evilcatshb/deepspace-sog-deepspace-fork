# Raid System — Testing Plan

Step-by-step tests to verify every raid subsystem works correctly. Run these in order — each section builds on the previous one confirming the foundation is solid before testing higher-level features.

All commands are typed into the Minecraft chat window with `/` prefix, or pasted into a command block. You must be an operator.

---

## Prerequisites

Before testing, confirm the datapack loaded without errors:

```
/reload
/function factions:load
```

Check no errors appear in the server log. Then verify you're in the right dimension:

```
/execute in deepspace:deep_space run say deep space exists
```

If nothing prints, the dimension isn't loaded. Teleport there or use whatever method your modpack provides.

Confirm your player is initialized:

```
/execute if entity @s[tag=faction_initialized] run say initialized
```

If nothing prints, run `/function factions:setup_player` manually.

---

## Test 1: Scoreboard Objectives Exist

Verify the new raid objectives were created by checking a few key ones:

```
/scoreboard objectives list
```

Look for `raid_state_pirate`, `raid_timer_pirate`, `provoke_cd_pirate`, `bribe_timer_pirate`, `raid_tier`, `raid_duration`, `taunt_cd`, `raid_seq` in the list.

Then confirm your player has values set:

```
/scoreboard players get @s raid_state_pirate
/scoreboard players get @s raid_timer_pirate
```

Expected: `raid_state_pirate` = 0, `raid_timer_pirate` = some value between 0 and 36000.

**Pass:** All objectives exist and player has default values.

---

## Test 2: Provoke — Friendly (Rep Above -128)

This tests the "not hostile enough" branch of provoke. Your pirate rep should be above -128 (default is -25).

**Step 1 — Check current rep:**

```
/scoreboard players get @s rep_pirate
```

Expected: -25 (default) or whatever your current value is — must be above -128.

**Step 2 — Provoke:**

```
/function factions:raid/provoke_pirate
```

Expected:
- A yellow `[Radio]` message appears with a pirate threat
- No raid starts (raid_state stays 0)
- Rep decreased by 8

**Step 3 — Verify rep decreased:**

```
/scoreboard players get @s rep_pirate
```

Expected: previous value minus 8 (e.g. -25 → -33).

**Step 4 — Verify no raid started:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 0

**Pass:** Radio message appeared, rep dropped by 8, no raid triggered.

---

## Test 3: Provoke — Hostile (Rep At or Below -128)

This tests the actual raid-triggering branch of provoke.

**Step 1 — Set rep to -128:**

```
/scoreboard players set @s rep_pirate -128
```

get cooldown :

/scoreboard players get @p provoke_cd_pirate

**Step 2 — Clear any existing cooldown:**

```
/scoreboard players set @s provoke_cd_pirate 0
```

**Step 3 — Confirm state is clean:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 0

**Step 4 — Provoke:**

```
/function factions:raid/provoke_pirate
```

Expected:
- `[Radio] [Unknown] ⚠️ Unknown signal detected in your sector...` message appears
- `raid_state_pirate` changes to 1

**Step 5 — Verify raid started:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 1 (warning phase)

**Step 6 — Wait 10 seconds.** Watch chat for the countdown: 5... 4... 3... 2... 1... then particle/sound FX, then pirate entities spawn around you.

**Step 7 — Verify entities spawned:**

```
/scoreboard players get @s raid_state_pirate
/execute at @s if entity @e[tag=raid_pirate,distance=..64] run say raid entities found
```

Expected: `raid_state_pirate` = 2, and "raid entities found" prints.

**Step 8 — Cleanup for next test:**

```
/function factions:raid/cleanup_pirate
```

**Pass:** Provoke triggered a raid, warning countdown played, entities spawned, state transitioned 0→1→2.

---

## Test 4: Provoke Cooldown

After a provoke-triggered raid, the 10-minute cooldown should block further provokes.

**Step 1 — Set rep and force a provoke:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

**Step 2 — Immediately cleanup the raid (don't wait for combat):**

```
/function factions:raid/cleanup_pirate
```

**Step 3 — Check cooldown was set:**

```
/scoreboard players get @s provoke_cd_pirate
```

Expected: 12000 (or slightly less if a few ticks passed)

**Step 4 — Try to provoke again:**

```
/function factions:raid/provoke_pirate
```

Expected: A `[Radio] [Pirates]` message saying "We're still regrouping. Try again later." and NO raid starts.

**Step 5 — Verify no raid:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 0

**Step 6 — Clean up cooldown for next tests:**

```
/scoreboard players set @s provoke_cd_pirate 0
```

**Pass:** Second provoke was blocked with a cooldown message.

---

## Test 5: Bribe — Small (1 Hour)

**Step 1 — Set rep hostile and clear existing bribe:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s bribe_timer_pirate 0
```

**Step 2 — Apply small bribe:**

```
/function factions:raid/bribe_pirate_small
```

Expected: A `[Radio] [Pirates]` bribe confirmation message appears.

**Step 3 — Check bribe timer:**

```
/scoreboard players get @s bribe_timer_pirate
```

Expected: 72000 (1 hour)

**Step 4 — Try to trigger a raid via evaluate (simulate the 30-min cycle):**

```
/function factions:raid/evaluate_pirate
```

Expected: Nothing happens — the bribe blocks the raid.

**Step 5 — Verify no raid:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 0

**Pass:** Bribe set timer correctly and blocked automatic raid evaluation.

---

## Test 6: Bribe — Large (6 Hours)

**Step 1 — Clear existing bribe:**

```
/scoreboard players set @s bribe_timer_pirate 0
```

**Step 2 — Apply large bribe:**

```
/function factions:raid/bribe_pirate_large
```

**Step 3 — Check bribe timer:**

```
/scoreboard players get @s bribe_timer_pirate
```

Expected: 432000 (6 hours)

**Pass:** Large bribe set the correct timer value.

---

## Test 7: Bribe Stacking and Cap

**Step 1 — Set bribe timer to near-max:**

```
/scoreboard players set @s bribe_timer_pirate 1700000
```

**Step 2 — Apply another small bribe (would push past 24h):**

```
/function factions:raid/bribe_pirate_small
```

**Step 3 — Check timer is capped:**

```
/scoreboard players get @s bribe_timer_pirate
```

Expected: 1728000 (capped at 24 hours), NOT 1772000.

**Step 4 — Clean up:**

```
/scoreboard players set @s bribe_timer_pirate 0
```

**Pass:** Bribe stacked but was capped at 24 hours.

---

## Test 8: Direct Raid Execute (Bypass All Gates)

This tests `execute_<faction>` which should always work regardless of rep, bribe, or cooldown.

**Step 1 — Set everything to a state that should block raids:**

```
/scoreboard players set @s rep_pirate 100
/scoreboard players set @s bribe_timer_pirate 999999
/scoreboard players set @s provoke_cd_pirate 999999
```

**Step 2 — Force execute:**

```
/function factions:raid/execute_pirate
```

Expected: Raid starts immediately (warning phase). `execute` bypasses all gates.

**Step 3 — Verify:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 1

**Step 4 — Cleanup:**

```
/function factions:raid/cleanup_pirate
/scoreboard players set @s rep_pirate -25
/scoreboard players set @s bribe_timer_pirate 0
/scoreboard players set @s provoke_cd_pirate 0
```

**Pass:** Direct execute worked despite positive rep, active bribe, and active cooldown.

---

## Test 9: Raid Tier Scaling

**Step 1 — Test T1 (rep -128):**

```
/scoreboard players set @s rep_pirate -128
/function factions:raid/execute_pirate
/scoreboard players get @s raid_tier
```

Expected: raid_tier = 1

```
/function factions:raid/cleanup_pirate
```

**Step 2 — Test T2 (rep -512):**

```
/scoreboard players set @s rep_pirate -512
/function factions:raid/execute_pirate
/scoreboard players get @s raid_tier
```

Expected: raid_tier = 2

```
/function factions:raid/cleanup_pirate
```

**Step 3 — Test T3 (rep -2048):**

```
/scoreboard players set @s rep_pirate -2048
/function factions:raid/execute_pirate
/scoreboard players get @s raid_tier
```

Expected: raid_tier = 3

```
/function factions:raid/cleanup_pirate
```

**Step 4 — Reset rep:**

```
/scoreboard players set @s rep_pirate -25
```

**Pass:** Tier correctly scales with reputation depth.

---

## Test 10: Raid Combat — Full Lifecycle

This is the end-to-end test. You will provoke a raid and fight it to completion.

**Step 1 — Prepare:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/gamemode survival
```

Equip yourself with a weapon (creative inventory or `/give`).

**Step 2 — Provoke:**

```
/function factions:raid/provoke_pirate
```

**Step 3 — Wait through the 10-second warning.** Watch for:
- `[Radio] [Unknown]` initial warning ✓
- `[Radio] [Pirates]` faction reveal + countdown 5...4...3...2...1 ✓
- Particle/sound FX burst ✓
- Pirate entities spawn around you ✓

**Step 4 — During combat, check for taunts.** Within 30 blocks of a taunter entity, you should see `[Pirates] [Pirate] Name: TAUNT MESSAGE` in chat every ~5 seconds.

**Step 5 — Kill all pirate raid entities.** After the last one dies, watch for:
- `[Radio] [Pirates] ...we are withdrawing... for now. ☠️` retreat message ✓
- Raid state resets to 0 ✓

**Step 6 — Verify completion:**

```
/scoreboard players get @s raid_state_pirate
/advancement list
```

Expected: `raid_state_pirate` = 0, and `factions:raids/pirate_raid_t1` advancement granted.

**Pass:** Full lifecycle completed — warning → countdown → FX → spawn → taunts → combat → kill all → success message → advancement.

---

## Test 11: Raid Failure — Timeout

**Step 1 — Start a raid:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

**Step 2 — Wait for entities to spawn (10s warning), then immediately force the duration to near-timeout:**

```
/scoreboard players set @s raid_duration 5999
```

**Step 3 — Wait 1 tick (stand still for a moment).** The runtime function should detect duration ≥ 6000 and trigger failure.

Expected:
- `[Radio] [Pirates]` failure message appears
- All raid entities are killed
- `raid_state_pirate` resets to 0

**Step 4 — Verify:**

```
/scoreboard players get @s raid_state_pirate
/execute at @s if entity @e[tag=raid_pirate,distance=..96] run say entities remain
```

Expected: state = 0, no "entities remain" message.

**Pass:** Timeout triggered failure correctly, entities cleaned up.

---

## Test 12: Raid Failure — Player Death

**Step 1 — Start a raid and let entities spawn:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

Wait for the entities to spawn (10s).

**Step 2 — Kill yourself:**

```
/kill @s
```

**Step 3 — After respawning, check raid state:**

```
/scoreboard players get @s raid_state_pirate
```

Expected: 0 (the death detection in `tick_player` should have called `fail_pirate`)

**Step 4 — Check for leftover entities:**

```
/execute if entity @e[tag=raid_pirate] run say leftover entities
```

Expected: No leftover entities (cleanup kills them within 128 blocks of the player's last position).

**Pass:** Player death triggered raid failure and cleanup.

---

## Test 13: Entity Tags and Team Membership

Verify that raid entities are correctly tagged and on the right team.

**Step 1 — Start a raid:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

Wait for spawn (10s).

**Step 2 — Check tags on nearest raid entity:**

```
/data get entity @e[tag=raid_pirate,limit=1,sort=nearest] Tags
```

Expected: Should include `pirate`, `raid_entity`, `raid_pirate`, and possibly `raid_taunter`.

**Step 3 — Check team:**

```
/team list pirate
```

Expected: Raid entities should appear in the pirate team.

**Step 4 — Check names are assigned (not "Unnamed"):**

```
/data get entity @e[tag=raid_pirate,limit=1,sort=nearest] CustomName
```

Expected: A JSON name like `[Pirate] Blacktide Rook`, NOT `[Pirate] Unnamed`.

**Step 5 — Check taunter exists:**

```
/execute if entity @e[tag=raid_taunter,tag=raid_pirate] run say taunter found
```

Expected: "taunter found"

**Step 6 — Cleanup:**

```
/function factions:raid/cleanup_pirate
```

**Pass:** Entities have correct tags, team, names, and taunter designation.

---

## Test 14: T3 Commander

**Step 1 — Force a T3 raid:**

```
/scoreboard players set @s rep_pirate -2048
/function factions:raid/execute_pirate
```

Wait for spawn (10s).

**Step 2 — Check for commander:**

```
/execute if entity @e[tag=raid_commander,tag=raid_pirate] run say commander found
/data get entity @e[tag=raid_commander,tag=raid_pirate,limit=1] CustomName
/data get entity @e[tag=raid_commander,tag=raid_pirate,limit=1] Attributes
```

Expected: Commander exists, has "⚔ Commander" in name, and has boosted max_health (54 for pirates).

**Step 3 — Check commander effects:**

```
/data get entity @e[tag=raid_commander,tag=raid_pirate,limit=1] ActiveEffects
```

Expected: Strength and Speed effects present.

**Step 4 — Count total entities (T3 should be 14):**

```
/execute if entity @e[tag=raid_pirate] run say found
```

Or use the debug tellraw:

```
/scoreboard players get @s raid_mob_count
```

Expected: 14

**Step 5 — Cleanup:**

```
/function factions:raid/cleanup_pirate
/scoreboard players set @s rep_pirate -25
```

**Pass:** Commander spawned with correct name, HP, and effects. Entity count matches T3 spec.

---

## Test 15: Taunts

**Step 1 — Start a raid and wait for spawn:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

Wait 10s for entities to spawn.

**Step 2 — Stand within 30 blocks of a taunter and wait 5-10 seconds.** Watch chat.

Expected: Taunts appear in the format `[Pirates] [Pirate] Name: TAUNT MESSAGE ☠️🔪` every ~5 seconds. Only taunter-tagged entities speak — the rest stay silent.

**Step 3 — Move more than 30 blocks away from all raid entities.** Wait 10 seconds.

Expected: No taunts appear (range check working).

**Step 4 — Cleanup:**

```
/function factions:raid/cleanup_pirate
```

**Pass:** Taunts fire at correct interval, only from taunters, only when player is within 30 blocks.

---

## Test 16: Cleanup Guarantees

**Step 1 — Start a raid:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

Wait for spawn.

**Step 2 — Run cleanup:**

```
/function factions:raid/cleanup_pirate
```

**Step 3 — Verify EVERYTHING is reset:**

```
/scoreboard players get @s raid_state_pirate
/scoreboard players get @s raid_tier
/scoreboard players get @s raid_duration
/scoreboard players get @s raid_mob_count
/scoreboard players get @s raid_seq
/execute if entity @e[tag=raid_pirate] run say FAIL: entities remain
/execute if entity @s[tag=raid_active_pirate] run say FAIL: active tag remains
/execute if entity @s[tag=raid_warning_pirate] run say FAIL: warning tag remains
```

Expected: All scores = 0, no "FAIL" messages.

**Pass:** Cleanup removed all entities, reset all scores, cleared all tags.

---

## Test 17: Multiple Faction Raids

Test that two factions can raid simultaneously without interfering.

**Step 1 — Set rep for two factions:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s rep_scav -128
/scoreboard players set @s provoke_cd_pirate 0
/scoreboard players set @s provoke_cd_scav 0
```

**Step 2 — Provoke both in quick succession:**

```
/function factions:raid/provoke_pirate
/function factions:raid/provoke_scav
```

**Step 3 — Verify both raids are active:**

```
/scoreboard players get @s raid_state_pirate
/scoreboard players get @s raid_state_scav
```

Expected: Both = 1 (or 2 if you waited for spawn).

**Step 4 — Wait for both to spawn. Verify entities from both factions exist:**

```
/execute if entity @e[tag=raid_pirate] run say pirates spawned
/execute if entity @e[tag=raid_scav] run say scavengers spawned
```

**Step 5 — Cleanup pirate only:**

```
/function factions:raid/cleanup_pirate
```

**Step 6 — Verify scavenger raid is unaffected:**

```
/scoreboard players get @s raid_state_pirate
/scoreboard players get @s raid_state_scav
/execute if entity @e[tag=raid_scav] run say scav entities still alive
```

Expected: pirate state = 0, scav state = 2, scav entities still exist.

**Step 7 — Cleanup scav:**

```
/function factions:raid/cleanup_scav
```

**Pass:** Two faction raids ran independently, cleaning one didn't affect the other.

---

## Test 18: Server Reload Desync Recovery

**Step 1 — Start a raid:**

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```

Wait for entities to spawn.

**Step 2 — Reload the datapack (simulates crash/restart):**

```
/reload
```

**Step 3 — Check that desync recovery cleaned up:**

```
/scoreboard players get @s raid_state_pirate
/execute if entity @e[tag=raid_entity] run say FAIL: orphaned entities
```

Expected: `raid_state_pirate` = 0, no orphaned entities. The `load.mcfunction` desync recovery killed all `raid_entity` tagged mobs and reset all player states.

**Pass:** Reload cleaned up all raid state automatically.

---

## Test 19: All Factions — Quick Smoke Test

Run through every faction with a minimal provoke→cleanup cycle to confirm nothing is broken per-faction.

For each faction, run the block below (replace `FACTION` and `REP_OBJ`):

| Faction | FACTION | REP_OBJ |
|---|---|---|
| Pirates | `pirate` | `rep_pirate` |
| Scavengers | `scav` | `rep_scav` |
| Lunarians | `luna` | `rep_luna` |
| Automata | `auto` | `rep_auto` |
| Republic | `republic` | `rep_republic` |
| Collective | `collective` | `rep_collective` |
| iMAX | `imax` | `rep_imax` |

```
/scoreboard players set @s REP_OBJ -128
/scoreboard players set @s provoke_cd_FACTION 0
/function factions:raid/provoke_FACTION
```

Wait 10 seconds, verify entities spawn and you see the faction-colored countdown messages, then cleanup:

```
/function factions:raid/cleanup_FACTION
/scoreboard players set @s REP_OBJ 0
```

Concrete commands for all seven (copy-paste block):

```
/scoreboard players set @s rep_pirate -128
/scoreboard players set @s provoke_cd_pirate 0
/function factions:raid/provoke_pirate
```
*(wait 12s, verify pirate pillagers spawn)*
```
/function factions:raid/cleanup_pirate
/scoreboard players set @s rep_pirate -25
```

```
/scoreboard players set @s rep_scav -128
/scoreboard players set @s provoke_cd_scav 0
/function factions:raid/provoke_scav
```
*(wait 12s, verify scavenger zombified piglins spawn)*
```
/function factions:raid/cleanup_scav
/scoreboard players set @s rep_scav 0
```

```
/scoreboard players set @s rep_luna -128
/scoreboard players set @s provoke_cd_luna 0
/function factions:raid/provoke_luna
```
*(wait 12s, verify lunarian entities spawn)*
```
/function factions:raid/cleanup_luna
/scoreboard players set @s rep_luna 0
```

```
/scoreboard players set @s rep_auto -128
/scoreboard players set @s provoke_cd_auto 0
/function factions:raid/provoke_auto
```
*(wait 12s, verify iron golems spawn)*
```
/function factions:raid/cleanup_auto
/scoreboard players set @s rep_auto 0
```

```
/scoreboard players set @s rep_republic -128
/scoreboard players set @s provoke_cd_republic 0
/function factions:raid/provoke_republic
```
*(wait 12s, verify republic zombified piglins spawn)*
```
/function factions:raid/cleanup_republic
/scoreboard players set @s rep_republic 10
```

```
/scoreboard players set @s rep_collective -128
/scoreboard players set @s provoke_cd_collective 0
/function factions:raid/provoke_collective
```
*(wait 12s, verify collective villagers spawn)*
```
/function factions:raid/cleanup_collective
/scoreboard players set @s rep_collective 0
```

```
/scoreboard players set @s rep_imax -128
/scoreboard players set @s provoke_cd_imax 0
/function factions:raid/provoke_imax
```
*(wait 12s, verify iMAX pillagers spawn)*
```
/function factions:raid/cleanup_imax
/scoreboard players set @s rep_imax 0
```

**Pass:** All 7 factions trigger raids, spawn correct entity types, show faction-colored messages, and clean up properly.

---

## Test Summary Checklist

| # | Test | Result |
|---|---|---|
| 1 | Scoreboard objectives exist | ☐ |
| 2 | Provoke — friendly (rep > -128) | ☐ |
| 3 | Provoke — hostile (rep ≤ -128) | ☐ |
| 4 | Provoke cooldown blocks re-provoke | ☐ |
| 5 | Bribe small (1hr) blocks evaluation | ☐ |
| 6 | Bribe large (6hr) sets correct timer | ☐ |
| 7 | Bribe stacking + 24hr cap | ☐ |
| 8 | Direct execute bypasses all gates | ☐ |
| 9 | Tier scaling (T1/T2/T3) | ☐ |
| 10 | Full combat lifecycle | ☐ |
| 11 | Raid failure — timeout | ☐ |
| 12 | Raid failure — player death | ☐ |
| 13 | Entity tags + team + names | ☐ |
| 14 | T3 commander (HP, effects, name) | ☐ |
| 15 | Taunts (interval, range, content) | ☐ |
| 16 | Cleanup guarantees | ☐ |
| 17 | Multiple faction raids simultaneously | ☐ |
| 18 | Server reload desync recovery | ☐ |
| 19 | All 7 factions smoke test | ☐ |
