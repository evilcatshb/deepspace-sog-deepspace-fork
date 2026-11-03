# deepspace-technical.md

## Deep Space Dimension — Technical Implementation Specification

Authoritative engineering specification for programming AI implementation.

---

# 1. Dependencies (Required Mods)

## Core

* Minecraft Forge 1.20.1
* GregTech CEu Modern
* Ad Astra
* Applied Energistics 2
* FTB Chunks
* KubeJS
* Architectury API
* GeckoLib
* Balm

## Recommended

* Tough As Nails (thirst)
* Botany Pots OR Bonsai Pots

---

# 2. Dimension Registration

Register dimension:

```
deepspace:deep_space
```

Characteristics:

* noise disabled
* custom chunk generator
* asteroid feature pipeline

---

# 3. Personal Seed Algorithm

```
long personalSeed =
    worldSeed ^ playerUUID.getMostSignificantBits()
```

Used ONLY inside crash bubble.

---

# 4. Crash Bubble Layout

Radius: 15 chunks.

Chunk tagging:

```
ChunkData:
  ownerUUID
  bubbleID
  tierOverride=true
```

---

# 5. Spawn Allocation Algorithm

```
for radius += 128 chunks:
   sample spiral points
   if safeRegion():
       reserveRegion()
       break
```

Safety checks:

* no FTBChunks claims
* no existing bubble center <120 chunks
* no structures present

---

# 6. Structure Placement

Load NBT:

```
data/deepspace/structures/crashed_ship.nbt
```

Placement order:

1. asteroid base
2. ship
3. tile entity initialization
4. inventory injection

---

# 8. Asteroid Generation Pipeline

```
ChunkGen →
 ZoneResolver →
 BubbleOverride →
 ArchetypeSelector →
 OreDecorator →
 FluidDecorator
```

---

# 9. Zone Resolver

Distance calculation:

```
distance =
 sqrt((chunkX - crashX)^2 + (chunkZ - crashZ)^2)
```

Tier lookup table applied.

---

# 10. Asteroid Archetype Table

| Archetype   | Weight | Zones |
| ----------- | ------ | ----- |
| Rocky       | 40     | All   |
| Metallic    | 20     | MV+   |
| Ice         | 15     | All   |
| Organic     | 10     | LV–HV |
| Gas         | 6      | MV+   |
| Crystal     | 5      | HV+   |
| Radioactive | 3      | EV+   |
| Exotic      | 1      | IV+   |

---

## Ore Probability (example)

| Tier | Ore Chance |
| ---- | ---------- |
| LV   | 12%        |
| MV   | 18%        |
| HV   | 24%        |
| EV   | 30%        |
| IV+  | 38%        |

---

# 11. Fluid Asteroids

Generate fluid vein matching GregTech vein rules.

Gas asteroids attach gas fluid node.

---

# 12. Oxygen Consumption Calibration

Ad Astra player oxygen drain:

```
1 oxygen unit / tick
20 units / second
```

Target production:

```
≥25 units/sec safety margin
```

---

# 13. Plant → Biomass Processing

### Bonsai Output

Assumption:

```
1 cactus / 40s
8 pots = 0.2 items/sec
```

---

### LV Macerator

```
1 cactus → 2 biochaff
Duration: 4s
EUt: 2
```

---

### Chemical Reactor A (Water Mode)

Circuit 1:

```
4 biochaff →
250 mB Water
Duration: 5s
EUt: 4
```

---

### Chemical Reactor B (Oxygen Mode)

Circuit 2:

```
4 biochaff →
500 mB Oxygen
Duration: 5s
EUt: 4
```

Combined output supports:

```
1 Oxygen Generator @ max rate
```

---

# 14. Ice Mechanics

Vacuum tick event:

```
if water && no atmosphere:
   replace → ice
```

---

### Heater Recipe

```
LV Heater:
1 Ice → 1000 mB Water
1 Exotic Ice → 1,024,000 mB Water
```

---

# 15. Small Breeder Reactor

TileEntity specs:

```
Tier: ZPM
Output: 16A ZPM
Fuel burn: 1728000 ticks
Fuel: uranium_ingot
```

No explosions.
Stops when empty.

---

# 16. FTBChunks Safety Integration

Hook:

```
ClaimManager.isClaimed(pos)
```

Abort generation if true.

---

# 17. Bubble Persistence

Saved in:

```
world/deepspace_bubbles.dat
```

Stores:

* UUID
* center coords
* seed
* reservation radius

---

# 18. Performance Constraints

Must support:

* 100 concurrent bubbles
* 10k asteroids loaded
* async structure placement
* no cascading chunk gen

---

# 19. Deliverables Checklist

Implementation MUST include:

* dimension generator
* bubble allocator
* asteroid generator
* tier resolver
* crash ship loader
* oxygen loop recipes
* exotic ice block
* breeder reactor block
* vacuum freezing system
* claim-safe spawning
* renewable oxygen calibration

END OF TECHNICAL DOCUMENT
