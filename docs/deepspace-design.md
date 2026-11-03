# deepspace-design.md

## Deep Space Dimension — Gameplay & Systems Design Specification

Authoritative gameplay design for the **Deep Space GregTech + Ad Astra expansion mod**.

This document defines ALL gameplay mechanics, progression rules, player systems, world logic, and deliverables.

---

# 1. Core Concept

Every player begins alone in deep space on a **personal crashed spaceship asteroid**.

The dimension is:

* infinite asteroid field
* survival dependent on oxygen + water + food
* economy driven by scarcity
* progression driven by exploration distance

Key pillars:

1. Isolation at start
2. Personal progression bubble
3. Shared endgame universe
4. Renewable life-support engineering
5. Distance = difficulty scaling

---

# 2. Dimension: `deepspace`

## Properties

| Property          | Value                  |
| ----------------- | ---------------------- |
| Sky               | Void / stars           |
| Gravity           | Ad Astra space gravity |
| Atmosphere        | Vacuum                 |
| Weather           | Disabled               |
| Day cycle         | Disabled               |
| Build Height      | -64 → 512              |
| Asteroid Band     | Y=0–120                |
| Construction Zone | Y=160–512              |

Asteroids intentionally spawn low to allow megastructures above.

---

# 3. Player Spawn System — Personal Crash Bubble

## Goal

Each player receives:

* unique spawn
* isolated early resources
* independent difficulty scaling

---

## Crash Bubble

Radius: **15 chunks**

Inside bubble:

* generation seeded using:

```
personalSeed = worldSeed XOR playerUUID.hash
```

Guarantees uniqueness.

---

### Bubble Guarantees

Each bubble contains:

* starter asteroid
* crashed ship schematic
* LV ores
* ice source
* organic asteroid
* minimal fuel resources

---

# 4. Spawn Placement Rules

When player first joins:

1. Search spiral outward from origin.
2. Reject positions if:

   * within 120 chunks of another crash site
   * overlaps FTBChunks claims
   * overlaps existing structures
3. Reserve region.
4. Force-load spawn chunks.
5. Generate asteroid + ship via NBT structure.

---

# 5. Crash Ship (NBT Structure)

Loaded via Structure Manager.

## Ship Contents

### Machines

| Machine               | Count |
| --------------------- | ----- |
| AE2 Controller        | 1     |
| AE2 Drive             | 1     |
| AE2 Energy Acceptor   | 1     |
| LV Macerator          | 1     |
| LV Assembler          | 1     |
| LV Chemical Reactor   | 2     |
| LV Heater             | 1     |
| Oxygen Distributor    | 1     |
| Oxygen Generator      | 1     |
| Small Breeder Reactor | 1     |

---

### Power System

**Small Breeder Reactor**

* Tier: ZPM
* Output: 16A @ ZPM
* Fuel: 1 Uranium Ingot / 24h realtime
* Starting fuel: 14 ingots (2 weeks)

Purpose:

* guarantees early survival
* prevents early power deadlock

---

### Ship State

| System     | Status                   |
| ---------- | ------------------------ |
| Warp Drive | Broken multiblock        |
| Oxygen     | Active but low reserve   |
| Water      | Nearly depleted          |
| Hull       | Damaged aesthetic blocks |

---

# 6. Difficulty Scaling

## Personal Zone (0–15 chunks)

LV resources only.

## Shared Scaling Beyond Bubble

Distance from crash site:

| Distance | Tier    |
| -------- | ------- |
| 0–15     | LV      |
| 16–64    | MV      |
| 64–160   | HV      |
| 160–320  | EV      |
| 320–640  | IV      |
| 640+     | LuV–ZPM |

Distance measured from **player crash coordinate**, not world origin.

---

# 7. Asteroid Archetypes

(All spawn below Y120)

## Core Types

| Archetype   | Contents          |
| ----------- | ----------------- |
| Rocky       | stone + metals    |
| Metallic    | dense ore         |
| Ice         | ice + fluids      |
| Organic     | biomass sources   |
| Gas Pocket  | gases             |
| Crystal     | rare minerals     |
| Radioactive | nuclear materials |
| Exotic      | endgame materials |

(Full probability tables in technical document.)

---

# 8. Renewable Oxygen System (Core Early Goal)

Players must build:

```
8 Bonsai Pots
→ LV Macerator
→ Biomass
→ 2 Chemical Reactors
→ Water + Oxygen
→ Oxygen Generator
→ Oxygen Distributor
```

Design rule:

> 8 continuously growing plants sustain exactly 1 player indefinitely.

Water becomes renewable ONLY after loop completion.

---

# 9. Water Scarcity Gameplay

Early sources:

* starter tank
* ice asteroids
* melted ice

Water freezing rule:

```
Water exposed to vacuum → Ice block
```

---

# 10. Exotic Ice

New block.

* Found in Ice Asteroids.
* Heater recipe:

  * 1 Exotic Ice → 1024 buckets water.

Mid-game logistics objective.

---

# 11. Farming Rules

Plants require:

* sealed oxygen room
* Ad Astra oxygen distributor

Plants consume LESS oxygen than loop produces.

---

# 12. Thirst & Hunger

Player systems enabled:

* Hunger: vanilla

---

# 13. Endgame Chemical Industry

Bulk production methods:

### Oxygen

Nitrogen + Hydrogen electrolysis (EV).

### Water Multiplication

Water cracking → Hydrogen recycling → net-positive synthesis.

Not renewable without energy infrastructure.

---

# 14. Multiplayer Design Goals

* players isolated early
* economy emerges later
* warp ships connect bubbles
* exploration rewarded

---

# 15. Anti-Grief & Claim Safety

Generation NEVER occurs if:

* chunk claimed by FTBChunks
* reserved spawn region exists
* structure bounding box intersects claim

---

# 16. Deliverables Summary

Mod must add:

* Deep Space dimension
* Crash bubble generator
* Distance-tier scaling
* Asteroid generator
* Renewable oxygen chain
* Exotic Ice block
* Small Breeder Reactor
* Water freezing mechanic
* Personal seed generation
* Claim-safe worldgen

END OF DESIGN DOCUMENT
