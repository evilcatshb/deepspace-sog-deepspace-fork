package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.registry.PlanetDefinition;
import com.deepspace.registry.PlanetRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic sector-based planet placement.
 *
 * Space is divided into PLANET_SECTOR_SIZE_CHUNKS × PLANET_SECTOR_SIZE_CHUNKS
 * chunk grids. Each sector may contain at most one planet, decided purely by
 * hashing the world seed and sector coordinates — no mutable shared state.
 *
 * For each chunk being generated, we check the current sector and all sectors
 * within PLANET_SECTOR_SEARCH_RADIUS to ensure no large planet is missed.
 */
public final class PlanetSectorGrid {

    private PlanetSectorGrid() {}

    /**
     * Returns all planets whose sphere or void zone can intersect chunk (cx, cz).
     * Thread-safe: pure function of inputs, no shared state.
     */
    public static List<PlanetCandidate> resolve(long worldSeed, int cx, int cz) {
        int S = DSConstants.PLANET_SECTOR_SIZE_CHUNKS;
        int thisSectorX = Math.floorDiv(cx, S);
        int thisSectorZ = Math.floorDiv(cz, S);
        int searchR = DSConstants.PLANET_SECTOR_SEARCH_RADIUS;

        List<PlanetCandidate> results = new ArrayList<>();

        for (int sx = thisSectorX - searchR; sx <= thisSectorX + searchR; sx++) {
            for (int sz = thisSectorZ - searchR; sz <= thisSectorZ + searchR; sz++) {
                PlanetCandidate p = resolveSector(worldSeed, sx, sz);
                if (p == null) continue;

                // Include this planet if its void zone or body can reach this chunk.
                // Treat the chunk as a point at its centre + 12-block margin for safety.
                double chunkCentreX = cx * 16.0 + 8.0;
                double chunkCentreZ = cz * 16.0 + 8.0;
                double dx = chunkCentreX - p.centerX;
                double dz = chunkCentreZ - p.centerZ;
                double distToChunkCentre = Math.sqrt(dx * dx + dz * dz);

                if (distToChunkCentre - 12 <= p.voidRadius()) {
                    results.add(p);
                }
            }
        }
        return results;
    }

    /**
     * Deterministically resolve whether a planet exists in sector (sectorX, sectorZ)
     * and, if so, what its properties are.
     * Returns null if the sector is empty.
     */
    private static PlanetCandidate resolveSector(long worldSeed, int sectorX, int sectorZ) {
        // Mix world seed with sector coords using distinct multipliers
        long s = worldSeed
            ^ ((long) sectorX * 0x9E3779B97F4A7C15L)
            ^ ((long) sectorZ * 0x6C62272E07BB0142L);
        s ^= s >>> 30; s *= 0xbf58476d1ce4e5b9L;
        s ^= s >>> 27; s *= 0x94d049bb133111ebL;
        s ^= s >>> 31;

        // Spawn chance gate
        int chancePct = (int)(Math.abs(s) % 100);
        if (chancePct >= DSConstants.PLANET_SPAWN_CHANCE_PCT) return null;

        // Second hash for position/size so spawn-chance bits don't bias placement
        long s2 = s ^ 0xDEADC0DEBEEFA1B2L;
        s2 ^= s2 >>> 30; s2 *= 0xbf58476d1ce4e5b9L; s2 ^= s2 >>> 27;

        int S  = DSConstants.PLANET_SECTOR_SIZE_CHUNKS;
        int S2 = S / 2;
        // Centre placed in the inner half of the sector to avoid edge adjacency issues
        int centerChunkX = sectorX * S + S2 / 2 + (int)(Math.abs(s2)       % S2);
        int centerChunkZ = sectorZ * S + S2 / 2 + (int)(Math.abs(s2 >>  8) % S2);

        int centerX = centerChunkX * 16 + 8;
        int centerZ = centerChunkZ * 16 + 8;

        // Radius in [PLANET_RADIUS_MIN, PLANET_RADIUS_MAX]
        int rMin   = DSConstants.PLANET_RADIUS_MIN;
        int rMax   = DSConstants.PLANET_RADIUS_MAX;
        int radius = rMin + (int)(Math.abs(s2 >> 16) % (rMax - rMin + 1));

        // Shell thickness in [PLANET_SHELL_THICKNESS_MIN, PLANET_SHELL_THICKNESS_MAX]
        int tMin      = DSConstants.PLANET_SHELL_THICKNESS_MIN;
        int tMax      = DSConstants.PLANET_SHELL_THICKNESS_MAX;
        int shellThick = tMin + (int)(Math.abs(s2 >> 24) % (tMax - tMin + 1));

        // Y centre: place at the asteroid-band midpoint, but clamp only to world
        // height bounds so the full sphere is always rendered.
        // ASTEROID_BAND_Y_MIN/MAX are intentionally NOT used as sphere-extent
        // limits here — those are asteroid-only constraints and are far too
        // narrow for large planets (radius up to PLANET_RADIUS_MAX=80 blocks).
        int bandCentre = (DSConstants.ASTEROID_BAND_Y_MIN + DSConstants.ASTEROID_BAND_Y_MAX) / 2;
        // World Y range matches DeepSpaceChunkGenerator: getMinY()=-64, getGenDepth()=384
        final int WORLD_MIN_Y = -64;
        final int WORLD_MAX_Y = 319;
        int centerY = Math.max(WORLD_MIN_Y + radius + 1,
                      Math.min(WORLD_MAX_Y - radius - 1, bandCentre));

        PlanetDefinition def = PlanetRegistry.select(s);
        if (def == null) return null;

        return new PlanetCandidate(centerX, centerY, centerZ, radius, shellThick, def, s2);
    }
}
