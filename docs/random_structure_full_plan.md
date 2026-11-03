Random Structure System — Full Implementation
Context
The DeepSpace mod needs a complete random-structure system: compile-time + runtime NBT loading, a unified StructurePlacer (reusing all crashed-ship logic), entity/NPC export/import, a /importship command, void-exclusion zones, a deterministic sector grid for structures (parallel to PlanetSectorGrid), and a public IStructureRadiusMap API. The crashed-ship first-spawn flow must not be broken.

Implementation Order & Steps
Step 1 — Placeholder NBT files (compile-time structures)
Create three 16×1×16 diamond-block platform .nbt files as placeholder random structures. These are generated programmatically at a development-time tool step and committed as binary. We'll create them as a small standalone Java utility that writes the correct Minecraft 1.20.1 gzip-NBT structure format (DataVersion 3120, size, palette, blocks, entities tags — same as ExportShipCommand writes), then include the resulting files.

Files created:

src/main/resources/data/deepspace/structures/abandoned_station.nbt
src/main/resources/data/deepspace/structures/trader_outpost.nbt
src/main/resources/data/deepspace/structures/wreck_small.nbt
The existing crashed_ship.nbt is not touched.

Step 2 — New data-model classes (package com.deepspace.structures)
2a. MobData.java
Holds per-entity info (spawn coords, entity type, StructureEntityInfo, weak refs to LivingEntity / Entity). Matches the spec exactly — note health field added even though the spec constructor lists it (the stored object block omits it from the field list, so we add it as a field).

2b. StructureLocation.java
(spec calls the class ResourceLocation — we rename to avoid clash with MC's net.minecraft.resources.ResourceLocation)
Fields: String dimensionName, int x, y, z, List<MobData> mobs.

2c. StructureTemplateEntry.java
(spec calls it StructureTemplate — we rename to avoid clash with MC's StructureTemplate)
Fields: String nbtFilename, boolean compileTime.

2d. RegisteredStructure.java
Fields: StructureLocation location, StructureTemplateEntry template, net.minecraft.world.level.levelgen.structure.BoundingBox bounds. Includes List<MobData> mobs delegated to location. Plus the deterministic sector key (sectorX, sectorZ) so the sector grid can look it up.

2e. PlacementContext.java
Exactly as specced. Static factory methods worldgen() and spawned(String username).

Files created:

src/main/java/com/deepspace/structures/MobData.java
src/main/java/com/deepspace/structures/StructureLocation.java
src/main/java/com/deepspace/structures/StructureTemplateEntry.java
src/main/java/com/deepspace/structures/RegisteredStructure.java
src/main/java/com/deepspace/structures/PlacementContext.java
Step 3 — Public API interface
3a. IStructureRadiusMap.java (package com.deepspace.api)
Exactly as specced:

getStructuresInRadius(LevelAccessor, long worldSeed, BlockPos, int radius)
predictStructures(ResourceLocation dimension, long worldSeed, BlockPos, int radius)
getLoadedStructures(ServerLevel)
findNearestStructure(LevelAccessor, BlockPos, int maxDistance)
getStructuresByTemplate(ResourceLocation templateId)
registerStructure(RegisteredStructure)
unregisterStructure(RegisteredStructure)
tickMobValidation(ServerLevel)
File created:

src/main/java/com/deepspace/api/IStructureRadiusMap.java
Step 4 — DeepSpaceStructureRegistry.java (package com.deepspace.structures)
Responsibilities:

On startup & /reload: scan data/deepspace/structures/*.nbt (resource manager) + FMLPaths.CONFIGDIR/deepspace/structures/*.nbt (filesystem Files.walk, only .nbt extension).
Runtime config files override compile-time files with the same filename (crashed_ship.nbt overrides the bundled one for the crashed ship spawn path; all other filenames are also overridden).
The pool for random generation excludes crashed_ship.nbt.
Provides getAllNames() (returns all filenames for command autocomplete).
Provides getPool() (random-generation pool, excludes crashed_ship.nbt).
Provides getTemplate(String filename) → CompoundTag (gzip NBT, loaded eagerly at startup).
Implements ResourceManagerReloadListener to hot-reload on /reload.
Creates the config dir if missing.
Skips/logs invalid NBT non-fatally.
File created:

src/main/java/com/deepspace/structures/DeepSpaceStructureRegistry.java
Step 5 — StructureSectorGrid.java (package com.deepspace.structures)
Deterministic sector-based random-structure placement grid — mirrors PlanetSectorGrid but for structures:

Sector size: STRUCTURE_SECTOR_SIZE_CHUNKS — must be 10× less dense than celestial bodies.
PLANET_SECTOR_SIZE_CHUNKS = 32 → planets spawn in 25% of sectors.
Structures must be 10× rarer overall: use STRUCTURE_SECTOR_SIZE_CHUNKS = 96 (3× bigger sectors) + STRUCTURE_SPAWN_CHANCE_PCT = 30 (vs planet 25%) — net effect ≈ 10× rarer.
Bias multiplier: if the chunk centre is within 256 blocks (16 chunks) of any PlanetCandidate, spawn chance is doubled (STRUCTURE_SPAWN_CHANCE_PCT * 2 = 60%).
Pure function of (worldSeed, sectorX, sectorZ) — no mutable state, thread-safe.
Returns StructureSectorCandidate (centerX, centerY, centerZ, nbtFilename, sectorX, sectorZ, seed).
Y is chosen within [DSConstants.ASTEROID_BAND_Y_MIN, DSConstants.ASTEROID_BAND_Y_MAX].
nbtFilename is chosen deterministically from the registry pool using the sector seed.
resolve(worldSeed, cx, cz) returns all StructureSectorCandidates whose void zone can reach chunk (cx, cz) — analogous to PlanetSectorGrid.resolve().
File created:

src/main/java/com/deepspace/structures/StructureSectorGrid.java
Step 6 — StructureRadiusMapImpl.java (package com.deepspace.structures)
Implements IStructureRadiusMap. Singleton accessible via StructureRadiusMapImpl.INSTANCE.

Key behaviours:

loadedStructures: ConcurrentHashMap<String dimensionId, List<RegisteredStructure>> — populated when chunks generate/structures are placed.
registerStructure(RegisteredStructure s): adds to loadedStructures.
unregisterStructure(RegisteredStructure s): removes.
getStructuresInRadius(...): union of getLoadedStructures(level) filtered by radius + predictStructures(...) for not-yet-loaded chunks (deduped by sector key).
predictStructures(dimension, worldSeed, center, radius): calls StructureSectorGrid.resolve(...) for all sectors within radius, returns their RegisteredStructure wrappers (no chunk loading required).
tickMobValidation(ServerLevel): every 100 ticks, iterates all RegisteredStructures for the level, iterates their MobData.entityLiving weak refs — if entity is non-null, loaded, and entity.isDeadOrDying() → remove from mobs list. Entities that are merely unloaded (chunk not active) are kept.
Exposed as public static final IStructureRadiusMap INSTANCE = new StructureRadiusMapImpl();
File created:

src/main/java/com/deepspace/structures/StructureRadiusMapImpl.java
Step 7 — StructurePlacer.java (package com.deepspace.structures)
The unified placement engine. Replaces / wraps the duplicated ship-placement logic in CrashedShipPlacer.

Public API:
public static PlacementResult placeStructure(
    ServerLevel level,
    BlockPos origin,         // desired centre; will spiral-search for safe location
    RegisteredStructure structure,
    PlacementContext context
)
Returns PlacementResult enum: SPAWNED, ERROR_CHUNK_CLAIMED, ERROR_SAFE_LOCATION, ERROR_INVALID_NBT, ERROR_FILENAME_MISSING, ERROR_ENTITY_MOD_MISSING, …

Placement steps (strict order per spec):
1. Safe-location search (spherical spiral)

Computes bounding box from the StructureTemplate (MC's StructureTemplate.getSize()).
Adds 4-block padding on all sides.
Spherical spiral search outward from origin: checks each candidate BlockPos — the entire padded cuboid volume must contain only air/void (no non-air blocks).
FTB Chunks check (only for /importship, not worldgen): if any chunk in the structure footprint is claimed by a player OTHER than the USERNAME argument (or their team), skip that position.
Returns the first safe BlockPos.
2. Block placement

Loads StructureTemplate via StructureTemplateManager (tries resource location first, then raw NBT from registry cache).
StructurePlaceSettings settings = new StructurePlaceSettings(); settings.setFinalizeEntities(true); — no rotation, no mirror.
Calls template.placeInWorld(level, safeOrigin, safeOrigin, settings, level.getRandom(), 2).
3. Entity/mob restoration (new — not in CrashedShipPlacer)

After block placement: template.placeEntities(...) is already called by placeInWorld when setFinalizeEntities(true).
Alternatively, if we use the raw NBT path: iterate entities list in the NBT, call EntityType.loadEntityRecursive(entityNbt, level, ...) for each, regenerate UUID on collision.
Tracks spawned LivingEntity refs → MobData → stored in RegisteredStructure.location.mobs.
4. GregTech rebuild pass

Reuses CrashedShipPlacer.reinitPipeConnections(level, safeOrigin) logic — BFS over PipeBlockEntity instances in the structure footprint + 2-block border, enqueues PendingPipeSync.
Multiblock validation: scan for IMultiblockController-implementing block entities; call the checkStructurePattern() or equivalent method via reflection (same pattern as the existing GTCEuCompat reflection approach) so multiblocks self-assemble.
5. Chunk sync

Calls level.sendBlockUpdated(...) for each changed block (or uses the existing setChanged() / PENDING_SYNCS approach).
6. Register with StructureRadiusMap

Creates RegisteredStructure and calls StructureRadiusMapImpl.INSTANCE.registerStructure(s).
File created:

src/main/java/com/deepspace/structures/StructurePlacer.java
Also add PlacementResult.java enum.

Step 8 — Update CrashedShipPlacer.java
Delegate to StructurePlacer for the block-placement + pipe-sync + machine-init path, so there is zero duplicated logic. The crashed-ship specific steps (starter asteroid, bubble data persistence, MeDrive init) remain in CrashedShipPlacer.

Changes:

place(ServerLevel, CrashBubbleData) method calls StructurePlacer.placeStructure(level, surfaceOrigin, crashedShipRegisteredStructure, PlacementContext.worldgen()) for the actual block/entity placement.
reinitPipeConnections and initMachines become static helpers called from StructurePlacer internally (moved there), and CrashedShipPlacer delegates.
PENDING_SYNCS queue and onServerTickEnd handler move to StructurePlacer (or remain in CrashedShipPlacer but are invoked via StructurePlacer).
No existing functionality is removed — same outcome, just deduped.
File modified:

src/main/java/com/deepspace/crash/CrashedShipPlacer.java
Step 9 — Fix /exportship — entity saving
Current problem: line 217 in ExportShipCommand.java: root.put("entities", new ListTag()) always produces an empty entities list. Entities inside the WorldEdit selection are NOT saved.

Fix:

After the block-entry loop, scan level.getEntities(EntityTypeTest.forClass(Entity.class), new AABB(minX, minY, minZ, maxX+1, maxY+1, maxZ+1), e -> true).
For each entity: call entity.saveWithoutId(new CompoundTag()) to get full NBT (includes VillagerData, Offers, Brain, etc.).
Build the vanilla structure NBT entity entry format:
CompoundTag entityEntry = new CompoundTag();
entityEntry.put("nbt", entityNbt);
ListTag posTag = new ListTag(); // relative double pos
entityEntry.put("pos", posTag);
ListTag blockPosTag = new ListTag(); // integer blockPos relative
entityEntry.put("blockPos", blockPosTag);
Collect all into ListTag entitiesTag and add to root as root.put("entities", entitiesTag).
Also update the command to accept an optional filename argument: /exportship [FILENAME] (if not provided, use crashed_ship_<timestamp> as before).
File modified:

src/main/java/com/deepspace/commands/ExportShipCommand.java
Step 10 — ImportShipCommand.java (package com.deepspace.commands)
New command: /importship <filename> [USERNAME]

Permission model:
src.hasPermission(2) — op level 2+.
Server console: src.getEntity() == null → requires USERNAME argument; if missing → fail with message.
FTB Quests elevated-permission execution: treated as console (no entity source), USERNAME required.
Player execution: USERNAME is optional; if omitted, uses executor's position.
Filename autocomplete:
SuggestionProviders returning DeepSpaceStructureRegistry.getInstance().getAllNames().
USERNAME autocomplete:
All online player names (level.getServer().getPlayerList().getPlayerNames()).
Behaviour:
Resolve filename → RegisteredStructure from registry (fail with ERROR_FILENAME_MISSING if not found).
Resolve target player (USERNAME if provided, else executor).
Call StructurePlacer.placeStructure(level, targetPos, structure, PlacementContext.spawned(username)).
Always send result message to USERNAME player (if online): [DeepSpace] [StructurePlacer] <filename> for <USERNAME> @ <dimension> [x,y,z] - RESULT: SPAWNED or RESULT: ERROR_xxx.
Log to server log with [DeepSpace_Structures] prefix.
FTB Chunks integration:
Uses FTBChunksCompat.isChunkClaimedBy(level, cx, cz, teamMemberUUIDs) — extended version — to allow placement on chunks owned by the USERNAME or their FTB Teams team.
File created:

src/main/java/com/deepspace/commands/ImportShipCommand.java
Step 11 — FTBChunksCompat.java — extend for team-aware check
Add method:

public static boolean isChunkClaimedByOther(ServerLevel level, int chunkX, int chunkZ, Set<UUID> allowedUUIDs)
Uses reflection to check ClaimedChunk.getTeamId() and FTBTeamsAPI to resolve team members — falls back to isChunkClaimed if reflection fails.

File modified:

src/main/java/com/deepspace/compat/FTBChunksCompat.java
Step 12 — Hook random structure generation into DeepSpaceChunkGenerator
In applyBiomeDecoration(...), after the planet pass and before the asteroid pass:

// --- Random structure pass ---
StructureSectorGrid.resolve(worldSeed, cx, cz)
    .forEach(candidate -> {
        // Check void suppression: if structure's void zone covers this chunk → insideVoid = true
        // Actual structure placement deferred to main thread
        if (shouldPlaceStructure(candidate, level, cx, cz)) {
            ServerLevel sl = ...;
            sl.getServer().execute(() -> {
                placeRandomStructure(sl, candidate);
            });
        }
    });
Also update the insideVoid check to include structure void zones:

Structure void radius = max(sizeX, sizeY, sizeZ) * 1.5 + 32 blocks (spec: larger structure = more void).
This is computed from the cached bounding box of each structure template (loaded at startup in registry).
File modified:

src/main/java/com/deepspace/dimension/DeepSpaceChunkGenerator.java
Step 13 — DSConstants.java — add structure constants
Add:

// --- Random Structures ---
public static final int STRUCTURE_SECTOR_SIZE_CHUNKS = 96;
public static final int STRUCTURE_SPAWN_CHANCE_PCT   = 30;
public static final int STRUCTURE_SECTOR_SEARCH_RADIUS = 3;
public static final int STRUCTURE_CELESTIAL_BIAS_RADIUS_BLOCKS = 256;
public static final int STRUCTURE_VOID_PADDING_BLOCKS = 32;
public static final double STRUCTURE_VOID_RADIUS_MULTIPLIER = 1.5;
File modified:

src/main/java/com/deepspace/DSConstants.java
Step 14 — DeepSpaceMod.java — register new event handlers and registry
Register ImportShipCommand on the forge bus.
Register DeepSpaceStructureRegistry as a ResourceManagerReloadListener (via AddReloadListenerEvent).
Register StructureRadiusMapImpl.INSTANCE tick handler (server tick event for tickMobValidation).
Call DeepSpaceStructureRegistry.getInstance().loadRuntimeStructures() in commonSetup.
Create config/deepspace/structures/ dir during commonSetup.
File modified:

src/main/java/com/deepspace/DeepSpaceMod.java
Step 15 — DSWorldData.java — add placed-structure persistence
Add a new section to the saved data to persist placed random structures (so they survive server restarts):

private final Map<String, PlacedStructureRecord> placedStructures = new ConcurrentHashMap<>();
Where PlacedStructureRecord holds (dimensionId, x, y, z, nbtFilename, sectorKey).

This is serialized in save() / loaded in load().

When StructurePlacer places a worldgen structure, it calls DSWorldData.getOrCreate(level).recordPlacedStructure(...).

File modified:

src/main/java/com/deepspace/crash/DSWorldData.java
Step 16 — NBT file generation utility (build-time)
Write a small standalone main-method class GeneratePlaceholderNbts.java (not a Forge class, just a standalone Java class with a main method that can be run to output the three NBT files) that writes the 16×1×16 diamond-block platform in exact Minecraft 1.20.1 gzip-NBT format. We generate the binary files and commit them.

Since we cannot run code, we will write the NBT binary directly by hand by constructing the byte-exact gzip-compressed NBT using Java's NbtIo.write format specification, encoding the three identical files programmatically in the apply_patch step.

Alternative: Write GeneratePlaceholderNbts.java as a utility and include the NBT data as base64-encoded resource to be decoded at startup if the files are missing — but this adds runtime complexity. Instead, we will generate the binary .nbt files correctly using a Python/manual byte-construction approach in the patch step — see implementation note below.

Files created:

src/main/resources/data/deepspace/structures/abandoned_station.nbt (binary)
src/main/resources/data/deepspace/structures/trader_outpost.nbt (binary)
src/main/resources/data/deepspace/structures/wreck_small.nbt (binary)
Implementation note: These are generated as correctly-formatted gzip-compressed NBT. The mod also includes a NbtPlaceholderGenerator that runs during FMLCommonSetupEvent to auto-generate any missing placeholder structure files to config/deepspace/structures/ if needed, ensuring no startup crash if the binary resources are malformed.

Summary of All Files
New Files
File	Purpose
structures/MobData.java	Entity data holder
structures/StructureLocation.java	Placement location + alive mob list
structures/StructureTemplateEntry.java	NBT filename + compile/runtime flag
structures/RegisteredStructure.java	Full structure record
structures/PlacementContext.java	Worldgen vs admin spawn context
structures/PlacementResult.java	Result enum
structures/DeepSpaceStructureRegistry.java	NBT loader (compile+runtime), hot-reload
structures/StructureSectorGrid.java	Deterministic structure placement grid
structures/StructurePlacer.java	Unified placement engine
structures/StructureRadiusMapImpl.java	IStructureRadiusMap implementation
api/IStructureRadiusMap.java	Public API interface
commands/ImportShipCommand.java	/importship command
Resources: abandoned_station.nbt, trader_outpost.nbt, wreck_small.nbt	Placeholder structures
Modified Files
File	Change
commands/ExportShipCommand.java	Add entity export; add optional FILENAME arg
crash/CrashedShipPlacer.java	Delegate placement to StructurePlacer; move shared logic
crash/DSWorldData.java	Add placed-structure persistence
dimension/DeepSpaceChunkGenerator.java	Hook random structure pass + void exclusion
DSConstants.java	Add structure constants
DeepSpaceMod.java	Register new handlers/commands/registry
compat/FTBChunksCompat.java	Add team-aware claim check
Verification Steps
Build: gradlew build — zero compile errors.
Structure loading: On server start, log shows [DeepSpace_Structures] [INFO] entries for each loaded .nbt file.
/exportship with a villager in selection: exported NBT contains non-empty entities list; re-import via /importship spawns villager with trades intact.
/importship myship.nbt: structure spawns at nearest safe air location to player; no blocks overwrite existing blocks; result message appears in player chat.
/importship myship.nbt PLAYER from console: structure spawns near named player; named player receives result chat message.
Random structures: flying through deep space reveals diamond-block platforms 10× less frequently than celestial bodies; platforms have void space around them; no asteroids spawn inside void zone.
Determinism: delete world, recreate with same seed — structures appear at identical coordinates.
IStructureRadiusMap: another mod (or test class) can call StructureRadiusMapImpl.INSTANCE.predictStructures(deepSpaceRL, worldSeed, pos, 2000) and get back structure locations matching what actually generates.
Crashed ship unbroken: new player first join still spawns crashed ship on asteroid with full GT pipe/network connectivity.
Mob tick: kill a villager that was spawned with a structure; within ~5 seconds it is removed from RegisteredStructure.location.mobs.
Files to modify
src/main/java/com/deepspace/structures/MobData.java
src/main/java/com/deepspace/structures/StructureLocation.java
src/main/java/com/deepspace/structures/StructureTemplateEntry.java
src/main/java/com/deepspace/structures/RegisteredStructure.java
src/main/java/com/deepspace/structures/PlacementContext.java
src/main/java/com/deepspace/structures/PlacementResult.java
src/main/java/com/deepspace/structures/DeepSpaceStructureRegistry.java
src/main/java/com/deepspace/structures/StructureSectorGrid.java
src/main/java/com/deepspace/structures/StructurePlacer.java
src/main/java/com/deepspace/structures/StructureRadiusMapImpl.java
src/main/java/com/deepspace/api/IStructureRadiusMap.java
src/main/java/com/deepspace/commands/ImportShipCommand.java
src/main/java/com/deepspace/commands/ExportShipCommand.java
src/main/java/com/deepspace/crash/CrashedShipPlacer.java
src/main/java/com/deepspace/crash/DSWorldData.java
src/main/java/com/deepspace/dimension/DeepSpaceChunkGenerator.java
src/main/java/com/deepspace/DSConstants.java
src/main/java/com/deepspace/DeepSpaceMod.java
src/main/java/com/deepspace/compat/FTBChunksCompat.java
src/main/resources/data/deepspace/structures/abandoned_station.nbt
src/main/resources/data/deepspace/structures/trader_outpost.nbt
src/main/resources/data/deepspace/structures/wreck_small.nbt