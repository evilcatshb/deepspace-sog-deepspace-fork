package com.deepspace.worldgen;

import com.deepspace.registry.AsteroidRegistry;
import com.deepspace.registry.ZoneDefinition;

/**
 * Resolves which progression zone a chunk belongs to based on its
 * distance from the player's crash site.
 *
 * Contract compliance:
 *  - MUST be pure: same inputs -> same output always
 *  - MUST NOT read world state, block states, or mutate any cache
 *  - MUST NOT spawn entities
 *  - Safe to call from async chunk generation threads
 */
public final class ZoneResolver {

    private ZoneResolver() {}

    /**
     * Resolve the zone for a chunk given its crash-site origin.
     *
     * @param chunkX       The chunk being resolved
     * @param chunkZ       The chunk being resolved
     * @param crashChunkX  The owning player's crash site chunk X
     * @param crashChunkZ  The owning player's crash site chunk Z
     * @return             The zone this chunk belongs to, or the outermost zone if beyond all bounds
     */
    public static ZoneDefinition resolve(int chunkX, int chunkZ,
                                         int crashChunkX, int crashChunkZ) {
        double blockDistance = computeBlockDistance(chunkX, chunkZ, crashChunkX, crashChunkZ);
        ZoneDefinition zone = AsteroidRegistry.resolveZone(blockDistance);
        if (zone == null) {
            // Fallback: return zone_4 equivalent -- farthest zone
            zone = AsteroidRegistry.resolveZone(Integer.MAX_VALUE);
        }
        return zone;
    }

    /**
     * Euclidean block-distance from chunk centre to crash-site chunk centre.
     */
    public static double computeBlockDistance(int chunkX, int chunkZ,
                                               int crashChunkX, int crashChunkZ) {
        double dx = (chunkX - crashChunkX) * 16.0;
        double dz = (chunkZ - crashChunkZ) * 16.0;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
