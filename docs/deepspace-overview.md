# Deep Space — Overview Specification
File: deepspace-overview.md  
Version: 1.0 (Authoritative)  
Target Minecraft Version: **1.20.1 Forge**

---

# 1. PURPOSE OF THIS DOCUMENT

This document is the **high-level authoritative overview** of the **Deep Space Mod**.

It explains:

- What the mod adds to the game
- Gameplay goals and progression philosophy
- All required systems and mechanics
- Dimension behavior
- Player experience expectations
- Integration with GregTech CEu Modern + Ad Astra
- Dependency jars required for byte-correct implementation
- Mixin compatibility expectations

This document describes **WHAT must exist**.

Implementation details live in:
- `deepspace-technical.md`
- `worldgen-contracts.md`
- `worldgen-flowchart.md`
- `asteroids.md`
- `deepspace-registry.json`

---

# 2. MOD IDENTITY

Mod Name: **Deep Space**  
Mod ID: `deepspace`  
Type: Dimension + Progression Framework Mod

Primary Concept:

> Replace the traditional overworld with a **procedurally generated asteroid universe** where technological progression equals distance traveled.

The player survives in vacuum using GregTech technology and Ad Astra oxygen mechanics.

---

# 3. CORE DESIGN PILLARS

## 3.1 Distance = Technology

Progression is spatial.

| Distance from crash site | Tech Tier |
|---|---|
| 0–1500 blocks | Steam / LV |
| 1.5–5 km | MV / HV |
| 5–15 km | EV / IV |
| 15km+ | LuV → ZPM → Post-ZPM |

The world itself becomes the tech tree.

---

## 3.2 No Natural Life

Deep Space is **biologically dead**.

The dimension MUST NOT naturally generate:

- grass
- trees
- flowers
- crops
- animals
- hostile mobs
- villages
- structures with life

All biology is player-created.

---

## 3.3 Engineering Survival

Player survival depends on:

- oxygen production
- water management
- pressure systems
- industrial automation

---

## 3.4 Deterministic Procedural Universe

Worldgen MUST be:

- deterministic
- async-safe
- multiplayer-safe
- progression-safe

---

# 4. THE DEEP SPACE DIMENSION

Dimension ID: