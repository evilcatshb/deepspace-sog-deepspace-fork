# asteroids.md

## Deep Space — Complete Asteroid Archetype Registry

This document defines **ALL asteroid archetypes**, their generation logic, composition rules, rarity values, and guarantees required for full progression from LV → ZPM using **GregTech CEu 7.5.2 + Vanilla Minecraft + Ad Astra** resources.

This file is the **authoritative world-generation reference** for the Deep Space dimension.

---

# 1. Global Generation Rules

## Dimension Properties

* Dimension ID: `deepspace:deep_space`
* Tagged as: `ad_astra:space`
* Atmosphere: vacuum
* Gravity: Ad Astra Moon gravity
* Y Range:

  * Asteroid band: **Y 0–120**
  * Construction space: **Y 121–384 (empty)**

---

## Asteroid Placement Rules

| Rule                       | Value                           |
| -------------------------- | ------------------------------- |
| Minimum spacing            | 48 blocks                       |
| Average spacing            | 96–160 blocks                   |
| Personal Bubble radius     | 10–15 chunks                    |
| Generation seed            | worldSeed + playerUUID modifier |
| Overwrite FTBChunks claims | NEVER                           |
| Spawn inside claims        | FORBIDDEN                       |

---

## Zone Mapping (Distance From Crash Site)

| Zone                     | Distance        | Tech Tier  |
| ------------------------ | --------------- | ---------- |
| Zone 1 — Industrial Belt | 300–1500 blocks | Steam → LV |
| Zone 2 — Advanced Ring   | 1.5–5 km        | MV         |
| Zone 3 — Deep Space Belt | 5–15 km         | HV–LuV     |
| Zone 4 — Outer System    | 15km+           | ZPM+       |

---

# 2. Asteroid Composition Model

Each asteroid defines:

```
outer_shell_blocks
inner_fill_blocks
ore_blocks
fluid_or_gas
drilling_vein
spawn_weight
zone
guarantee_rules
```

---

# 3. Universal Ore Coverage Requirement

The generator MUST include:

### Vanilla Ores

* coal
* iron
* copper
* gold
* redstone
* lapis
* diamond
* emerald
* quartz

### GregTech Overworld Materials (raw variants)

ALL GTCEu overworld vein materials including:

* raw_copper
* raw_tin
* raw_iron
* raw_lead
* raw_silver
* raw_gold
* raw_nickel
* raw_zinc
* raw_bauxite
* raw_aluminium
* raw_platinum
* raw_iridium
* raw_tungsten
* raw_titanium
* raw_chrome
* raw_manganese
* raw_molybdenum
* raw_vanadium
* raw_lithium
* raw_cobalt
* raw_osmium
* raw_uranium
* raw_naquadah (outer zones)

---

# 4. Asteroid Archetype Table

## FORMAT

| Archetype | Zone | Weight | Shell | Interior | Ores | Fluid/Gas | GT Fluid Vein | Avg Fluid Qty |
| --------- | ---- | ------ | ----- | -------- | ---- | --------- | ------------- | ------------- |

---

## ZONE 1 — Industrial Belt (Starter Guaranteed)

| Archetype       | Zone | Weight | Shell       | Interior       | Ores       | Fluid/Gas | Vein | Qty          |
| --------------- | ---- | ------ | ----------- | -------------- | ---------- | --------- | ---- | ------------ |
| Stone Asteroid  | Z1   | 120    | stone       | stone          | coal, iron | none      | no   | —            |
| Copper Asteroid | Z1   | 90     | stone       | granite        | raw_copper | none      | no   | —            |
| Tin Asteroid    | Z1   | 80     | stone       | andesite       | raw_tin    | none      | no   | —            |
| Iron Core       | Z1   | 85     | stone       | iron_block mix | raw_iron   | none      | no   | —            |
| Ice Asteroid    | Z1   | 70     | packed_ice  | ice            | none       | water     | yes  | 100k–300k mB |
| Soil Asteroid   | Z1   | 60     | coarse_dirt | dirt/stone     | trace_coal | none      | no   | —            |
| Lava Asteroid   | Z1   | 40     | basalt      | lava pockets   | none       | lava      | yes  | 80k–200k mB  |

Guarantee:

* At least 1 Copper
* At least 1 Iron
* At least 2 Ice within bubble

---

## ZONE 2 — Advanced Resource Ring

| Archetype           | Zone | Weight | Shell      | Interior  | Ores         | Fluid/Gas  | Vein | Qty          |
| ------------------- | ---- | ------ | ---------- | --------- | ------------ | ---------- | ---- | ------------ |
| Redstone Asteroid   | Z2   | 60     | stone      | deepslate | raw_redstone | none       | no   | —            |
| Gold Asteroid       | Z2   | 50     | stone      | granite   | raw_gold     | none       | no   | —            |
| Lead Asteroid       | Z2   | 45     | stone      | basalt    | raw_lead     | none       | no   | —            |
| Silver Asteroid     | Z2   | 45     | stone      | calcite   | raw_silver   | none       | no   | —            |
| Nickel Asteroid     | Z2   | 40     | stone      | basalt    | raw_nickel   | none       | no   | —            |
| Oil Asteroid        | Z2   | 35     | dark_shell | stone     | none         | oil        | yes  | 400k–900k mB |
| Salt Water Asteroid | Z2   | 30     | ice        | ice       | none         | salt_water | yes  | 300k–600k mB |

---

## ZONE 3 — Deep Space Belt

| Archetype             | Zone | Weight | Shell    | Interior    | Ores             | Fluid/Gas | Vein | Qty          |
| --------------------- | ---- | ------ | -------- | ----------- | ---------------- | --------- | ---- | ------------ |
| Bauxite Asteroid      | Z3   | 40     | stone    | clay        | raw_bauxite      | none      | no   | —            |
| Aluminium Asteroid    | Z3   | 40     | stone    | bauxite_mix | raw_aluminium    | none      | no   | —            |
| Titanium Asteroid     | Z3   | 30     | basalt   | dense_stone | raw_titanium     | none      | no   | —            |
| Chrome Asteroid       | Z3   | 25     | basalt   | stone       | raw_chrome       | none      | no   | —            |
| Methane Gas Asteroid  | Z3   | 20     | stone    | hollow      | methane pockets  | methane   | yes  | 500k–1.2M mB |
| Hydrogen Gas Asteroid | Z3   | 18     | stone    | hollow      | hydrogen pockets | hydrogen  | yes  | 400k–1M mB   |
| Nitrogen Gas Asteroid | Z3   | 18     | stone    | hollow      | nitrogen pockets | nitrogen  | yes  | 400k–1M mB   |
| Exotic Ice Asteroid   | Z3   | 15     | blue_ice | exotic_ice  | none             | water     | yes  | 2M–5M mB     |

---

## ZONE 4 — Outer System (Endgame)

| Archetype          | Zone | Weight | Shell     | Interior    | Ores         | Fluid/Gas | Vein | Qty      |
| ------------------ | ---- | ------ | --------- | ----------- | ------------ | --------- | ---- | -------- |
| Platinum Asteroid  | Z4   | 20     | basalt    | dense_core  | raw_platinum | none      | no   | —        |
| Iridium Asteroid   | Z4   | 15     | basalt    | dense_core  | raw_iridium  | none      | no   | —        |
| Tungsten Asteroid  | Z4   | 18     | basalt    | dense_stone | raw_tungsten | none      | no   | —        |
| Uranium Asteroid   | Z4   | 15     | basalt    | stone       | raw_uranium  | none      | no   | —        |
| Naquadah Asteroid  | Z4   | 5      | obsidian  | alien_stone | raw_naquadah | none      | no   | —        |
| Heavy Oil Asteroid | Z4   | 12     | tar_shell | stone       | none         | heavy_oil | yes  | 2M–6M mB |

---

# 5. Fluid & Gas Behavior

Gas asteroids:

* Use vanilla/GT fluid source blocks.
* Spawn in sealed pockets.
* Never free-flow.

Fluid asteroids:

* Finite volume.
* Matched GT fluid vein below center chunk.

---

# 6. Spawn Guarantee System

If resource NOT found within:

```
1200 blocks of crash site
```

Generator MUST inject missing asteroid type inside player bubble.

Priority order:

1. Ice
2. Iron
3. Copper
4. Tin
5. Stone

---

# 7. Automatic Injection Conditions

Injection allowed only if:

* Chunk ungenerated
* Not claimed
* Outside structures
* Inside personal bubble

---

# 8. Geological Logic

Asteroids follow classification:

| Type         | Materials              |
| ------------ | ---------------------- |
| Metallic     | iron, nickel, platinum |
| Carbonaceous | coal, oil              |
| Silicate     | stone ores             |
| Volcanic     | lava, basalt           |
| Icy          | water, exotic ice      |
| Gas          | methane, hydrogen      |

No random mixtures allowed.

---

END OF ASTEROID REGISTRY
