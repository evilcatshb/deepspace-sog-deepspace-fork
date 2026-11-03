# Ship-to-Ship Combat System — Technical Requirements Document
## WarpDrive Mod Integration | Minecraft Forge 1.20.1 | Forge 47.2.0

---

## Table of Contents

1. [Codebase Baseline & Conventions](#1-codebase-baseline--conventions)
2. [Critical Bug — Existing Laser Renderer (Fix First)](#2-critical-bug--existing-laser-renderer-fix-first)
3. [UUID & Ship Identity System](#3-uuid--ship-identity-system)
4. [Targetable Block Registry](#4-targetable-block-registry)
5. [New Blocks — Weapons Core & Turrets](#5-new-blocks--weapons-core--turrets)
6. [New Block Entities](#6-new-block-entities)
7. [Projectile Entity System](#7-projectile-entity-system)
8. [Client-Side Visual Rendering](#8-client-side-visual-rendering)
9. [Targeting & Combat Logic](#9-targeting--combat-logic)
10. [Network Packets](#10-network-packets)
11. [GUI System](#11-gui-system)
12. [WarpDrive Teleport Persistence](#12-warpdrive-teleport-persistence)
13. [CC:Tweaked Integration](#13-cctweaked-integration)
14. [Ship Ownership & Faction System](#14-ship-ownership--faction-system)
15. [Registration Changes Summary](#15-registration-changes-summary)
16. [NBT Persistence Reference](#16-nbt-persistence-reference)
17. [Implementation Checklist](#17-implementation-checklist)

---

## 1. Codebase Baseline & Conventions

### 1.1 Project Configuration (Do Not Change)

All new work must conform to the existing project setup. Nothing below changes.

| Setting | Value |
|---|---|
| Package root | `com.warpdrive` |
| Mod ID | `warpdrive` (from `WarpDriveMod.MOD_ID`) |
| Forge version | `1.20.1-47.2.0` |
| Mappings | `channel: 'official', version: '1.20.1'` |
| Mixin config | `warpdrive.mixins.json` via `MixinConfigs` JAR manifest |
| Energy system | GTCEu `IEnergyContainer` via `GTCapability.CAPABILITY_ENERGY_CONTAINER` |
| Energy tier for turrets | Match laser tier: ZPM (`131_072 EU/t`) |
| Build tool | Gradle with `gradlew` wrapper |

### 1.2 Existing Patterns All New Code Must Follow

**Block registration** — add entries to `WarpDriveBlocks.BLOCKS` (`DeferredRegister<Block>` in `WarpDriveBlocks.java`). New blocks must also be registered via `registerBlockItems()` which already iterates all entries automatically.

**Block entity registration** — add entries to `WarpDriveBlockEntities.BLOCK_ENTITIES` (`DeferredRegister<BlockEntityType<?>>` in `WarpDriveBlockEntities.java`).

**Client renderer registration** — add calls to `WarpDriveClientSetup.onRegisterRenderers()` via `EntityRenderersEvent.RegisterRenderers`. This method is already subscribed on the MOD bus at `Dist.CLIENT`.

**Network packets** — register all new packets in `WarpDriveNetwork.init()` using the existing `packetId++` counter pattern and `CHANNEL.registerMessage(...)`.

**Capability re-arming** — any block entity that exposes `LazyOptional` capabilities must override both `invalidateCaps()` and `reviveCaps()` exactly as `ShipLaserBlockEntity` does. Forgetting `reviveCaps()` is what causes the GTMThings wireless cover bug on chunk reload (BUG_4).

**Mod-load guards** — all optional integration code must be gated behind `WarpDriveMod.CC_TWEAKED_LOADED`, `WarpDriveMod.GTCEU_LOADED`, etc. No new boolean flags should be introduced for mods already tracked in `WarpDriveMod`.

**Cover support** — any new block entity that players may attach GTMThings wireless energy covers to must follow the `WarpDriveCoverHolder` pattern from `ShipLaserBlockEntity`: lazy-initialise on `getCoverHolder()`, tick in `serverTick()`, persist with `coverHolder.saveToTag(tag)` / `loadFromTag(tag)`, and clean up in `setRemoved()` / `onLoad()`.

### 1.3 Integration Libraries Already on the Classpath

The following jars are already in `libs/` as `compileOnly` dependencies. No new Gradle entries are needed.

- `cc-tweaked-1_20_1-forge-1_113_1.jar` — CC:Tweaked peripheral API
- `gtceu-1_20_1-7_5_2.jar` — Energy capability (`IEnergyContainer`, `GTCapability`)
- `gtmthings-1.6.0.jar` — Wireless energy covers (`WarpDriveCoverHolder`)
- `ftb-chunks-forge-2001_3_6.jar` — Chunk protection checks (reuse `canModifyBlock` pattern)
- `ftb-teams-forge-2001.3.1.jar` — Team/faction UUID lookups

---

## 2. Critical Bug — Existing Laser Renderer (Fix First)

The mining laser and salvage laser produce no visible beam in the current build. This must be diagnosed and fixed before any combat rendering is built, because all combat projectile rendering reuses the same rendering pipeline.

### 2.1 Root Causes

**Bug A — Line width silently ignored by the OpenGL core profile.**
`ShipLaserRenderer` calls `RenderSystem.lineWidth(3.0f)` but Minecraft 1.20.1 runs on an OpenGL 3.2+ core profile where `glLineWidth` values above `1.0` are undefined behaviour and silently clamped to `1.0` on most drivers. The three parallel-offset passes meant to fake thickness all collapse onto the same sub-pixel line, producing nothing visible at typical zoom distances.

**Fix:** Replace the three-pass line offset approach with a quad-strip (two triangles per beam segment) drawn in `VertexFormat.Mode.QUADS` using `DefaultVertexFormat.POSITION_COLOR`. Compute the beam's screen-space perpendicular each frame using the camera direction, expand the beam geometry by the desired pixel width in world space, and render the result as a flat billboard quad. This is the same technique used by Minecraft's own beacon beam and leash rendering.

**Bug B — `syncClient()` uses flag `2` (`Block.UPDATE_CLIENTS`) which does not guarantee immediate packet dispatch.**
`sendBlockUpdated(worldPosition, state, state, 2)` marks the block for a batched network update. Under high server tick load or when the chunk dirty-flag hasn't been flushed, the update may not reach the client within the same tick, or may be dropped entirely if the chunk is at the edge of render distance. This causes the beam to flicker or never appear.

**Fix:** Replace the `sendBlockUpdated` call inside `syncClient()` with a direct `WarpDriveNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(...), new LaserBeamSyncPacket(...))`. This sends the beam position immediately and reliably to all clients watching the chunk. This packet must be added to `WarpDriveNetwork.init()`. All new combat rendering must use direct channel packets — never `sendBlockUpdated` — for latency-sensitive visual state.

**Bug C — Static `CLIENT_BEAM_TARGET` / `CLIENT_IS_ACTIVE` / `CLIENT_IS_SALVAGE` maps are never cleared on dimension change.**
If a player travels between dimensions the maps retain stale entries keyed to `BlockPos` values that may collide with positions in the new dimension, causing phantom beams.

**Fix:** Subscribe to `LevelEvent.Unload` on the Forge bus at `Dist.CLIENT` and clear all three static maps when `event.getLevel().isClientSide()` is true.

### 2.2 Beam Rendering Architecture (Corrected, Reused by Combat)

The corrected laser renderer and all new combat beam / projectile trail renderers must follow this structure:

```
RenderLevelStageEvent.Stage.AFTER_PARTICLES
  └─ RenderSystem: disable depth test, enable blend, disable cull
  └─ Shader: GameRenderer::getPositionColorShader  (NOT Lines shader)
  └─ VertexFormat.Mode.QUADS + DefaultVertexFormat.POSITION_COLOR
  └─ Per beam: compute billboard quad from (origin → target) and camera right-vector
  └─ Draw 4 vertices forming a screen-aligned flat quad of configurable world-space width
  └─ RenderSystem: restore all state
```

The `ShipLaserRenderer` class must be refactored to implement this. All new turret beam and projectile trail renderers extend the same approach rather than implementing their own GL state management.

---

## 3. UUID & Ship Identity System

### 3.1 What Already Exists

`ShipCoreBlockEntity` already contains a `private UUID coreId = UUID.randomUUID()`. This field is the ship's persistent identity and is already serialised to NBT via the existing `saveAdditional` / `load` cycle (field name `"CoreId"` — verify the exact key in the existing NBT write block and use it consistently).

### 3.2 What Needs to Be Added

The existing `coreId` is sufficient as `ship_uuid` for all combat systems. No second UUID needs to be introduced on `ShipCoreBlockEntity`. The combat system reads `coreId` via a new public accessor `getShipUUID()` which simply returns `coreId`.

**Add to `ShipCoreBlockEntity`:**

```java
// New public accessor — do not rename or regenerate coreId
public UUID getShipUUID() { return coreId; }
```

The `WeaponsCoreBlockEntity` (new) generates its own `core_uuid` on first placement and stores `ship_uuid` copied from `ShipCoreBlockEntity.getShipUUID()` at link time. Turret block entities receive both `ship_uuid` and `linked_core_uuid` from the Targeting Link Tool during the linking interaction.

### 3.3 UUID Assignment Rules

| Block Entity | UUID Source | When Generated | Persistence Key |
|---|---|---|---|
| `ShipCoreBlockEntity` | `coreId` (existing) | First placement | `"CoreId"` (existing) |
| `WeaponsCoreBlockEntity` | New `core_uuid` field | First placement | `"CoreUUID"` |
| Any turret BE | `turret_uuid` field | First placement | `"TurretUUID"` |
| Any damageable block BE | `block_uuid` field | On registration | `"BlockUUID"` |

**Rule:** In every `load()` / `readFromNBT()` method, check for the UUID key before generating. Generate only if the key is absent. Never regenerate unconditionally.

```java
// Correct pattern — used in every BE that owns a UUID
if (tag.hasUUID("TurretUUID")) {
    turretUUID = tag.getUUID("TurretUUID");
} else {
    turretUUID = UUID.randomUUID(); // first placement only
}
```

---

## 4. Targetable Block Registry

### 4.1 Purpose

The combat targeting system needs to know which blocks on an enemy ship are damageable, what subsystem they belong to, and where they are — both before and after a WarpDrive teleport (which shifts positions).

### 4.2 Where the Registry Lives

The registry is stored inside `ShipCoreBlockEntity` as a new field:

```java
// New field on ShipCoreBlockEntity
private final Map<UUID, DamageableBlockEntry> blockRegistry = new LinkedHashMap<>();

public static class DamageableBlockEntry {
    public BlockPos pos;
    public String  subsystem; // "engine" | "weapon" | "shield" | "hull"
    public int     health;    // 0–100
}
```

This is serialised to NBT inside `ShipCoreBlockEntity.saveAdditional()` under the key `"BlockRegistry"` as a `ListTag` of `CompoundTag`. Each entry stores `block_uuid` (UUID), `pos` (x/y/z ints), `subsystem` (String), and `health` (int).

### 4.3 What Blocks Are Registered

The following existing block types are automatically registered as damageable when the ship is scanned (scanning already exists via `ShipGraph` / `ShipScanner`):

| Block Class | Subsystem Tag |
|---|---|
| `ShipThrusterBlock` (all tiers) | `"engine"` |
| `WeaponsCoreBlock` (new) | `"weapon"` |
| All turret blocks (new) | `"weapon"` |
| `ShipNavCoreBlock` | `"weapon"` |
| `ShipCoreBlock` | `"hull"` |
| `ShipHullBlock` T3 / T4 | `"shield"` |
| All other `ShipHullBlock` tiers | `"hull"` |

### 4.4 Registry Rebuild

The registry must be rebuilt in two situations:

**Situation 1 — Full rebuild on ship scan.** The existing `ShipScanner` / `ShipGraph` pipeline already walks every block in the ship structure. A new `BlockRegistryBuilder` class (in new package `com.warpdrive.combat`) hooks into the scan completion callback and populates the registry by iterating the scanned block set, checking each block type against the table above, assigning a `block_uuid` (or reading the existing one from the block entity's NBT if present), and writing the entry.

**Situation 2 — Position-only rebuild after WarpDrive teleport.** Block UUIDs survive teleport. Only positions change. The `onShipRelocated()` hook (see Section 12) calls `blockRegistry.forEach((uuid, entry) -> entry.pos = newPositionOf(uuid))` where `newPositionOf` looks up the UUID in the world's loaded block entities.

### 4.5 Block Entity Requirement for Damageable Blocks

Blocks that participate in the registry need a way to store their `block_uuid` persistently so the registry can re-read it after a reload. For new combat blocks (turrets, weapons core) the UUID lives in their own `BlockEntity`. For existing hull and thruster blocks, a lightweight `BlockEntity` is not appropriate.

**Solution:** Store `block_uuid` in the `blockRegistry` map only (inside `ShipCoreBlockEntity`). The registry is the authoritative source. Block UUIDs are rebuilt from the registry on reload, not from individual block NBT. This avoids adding block entities to every hull block.

---

## 5. New Blocks — Weapons Core & Turrets

### 5.1 New Block Classes

All new block classes go in `com.warpdrive.block`. Follow `ShipCoreBlock` as the template: extend `BaseEntityBlock`, implement `FACING` blockstate, override `getRenderShape` to return `MODEL`, implement `getTicker`, and open the GUI in `use()` via `NetworkHooks.openScreen`.

| Class | Registry Name | BE Class |
|---|---|---|
| `WeaponsCoreBlock` | `weapons_core` | `WeaponsCoreBlockEntity` |
| `LaserTurretBlock` | `laser_turret` | `LaserTurretBlockEntity` |
| `BallisticTurretBlock` | `ballistic_turret` | `BallisticTurretBlockEntity` |
| `MissileLauncherBlock` | `missile_launcher` | `MissileLauncherBlockEntity` |
| `PdcTurretBlock` | `pdc_turret` | `PdcTurretBlockEntity` |
| `EmpTurretBlock` | `emp_turret` | `EmpTurretBlockEntity` |

**Block properties for all new combat blocks:**

```java
BlockBehaviour.Properties.of()
    .mapColor(MapColor.METAL)
    .strength(5.0f, 10.0f)
    .sound(SoundType.METAL)
    .noOcclusion()
```

**Destroy behaviour:** All combat blocks use `ShipHullWrenchHandler.destroyProgress(player, 60)` for wrench-only destruction, exactly matching `ShipCoreBlock`. This prevents accidental breaking during combat.

### 5.2 Cover Support

`WeaponsCoreBlock` exposes a `WarpDriveCoverHolder` via `getCoverHolder()` following the `ShipCoreBlock` / `ShipLaserBlockEntity` pattern so that GTMThings wireless energy covers can be attached to it. Individual turret blocks do not need cover support — they draw power from the weapons core, not directly from a GT cable.

### 5.3 Registration in `WarpDriveBlocks`

Add to the `BLOCKS` register after the existing laser blocks:

```java
// ── Combat Blocks ──────────────────────────────────────────────────────────
public static final RegistryObject<Block> WEAPONS_CORE =
    BLOCKS.register("weapons_core", () -> new WeaponsCoreBlock(...));

public static final RegistryObject<Block> LASER_TURRET =
    BLOCKS.register("laser_turret", () -> new LaserTurretBlock(...));

public static final RegistryObject<Block> BALLISTIC_TURRET =
    BLOCKS.register("ballistic_turret", () -> new BallisticTurretBlock(...));

public static final RegistryObject<Block> MISSILE_LAUNCHER =
    BLOCKS.register("missile_launcher", () -> new MissileLauncherBlock(...));

public static final RegistryObject<Block> PDC_TURRET =
    BLOCKS.register("pdc_turret", () -> new PdcTurretBlock(...));

public static final RegistryObject<Block> EMP_TURRET =
    BLOCKS.register("emp_turret", () -> new EmpTurretBlock(...));
```

`registerBlockItems()` already iterates all entries, so block items are registered automatically.

---

## 6. New Block Entities

All new block entity classes go in `com.warpdrive.blockentity`. All follow the same lifecycle pattern as `ShipLaserBlockEntity`: `saveAdditional` / `load` for persistence, `getUpdateTag` / `handleUpdateTag` for client sync, `getUpdatePacket` returning `ClientboundBlockEntityDataPacket.create(this)`, `invalidateCaps` / `reviveCaps` for capability lifecycle.

### 6.1 `WeaponsCoreBlockEntity`

This is the central controller. It owns the faction config, targeting state, the linked turret list, and the GUI state. It ticks on the server side every tick to manage the combat loop.

**Fields:**

```java
private UUID   coreUUID    = UUID.randomUUID();  // generated once on placement
private UUID   shipUUID    = null;               // copied from ShipCoreBlockEntity at link time

private boolean enabled    = true;
private String  fireMode   = "auto_attack";      // "auto_attack" | "manual" | "retaliate"

private String  faction    = "neutral";
private List<String> hostileTo  = new ArrayList<>();
private List<String> friendlyTo = new ArrayList<>();

private String  targetingMode       = "closest"; // "closest" | "highest_threat" | "manual"
private UUID    currentTargetUUID   = null;      // ship_uuid of current target

private int     scanIntervalTicks   = 40;
private int     scanCooldown        = 0;

// Linked turrets: UUID → turret type string
private final Map<UUID, String> linkedTurrets = new LinkedHashMap<>();

// GUI state (persisted so GUI reopens on the correct tab after server restart)
private String  guiSelectedTab = "main";

// Energy — same IEnergyContainer pattern as ShipLaserBlockEntity
private long    energyBuffer   = 0L;

// Cover support — same pattern as ShipCoreBlockEntity
private WarpDriveCoverHolder coverHolder;
private LazyOptional<ICoverable> coverableCap = LazyOptional.empty();
```

**Server tick responsibilities:**

1. Tick `coverHolder` if GTMThings is loaded.
2. Decrement `scanCooldown`. When it reaches zero, run a scan for enemy ships within range. Reset `scanCooldown = scanIntervalTicks`.
3. If `currentTargetUUID` is non-null, verify the target ship still exists and is in range. Clear the target if not.
4. If `fireMode == "auto_attack"` and a target exists, instruct all linked turrets to fire at the target's registered blocks according to each turret's `priority` setting.
5. Each turret fires independently (not via the weapons core tick) — the weapons core tick only decides the target and sends the fire command.

**NBT — `saveAdditional`:** Write all fields. Use `tag.putUUID`, `tag.putString`, `tag.putBoolean`, and `tag.put("LinkedTurrets", ListTag)` for the turret map. Never skip a field.

**Client sync — `getUpdateTag`:** Send the power state, enabled flag, current target UUID (as string), number of linked turrets, and gui state. Turret-level detail is not needed on the client for the HUD — only for the open GUI, which fetches from the menu's server-side container.

### 6.2 `BaseTurretBlockEntity`

Abstract base class for all five turret types. Extends `BlockEntity` directly (not `ShipLaserBlockEntity`). Contains all shared turret state.

**Fields:**

```java
protected UUID   turretUUID      = UUID.randomUUID();
protected UUID   shipUUID        = null;
protected UUID   linkedCoreUUID  = null;

protected String turretType;   // set by subclass constructor

protected boolean enabled       = true;
protected boolean firing        = false;
protected boolean holdFire      = false;

protected int    cooldownTicks  = 0;
protected int    heat           = 0;
protected int    maxHeat;       // set by subclass
protected int    heatPerShot;   // set by subclass
protected int    cooldownPerTick = 2;

protected int    range          = 128;
protected String targetPriority = "engines"; // "engines" | "weapons" | "random"
protected String fireModeLocal  = "burst";   // "burst" | "continuous"

// Energy drawn per shot from the linked WeaponsCoreBlockEntity energy buffer
protected long   energyPerShot;

// Power state cached from the weapons core energy check
protected boolean hasPower = false;
```

**Server tick responsibilities:**

1. Decrement `cooldownTicks` if > 0.
2. If `cooldownTicks == 0` and `firing == true` and `hasPower == true` and `!holdFire`:
   - Resolve target block position from `WeaponsCoreBlockEntity.blockRegistry` using `targetPriority`.
   - Spawn the appropriate projectile entity at this turret's world position aimed at the resolved target.
   - Subtract `heatPerShot` from the weapons core energy buffer (access via server-level lookup by `linkedCoreUUID`).
   - Increment `heat`. If `heat >= maxHeat`, set `firing = false` and begin cooldown.
   - Reset `cooldownTicks` to the turret-type's base cooldown value.
3. Passively decrement `heat` by `cooldownPerTick` each tick (floor 0).
4. Sync client when `firing` or `heat` changes.

**NBT:** All fields persisted. UUID fields use `tag.putUUID` / `tag.getUUID`. Never skip any field.

### 6.3 Concrete Turret Block Entities

Each subclass of `BaseTurretBlockEntity` sets its type-specific constants in the constructor. The projectile type spawned is determined by `turretType`.

| Class | `turretType` | `maxHeat` | `heatPerShot` | `cooldownTicks` | `energyPerShot` |
|---|---|---|---|---|---|
| `LaserTurretBlockEntity` | `"laser"` | 100 | 10 | 4 | 32768 |
| `BallisticTurretBlockEntity` | `"ballistic"` | 60 | 5 | 10 | 8192 |
| `MissileLauncherBlockEntity` | `"missile"` | 40 | 20 | 40 | 65536 |
| `PdcTurretBlockEntity` | `"pdc"` | 80 | 3 | 2 | 4096 |
| `EmpTurretBlockEntity` | `"emp"` | 100 | 50 | 100 | 131072 |

### 6.4 Registration in `WarpDriveBlockEntities`

Add after the existing `SHIP_SALVAGE_LASER` entry:

```java
// ── Combat Block Entities ─────────────────────────────────────────────────
public static final RegistryObject<BlockEntityType<WeaponsCoreBlockEntity>> WEAPONS_CORE =
    BLOCK_ENTITIES.register("weapons_core", () ->
        BlockEntityType.Builder.of(WeaponsCoreBlockEntity::new,
            WarpDriveBlocks.WEAPONS_CORE.get()).build(null));

public static final RegistryObject<BlockEntityType<LaserTurretBlockEntity>> LASER_TURRET =
    BLOCK_ENTITIES.register("laser_turret", () ->
        BlockEntityType.Builder.of(LaserTurretBlockEntity::new,
            WarpDriveBlocks.LASER_TURRET.get()).build(null));

// ... repeat for BALLISTIC_TURRET, MISSILE_LAUNCHER, PDC_TURRET, EMP_TURRET
```

---

## 7. Projectile Entity System

### 7.1 New Registry File — `WarpDriveEntities`

Create `com.warpdrive.registry.WarpDriveEntities`:

```java
public class WarpDriveEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WarpDriveMod.MOD_ID);

    public static final RegistryObject<EntityType<LaserBoltEntity>> LASER_BOLT =
        ENTITIES.register("laser_bolt", () -> EntityType.Builder
            .<LaserBoltEntity>of(LaserBoltEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(128)
            .updateInterval(1)
            .build("laser_bolt"));

    // ... BALLISTIC_ROUND, MISSILE, PDC_ROUND, EMP_PULSE
}
```

Register in `WarpDriveMod` constructor:

```java
WarpDriveEntities.ENTITIES.register(modBus);
```

### 7.2 `BaseProjectileEntity`

All projectile types extend this. Place in `com.warpdrive.entity.projectile`.

**Key design decision:** Projectiles are standard `Entity` subclasses (not `Projectile` subclasses) to avoid vanilla collision/physics assumptions. Motion is entirely controlled in `tick()`.

**Fields:**

```java
protected UUID   ownerShipUUID;    // ship_uuid of the firing ship
protected UUID   targetBlockUUID;  // block_uuid being aimed at
protected Vec3   targetPos;        // world-space position resolved at fire time

protected float  speed;            // blocks per tick, set by subclass
protected int    maxLifeTicks;     // despawn timer
protected int    lifeTicks = 0;
protected int    damage;           // damage dealt on impact (block health reduction)

protected boolean hasHit = false;
```

**Server-side `tick()` logic:**

```java
@Override
public void tick() {
    super.tick();
    if (level().isClientSide) return;

    lifeTicks++;
    if (lifeTicks >= maxLifeTicks || hasHit) {
        discard();
        return;
    }

    // Move along straight line toward targetPos
    Vec3 pos = position();
    Vec3 dir = targetPos.subtract(pos).normalize();
    Vec3 next = pos.add(dir.scale(speed));

    // Check for block hit along the path (AABB ray cast)
    BlockHitResult hit = level().clip(new ClipContext(
        pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

    if (hit.getType() != HitResult.Type.MISS) {
        onBlockHit(hit.getBlockPos());
        hasHit = true;
        return;
    }

    // Check arrival at target
    if (next.distanceTo(targetPos) < speed) {
        onArrive();
        hasHit = true;
        return;
    }

    setPos(next);
    setDeltaMovement(dir.scale(speed));
}

protected abstract void onBlockHit(BlockPos hitPos);
protected abstract void onArrive();
```

**Client-side:** Projectile entities are tracked by Forge's standard entity tracking system (`clientTrackingRange(128)`, `updateInterval(1)`). The client receives entity spawn packets automatically. Rendering is handled by a dedicated `EntityRenderer` registered in `WarpDriveClientSetup`.

### 7.3 Concrete Projectile Classes

**`LaserBoltEntity`** — moves at 4.0 blocks/tick. Max life 80 ticks (range ~320 blocks). On block hit or arrival: deal `damage` to the target block's `health` field in the `ShipCoreBlockEntity.blockRegistry`. No physical block break — only health reduction. Emit impact particle packet.

**`BallisticRoundEntity`** — moves at 3.0 blocks/tick. Max life 60 ticks. On block hit: deal damage to block health AND apply a `0.15` knockback impulse to any entity within 2 blocks of impact using `entity.push()`. Emit smoke + spark particle packet.

**`MissileEntity`** — moves at 1.5 blocks/tick, accelerates by +0.05 each tick up to 3.0 blocks/tick. Has a `thrusterTrail` boolean that is true, used by the renderer to draw a particle trail. On arrive: deal high `damage` in a 4-block radius (AOE — damages all block registry entries within radius 4 of the target position). Emit large explosion particle packet.

**`PdcRoundEntity`** — moves at 5.0 blocks/tick. Max life 30 ticks. Intended to intercept incoming missiles. On arrival at a `MissileEntity`'s position: call `missile.hasHit = true` to destroy it. If no missile intercept, deals light damage on block hit.

**`EmpPulseEntity`** — moves at 2.0 blocks/tick. Max life 100 ticks. On arrive: instead of dealing `health` damage, sets `hasPower = false` on all `BaseTurretBlockEntity` instances within 16 blocks of impact for 200 ticks. Also sets `energyBuffer = 0` on `WeaponsCoreBlockEntity` of the target ship.

### 7.4 Projectile Spawning

Projectiles are spawned exclusively on the server inside `BaseTurretBlockEntity.tick()`. After spawning, Forge's entity tracking system handles replication to all watching clients automatically. No manual packet is needed for entity spawn.

```java
// Inside BaseTurretBlockEntity — example for LaserTurretBlockEntity
LaserBoltEntity bolt = new LaserBoltEntity(level, worldPosition.getCenter());
bolt.setOwnerShipUUID(shipUUID);
bolt.setTargetPos(resolvedPos);
level.addFreshEntity(bolt);
```

### 7.5 Damage Application

When a projectile impacts, it must apply damage to the target block's `health` in the owning ship's `ShipCoreBlockEntity.blockRegistry`. The process:

1. The projectile knows `targetBlockUUID`.
2. Use `ShipCoreBlockEntity.findByShipUUID(level, targetShipUUID)` — a new static helper method that looks up `ShipCoreBlockEntity` instances via `ShipChunkIndex` (already exists in the codebase).
3. Call `core.applyDamage(targetBlockUUID, damageAmount)` on the found core.
4. `applyDamage` reduces `health` in the `blockRegistry`. If `health <= 0`, call `core.destroyBlock(blockUUID)` which calls `level.destroyBlock(pos, false)` and removes the entry from the registry.
5. Call `core.setChanged()` and `core.syncClient(level)` after any registry mutation.

---

## 8. Client-Side Visual Rendering

### 8.1 Fixed Laser Beam Renderer (`ShipLaserRenderer`)

Rewrite the existing `ShipLaserRenderer` to replace the broken Lines shader approach with a billboard quad approach. The class stays in `com.warpdrive.client` and keeps its `@Mod.EventBusSubscriber` annotation. The static maps (`CLIENT_BEAM_TARGET`, `CLIENT_IS_ACTIVE`, `CLIENT_IS_SALVAGE`) stay on `ShipLaserBlockEntity` — they are correct in concept, only the rendering technique changes.

**New rendering approach in `onRenderLevel`:**

```
// Per beam entry in CLIENT_BEAM_TARGET:
// 1. Compute world-space direction vector: dir = (target - origin).normalize()
// 2. Compute the camera's right vector: right = dir.cross(cameraForward).normalize()
// 3. Expand the beam into a quad:
//    v0 = origin + right * halfWidth
//    v1 = origin - right * halfWidth
//    v2 = target - right * halfWidth
//    v3 = target + right * halfWidth
// 4. Write 4 POSITION_COLOR vertices in QUADS mode
// 5. Use GameRenderer::getPositionColorShader
// 6. Alpha-blend the beam color with a = 0.85f
```

The `halfWidth` in world-space should be approximately `0.04f` for the mining laser (tight beam) and `0.08f` for the salvage laser (slightly fatter).

**Clearing the maps on dimension change** — add to a new `@Mod.EventBusSubscriber(value = Dist.CLIENT)` listener class `WarpDriveClientEvents`:

```java
@SubscribeEvent
public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
        ShipLaserBlockEntity.CLIENT_BEAM_TARGET.clear();
        ShipLaserBlockEntity.CLIENT_IS_ACTIVE.clear();
        ShipLaserBlockEntity.CLIENT_IS_SALVAGE.clear();
    }
}
```

### 8.2 Projectile Renderers

One `EntityRenderer` per projectile type. Register all in `WarpDriveClientSetup.onRegisterRenderers`:

```java
event.registerEntityRenderer(WarpDriveEntities.LASER_BOLT.get(),     LaserBoltRenderer::new);
event.registerEntityRenderer(WarpDriveEntities.BALLISTIC_ROUND.get(), BallisticRoundRenderer::new);
event.registerEntityRenderer(WarpDriveEntities.MISSILE.get(),         MissileRenderer::new);
event.registerEntityRenderer(WarpDriveEntities.PDC_ROUND.get(),       PdcRoundRenderer::new);
event.registerEntityRenderer(WarpDriveEntities.EMP_PULSE.get(),       EmpPulseRenderer::new);
```

All renderers extend `EntityRenderer<T extends BaseProjectileEntity>`. They do not use models or textures — they render procedural geometry directly in `render()` using `PoseStack` + `MultiBufferSource`, which is the standard MC 1.20.1 entity renderer approach.

**`LaserBoltRenderer`** — renders a short glowing line segment (billboard quad, length = 1.5 blocks, width = 0.06 blocks). Colour: bright red/orange (`R=1.0, G=0.3, B=0.0, A=1.0`). The `EntityRenderer.render()` method receives the current partial tick and uses `Mth.lerp(partialTick, entity.xOld, entity.getX())` etc. for smooth interpolation.

**`BallisticRoundRenderer`** — renders a small dark grey elongated cylinder approximated as a billboard quad. Length = 0.4 blocks, width = 0.05 blocks. Uses `RenderType.entitySolid(ROUND_TEXTURE)` with a simple greyscale texture (`textures/entity/ballistic_round.png` — a 16×16 dark grey oval on transparent background).

**`MissileRenderer`** — renders a textured quad using `textures/entity/missile.png` (a 16×32 rocket sprite). Additionally, on each client tick, emits 1–2 `ThrusterParticle` instances at the missile's tail position offset by `(-direction * 0.5f)` for the thruster trail. Particle spawning uses `level.addParticle(WarpDriveParticles.THRUSTER.get(), ...)`.

**`PdcRoundRenderer`** — tiny bright white quad (length = 0.2 blocks, width = 0.03 blocks). Very fast movement makes it appear as a streak due to motion blur from the short segment length vs speed.

**`EmpPulseRenderer`** — renders an expanding semi-transparent sphere using a `RenderType.translucent()` buffer. Starting radius = 0.3 blocks, expanding by `+0.05` per tick via `entity.lifeTicks` in the renderer. Colour: cyan-blue (`R=0.0, G=0.8, B=1.0, A=0.4`). The sphere is approximated with a UV sphere using `TRIANGLE_FAN` tesselation.

### 8.3 Particle Types

Add to a new `com.warpdrive.registry.WarpDriveParticles`:

```java
public static final DeferredRegister<ParticleType<?>> PARTICLES =
    DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, WarpDriveMod.MOD_ID);

public static final RegistryObject<SimpleParticleType> MUZZLE_FLASH =
    PARTICLES.register("muzzle_flash", () -> new SimpleParticleType(false));

public static final RegistryObject<SimpleParticleType> IMPACT_SPARK =
    PARTICLES.register("impact_spark", () -> new SimpleParticleType(false));

public static final RegistryObject<SimpleParticleType> THRUSTER_TRAIL =
    PARTICLES.register("thruster_trail", () -> new SimpleParticleType(false));
```

Register in `WarpDriveMod` constructor:

```java
WarpDriveParticles.PARTICLES.register(modBus);
```

Client-side particle factories are registered in `WarpDriveClientSetup` via `RegisterParticleProvidersEvent`. Each particle class goes in `com.warpdrive.client.particle`. They extend `TextureSheetParticle` using the existing MC particle rendering pipeline — no custom GL state.

---

## 9. Targeting & Combat Logic

### 9.1 `CombatManager` — New Class

Place in `com.warpdrive.combat.CombatManager`. This is a server-side singleton (one per `ServerLevel`, stored as a `LevelAttachment` via Forge's `AttachCapabilitiesEvent<Level>`, or more simply as a plain `HashMap<UUID, CombatSession>` held statically and cleared on `LevelEvent.Unload`).

`CombatManager` is responsible for:

- Tracking which `WeaponsCoreBlockEntity` is actively engaging which target ship.
- Running the inter-ship target scan every `scanIntervalTicks`.
- Resolving which blocks on the target ship to engage.

### 9.2 Target Scan Process

The scan runs inside `WeaponsCoreBlockEntity.tick()` when `scanCooldown` reaches zero:

1. Get the ship's current position from `ShipCoreBlockEntity.getShipUUID()`.
2. Query `ShipChunkIndex` (already exists) for all `ShipCoreBlockEntity` instances within range (default 128 blocks centre-to-centre).
3. Filter by faction rules: skip any ship whose `owner_id` is in `friendlyTo`. Include ships whose `owner_id` is in `hostileTo` OR whose faction is hostile.
4. Select the best target according to `targetingMode`:
   - `"closest"` — ship with smallest centre-to-centre distance.
   - `"highest_threat"` — ship with the most `"weapon"` subsystem blocks in its registry (most armed).
   - `"manual"` — no automatic selection; keep `currentTargetUUID` as set via GUI or CC:Tweaked.
5. Write `currentTargetUUID` to NBT immediately via `setChanged()`.
6. Fire `"ship_target_locked"` CC:Tweaked event if target changed.

### 9.3 Target Resolution for Turrets

When `BaseTurretBlockEntity` needs a block to aim at:

1. Get `WeaponsCoreBlockEntity` via `linkedCoreUUID`.
2. Get the target ship's `ShipCoreBlockEntity` via `currentTargetUUID`.
3. Get the target ship's `blockRegistry`.
4. Filter by `targetPriority`:
   - `"engines"` → entries where `subsystem == "engine"`.
   - `"weapons"` → entries where `subsystem == "weapon"`.
   - `"random"` → all entries.
5. Select a random entry from the filtered list using `level.random`.
6. Return `entry.pos`.

If the filtered list is empty (all blocks of that subsystem destroyed), fall back to `"random"`. If the registry is empty, the target ship is effectively dead — set `currentTargetUUID = null` and fire the `"ship_destroyed"` CC:Tweaked event.

### 9.4 Fire Mode Behaviour

| Mode | Behaviour |
|---|---|
| `auto_attack` | Scan for targets automatically. Fire all linked turrets when target acquired. |
| `retaliate` | Do not scan proactively. Only acquire a target when `"ship_under_attack"` is fired by `applyDamage`. Then behave like `auto_attack`. |
| `manual` | No automatic scanning or firing. Only fire when CC:Tweaked calls `weapons.fireAll()` or when the player presses Fire in the GUI. |

---

## 10. Network Packets

All packets are registered in `WarpDriveNetwork.init()` using the existing counter pattern. All are placed in `com.warpdrive.network`.

### 10.1 `LaserBeamSyncPacket` (fix for Bug B)

**Direction:** Server → Client (tracking chunk)
**Purpose:** Replace the broken `sendBlockUpdated` approach for laser beam sync.
**Contents:** `BlockPos laserPos`, `BlockPos targetPos`, `boolean active`, `boolean isSalvage`.
**Handling:** Client handler updates `ShipLaserBlockEntity.CLIENT_BEAM_TARGET`, `CLIENT_IS_ACTIVE`, `CLIENT_IS_SALVAGE` directly.

### 10.2 `WeaponsCoreStatePacket`

**Direction:** Server → Client (tracking chunk)
**Purpose:** Sync weapons core power state, current target, and enabled flag to clients watching the block.
**Contents:** `BlockPos corePos`, `boolean enabled`, `String targetUUID`, `int linkedTurretCount`, `String fireMode`.
**When sent:** On any state change in `WeaponsCoreBlockEntity` (target acquired/lost, mode changed, enabled toggled).

### 10.3 `TurretStatePacket`

**Direction:** Server → Client (tracking chunk)
**Purpose:** Sync turret heat, firing state, and hold-fire flag to clients.
**Contents:** `BlockPos turretPos`, `boolean firing`, `int heat`, `boolean holdFire`.
**When sent:** Whenever `firing` or `heat` changes in `BaseTurretBlockEntity`. Throttle to once per 4 ticks minimum to avoid packet floods.

### 10.4 `WeaponsCoreUpdatePacket`

**Direction:** Client → Server
**Purpose:** Send GUI changes (fire mode, targeting mode, faction config, hold-fire toggles) from the client to the server.
**Contents:** `BlockPos corePos`, `CompoundTag changes` (a CompoundTag containing only the fields that changed — allows partial updates).
**Security:** Server handler validates that the sending player is the ship owner (`ShipCoreBlockEntity.ownerUUID`) before applying any change.

### 10.5 `ProjectileImpactEffectPacket`

**Direction:** Server → Client (tracking chunk)
**Purpose:** Tell clients to spawn impact particles at the hit location. Projectile entities are already tracked by Forge's entity system so the entity movement is already synced. This packet is only needed for the one-shot impact effect at the moment of hit (the entity despawns on hit, so there's no entity update to piggyback on).
**Contents:** `Vec3 impactPos`, `String projectileType`.
**Handling:** Client spawns the appropriate particle burst at `impactPos`.

---

## 11. GUI System

### 11.1 `WeaponsCoreMenu`

Create `com.warpdrive.gui.WeaponsCoreMenu`. Extend `AbstractContainerMenu`. Follow the pattern of `ShipLaserMenu` (which already exists): accept `FriendlyByteBuf` in the network constructor to read the block's world position, look up the BE, and use `ContainerLevelAccess.create` for the stillValid check.

The weapons core GUI has no item slots. It is a pure configuration screen. `WeaponsCoreMenu` only needs `stillValid()` and the data sync fields.

Use `addDataSlot(DataSlot.standalone())` for integer values that must be kept in sync between server container and client screen (heat values, turret count, etc.). For string values (fire mode, target UUID), use a `MenuType`-level sync by overriding `broadcastChanges()` to send a `WeaponsCoreStatePacket` whenever these change.

### 11.2 `WeaponsCoreScreen`

Create `com.warpdrive.gui.WeaponsCoreScreen`. Extend `AbstractContainerScreen<WeaponsCoreMenu>`. Follow the pattern of `ShipLaserScreen` for the background texture rendering and button layout.

**Tab layout:**

| Tab | Contents |
|---|---|
| `main` | Enabled toggle, fire mode dropdown, targeting mode dropdown, current target display |
| `turrets` | Scrollable list of linked turrets with UUID, type, heat bar, hold-fire toggle, fire-mode toggle per turret |
| `faction` | Faction name text field, hostile-to list (add/remove), friendly-to list (add/remove) |
| `registry` | Read-only list of all blocks in `blockRegistry` showing UUID, subsystem, health bar |

Each tab is a sub-panel rendered conditionally based on `selectedTab`. Tab buttons use `Button` widget added in `init()`. All config changes are sent immediately to the server via `WeaponsCoreUpdatePacket`.

### 11.3 `WeaponsCoreMenuType` Registration

Add to `WarpDriveMenuTypes`:

```java
public static final RegistryObject<MenuType<WeaponsCoreMenu>> WEAPONS_CORE =
    MENU_TYPES.register("weapons_core", () ->
        IForgeMenuType.create(WeaponsCoreMenu::new));
```

### 11.4 Opening the GUI

In `WeaponsCoreBlock.use()`, following the `ShipCoreBlock.use()` pattern exactly:

```java
NetworkHooks.openScreen(sp, weaponsCore, weaponsCore.getExtraData());
```

Where `getExtraData()` writes `worldPosition` to a `FriendlyByteBuf`. The `WeaponsCoreMenu` network constructor reads this position and looks up the BE — same pattern as `ShipCoreMenu`.

---

## 12. WarpDrive Teleport Persistence

### 12.1 Where to Hook

The existing warp logic lives in `ShipCoreBlockEntity`. The warp state machine uses `WarpState` and `WarpPlan`. After a successful warp, blocks are moved via WarpDrive's internal block copy/paste mechanism. The post-warp callback point must be identified in `ShipCoreBlockEntity`'s warp completion logic.

Search for where `warpState` is set back to `WarpState.IDLE` after a successful teleport. The post-warp rebuild calls go immediately after this state transition, still inside `ShipCoreBlockEntity` (not in a separate system), to keep all ship state management in one class.

### 12.2 `onShipRelocated()` — New Method on `ShipCoreBlockEntity`

```java
public void onShipRelocated() {
    // 1. Rebuild block registry positions — UUIDs stay the same, positions shift
    rebuildBlockRegistryPositions();

    // 2. Tell the weapons core (if present) to rebuild its turret resolution cache
    if (weaponsCorePos != null) {
        BlockEntity be = level.getBlockEntity(weaponsCorePos);
        if (be instanceof WeaponsCoreBlockEntity wc) {
            wc.onShipRelocated();
        }
    }
}

private void rebuildBlockRegistryPositions() {
    // Walk the current shipGraph (which has already been updated by WarpDrive's
    // block-move pipeline) and for each block in the registry, find the new
    // position by scanning the graph for a block whose UUID matches.
    // If not found (block was destroyed during warp), remove from registry.
    for (Map.Entry<UUID, DamageableBlockEntry> entry : blockRegistry.entrySet()) {
        BlockPos newPos = findBlockByUUID(entry.getKey());
        if (newPos != null) {
            entry.getValue().pos = newPos;
        } else {
            entry.getValue().health = 0; // mark destroyed
        }
    }
    blockRegistry.entrySet().removeIf(e -> e.getValue().health <= 0);
    setChanged();
}
```

`findBlockByUUID` walks the loaded blocks in the ship graph (available via the existing `shipGraph` field) and checks each block entity's stored UUID. This is O(n) over ship blocks but runs once per teleport — acceptable.

### 12.3 `WeaponsCoreBlockEntity.onShipRelocated()`

```java
public void onShipRelocated() {
    // Re-resolve all linked turret positions by UUID lookup
    // The linkedTurrets map stores UUID → type; clear the runtime position cache
    resolvedTurretPositions.clear(); // new Map<UUID, BlockPos> field
    for (UUID tUUID : linkedTurrets.keySet()) {
        BlockPos pos = findTurretByUUID(tUUID);
        if (pos != null) resolvedTurretPositions.put(tUUID, pos);
    }
    // Re-verify target ship still exists
    if (currentTargetUUID != null) {
        ShipCoreBlockEntity target = ShipCoreBlockEntity.findByShipUUID(
            (ServerLevel) level, currentTargetUUID);
        if (target == null) currentTargetUUID = null;
    }
    setChanged();
}
```

### 12.4 Chunk Load / Reload Survival

All combat state is in NBT (`saveAdditional` / `load`). As long as every field is written in `saveAdditional` and read in `load`, the system survives chunk unload/reload automatically. There are no transient-only fields in any combat block entity. The resolved turret position cache (`resolvedTurretPositions`) is transient — it is rebuilt in `onLoad()` using the same UUID-lookup walk used by `onShipRelocated()`.

---

## 13. CC:Tweaked Integration

### 13.1 Where to Add Code

`CCTweakedIntegration.java` already exists at `com.warpdrive.integration.cctweaked`. Follow the same pattern used there for any existing peripherals.

The new `WeaponsCorePeripheral` class registers itself via Forge capability on the `WeaponsCoreBlockEntity`. `CCTweakedIntegration.init()` registers the capability binding:

```java
// Inside CCTweakedIntegration.init()
CapabilityUtil.registerBlockEntityProvider(
    WarpDriveBlockEntities.WEAPONS_CORE.get(),
    WeaponsCorePeripheral::new);
```

The peripheral is only instantiated when CC:Tweaked is loaded (`WarpDriveMod.CC_TWEAKED_LOADED` guard in `WeaponsCoreBlockEntity.getCapability`).

### 13.2 Required API Methods

All methods are annotated `@LuaFunction` and may only be called from a CC computer whose block entity's level position shares the same `ship_uuid` as the weapons core, OR whose UUID is in the weapons core's `ccWhitelist` set (configurable via the GUI's faction tab).

```lua
-- Returns array of { uuid, type, heat, maxHeat, firing, holdFire } for each turret
weapons.getTurrets()

-- Returns the ship_uuid string of the current target, or nil
weapons.getTarget()

-- Sets the current target by ship_uuid string. Returns true on success.
-- Only works when fireMode == "manual"
weapons.setTarget(uuid)

-- Commands all enabled, non-hold-fire turrets to fire one burst immediately.
-- Only works when fireMode == "manual"
weapons.fireAll()

-- Sets fire mode. Valid values: "auto_attack", "manual", "retaliate"
weapons.setFireMode(mode)

-- Returns { available, capacity } EU values from the weapons core energy buffer
weapons.getPowerState()

-- Returns the block registry as { [uuid] = { subsystem, health, x, y, z } }
weapons.getBlockRegistry()

-- Sets hold_fire on a specific turret by UUID. value = true or false
weapons.setHoldFire(turretUUID, value)
```

### 13.3 Required Events

Events are queued via `ComputerCraftAPI.queueEvent` and fired by the server-side combat logic:

| Event Name | Payload | Fired When |
|---|---|---|
| `ship_target_locked` | `{ target_uuid, subsystem_targeted }` | `currentTargetUUID` changes to non-nil |
| `ship_under_attack` | `{ attacker_uuid, damage, block_uuid }` | `applyDamage()` is called on this ship |
| `turret_overheated` | `{ turret_uuid }` | `heat >= maxHeat` in a turret |
| `power_critical` | `{ available, required }` | Energy buffer < one shot's cost |
| `ship_destroyed` | `{ ship_uuid }` | Target's block registry is empty (all blocks health 0) |

Events are fired in the relevant server-side methods: `applyDamage` fires `ship_under_attack`; turret tick fires `turret_overheated`; scan fires `ship_target_locked`; weapon core tick fires `power_critical`; damage application fires `ship_destroyed`.

---

## 14. Ship Ownership & Faction System

### 14.1 What Already Exists

`ShipCoreBlockEntity` already has:
- `private volatile UUID ownerUUID` — the ship owner's player UUID
- `claimIfUnowned(ServerPlayer)` — claims the ship for the first player to interact
- `getShipName()` / `setShipName()` — ship name management

### 14.2 New Fields on `ShipCoreBlockEntity`

```java
// New fields — added alongside existing ownerUUID
private String ownerType    = "neutral"; // "player" | "faction" | "team" | "neutral"
private String faction      = "neutral"; // faction name string for NPC ships
private List<String> alliedFactions   = new ArrayList<>();
private List<String> hostileFactions  = new ArrayList<>();
```

These are serialised in `saveAdditional` / `load` using `tag.putString` and `ListTag`. `WeaponsCoreBlockEntity` reads these from the linked `ShipCoreBlockEntity` when evaluating whether to fire on a target ship. The weapons core does not duplicate this data — it reads it at scan time via the ship core accessor.

### 14.3 Faction Resolution During Target Scan

In `WeaponsCoreBlockEntity`'s scan, for each candidate target ship `T`:

1. If `T.ownerUUID` == our `ownerUUID` → skip (same player).
2. If FTB Teams is loaded: if `T.ownerUUID` is in the same team as our `ownerUUID` → skip.
3. If `T.faction` is in our `friendlyTo` list → skip.
4. If `T.faction` is in our `hostileTo` list → valid target.
5. If `hostileTo` is empty (attack all) → valid target.
6. Otherwise → skip.

---

## 15. Registration Changes Summary

The following registration files require additions. No existing entries are removed or modified.

### `WarpDriveMod.java` constructor — add:

```java
WarpDriveEntities.ENTITIES.register(modBus);
WarpDriveParticles.PARTICLES.register(modBus);
```

### `WarpDriveBlocks.java` — add 6 entries:
`weapons_core`, `laser_turret`, `ballistic_turret`, `missile_launcher`, `pdc_turret`, `emp_turret`

### `WarpDriveBlockEntities.java` — add 6 entries:
`WEAPONS_CORE`, `LASER_TURRET`, `BALLISTIC_TURRET`, `MISSILE_LAUNCHER`, `PDC_TURRET`, `EMP_TURRET`

### New file `WarpDriveEntities.java` — 5 entity types:
`LASER_BOLT`, `BALLISTIC_ROUND`, `MISSILE`, `PDC_ROUND`, `EMP_PULSE`

### New file `WarpDriveParticles.java` — 3 particle types:
`MUZZLE_FLASH`, `IMPACT_SPARK`, `THRUSTER_TRAIL`

### `WarpDriveMenuTypes.java` — add 1 entry:
`WEAPONS_CORE`

### `WarpDriveNetwork.init()` — add 5 packets:
`LaserBeamSyncPacket`, `WeaponsCoreStatePacket`, `TurretStatePacket`, `WeaponsCoreUpdatePacket`, `ProjectileImpactEffectPacket`

### `WarpDriveClientSetup.onRegisterRenderers` — add:
5 entity renderer registrations, 3 particle factory registrations

### `WarpDriveClientSetup` or new `WarpDriveClientEvents` — add:
`LevelEvent.Unload` subscriber to clear laser beam static maps

---

## 16. NBT Persistence Reference

Complete list of NBT keys written by each new or modified block entity. Every key listed here must be written in `saveAdditional` and read in `load` with no exceptions.

### `ShipCoreBlockEntity` — New Keys Added

| Key | Type | Description |
|---|---|---|
| `"OwnerType"` | String | `"player"`, `"faction"`, `"team"`, or `"neutral"` |
| `"Faction"` | String | Faction name for NPC ships |
| `"AlliedFactions"` | ListTag (String) | List of friendly faction names |
| `"HostileFactions"` | ListTag (String) | List of hostile faction names |
| `"BlockRegistry"` | ListTag (CompoundTag) | Each entry: `"UUID"`, `"X"`, `"Y"`, `"Z"`, `"Subsystem"`, `"Health"` |
| `"WeaponsCorePos"` | CompoundTag | x/y/z of linked weapons core, or absent if unlinked |

### `WeaponsCoreBlockEntity` — All Keys

| Key | Type | Description |
|---|---|---|
| `"CoreUUID"` | UUID | This weapons core's persistent UUID |
| `"ShipUUID"` | UUID | Copied from the owning ShipCoreBlockEntity |
| `"Enabled"` | Boolean | Combat enabled flag |
| `"FireMode"` | String | `"auto_attack"`, `"manual"`, `"retaliate"` |
| `"Faction"` | String | Faction identifier |
| `"HostileTo"` | ListTag (String) | Hostile faction / UUID list |
| `"FriendlyTo"` | ListTag (String) | Friendly faction / UUID list |
| `"TargetingMode"` | String | `"closest"`, `"highest_threat"`, `"manual"` |
| `"CurrentTarget"` | String | `currentTargetUUID.toString()`, or absent |
| `"ScanInterval"` | Int | Scan cooldown in ticks |
| `"LinkedTurrets"` | ListTag (CompoundTag) | Each entry: `"UUID"` (UUID), `"Type"` (String) |
| `"GUITab"` | String | Last open tab name |
| `"Energy"` | Long | Current EU buffer |
| `"CCWhitelist"` | ListTag (String) | CC computer UUIDs allowed to control this core |
| `"WarpDriveCovers"` | CompoundTag | GTMThings cover holder NBT (if GTMThings loaded) |

### `BaseTurretBlockEntity` — All Keys

| Key | Type | Description |
|---|---|---|
| `"TurretUUID"` | UUID | This turret's persistent UUID |
| `"ShipUUID"` | UUID | Owning ship's UUID |
| `"LinkedCoreUUID"` | UUID | UUID of the linked WeaponsCoreBlockEntity |
| `"Enabled"` | Boolean | Turret enabled flag |
| `"HoldFire"` | Boolean | Manual hold-fire flag |
| `"Heat"` | Int | Current heat value |
| `"CooldownTicks"` | Int | Remaining cooldown ticks |
| `"Range"` | Int | Targeting range in blocks |
| `"TargetPriority"` | String | `"engines"`, `"weapons"`, `"random"` |
| `"FireModeLocal"` | String | `"burst"`, `"continuous"` |

---

## 17. Implementation Checklist

Use this checklist to track integration progress. Items are ordered by dependency — each item should be completed before the next.

### Phase 1 — Fix Existing System

- [ ] Diagnose and fix `ShipLaserRenderer` — replace Lines shader with billboard quad approach
- [ ] Add `LaserBeamSyncPacket` to `WarpDriveNetwork`
- [ ] Replace `syncClient()` `sendBlockUpdated` call with direct channel packet in `ShipLaserBlockEntity`
- [ ] Add `LevelEvent.Unload` subscriber to clear static beam maps on dimension change
- [ ] Verify mining laser beam is now visible in-game before proceeding

### Phase 2 — UUID & Registry Foundation

- [ ] Add `getShipUUID()` accessor to `ShipCoreBlockEntity`
- [ ] Add `blockRegistry` field, `DamageableBlockEntry` inner class to `ShipCoreBlockEntity`
- [ ] Implement `saveAdditional` / `load` for all new `ShipCoreBlockEntity` fields
- [ ] Implement `rebuildBlockRegistryPositions()` and `onShipRelocated()` on `ShipCoreBlockEntity`
- [ ] Add `BlockRegistryBuilder` hook into the existing scan completion path
- [ ] Verify registry is populated after a `/wd` scan and survives server restart via NBT

### Phase 3 — New Blocks & Block Entities

- [ ] Create all 6 block classes in `com.warpdrive.block`
- [ ] Register all 6 blocks in `WarpDriveBlocks`
- [ ] Create `BaseTurretBlockEntity` with full NBT, tick, capability, and sync logic
- [ ] Create all 5 concrete turret block entities
- [ ] Create `WeaponsCoreBlockEntity` with full NBT, tick, capability, cover support, and sync logic
- [ ] Register all 6 block entities in `WarpDriveBlockEntities`
- [ ] Add all resource files: blockstate JSONs, block model JSONs, block textures, loot tables, recipes

### Phase 4 — Projectile Entities

- [ ] Create `WarpDriveEntities` registry file
- [ ] Register in `WarpDriveMod` constructor
- [ ] Create `BaseProjectileEntity` with movement and damage logic
- [ ] Create all 5 concrete projectile entity classes
- [ ] Implement `applyDamage()` and `destroyBlock()` on `ShipCoreBlockEntity`
- [ ] Implement `ShipCoreBlockEntity.findByShipUUID()` static helper
- [ ] Verify projectile is spawned server-side, tracked by Forge entity system, and received by clients

### Phase 5 — Client Rendering

- [ ] Create `WarpDriveParticles` registry, register in mod constructor
- [ ] Create 3 particle classes in `com.warpdrive.client.particle`
- [ ] Register particle factories in `WarpDriveClientSetup`
- [ ] Create 5 entity renderer classes in `com.warpdrive.client.renderer`
- [ ] Register entity renderers in `WarpDriveClientSetup.onRegisterRenderers`
- [ ] Add entity model JSON files and textures for ballistic round and missile
- [ ] Verify each projectile type is visible in-game from spawn to impact

### Phase 6 — Network Packets

- [ ] Implement and register all 5 new packets in `WarpDriveNetwork`
- [ ] Implement server handlers for `WeaponsCoreUpdatePacket` (with owner validation)
- [ ] Implement client handlers for all server→client packets
- [ ] Verify GUI changes round-trip correctly: client sends → server applies → server sends state update → client reflects change

### Phase 7 — GUI

- [ ] Create `WeaponsCoreMenu` and register `MenuType`
- [ ] Create `WeaponsCoreScreen` with all 4 tabs
- [ ] Wire all button/field interactions to `WeaponsCoreUpdatePacket`
- [ ] Register screen in `ClientSetup` via `RegisterMenuScreensEvent`
- [ ] Verify GUI opens, tabs switch, config changes persist across server restart

### Phase 8 — Combat Logic

- [ ] Implement faction resolution in target scan
- [ ] Implement `CombatManager` / `WeaponsCoreBlockEntity` target scan loop
- [ ] Implement target resolution in `BaseTurretBlockEntity.tick()`
- [ ] Implement fire mode behaviour (`auto_attack`, `retaliate`, `manual`)
- [ ] Verify turrets fire at the correct blocks based on `targetPriority`
- [ ] Verify block health reduction and block destruction on health reaching zero

### Phase 9 — WarpDrive Teleport Survival

- [ ] Hook `onShipRelocated()` into the warp completion point in `ShipCoreBlockEntity`
- [ ] Implement `WeaponsCoreBlockEntity.onShipRelocated()`
- [ ] Implement `onLoad()` position cache rebuild in `WeaponsCoreBlockEntity`
- [ ] Test: fire a turret, warp the ship, verify turrets are still linked and combat resumes

### Phase 10 — CC:Tweaked Integration

- [ ] Create `WeaponsCorePeripheral` with all `@LuaFunction` methods
- [ ] Register peripheral capability binding in `CCTweakedIntegration.init()`
- [ ] Add `WarpDriveMod.CC_TWEAKED_LOADED` guard in `WeaponsCoreBlockEntity.getCapability`
- [ ] Implement all 5 CC:Tweaked events in the correct server-side methods
- [ ] Test all peripheral API methods from a CC computer on the same ship
- [ ] Test security: verify CC computer on a different ship cannot control this core
