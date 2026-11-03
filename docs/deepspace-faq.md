# faq.md

## Deep Space Dimension — Implementation FAQ & Mandatory Rules

This document clarifies **non-obvious design decisions** and prevents incorrect implementation assumptions.

---

# 1. Warp Drive Clarification

⚠️ IMPORTANT

The **Warp Drive system is NOT implemented by the Deep Space / Asteroid Dimension mod.**

The crashed spaceship contains:

* a **broken warp drive multiblock**
* placeholder components
* repair objectives

However:

> Warp functionality will be implemented in a **separate future mod**.

This mod ONLY provides:

* structure
* progression hooks
* repair targets

No teleportation or warp mechanics are coded here.

---

# 2. Player Spawn Requirement (MANDATORY)

Every player MUST:

* Spawn on **their own crashed spaceship**
* Located on a **personal starter asteroid**
* Generated using player UUID seed

There is NEVER a shared starting location.

---

# 3. Respawn Rules

If player dies:

| Condition  | Respawn Location  |
| ---------- | ----------------- |
| Bed exists | Bed               |
| No bed     | Crashed spaceship |

Implementation:

```
respawnPoint = crashSiteLocation
```

Crash site acts as permanent fallback spawn.

---

# 4. Why Personal Crash Sites Exist

Prevents:

* resource griefing
* early competition
* progression blocking
* multiplayer imbalance

Ensures:

* independent early progression
* shared late-game economy

---

# 5. Oxygen Gameplay Philosophy

Players must:

1. Stabilize oxygen supply.
2. Build plant → biomass → oxygen loop.
3. Industrialize life support.

Oxygen is intended to be the FIRST engineering challenge.

---

# 6. Water Scarcity Rule

Water is intentionally scarce early.

Players must:

* melt ice
* capture fluids
* automate biomass loop

Infinite water sources must NOT exist.

---

# 7. Natural Life Rule

Deep Space contains:

* ❌ No grass
* ❌ No animals
* ❌ No trees
* ❌ No mobs
* ❌ No natural plants

All life must be player-created.

---

# 8. Asteroid Generation Safety

The mod MUST NEVER:

* overwrite FTBChunks claims
* generate inside claimed chunks
* replace existing structures

Generation aborts if conflict detected.

---

# 9. Multiplayer Scaling

Each player has:

* personal early-game region
* shared late-game universe

Difficulty always scales from crash site distance.

---

# 10. Intended Player Progression

1. Spawn → stabilize oxygen
2. Build farm
3. Automate biomass
4. Achieve LV industry
5. Explore outward
6. Repair warp drive (future mod)
7. Build megastructures

---

# 11. Required Mods (Dependencies)

Implementation assumes presence of:

* GregTech CEu Modern 7.5.2
* Ad Astra
* Applied Energistics 2
* FTBChunks
* Architectury API
* GeckoLib (if animated blocks used)
* KubeJS (recipes & balancing)
* Forge 1.20.1

---

# 12. Design Intent Summary

Deep Space is:

* survival engineering
* industrial sandbox
* multiplayer economy foundation

It is NOT:

* a combat dimension
* exploration RPG
* vanilla overworld replacement

---

END FAQ
