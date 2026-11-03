# Deep Space — Oxygen Loop Balance Specification
File: docs/balance/oxygen_loop.md
Version: 1.0 (Authoritative)
Status: FINAL — mathematically verified

All values derived from:
- `gtceu-1_20_1-7_5_2.jar` bytecode analysis
- `ad_astra-forge-1_20_1-1_15_20.jar` bytecode analysis
- Specified bonsai pot growth rate (1 cactus / 40s / pot)
- Specified brewery recipe (1 cactus + 20 mB water → 20 mB biomass, 8s)

---

## FLUID ID CORRECTION (MANDATORY)

> `gtceu:oxygen_gas` **does not exist** in GTCEu 7.5.2.
>
> Full jar byte-scan: zero occurrences of the string `oxygen_gas`.
>
> The correct fluid ID for gaseous oxygen in GTCEu is: **`gtceu:oxygen`**
>
> This is because oxygen is an elemental material (element O).
> GTCEu names elemental gases by bare material name, not with `_gas` suffix.

All recipes in this document use `gtceu:oxygen`.

---

## Confirmed Fluid IDs

| Fluid | Registry ID | Notes |
|---|---|---|
| GTCEu Biomass | `gtceu:biomass` | confirmed — `biomass_from_cactus` recipe exists in GT |
| GTCEu Oxygen (gas) | `gtceu:oxygen` | element O, GAS state, bare name |
| GTCEu Liquid Oxygen | `gtceu:liquid_oxygen` | cryogenic form, not used at LV |
| Vanilla Water | `minecraft:water` | |

---

## Stage 0 — Bonsai Pots (Input Source)

| Parameter | Value |
|---|---|
| Pots | 8 |
| Growth rate | 1 cactus per 40 s per pot |
| Total cactus output | **0.200 cactus/s** |

---

## Stage 1 — LV Brewery: Cactus → Biomass

**Recipe (user-specified):**

| Field | Value |
|---|---|
| Machine | LV Brewery |
| Item input | 1× `minecraft:cactus` |
| Fluid input | 20 mB `minecraft:water` |
| Fluid output | 20 mB `gtceu:biomass` |
| Duration | 8 s (160 ticks) |
| EUt | 8 EU/t |

**Throughput analysis:**

| | Value |
|---|---|
| Per-brewery rate | 0.125 cactus/s |
| Required for 0.200 cactus/s | ⌈0.200 ÷ 0.125⌉ = **2 LV Breweries** |
| Effective cactus processed | 0.200 cactus/s (bonsai-limited) |
| **Biomass produced** | **4.000 mB/s** |
| Water consumed | 4.000 mB/s |

Note: 2 breweries have theoretical capacity of 0.250 cactus/s but are throttled to
0.200 cactus/s by bonsai output. Second brewery runs at ~80% duty cycle.

---

## Stage 2a — Custom Recipe: Biomass → Water (Circuit 1)

**Purpose:** Recover water to make the brewery self-sustaining.
Must produce ≥ 4.000 mB/s to cover brewery consumption.

**Recipe (custom — added by Deep Space mod via KubeJS):**

| Field | Value |
|---|---|
| Machine | LV Chemical Reactor |
| Circuit | 1 |
| Fluid input | 4 mB `gtceu:biomass` |
| Fluid output | 40 mB `minecraft:water` |
| Duration | 4 s (80 ticks) |
| EUt | 7 EU/t |

**Throughput per machine:**

| | Value |
|---|---|
| Biomass consumed | 1.000 mB/s |
| Water produced | 10.000 mB/s |

**Machines deployed: 1**

| | Value |
|---|---|
| Biomass consumed total | 1.000 mB/s |
| Water produced | 10.000 mB/s |
| Brewery water demand | −4.000 mB/s |
| **Net water surplus** | **+6.000 mB/s** ✓ water-positive |

---

## Stage 2b — Custom Recipe: Biomass → Oxygen (Circuit 2)

**Purpose:** Produce `gtceu:oxygen` for life support.

**Recipe (custom — added by Deep Space mod via KubeJS):**

| Field | Value |
|---|---|
| Machine | LV Chemical Reactor |
| Circuit | 2 |
| Fluid input | 4 mB `gtceu:biomass` |
| Fluid output | 1000 mB `gtceu:oxygen` |
| Duration | 4 s (80 ticks) |
| EUt | 7 EU/t |

**Throughput per machine:**

| | Value |
|---|---|
| Biomass consumed | 1.000 mB/s |
| Oxygen produced | 250.0 mB/s |

**Biomass budget:**

| | Value |
|---|---|
| Total biomass from Stage 1 | 4.000 mB/s |
| Used by Stage 2a (water) | −1.000 mB/s |
| **Available for oxygen** | **3.000 mB/s** |

**Machines deployed: 3**

| | Value |
|---|---|
| Biomass consumed | 3.000 mB/s |
| **Oxygen produced** | **750.0 mB/s `gtceu:oxygen`** |
| Biomass remaining | 0.000 mB/s (loop fully balanced) |

---

## Mass Balance Summary

| Flow | Rate | Direction |
|---|---|---|
| Biomass in (from brewery) | 4.000 mB/s | + |
| Biomass to water reactor | 1.000 mB/s | − |
| Biomass to oxygen reactor | 3.000 mB/s | − |
| **Biomass leftover** | **0.000 mB/s** | balanced ✓ |
| | | |
| Water produced (Stage 2a) | 10.000 mB/s | + |
| Water consumed (brewery) | 4.000 mB/s | − |
| **Net water gain** | **+6.000 mB/s** | surplus ✓ |
| | | |
| **Oxygen out (gtceu:oxygen)** | **750 mB/s** | output ✓ |

---

## Energy Budget

| Machine | Count | EU/t each | Total EU/t |
|---|---|---|---|
| LV Brewery | 2 | 8 | 16 |
| LV Chemical Reactor (water) | 1 | 7 | 7 |
| LV Chemical Reactor (oxygen) | 3 | 7 | 21 |
| **TOTAL** | **6** | | **44 EU/t** |

Breeder Reactor in crash ship: 16 Ampere ZPM output.
The loop draws 44 EU/t total — negligible vs reactor output.

---

## Complete Machine Layout

```
8× Bonsai Pot
   └─ cactus (0.200/s) ──────────────────────────────┐
                                                       │
2× LV Brewery                                          │
   input:  cactus + 4 mB water/s                      │
   output: 4 mB biomass/s  ───────────────────────────┤
                                                       │
                              ┌── 1 mB/s ─────────────┤
                              │                        │
1× LV Chem Reactor [Circ 1]  │                        │
   input:  1 mB biomass/s    │   3 mB/s ──────────────┘
   output: 10 mB water/s ────┤
           (→ brewery: 4 mB/s consumed)           3× LV Chem Reactor [Circ 2]
           (net surplus: +6 mB/s)                    input:  3 mB biomass/s
                                                     output: 750 mB gtceu:oxygen/s
Total: 14 machines
(8 pots + 2 breweries + 1 water reactor + 3 oxygen reactors)
```

---

## Steady-State Output

| Output | Rate |
|---|---|
| `gtceu:oxygen` | **750 mB/s** |
| `minecraft:water` (surplus) | **+6 mB/s** (accumulates) |

750 mB/s = 45,000 mB/min = 2,700,000 mB/hr

---

## KubeJS Recipe Scripts

### File: `kubejs/server_scripts/deepspace_oxygen_loop.js`

```js
ServerEvents.recipes(event => {

    // ─────────────────────────────────────────────────────────────────
    // STAGE 1 — LV Brewery: Cactus + Water → Biomass
    // NOTE: GTCEu already has biomass_from_cactus recipe in its own data.
    // This custom recipe overrides/supplements with specific water+biomass ratios.
    // If GTCEu's default biomass_from_cactus conflicts, remove theirs first.
    // ─────────────────────────────────────────────────────────────────
    event.recipes.gtceu.brewery('deepspace:cactus_to_biomass')
        .itemInputs('1x minecraft:cactus')
        .inputFluids(Fluid.of('minecraft:water', 20))
        .outputFluids(Fluid.of('gtceu:biomass', 20))
        .duration(160)   // 8 seconds
        .EUt(8);

    // ─────────────────────────────────────────────────────────────────
    // STAGE 2a — LV Chemical Reactor: Biomass → Water  (Circuit 1)
    // ─────────────────────────────────────────────────────────────────
    event.recipes.gtceu.chemical_reactor('deepspace:biomass_to_water')
        .inputFluids(Fluid.of('gtceu:biomass', 4))
        .notConsumable(Item.of('gtceu:integrated_circuit').withNBT('{Configuration: 1}'))
        .outputFluids(Fluid.of('minecraft:water', 40))
        .duration(80)    // 4 seconds
        .EUt(7);

    // ─────────────────────────────────────────────────────────────────
    // STAGE 2b — LV Chemical Reactor: Biomass → gtceu:oxygen  (Circuit 2)
    //
    // FLUID ID: gtceu:oxygen
    // "gtceu:oxygen_gas" does NOT exist in GTCEu 7.5.2.
    // Oxygen is element O → GAS state → bare name → gtceu:oxygen.
    // ─────────────────────────────────────────────────────────────────
    event.recipes.gtceu.chemical_reactor('deepspace:biomass_to_oxygen')
        .inputFluids(Fluid.of('gtceu:biomass', 4))
        .notConsumable(Item.of('gtceu:integrated_circuit').withNBT('{Configuration: 2}'))
        .outputFluids(Fluid.of('gtceu:oxygen', 1000))
        .duration(80)    // 4 seconds
        .EUt(7);

});
```

---

## Notes on GTCEu Existing Biomass Recipes

The GT jar contains `biomass_from_cactus` — this is a vanilla GT recipe
(likely cactus → biomass without water input, different ratios).

**Implementation must:**
1. Check if vanilla `biomass_from_cactus` conflicts with the Deep Space recipe
2. If conflict: remove vanilla recipe first via `event.remove({ id: 'gtceu:biomass_from_cactus' })`
3. Then add the Deep Space recipe above

The Deep Space recipe intentionally requires water as input to enforce water
scarcity gameplay and create the dependency loop.

---

## Validation Targets

An implementation is correct when all of the following hold simultaneously:

| Test | Condition |
|---|---|
| Loop runs indefinitely | No resource depletes over 10 min runtime |
| Water self-sustaining | Water tank level non-decreasing after steady state |
| Oxygen net positive | gtceu:oxygen tank fills continuously |
| No biomass buildup | Biomass buffer stays bounded (consumed = produced) |
| Energy draw | ≤ 44 EU/t total across all 6 machines |

END OF OXYGEN LOOP BALANCE DOCUMENT
