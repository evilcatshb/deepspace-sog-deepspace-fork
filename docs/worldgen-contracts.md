worldgen-contracts.md
# Deep Space Worldgen — Formal Contracts Specification

Version: 1.0
Authority: Deep Space Registry System
Purpose: Define strict interfaces and behavioral guarantees required
for autonomous implementation of Deep Space world generation.

THIS DOCUMENT IS NORMATIVE.
All implementations MUST comply.

---

# 1. TERMINOLOGY

## REQUIRED KEYWORDS

| Keyword | Meaning |
|--------|--------|
| MUST | Mandatory requirement |
| MUST NOT | Absolute prohibition |
| SHOULD | Strong recommendation |
| MAY | Optional behavior |

---

# 2. CORE PRINCIPLES

Implementations MUST guarantee:

1. Deterministic generation
2. Thread safety
3. Crash containment
4. Registry-driven behavior
5. Zero hardcoded progression logic
6. Chunk-local isolation

---

# 3. GLOBAL OBJECT MODEL

---

## 3.1 WorldgenContext

Immutable runtime context.

```ts
interface WorldgenContext {
    worldSeed: long
    dimensionId: string
    registryVersion: string
    registryHash: string
}
Contract

MUST be immutable after creation.

MUST be shared read-only across threads.

3.2 ChunkContext

Created once per chunk generation.

interface ChunkContext {
    chunkX: int
    chunkZ: int
    chunkSeed: long

    zoneId: string
    asteroidSelections: AsteroidSelection[]

    guaranteesPending: string[]
    rng: DeterministicRandom
}
Rules

MUST NOT escape chunk lifecycle.

MUST NOT be cached globally.

4. REGISTRY CONTRACT

Implementation MUST load:

deepspace-registry.json

Required validation:

registry.hash == expectedHash
registry.version compatible

Failure → worldgen disabled.

4.1 Registry Access
interface RegistryProvider {
    Zone getZone(id)
    Asteroid getAsteroid(id)
    Fluid getFluid(id)
}
Threading

RegistryProvider MUST be:

lock-free

immutable

thread-safe

5. ZONE RESOLUTION CONTRACT
interface ZoneResolver {
    string resolveZone(WorldgenContext ctx,
                       int chunkX,
                       int chunkZ,
                       long chunkSeed)
}
Requirements

Resolver MUST:

be deterministic

avoid world access

depend only on inputs + registry

Resolver MUST NOT:

read block states

spawn entities

mutate caches outside ZoneCache

6. ASTEROID SELECTION CONTRACT
interface AsteroidSelector {
    AsteroidSelection[] select(
        Zone zone,
        long chunkSeed
    )
}

Selection MUST:

respect spawn weights

respect progression tiers

be RNG-seed deterministic

AsteroidSelection
interface AsteroidSelection {
    asteroidId: string
    rarityRoll: float
    transformSeed: long
}
7. NOISE GENERATION CONTRACT
interface NoiseGenerator {
    DensityField generate(
        ChunkContext ctx
    )
}
HARD RULES

NoiseGenerator MUST:

✔ be pure
✔ allocate no world objects
✔ use deterministic RNG

NoiseGenerator MUST NOT:

✖ access world instance
✖ access registry mutably
✖ allocate entities

DensityField
interface DensityField {
    float get(x,y,z)
}

Range:

-1.0 → vacuum
+1.0 → solid
8. BLOCK PLACEMENT CONTRACT
interface TerrainBuilder {
    void buildTerrain(
        ChunkContext ctx,
        DensityField density,
        ChunkAccess chunk
    )
}
Rules

MUST execute on main thread

MUST NOT replace player blocks

MUST obey sealable-space rules

9. FLUID INJECTION CONTRACT
interface FluidInjector {
    void apply(
        ChunkContext ctx,
        ChunkAccess chunk
    )
}

Injector MUST:

only place allowed fluids

validate pressure compatibility

avoid exposed vacuum placement

10. FEATURE SYSTEM CONTRACT

Two-phase system REQUIRED.

10.1 Feature Planning (ASYNC)
interface FeaturePlanner {
    PlannedFeature[] plan(ChunkContext ctx)
}

Allowed async operations:

RNG

geometry math

registry reads

10.2 Feature Placement (SYNC)
interface FeaturePlacer {
    void place(
        PlannedFeature feature,
        ChunkAccess chunk
    )
}

Placement MUST occur on main thread.

11. GUARANTEE SYSTEM CONTRACT

Guarantees prevent progression deadlock.

interface GuaranteeManager {
    void evaluate(
        ChunkContext ctx,
        RegionState region
    )
}

Rules:

Guarantees tracked per REGION (32x32 chunks)

MUST NOT duplicate guarantees

MUST persist completion state

12. CRASH BUBBLE CONTRACT (MANDATORY)

All stages MUST run inside:

CrashBubble.execute(stageId, chunkPos, runnable)

Behavior:

If exception occurs:

Catch exception

Log structured error

Mark chunk SAFE_EMPTY

Resume pipeline

Implementation MUST NEVER rethrow.

13. CACHE CONTRACTS
ZoneCache

Key:

(chunkX >> 4, chunkZ >> 4)

Requirements:

bounded size

LRU eviction

thread-safe reads

AsteroidPickCache

Lifetime: chunk-only.

14. DETERMINISM CONTRACT

Implementations MUST guarantee:

F(seed, registry, chunkPos) = identical output

Validation mode MAY compute:

SHA256(blockStates)

Mismatch → determinism violation.

15. THREAD SAFETY MATRIX
Operation	Async Allowed
Noise	YES
Zone resolve	YES
Feature plan	YES
Block placement	NO
Entity spawn	NO
Registry mutation	NEVER
16. EXTENSION API CONTRACT

Mods MAY register:

ZoneModifier
AsteroidModifier
FeatureInjector
GuaranteeProvider

Extensions MUST:

be deterministic

declare priority

avoid world mutation async

17. VERSIONING RULES

Registry version changes MUST:

invalidate caches

recompute guarantees

rebuild zone assignments

18. VALIDATION CONTRACT

Before chunk completion:

Implementation MUST verify:

no illegal fluids

valid block palette

registry references exist

guarantees resolved

Failure → repair pass.

19. FAILURE LEVELS
Level	Required Action
Recoverable	retry stage
Soft	empty asteroid
Hard	void chunk
Critical	disable system
20. IMPLEMENTATION CHECKLIST

An implementation is COMPLIANT only if:

[ ] deterministic output verified
[ ] async rules enforced
[ ] crash bubble wrapping complete
[ ] registry immutable
[ ] guarantees persistent
[ ] cache bounded
[ ] validation pass enabled

END OF SPECIFICATION