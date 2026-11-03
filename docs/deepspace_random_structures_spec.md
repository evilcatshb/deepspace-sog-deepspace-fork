# DeepSpace Mod --- Random Structure System Technical Specification

**Target:** Minecraft Forge 1.20.1\
**Dimension:** `deepspace:deep_space`\
**Feature Group:** Runtime + Compile-time NBT Structure Generation &
Import/Export System

------------------------------------------------------------------------

# 1. Goals

Add a **unified structure system** allowing:

1.  Randomly generated structures in deep space.
2.  Structures loaded from:
    -   compile-time bundled `.nbt`
    -   runtime config folder `.nbt` ("config/deepspace/structures/*.nbt")
3.  Structures exported with `/exportship [FILENAME]` including **entities + full
    NBT**
4.  Structures spawn with:
    -   blocks
    -   entities/NPCs
    -   GregTech networks
    -   multiblocks
5.  Admin/op/console import command `/importship [FILENAME] [USERNAME]` where FILENAME arg is the .nbt file being loaded, and USERNAME arg is optional, but which username to spawn the structure at based on their current position. this command should use the bounding box a few blocks larger than the max height / x y z size in a cuboid shape around the structure and find a safe area where that bounding box fits closest to the player that doesnt intersect with any other blocks, even the air/void blocks inside the structure should not intersect with any other blocks, that is where the structure is placed when this command is called. the structure is placed in the safe position close to whatever online players username is defined in USERNAME arg, or else if the USERNAME argument not supplied, in the nearest safe location the cuboid bounding box around the structure fits without intersecting with any blocks near the player that ran the commands location/dimension. "/importship" command should work in ANY dimension, not just deepspace. "/importship" command may be run by server/console or by FTB quests with "Run with Elevated Permission" is enabled/true, but when run by server/console the USERNAME argument is required. 

If USERNAME argument is supplied to "/importship" command, the USERNAME specified will ALWAYS be sent in chat a message detailing if the spawning/loading/placement of the structure/ship was a sucess or an error. chat message output sent to the PLAYER argument will look like as follows when the command "/importship ship.nbt BOB":

"[DeepSpace] [StructurePlacer] ship.nbt for BOB @ deepspace:deep_space [-253,-132,143] - RESULT: SPAWNED"               <<< this is the sucess message, if the structure was spawned with no issues/errors
"[DeepSpace] [StructurePlacer] ship.nbt for BOB @ deepspace:deep_space [-253,-132,143] - RESULT: ERROR_CHUNK_CLAIMED"   <<< this is the fail message format, use this but replace ERROR_CHUNK_CLAIMED with whatever error that caused the structure/ship not to be able to be spawned near the player BOB

if no USERNAME argument is supplied to "/importship" then the player that executed the command is used as the center location of where to attempt to spawn the structure
"/importship" and command can only be used by admins/ops/ftb quests/server console
"/exportship" command can only be used by admins/ops and not ftb quests or server console. it should still use the worldedit //wand tool to select the cuboid/box in which to export the structure inside

6.  Shared placement pipeline used by:
    -   spawn crashed ship
    -   random structure generation
    -   admin imports

------------------------------------------------------------------------

# 2. High-Level Architecture

                        ┌─────────────────────┐
                        │ Structure Registry  │
                        │ (NBT Sources)       │
                        └──────────┬──────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │ Unified Structure Placement │
                    │     (StructurePlacer)       │
                    └───────┬───────────┬────────┘
                            │           │
            Random Gen   /importship   Spawn Ship

------------------------------------------------------------------------

# 3. Directory Layout

## 3.1 Compile-Time Structures

    src/main/resources/data/deepspace/structures/
        abandoned_station.nbt
        trader_outpost.nbt
        wreck_small.nbt
		crashed_ship.nbt  << (the "Crashed Ship" structure that every player spawns with on an asteroid when they join the server for the first time.

Loaded via resource manager during datapack reload.
the crashed_ship.nbt that is in the current source tree should remain unmodified.
generate 3x .nbt files all the same with a "minecraft:diamond_block" 16x1x16 x y z one block high square platform, e.g a single block high platform that takes up 16x16 made of minecraft:diamond_block, we will use that as the placeholder data/randomly placed structure for the following files: abandoned_station.nbt, trader_outpost.nbt, wreck_small.nbt

the structure "crashed_ship.nbt" in special, it is only placed, when ANY player is joining the server for the first time on an asteroid. it is not placed randomly in the deepspace dimension as a naturally generating randomly placed structure
crashed_ship.nbt is special not to be placed as a random structure anywhere. only spawned in world when a any player first joins the server (even in multiplayer, so make sure that it will always spawn one crashed_ship.nbt for EVERY player that joins the server for the first time on an asteroid. use the spiral find radius still that must always respect FTB chunks claims/force claims so that no players starting ship is EVER placed on another players FTB chunks claimed/force loaded chunk, and also ensure that the spawn/starting asteroid/crashed space ship location is a safe distance in blocks away from any other players crashed ship/spawn location. use the spiral search to find a safe placement location for every new player that joins the servers own "crashed_ship.nbt" to be spawned at, that isnt within 128 blocks of another players crashed ship spawn location)

------------------------------------------------------------------------

## 3.2 Runtime Structures (User Config)

    config/deepspace/structures/
	
Directory tree created automatically if directories missing.

crashed_ship.nbt     <<< the crashed_ship.nbt file, if it exists in path: config/deepspace/structures/crashed_ship.nbt overwrites the crashed_ship.nbt that is placed when the player first joins the server and a crashed ship is placed for them on an asteroid. so the server owner/admin can define a custom starting crashed space ship for every player that joins the server/starts the game for the first time via just placing this file, instead of having to recompile the whole mod. overwrites the compile-time .nbt at "src\main\resources\data\deepspace\structures\crashed_ship.nbt"

### Schema / Data Model Requirements

Must be built in a way that in the future, other blocks with e.g cc: tweaked perepheral API can find the x y z coordanates of the placed random structures, even if they are not on any loaded chunk / generated yet
All placed random structures should be put into an array with their x y z center of the structures x y z coordinates, so that later developed cc: tweaked block perepheral API can get lists of every random structure in a radius of the players coordinates regardless of if it is spawned yet, on a loaded chunk, or even a chunk that has not been generated yet. it should use predeterminsitic random number generators to choose the locations of the randomply placed structures and what structure random .nbt file is placed at what position to ensure that the same structure will ALWAYS be in the same position given a specific world generation seed, the data schema that keeps track of the coordinates of the random structures should also include the .nbt filename of the structure e.g other_crashed_ship.nbt or space_station.nbt so that the cc: tweaked perepheral api can say the coordinates of the random structure in the radius of the api block, and also know the name of the type of structure that it is.

the randomly placed structures may be multiple chunks wide/high and should be spawned with a cuboid bounding box around it where for a radius around the bounding box is air/void and asteroids or celestial bodies may NEVER spawn in the "structure void spacing" radius around the randomly placed structure.

### Randomly Placed NBT structures Requirements
- the x y z coordinate and type/nbt filename of the structure must be generated with a semirandom deterministic random number generator seeded by the minecraft world seed for that world, so that the same structures will spawn in the same locations given the same world seed, even if the world is deleted and generated multiple times with the same minecraft world seed
- the randomly generated structure will be chosen from a list of filenames (compile-time+runtime), excluding: "crashed_ship.nbt", that is only placed when a new players spawn into the world for the first time.
- if a user puts a file with the same name as any of the compile-time .nbt files, the one in the config/deepspace/structures/ directory should override/replace the one that was included at compile time for the same given filename.
- the randomly placed nbt structures will be 10 times rarer/more difficult to find than "Celestial Body" spheres already being generated in the "deepspace:deep_space" Dimension
- the randomly placed nbt structures will be 2 times more likely to spawn when within the radius of 256 blocks to an existing "Celestial Body" / sphere that is generated in the "deepspace:deep_space" Dimension. 
- the randomly placed nbt structure will have a void/air radius of padding around it the same kind of distance as the "Celestial Body" / spheres have that prevent asteroids from spawning in the radius around the randomly placed nbt structure. the larger the randomly placed nbt structures bounding box the longer/more distance the void/air/empty space that it needs around it before asteroids /celestial bodies/spheres are generated. so larget nbt structure = more void space around the structure. smaller nbt structure = less void space around the structure.
- the random structures will generate and populate a realtime java public IStructureRadiusMap interface, that will allow other future mods to retreive the location of randomly placed nbt structures that already exist (already generated chunks), or that will exist (not yet generated chunks), in a radius/distance centering around an x,y,z coordinate in a specified dimension e.g "deepspace:deep_space". it should be realtime in the sense of that its ResourceLocation ```public List<MobData> mobs;``` should update in realtime, so that if a mob/entity on that was spawned with the structure gets killed, that the mob/entity should be removed from the ```public List<MobData> mobs;``` by periodicly following all references to LivingEntity in the MobData and checking the health of the mob, if the mob is dead or the LivingEntity reference no longer exists, and it has not just despawned / the mob has actually been killed / is dead then it should be removed from the ```public List<MobData> mobs;```


### Requirements

  Requirement        Description
  ------------------ -------------------------
  Hot reload         Supported via `/reload`
  Non-fatal errors   Invalid NBT skipped

------------------------------------------------------------------------

# 4. Structure Registry System

## Class: `DeepSpaceStructureRegistry`

### Responsibilities

-   Discover structures
-   Validate NBT
-   Store metadata
-   Provide weighted deterministic semi-random selection based on the minecraft world seed to ensure that given the same minecraft world seed, that the random structure type/filename and x y z coordinates will ALWAYS be in the same location, as lon gas the minecraft world seed at the time when the world is created is the same as the previous world, both worlds should have the same structures in the same locations

### Sources

#### A --- Resource Structures

Loaded via:

    ResourceManagerReloadListener

Scan path:

    data/deepspace/structures/*.nbt

#### B --- Runtime Structures

Loaded from:

    FMLPaths.CONFIGDIR/deepspace/structures/*.nbt

Use:

    Files.walk()
Reqs:
	only loads files with .nbt extension, so e.g .disabled file extension is not loaded

### Stored Object

```
package com.yourmod.deepspace.structures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;

public class MobData {
	public RegisteredStructure parentStructure;

    /** Spawn position inside the structure */
    public int spawn_x;
    public int spawn_y;
    public int spawn_z;

    /** Entity type name (Cow, Pig, Villager, Trader, etc.) */
    public String entity_type;

    /** Vanilla structure entity storage data */
    public StructureEntityInfo entityInfo;

    /** Living entity reference (nullable outside runtime placement) */
    public LivingEntity entityLiving;

    /** Base entity reference */
    public Entity entityBase;

    public MobData(
            int spawn_x,
            int spawn_y,
            int spawn_z,
            int health,
            String entity_type,
            StructureEntityInfo entityInfo,
            LivingEntity entityLiving,
            Entity entityBase
    ) {
        this.spawn_x = spawn_x;
        this.spawn_y = spawn_y;
        this.spawn_z = spawn_z;
        this.health = health;
        this.entity_type = entity_type;
        this.entityInfo = entityInfo;
        this.entityLiving = entityLiving;
        this.entityBase = entityBase;
    }
}
```

```
public class ResourceLocation {

    /** Dimension ID (example: "deepspace:deep_space") */
    public String dimension_name;

    /** Structure coordinates */
    public int x;
    public int y;
    public int z;

    /** Stored NPC/Mob data of Entitys/Mobs/Animals that are spawned inside the structure & are currently still alive. if the mob gets killed it should be removed from this List of mobs so that when the e.g cc: tweaked scanner use this interface, it will only return a list of alive mobs on the structure, and not mobs that have been killed by players or mobs that have died/are dead/despawned due to any cause */
    public List<MobData> mobs;

    public ResourceLocation(String dimension_name, int x, int y, int z) {
        this.dimension_name = dimension_name;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
```

```
public class StructureTemplate {

    /** Filename of the structure NBT */
    public String NBT_filename;

    /** True = bundled at compile time, false = runtime config structure */
    public boolean compileTime;

    public StructureTemplate(String NBT_filename, boolean compileTime) {
        this.NBT_filename = NBT_filename;
        this.compileTime = compileTime;
    }
}
```


``` java
class RegisteredStructure {
    ResourceLocation id;
    StructureTemplate template;
    BoundingBox bounds;
}
```

```
public class PlacementContext {

    /** True when structure is placed by world generation */
    public boolean isWorldgen;

    /** True when structure is spawned manually (command, quest, admin, etc.) */
    public boolean isSpawned;

    /** Username of player that triggered placement (nullable if worldgen) */
    public String spawnerUsername;

    public PlacementContext(boolean isWorldgen, boolean isSpawned, String spawnerUsername) {
        this.isWorldgen = isWorldgen;
        this.isSpawned = isSpawned;
        this.spawnerUsername = spawnerUsername;
    }

    /** Convenience factory for world generation placements */
    public static PlacementContext worldgen() {
        return new PlacementContext(true, false, null);
    }

    /** Convenience factory for player/admin spawned structures */
    public static PlacementContext spawned(String username) {
        return new PlacementContext(false, true, username);
    }
}
```

------------------------------------------------------------------------

# 5. Unified Structure Placement System (CRITICAL)

## Class: `StructurePlacer`

This replaces duplicated ship placement logic, but reusing all existing ship placement logic all that can be reused

### MUST reuse logic from:

-   crashed ship spawn
-   `/exportship` format
-   GregTech pipe/cable net refresh logic and visual clientside connectedness
-  GregTech multiblock spawning 
-  Any other existing logic/features/bug fixes that has been added to the existing "Crashed Ship" on asteroid when any new player joins game for first time.

## MUST not break:
- Any existing player first join "Crashed Ship" on asteroid placement when new player join game for first time. 
- Any GregTech pipe/cable net refresh logic and visual clientside correctedness
- gregtech multiblock spawning / assembling/validating gregtech multiblock structures inside the radius of the spawned .nbt

## Public API

``` java
public class StructurePlacer {

   public static void placeStructure(
       ServerLevel level,
       BlockPos origin,
       RegisteredStructure structure,
       PlacementContext context
   );
}
```

------------------------------------------------------------------------

## Placement Steps (STRICT ORDER)

### 1 --- Load Template

    StructureTemplate.placeInWorld()

### 2 --- Block Placement

-   identical settings as crashed ship
-   ignore air blocks OFF
-   entities enabled
-   NO rotation or mirroring is ever applied to structures
-   cuboid bounding box with a 4 blocks padding around the structure being placed
-   placed in a safe location. Safe is considered, a cuboid equal to or larger than the bounding box of the structure, that is not filled with any blocks/ALL blocks inside the cuboid area are only void/air. 
-   Safe location means: no intersection/collision with any existing blocks that are not air/void inside the size of the structures bounding box, so the structure being placed will never replace an existing block in the game. it should search in a 3d spherical spiral around the center location where the structure is to be spawned, e.g the center of the radius of void when it is being placed randomly inside the dimension, OR the PLAYER argument of "/importship" command or player that executed the "/importship" command.
-   Ad Astra mods "Oxygen Distributor" / "ad_astra:oxygen_distributor" should be reverse engineered from the attached .jar to figure out how to initialize it / other ad astra blocks correctly on spawn/placement in the same way as if they were manually placed by the player, to ensure that after placement/spawning in the ad astra blocks e.g "ad_astra:oxygen_distributor" that it still accepts FE and consumes oxygen in its tank to produce oxygen in the radius of it or if in a sealed room to produce oxygen into that sealed room

### 3 --- Entity/Mob/Animal Spawn Restoration

After placement:

    template.placeEntities(...)

Entity/Mob/Animal must preserve:

-   UUID (regenerate if collision)
-   position
-   facing direction
-   motion
-   custom name
-   AI state
-   trades
-   health
-   inventory
-   brain data
-   ALL NBT DATA

### REQUIRED FLAG

    StructurePlaceSettings#setFinalizeEntities(true)

### 4 --- GregTech Integration Pass

After placement:

    GTWireNetManager.rebuildAround(origin)

Re-run:

-   cable networks
-   pipe networks
-   multiblock validation / should call the same gregtech function on the main block of the multiblock that checks if the structure is correct / not "invalid structure" to assemble the multiblock and ensure that it can start work without breaking/rebreaking the main block of the multiblock after the blocks for the multiblock are placed/spawned into the world

### 5 --- Chunk Sync

Force block updates:

    level.sendBlockUpdated(...)

------------------------------------------------------------------------

# 6. /exportship Enhancements

## Required Changes

During export:

### Step 1 --- Detect Entities

    level.getEntities(
        null,
        new AABB(exportBounds)
    )

### Step 2 --- Serialize Entity NBT

``` java
CompoundTag tag = entity.saveWithoutId(new CompoundTag());
```

Store inside structure template entity list.

### Step 3 --- Preserve FULL Entity/Mob/Animal DATA

Must include:

-   VillagerData
-   Offers
-   Brain
-   Attributes
-   Equipment
-   Rotation
-   Pose

Example preserved:

    VillagerData
    Offers
    Recipes
    maxUses

NO filtering allowed.
ALL NBT DATA/CUSTOM CONFIGURATION MADE BY ADMIN COMMANDS FOR THE Entity/Mob/Animal must be preserved in the /exportship and /importship and worldgen random structure placement code + player crashed space ship spawn on first join code

------------------------------------------------------------------------

# 7. Random Structure Generation

## Generation Frequency

    SpawnChance = CelestialBodyChance / 10

(10× rarer)

## Generator Hook

Hook into SAME phase as celestial body generation.

Developer must inspect:

    DeepSpaceChunkGenerator

(or equivalent class)

## Altitude Range

Implementation requirement:

1.  Inspect asteroid spawn Y range.
2.  Cache values:

``` java
minY = asteroidGenerator.getMinY();
maxY = asteroidGenerator.getMaxY();
```

Structures spawn ONLY inside this band.

------------------------------------------------------------------------

# 8. Spawn Location Rules

## Void Radius System

Each structure creates exclusion zone.

### Compute Bounding Box

    template.getBoundingBox()

Expand:

    radius = maxDimension + safetyMargin

Recommended:

    safetyMargin = 32 blocks

### Register exclusion region

    StructureVoidManager.reserve(AABB)

Asteroid generator must query:

    isLocationReserved(pos)

before spawning.

### FTB Chunks integration

natural generation of randomly placed structures does not require any ftb chunks checking for if it is being placed on an existing players claimed chunk or not
admin/op/console/ftbquests spawning of structures does require "FTB Chunks" checking. It requires the structure/any blocks inside the structure to NEVER be spawned/the structure to NEVER be allowed to spawn on any chunks that are claimed by a player with "FTB Chunks", UNLESS the username defined in the USERNAME argument of "/importship [FILENAME] [USERNAME]" is the SAME as the username of the owner of the ftb claimed chunk, or any usernames in the team that owns the ftb claimed or force loaded chunk. So if "/importship ship.nbt ALICE" is run, by the server console, it will place the ship.nbt at the nearest safe location to the player ALICE, and will allow structure to be placed on claimed/force loaded "FTB Chunks" chunks, IF ALICE is the owner of the chunk the structures being placed on, or that ALICE is a member of the team that has claimed the chunk that ALICE player is located on. So a structure/ship will be able to be spawned on ALICE's chunk, ONLY if the username ALICE is supplied in the username argument of "/importship".

If "/importship ship.nbt BOB" is run, and BOB is on the player ALICE's "FTB Chunks" force-claim chunk, the structure should instead be spawned at the nearest safe location such as that no blocks inside the radius of the bounding box are placed on any chunk that is owned by the player ALICE. The only case where this would be ok, is if both ALICE and BOB are on the same team, and the chunk is claimed by both of them.

So in short, with "/importship ship.nbt BOB" if BOB is standing on ALICES "FTB Chunks" force-claimed/claimed chunk, it should NEVER be able to spawn a structure on ALICE's chunk, unless they are on the same "FTB Teams" team.

------------------------------------------------------------------------

# 9. Spawn Bias Toward Celestial Bodies

Probability modifier:

    if (nearCelestialBody < 256 blocks) (16 chunk circular radius around celestial body)
        spawnWeight *= 2  (DOUBLE CHANCE OF SPAWNING NEAR CELESTIAL BODY SPHERE)

Encourages discovery gameplay.

------------------------------------------------------------------------

# 10. Random Selection Logic

Pool =

    compileTimeStructures + runtimeStructures

Uniform random selection unless weights added later.

------------------------------------------------------------------------

# 11. Admin Command --- `/importship`

## Syntax

    /importship <filename> [PLAYER]

## Permissions

    requires OP level 2+

Also executable by:

-   server console
-   FTB Quests (elevated permissions)
-   FTB Quests / server console, PLAYER argument is ALWAYS required instead of optinal when player runs it. used to define players current x y z coordinates to which to spawn the structure/ship in the closest safe area near either the ADMIN/op that ran the command, or the PLAYER username argument, should have Autocomplete with all online players usernames

## Behaviour

### Player Execution

    spawn at executor position

### Console Execution

PLAYER argument REQUIRED.

## Autocomplete

Autocomplete must include:
for <filename>

    DeepSpaceStructureRegistry.getAllNames()

(compile + runtime)

will autocomplete with all .nbt filenames from compiletime / overriden by if their is the same name structure in the runtime, the runtime .nbt will replace the compile-time .nbt for that structure. e.g admins can replace compile-time "crashed_ship.nbt" to make it so players spawn/start the game with a different crashed ship on first join, by placing a .nbt file: FMLPaths.CONFIGDIR/deepspace/structures/crashed_ship.nbt
if a .nbt file with the same name as a compile-time .nbt file is put in FMLPaths.CONFIGDIR/deepspace/structures/, it should override/replace the .nbt file used at compile-time. so server admins can replace compile-time structures with updated versions without recompiling by just adding the file with the same name as the internal .nbt file to the "FMLPaths.CONFIGDIR/deepspace/structures/" directory

## Implementation

Command uses:

    StructurePlacer.placeStructure()

NO duplicate logic allowed.

------------------------------------------------------------------------

# 12. Command Class

    ImportShipCommand

Register in:

    RegisterCommandsEvent

------------------------------------------------------------------------

# 13. Required Dependencies / Reverse Engineering Targets

## Mandatory Inspection Targets

### 1️⃣ DeepSpace Mod (current codebase)

Needed for:

-   crashed ship placement
-   celestial generation
-   asteroid altitude logic

### 2️⃣ GregTech CEu (GTCEu)

Required for:

-   wire network rebuild
-   multiblock refresh

Key classes:

    PipeNetManager
    WireNetManager
    MultiblockController

### 3️⃣ Ad Astra

Jar:

    ad_astra-forge-1.20.1-1.15.20.jar

Used for:

-   vacuum mechanics compatibility
-   entity survival checks

### 4️⃣ FTB Quests

Jar:

    ftb-quests-forge-2001.4.17.jar

Needed to verify:

    Run with Elevated Permission

executes commands properly.

------------------------------------------------------------------------

# 14. Data Safety Requirements

  Rule                   Requirement
  ---------------------- ------------------
  Invalid NBT            Skip safely
  Missing entity types   Log + continue
  UUID conflicts         regenerate

------------------------------------------------------------------------

# 15. Performance Constraints

-   Runtime structure scan ONLY on reload/startup
-   Random generation runs server-side only
-   Cache bounding boxes
-   Avoid per-tick scans
-   random structure generation x y z location / randomly chosen nbt filename must be use determinstic psudo-random number generators that are seeded by the minecraft world seed, so given the player creates a world with the minecraft world seed "test", the locations and type of randomly placed structures will ALWAYS generate in the same x y z location and WILL ALWAYS be the same random filename of structure in the same location, even if the player deletes the world and recreates it, as long as they use the same world seed "TEST" those randomly placed structures WILL ALWAYS spawn the same type/filename of structure in the SAME x y z coordanates for the given minecraft world seed

------------------------------------------------------------------------

# 16. Logging

Prefix:

    [TIMESTAMP] [DeepSpace_Structures] [INFO/WARN/ERR] LOG_MESSAGE
e.g
	[02:17:42] [DeepSpace_Structures] [INFO] DeepSpace Structures Sucessfully Started.
	[02:17:42] [DeepSpace_Structures] [ERR] "/importship ship.nbt ALICE" was executed by BOB @ deepspace:deep_space [-253,-132,143] - RESULT: ERROR_CHUNK_CLAIMED
	[02:17:42] [DeepSpace_Structures] [INFO] "/importship ship.nbt ALICE" was executed by BOB @ deepspace:deep_space [-253,-132,143] - RESULT: SPAWNED

Events logged:

-   structure load
-   placement
-   entity restore
-   errors (cannot find safe location etc)
-   /importship  command status, e.g SPAWNED/ERROR_SAFE_LOCATION etc. should log every /importship command, should log the result of the command so that an admin can always check if an /importship command execution was SPAWNED sucessfully or if it errored in the process and aborted spawning the structure, it should always report an error e.g ERROR_SAFE_LOCATION, ERROR_CHUNK_CLAIMED, ERROR_INVALID_NBT, ERROR_FILENAME_MISSING, etc

------------------------------------------------------------------------

# 17. Edge Cases

  Case                               Behaviour
  ---------------------------------- -------------------
  Entity mod missing                 skip entity
  Structure overlaps "FTB Chunks" claimed chunk or force loaded chunk.  retry position in a spherical spiral starting at the location of the player that issued /importship, or the center of the location the structure should be spawned in. the spherical spiral search for the free space equal to or larger than the bounding box of the structure being placed, the shperical spiral search should start at the center location and check for safe locations in a spherical spiral moving outwards from the center location (player, or center of the void/asteroidless/celestial bodyless area that the bounding box / random structure is being placed in)
  Player offline (/importship)       fail gracefully
  Invalid filename                   fail gracefully, return error to chat if op/admin online user entered the command and it wasn't run by the server console/ftb quests
  ANY ERROR during /importship placement, that means import/placement etc wasn't a sucess should be printed to the player that executed the commands chat, or in the case it was run by the ftb chunks/console the PLAYER arguments chat (if they are online)

------------------------------------------------------------------------

# 18. Future Extension Hooks 

Prepare interfaces:
	
	IStructureRadiusMap   << should be used to return a list of all naturally generated RegisteredStructure objects with populated x y z coordinates / structure .nbt filenames in a radius around the specified coordanates and distance. should include data on randomly generated structures on chunks that have not been generated yet based on the deterministic random number generator, and the minecraft worldseed, should 100% represent the actual real locations and structure filenames that will generate in the world within a radius/distance of the given coordanates so as that when the in-game player explores / generates those chunks, that the randomly generated structure is ALWAYS at that location/coordinates that the IStructureMap gives. It should also include all currently loaded random structures on chunks that are already generated/loaded in memory aswell as the ones that haven't yet been generated / loaded in the radius of the given x y z coordinates

```
package com.yourmod.deepspace.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Public API allowing mods to query deterministic locations
 * of randomly generated DeepSpace structures.
 *
 * Implementations MUST return identical results for:
 *  - generated chunks
 *  - not yet generated chunks
 * using only world seed + deterministic RNG.
 */
public interface IStructureRadiusMap {

    /**
     * Returns ALL registered random structures within a radius.
     *
     * MUST include:
     *  - already generated structures
     *  - structures that WILL generate in future chunks
     *
     * Results MUST be deterministic based on world seed.
     *
     * @param level      Level accessor (dimension source)
     * @param center     search origin
     * @param radius     search radius in blocks
     * @return list of structures guaranteed to exist or generate
     */
    List<RegisteredStructure> getStructuresInRadius(
            LevelAccessor level,
			long worldSeed,
            BlockPos center,
            int radius
    );

    /**
     * Deterministically calculates structures WITHOUT requiring chunks
     * to be generated or loaded.
     *
     * Used by map previews, scanners, quests, etc.
     */
    List<RegisteredStructure> predictStructures(
            ResourceLocation dimension,
            long worldSeed,
            BlockPos center,
            int radius
    );

    /**
     * Returns structures currently loaded in memory.
     * (Already generated chunks only)
     */
    List<RegisteredStructure> getLoadedStructures(ServerLevel level);

    /**
     * Returns nearest structure to position.
     */
    Optional<RegisteredStructure> findNearestStructure(
            LevelAccessor level,
            BlockPos origin,
            int maxDistance
    );

    /**
     * Returns structures belonging to a specific template (.nbt filename).
     */
    List<RegisteredStructure> getStructuresByTemplate(
            ResourceLocation templateId
    );

    /**
     * Called by worldgen when a structure is placed.
     * Implementation must register structure in realtime map.
     */
    void registerStructure(RegisteredStructure structure);

    /**
     * Called when a structure is unloaded or removed.
     */
    void unregisterStructure(RegisteredStructure structure);

    /**
     * Performs realtime mob validation.
     *
     * Implementation MUST:
     *  - iterate MobData LivingEntity references
     *  - remove mobs that are confirmed dead
     *  - keep mobs that merely despawned temporarily
     *
     * Expected to run periodically (server tick task).
     */
    void tickMobValidation(ServerLevel level);
}
```

Allows future mods to inject behavior.

------------------------------------------------------------------------

# 19. Acceptance Criteria

✅ Structures load from both sources\
✅ `/exportship [FILENAME]` preserves NPC trades exactly\
✅ Villagers retain Offers NBT\
✅ GregTech networks reconnect\
✅ Multiblocks form automatically\
✅ Random spawn rarer than celestial bodies\ twice as likely to generate when within 256 blocks of celestial body sphere
✅ Void/air exclusion padding around the cuboid bounding box of the placed random location .nbt structure works\
✅ `/importship [FILENAME] [USERNAME]` works console + op/admins/ftbquests \
✅ `/importship [FILENAME] [USERNAME]` ALWAYS responds with a structure placement sucess/error status message to chat to USERNAME, if USERNAME argument is supplied,  \
✅ Autocomplete for .nbt filenames in /importship functional and also autocomplete of optional USERNAME argument of /importship populates to all online users usernames
✅ Fully deterministic semirandom number generator used to decide what .nbt file to use, and the x y z coordinates it will be placed in, so you can very simply calculate the x y z location's and filenames/types of naturally spawning randomly placed structures, even random structures that are on chunks that haven't yet been generated by using the public IStructureRadiusMap interface a function that takes arguments for dimension, x y z search center coordinates, distance int, so the interface will allow you to search for ALL possible/chunk generated or chunk not yet generated locations of the structures that get placed or will be placed in the world when that chunk generates \
------------------------------------------------------------------------

# 20. Implementation Order (Recommended)

1.  Extract crashed ship placement → `StructurePlacer`
2.  Fix `/exportship` entity saving
3.  Implement StructureRegistry
4.  Runtime folder loader
5.  `/importship` command
6.  Random generator hook
7.  Void exclusion system
8.  GregTech rebuild pass
9.  Testing with trader villagers
10. Stress testing generation
