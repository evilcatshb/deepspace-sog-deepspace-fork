package com.deepspace.compat.cctweaked;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.blocks.ShipScannerBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * CC:Tweaked peripheral for the Ship Scanner block.
 * Peripheral type: {@code "ship_scanner"}
 *
 * <h3>Lua API</h3>
 * <pre>
 *   local scanner = peripheral.find("ship_scanner")
 *
 *   -- Structures
 *   local result  = scanner.getStructuresInRadius(2500)
 *   local nearest = scanner.findNearestStructure(2500)
 *
 *   -- Ships  (warpdrive:ship_core)
 *   local ships      = scanner.getShipsInRadius(2500)
 *   local nearShip   = scanner.findNearestShip(2500)
 *
 *   -- Planets (celestial spheres)
 *   local planets    = scanner.getPlanetsInRadius(2500)
 *   local nearPlanet = scanner.findNearestPlanet(2500)
 * </pre>
 *
 * <h3>Threading model</h3>
 * All functions use {@code @LuaFunction(mainThread = true)}.
 *
 * <ul>
 *   <li><b>Structure and planet scans</b> run asynchronously. The first call kicks off
 *       the scan and returns {@code "scanning":true} with an empty result set.
 *       Subsequent calls return cached results. Poll until {@code "scanning":false}:
 * <pre>
 *   local result
 *   repeat
 *     result = textutils.unserializeJSON(scanner.getPlanetsInRadius(2500))
 *     if result.scanning then os.sleep(0.5) end
 *   until not result.scanning
 * </pre>
 *   </li>
 *   <li><b>Ship scans</b> run synchronously on the server thread. Results are always
 *       fresh — {@code "scanning"} is always {@code false} for ship functions.</li>
 * </ul>
 *
 * <h3>Common error responses</h3>
 * <ul>
 *   <li>{@code {"ERROR":"UNPOWERED"}} — scanner not receiving ZPM EU</li>
 *   <li>{@code {"ERROR":"INVALID_RADIUS"}} — radius ≤ 0 or > 5000</li>
 *   <li>{@code {"ERROR":"WARPDRIVE_NOT_LOADED"}} — ship functions only; Warpdrive mod absent</li>
 *   <li>{@code {"ERROR":"WRONG_DIMENSION"}} — planet functions only; scanner not in deepspace:deep_space</li>
 *   <li>{@code {"ERROR":"SCAN_FAILED"}} — unexpected async exception during scan</li>
 * </ul>
 */
public class ShipScannerPeripheral implements IPeripheral {

    // ── Shared error constants ─────────────────────────────────────────────────
    private static final String UNPOWERED_JSON            = "{\"ERROR\":\"UNPOWERED\"}";
    private static final String INVALID_RADIUS_JSON       = "{\"ERROR\":\"INVALID_RADIUS\"}";
    private static final String WARPDRIVE_NOT_LOADED_JSON = "{\"ERROR\":\"WARPDRIVE_NOT_LOADED\"}";
    private static final String WRONG_DIMENSION_JSON      = "{\"ERROR\":\"WRONG_DIMENSION\"}";

    private final ShipScannerBlockEntity scanner;
    private final Set<IComputerAccess> attachedComputers = new CopyOnWriteArraySet<>();

    public ShipScannerPeripheral(ShipScannerBlockEntity scanner) {
        this.scanner = scanner;
    }

    @Nonnull
    @Override
    public String getType() {
        return "ship_scanner";
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        attachedComputers.add(computer);
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        attachedComputers.remove(computer);
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ShipScannerPeripheral p
            && p.scanner.getBlockPos().equals(scanner.getBlockPos());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STRUCTURE FUNCTIONS  (existing API, preserved)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns JSON of all structures within the given radius.
     *
     * <ul>
     *   <li>Unpowered:          {@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>Invalid radius:     {@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>Scanning:           {@code {"success":true,"scanning":true,"structures":[]}}</li>
     *   <li>Complete (none):    {@code {"success":true,"scanning":false,"structures":[]}}</li>
     *   <li>Complete (found):   {@code {"success":true,"scanning":false,"structures":[...]}}</li>
     * </ul>
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String getStructuresInRadius(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        return scanner.getStructuresJson(radius);
    }

    /**
     * Returns JSON of the nearest structure within the given radius.
     *
     * <ul>
     *   <li>Unpowered:          {@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>Invalid radius:     {@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>Scanning:           {@code {"success":true,"scanning":true,"nearest":null}}</li>
     *   <li>Complete (none):    {@code {"success":true,"scanning":false,"nearest":null}}</li>
     *   <li>Complete (found):   {@code {"success":true,"scanning":false,"nearest":{...}}}</li>
     * </ul>
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String findNearestStructure(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        return scanner.getNearestJson(radius);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHIP FUNCTIONS  (warpdrive:ship_core)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns JSON of all {@code warpdrive:ship_core} blocks within radius,
     * sorted by distance ascending. Only ships in loaded chunks appear.
     *
     * <p>Each ship entry includes:
     * <ul>
     *   <li>{@code name}     — ship display name (e.g. {@code "SHIP_0"}, or player-set name)</li>
     *   <li>{@code x, y, z} — world coordinates of the ship core block</li>
     *   <li>{@code distance} — 3-D Euclidean distance from the scanner (integer blocks)</li>
     * </ul>
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>{@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>{@code {"ERROR":"WARPDRIVE_NOT_LOADED"}}</li>
     *   <li>{@code {"ERROR":"SCAN_FAILED"}}</li>
     *   <li>{@code {"success":true,"scanning":false,"ships":[]}} — none in range</li>
     *   <li>{@code {"success":true,"scanning":false,"ships":[{"name":"ECLIPSE","x":100,"y":64,"z":200,"distance":142},...]}}</li>
     * </ul>
     *
     * <p>Cache TTL: results are refreshed every 10 s (200 ticks) even when
     * the scanner hasn't moved, to catch ships that have warped.
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String getShipsInRadius(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        if (!DeepSpaceMod.WARPDRIVE_LOADED) return WARPDRIVE_NOT_LOADED_JSON;
        return scanner.getShipsJson(radius);
    }

    /**
     * Returns JSON of the nearest {@code warpdrive:ship_core} within radius.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>{@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>{@code {"ERROR":"WARPDRIVE_NOT_LOADED"}}</li>
     *   <li>{@code {"ERROR":"SCAN_FAILED"}}</li>
     *   <li>{@code {"success":true,"scanning":false,"nearest":null}} — none in range</li>
     *   <li>{@code {"success":true,"scanning":false,"nearest":{"name":"ECLIPSE","x":100,"y":64,"z":200,"distance":142}}}</li>
     * </ul>
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String findNearestShip(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        if (!DeepSpaceMod.WARPDRIVE_LOADED) return WARPDRIVE_NOT_LOADED_JSON;
        return scanner.getNearestShipJson(radius);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLANET FUNCTIONS  (celestial spheres via PlanetSectorGrid)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns JSON of all celestial sphere planets within radius, sorted by
     * distance ascending. Only valid inside {@code deepspace:deep_space}.
     *
     * <p>Each planet entry includes:
     * <ul>
     *   <li>{@code x, y, z}       — world coordinates of the sphere centre</li>
     *   <li>{@code radius}         — sphere radius in blocks</li>
     *   <li>{@code shell_block}    — outer surface block (e.g. {@code "minecraft:packed_ice"})</li>
     *   <li>{@code distance}       — 3-D Euclidean distance from the scanner (integer blocks)</li>
     * </ul>
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>{@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>{@code {"ERROR":"WRONG_DIMENSION"}} — scanner not in deepspace:deep_space</li>
     *   <li>{@code {"ERROR":"SCAN_FAILED"}}</li>
     *   <li>{@code {"success":true,"scanning":true,"planets":[]}}</li>
     *   <li>{@code {"success":true,"scanning":false,"planets":[{"x":512,"y":60,"z":768,"radius":45,"shell_block":"minecraft:packed_ice","distance":320},...]}}</li>
     * </ul>
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String getPlanetsInRadius(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        if (!isInDeepSpace()) return WRONG_DIMENSION_JSON;
        return scanner.getPlanetsJson(radius);
    }

    /**
     * Returns JSON of the nearest celestial sphere planet within radius.
     * Only valid inside {@code deepspace:deep_space}.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>{@code {"ERROR":"UNPOWERED"}}</li>
     *   <li>{@code {"ERROR":"INVALID_RADIUS"}}</li>
     *   <li>{@code {"ERROR":"WRONG_DIMENSION"}}</li>
     *   <li>{@code {"ERROR":"SCAN_FAILED"}}</li>
     *   <li>{@code {"success":true,"scanning":true,"nearest":null}}</li>
     *   <li>{@code {"success":true,"scanning":false,"nearest":null}} — none in range</li>
     *   <li>{@code {"success":true,"scanning":false,"nearest":{"x":512,"y":60,"z":768,"radius":45,"shell_block":"minecraft:packed_ice","distance":320}}}</li>
     * </ul>
     *
     * @param radius Search radius in blocks (1–5000)
     * @return JSON string
     */
    @LuaFunction(mainThread = true)
    public final String findNearestPlanet(int radius) throws LuaException {
        if (!scanner.hasSufficientPower()) return UNPOWERED_JSON;
        if (radius <= 0 || radius > ShipScannerBlockEntity.MAX_SCAN_RANGE) return INVALID_RADIUS_JSON;
        if (!isInDeepSpace()) return WRONG_DIMENSION_JSON;
        return scanner.getNearestPlanetJson(radius);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the scanner block entity is currently located inside
     * {@code deepspace:deep_space}. Planet calculations are only meaningful
     * in that dimension — calling them from the Overworld would return
     * mathematically-valid but physically-nonexistent coordinates.
     */
    private boolean isInDeepSpace() {
        if (scanner.getLevel() == null) return false;
        return DSConstants.DEEP_SPACE_RL.equals(
            scanner.getLevel().dimension().location());
    }
}
