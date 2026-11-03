# Deep Space Mod — Build Setup

## Step 1: Java & Gradle

- Java 17 required
- Add Gradle wrapper: run `gradle wrapper --gradle-version 8.4` in project root
  (or manually add gradlew/gradlew.bat and gradle/wrapper/ from a Gradle 8.4 distribution)

## Step 2: libs/ directory

**Jars already included in libs/:**

| File | Notes |
|---|---|
| `gtceu-1_20_1-7_5_2.jar` | GregTech CEu Modern |
| `ad_astra-forge-1_20_1-1_15_20.jar` | Ad Astra |
| `ftb-chunks-forge-2001_3_6.jar` | FTB Chunks |
| `appliedenergistics2-forge-15_4_10.jar` | Applied Energistics 2 |
| `worldedit-mod-7_2_15.jar` | WorldEdit (optional — for /exportship) |

**You must add these to libs/ before building:**

| File | Download |
|---|---|
| `kubejs-forge-1902.6.1-build.405.jar` | CurseForge KubeJS |
| `architectury-9.1.12-forge.jar` | CurseForge Architectury API |
| `ftb-library-forge-2001.2.4.jar` | CurseForge FTB Library |
| `geckolib-forge-1.20.1-4.3.4.jar` | CurseForge GeckoLib |

The mod compiles without the optional libs. They are only needed at runtime.

## Step 3: Structure NBT (crashed_ship.nbt)

A valid placeholder NBT is included with:
- 15×8×15 ship hull with iron-block walls, tinted glass windows
- AE2 ME Drive pre-loaded with starter items (4k item cell) and fluids (1k fluid cell)
- Breeder Reactor, AE2 Controller, Oxygen Distributor placeholders

**To build your own ship (recommended):**
1. Build the ship in-game in any dimension
2. Install WorldEdit in your server/client
3. Use the WorldEdit wand to select the ship
4. Run `/exportship` as an op
5. The command will print the path to the saved .nbt file
6. Copy the file to `src/main/resources/data/deepspace/structures/crashed_ship.nbt`
7. Rebuild the mod

The `/exportship` command fully preserves:
- All AE2 ME Drive storage cell contents (items and fluids)
- All other block entity NBT (chest inventories, breeder reactor fuel, etc.)
- All blockstate properties

## Step 4: KubeJS scripts

Copy the `kubejs/` directory from this zip to your Minecraft instance root
(the folder that contains `mods/`, `config/`, etc.).

The kubejs directory structure:
```
kubejs/
  server_scripts/
    deepspace_recipes.js    — GT recipe additions (oxygen loop, biomass→water/oxygen)
    deepspace_events.js     — First-join messages, starter chest injection, warnings
  startup_scripts/
    deepspace_startup.js    — Creative tab, global item definitions
  client_scripts/
    deepspace_client.js     — Item tooltips
```

## Step 5: Build

```bash
./gradlew build
```

Output: `build/libs/deepspace-1.0.0.jar`

## Step 6: Install

Place in your modpack `mods/` folder alongside all required mods.

## Dimension Access Policy

| Dimension | Status |
|---|---|
| `minecraft:overworld` | **BLOCKED** — players are immediately redirected |
| `minecraft:the_nether` | **BLOCKED** — no nether in deep space |
| `minecraft:the_end` | ✅ Accessible |
| `deepspace:deep_space` | ✅ **Default spawn dimension** |
| `ad_astra:moon` + orbits | ✅ Accessible via Ad Astra rockets |
| All other Ad Astra dims | ✅ Accessible via Ad Astra rockets |

All players (singleplayer and multiplayer) spawn directly in Deep Space.
On death without a bed/respawn anchor, players return to their crash site.
