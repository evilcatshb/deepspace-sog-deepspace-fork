DeepSpace Mod – Ship Scanner Technical Specification (Revised)

Target: Minecraft Forge 1.20.1
CC:Tweaked Integration: cc-tweaked-1.20.1-forge-1.113.1.jar
Energy System: GregTech CE U (ZPM tier, 1a consumption)

1. Overview

The Ship Scanner is a CC:Tweaked peripheral block that detects randomly placed .nbt structures in the deepspace:deep_space dimension.

Key points:

Default scan range: 2500 blocks
Maximum scan radius: 5000 blocks
JSON-style output for Lua scripts
No crafting recipe included (manual registration via KubeJS)
Power requirement: 1a of ZPM tier GregTech EU

When unpowered, all CC:Tweaked API calls return:

{"ERROR":"UNPOWERED"}

Only when powered with at least 1a ZPM EU will the scanner return real results.

2. Block – ShipScannerBlock
package com.deepspace.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import com.deepspace.tileentity.ShipScannerBlockEntity;

public class ShipScannerBlock extends Block {

    public ShipScannerBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL).strength(3.0f, 15.0f));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShipScannerBlockEntity(pos, state);
    }
}

Texture Notes:

Front: Dark green circular radar display with square red blips, surrounded by white plastic.
Top: White plastic with black carry handle and four dark gray screws in corners.
Sides: White plastic with four dark gray screws per corner.
Back: Messy wires, cables, and circular plug sockets.

Overall appearance: box-shaped device with uniform white plastic on sides, top, and front frame.

3. Item – ShipScannerItem
package com.deepspace.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ShipScannerItem extends BlockItem {

    public ShipScannerItem(Block block) {
        super(block, new Item.Properties().stacksTo(64));
    }
}

Notes:

Stack size 64
Recipe registered manually via KubeJS, item has no java recipe, but is registered in creative mode items/JEI 

4. Tile Entity – ShipScannerBlockEntity

Integrates GregTech ZPM energy consumption and CC:Tweaked peripheral access control.

// GT CE energy storage (buffer ~1 tick of 1a ZPM)
private long euStored = 0L;
// Buffer reduced: only enough for 1 tick at ZPM tier
private final long EU_BUFFER_SIZE = com.gregtechceu.gtceu.api.GTValues.V[com.gregtechceu.gtceu.api.GTValues.ZPM]; 

private final IEnergyContainer energyContainer = new IEnergyContainer() {
    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        long minVoltage = com.gregtechceu.gtceu.api.GTValues.V[com.gregtechceu.gtceu.api.GTValues.ZPM];
        if (voltage < minVoltage) return 0;

        long canAccept = EU_BUFFER_SIZE - euStored;
        if (canAccept <= 0) return 0;

        // Only accept 1a max
        long amperesAccepted = Math.min(amperage, 1);
        long euAccepted = Math.min(voltage * amperesAccepted, canAccept);

        euStored = Math.min(euStored + euAccepted, EU_BUFFER_SIZE);
        setChanged();
        return amperesAccepted;
    }

    @Override public boolean inputsEnergy(Direction side) { return true; }

    @Override
    public long changeEnergy(long energyToAdd) { 
        euStored = Math.max(0, Math.min(EU_BUFFER_SIZE, euStored + energyToAdd)); 
        setChanged();
        return euStored; 
    }

    @Override public long getEnergyStored() { return euStored; }
    @Override public long getEnergyCapacity() { return EU_BUFFER_SIZE; }
    @Override public long getInputVoltage() { return com.gregtechceu.gtceu.api.GTValues.V[com.gregtechceu.gtceu.api.GTValues.ZPM]; }
    @Override public long getInputAmperage() { return 1; } // 1a now
};
5. Peripheral – ShipScannerPeripheral

Updated for max radius 5000 and default 2500, gatekept by energy:

@LuaFunction
public final String scan() {
    tile.performScan(); // Only returns real scan if powered
    return tile.getLastScanJson();
}

@LuaFunction
public final String getLastScan() {
    return tile.getLastScanJson();
}

@LuaFunction
public final void setRadius(int radius) throws LuaException {
    if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) 
        throw new LuaException("Radius out of bounds");
    tile.setScanRange(radius);
}
6. Lua Peripheral API Usage Examples
-- Scan ships
local result = shipScanner.scan()
print(result) -- JSON, or {"ERROR":"INSUFFICIENT_POWER"} if insufficient power, {"ERROR":"INVALID_RADIUS"} if radius is out of bounds

-- Get last cached scan
local cached = shipScanner.getLastScan()
print(cached)

-- Change scan radius
shipScanner.setRadius(5000) -- max

7. concise snippet showing how to attach CC:Tweaked peripheral functions to your ShipScannerBlockEntity. This will let Lua scripts call your scanner commands (getScanData(), setScanRange(), etc.) and return JSON, gated on the EU power check.
```
package com.deepspace.peripheral;

import dan200.computercraft.api.peripheral.BasePeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import com.deepspace.tileentity.ShipScannerBlockEntity;

public class ShipScannerPeripheral extends BasePeripheral {

    private final ShipScannerBlockEntity scanner;

    public ShipScannerPeripheral(ShipScannerBlockEntity scanner) {
        this.scanner = scanner;
    }

    @Override
    public String getType() {
        return "ship_scanner";
    }

    /**
     * Lua: scanner:getStructuresInRadius(radius)
     * Returns JSON of all structures within radius.
     * If unpowered, returns {"ERROR":"UNPOWERED"}
     */
    @LuaFunction(mainThread = true)
    public final String getStructuresInRadius(int radius) {
        // Clamp radius to max allowed
        radius = Math.min(radius, ShipScannerBlockEntity.MAX_SCAN_RANGE);
        if (!scanner.hasSufficientPower()) {
            return "{\"ERROR\":\"UNPOWERED\"}";
        }
        // Perform actual scan logic (placeholder)
        return scanner.scanStructuresInRadius(radius); // BlockEntity method returns JSON string
    }

    /**
     * Lua: scanner:findNearestStructure(radius)
     * Returns JSON with nearest structure within radius.
     * If unpowered, returns {"ERROR":"UNPOWERED"}
     */
    @LuaFunction(mainThread = true)
    public final String findNearestStructure(int radius) {
        radius = Math.min(radius, ShipScannerBlockEntity.MAX_SCAN_RANGE);
        if (!scanner.hasSufficientPower()) {
            return "{\"ERROR\":\"UNPOWERED\"}";
        }
        // Perform nearest-structure search (placeholder)
        return scanner.findNearestStructureJson(radius); // BlockEntity method returns JSON string
    }
}
```	

8. example command output for the two new getStructuresInRadius(radius) and findNearestStructure(radius) functions


1️⃣ getStructuresInRadius(radius)

Possible outputs:

Unpowered (no 1a ZPM EU available)
{"ERROR":"UNPOWERED"}
Powered, no structures found in radius
{
  "success": true,
  "structures": []
}
Powered, one or more structures found
{
  "success": true,
  "structures": [
    {
      "type": "outpost",
      "x": 1024,
      "y": 64,
      "z": -512,
      "entities": [
        {"type": "guard", "x": 1025, "y": 64, "z": -510, "health": 20.0}, 
        {"type": "guard", "x": 1023, "y": 64, "z": -514, "health": 20.0}
      ]
    },
    {
      "type": "shipwreck",
      "x": 2048,
      "y": 70,
      "z": -2048,
      "entities": [
        {"type": "creeper", "x": 2050, "y": 70, "z": -2047, "health": 20.0}
      ]
    }
  ]
}

NOTE: structures.type = RegisteredStructure.template.nbtFilename string, but with the .nbt file extension removed. so e.g if RegisteredStructure.template.nbtFilename = "outpost.nbt" then the rendered "type" in the structures cc: tweaked json output is "outpost", simply trim the suffix ".nbt"


Note: health is a float where 1.0 = 1 heart. If the mob isn’t loaded in the world, set health to the default max health for that mob type.

Invalid radius (negative or zero)
{
  "success": true,
  "structures": []
}
2️⃣ findNearestStructure(radius)

Possible outputs:

Unpowered (no 1a ZPM EU available)
{"ERROR":"UNPOWERED"}
Powered, no structure found within radius
{
  "success": true,
  "nearest": null
}
Powered, one or more structures found — nearest returned
{
  "success": true,
  "nearest": {
    "type": "outpost",
    "x": 1024,
    "y": 64,
    "z": -512,
    "distance": 1420,
    "entities": [
      {"type": "guard", "x": 1025, "y": 64, "z": -510, "health": 20.0},
      {"type": "guard", "x": 1023, "y": 64, "z": -514, "health": 20.0}
    ]
  }
}

Note: Same as above — health is a float where 1.0 = 1 heart, default max health if the mob isn’t loaded.

Invalid radius (negative or zero)
{
  "success": true,
  "nearest": null
}
✅ Updated Summary Table
Function	Power State	Radius Validity	Output Example
getStructuresInRadius	Unpowered	Any	{"ERROR":"UNPOWERED"}
getStructuresInRadius	Powered	No structures	{"success": true, "structures": []}
getStructuresInRadius	Powered	Structures	{"success": true, "structures": [...entities with type,x,y,z,health...]}
getStructuresInRadius	Powered	Invalid radius	{"success": true, "structures": []}
findNearestStructure	Unpowered	Any	{"ERROR":"UNPOWERED"}
findNearestStructure	Powered	No structures	{"success": true, "nearest": null}
findNearestStructure	Powered	Structures	{"success": true, "nearest": {...entities with type,x,y,z,health...}}
findNearestStructure	Powered	Invalid radius	{"success": true, "nearest": null}

9. potential gaps, pitfalls, and implementation notes

Potential Issue: The scanStructuresInRadius() and findNearestStructureJson() calls may involve scanning a very large area of the world (up to 5000 blocks radius). Doing this synchronously on the main thread would freeze Minecraft.

Recommendation:
Perform the scanning/search asynchronously (off the main thread) and cache results for quick retrieval by CC:Tweaked.
Use a scheduled task or CompletableFuture to update lastScanJson in the tile entity.
Only allow Lua functions to return the cached lastScanJson, rather than recalculating every tick.

Risk if ignored: Minecraft will stutter or crash when the scanner is used for large radii.
e.g cache the result by dimension, center coords/radius, and radius only if the center x y z coordinates of the scan change or radius change or dimension changes should the cache be invalidated, e.g the warpdrive mod teleports the location of the ship scanner block to another x y z location / dimension, or it is moved with a copy/paste gadget


Radius Validation / Error Outputs
Current Spec: Negative or zero radius returns empty JSON ({"success": true, "structures": []}) rather than an explicit "INVALID_RADIUS" error.
Recommendation: For consistency, all invalid radius checks should return:
{"ERROR":"INVALID_RADIUS"}

This prevents scripts from misinterpreting an empty result as “no structures” vs “invalid input.”

6️⃣ Peripheral CC:Tweaked Integration
@LuaFunction(mainThread = true) is currently main-threaded, which is dangerous for large scans.
Recommendation:
For heavy operations, perform asynchronous scanning in the tile entity.
mainThread = true is fine if the function just returns cached JSON.
Never perform the scan logic itself on the main thread.

8️⃣ Miscellaneous Implementation Notes
Tile Entity Persistence:
Make sure euStored is saved/loaded correctly. Loss of data on world reload can make scanner unusable.
Peripheral Registration:
Ensure ShipScannerBlockEntity exposes ShipScannerPeripheral properly via IPeripheralProvider.
Units:
Clearly document that health = hearts (1.0 = 1 heart).
Distance in findNearestStructure is Euclidean from the scanner to the structure origin.
Error Consistency:
Make sure all JSON outputs follow a consistent schema for scripts to parse safely.


10. Deliverables & Recommended Implementation Order
Block & Item classes (ShipScannerBlock, ShipScannerItem)
Tile Entity with energy storage (ShipScannerBlockEntity)
Implement GT CE ZPM energy buffer (1a consumption)
Implement power check (hasSufficientPower())
Peripheral classes (ShipScannerPeripheral)
Gatekeep all CC:Tweaked functions to output {"ERROR":"INSUFFICIENT_POWER"} if insufficient power
return {"ERROR":"INVALID_RADIUS"} if radius > 5000 as output to both cc: tweaked perepheral functions 
Texture & model
Front: dark green circular radar with red blips
Top: white plastic + black handle + gray screws
Sides/back: white plastic + screws + back cables/plugs
Registry & initialization
Register block, item, tile entity, and peripheral provider
KubeJS recipe addition (manual)
Testing
Confirm energy consumption
Verify CC:Tweaked outputs correctly under powered/unpowered conditions
Validate max radius and default radius behavior
Notes
Power buffer ensures scanner smooth operation even with intermittent energy delivery.
All peripheral calls check hasSufficientPower() before returning real JSON.
Default radius: 2500, max radius: 5000.
Texture and design requirements as per section 2.