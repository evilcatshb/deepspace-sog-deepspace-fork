package com.deepspace.structures;

import com.deepspace.DSConstants;
import com.deepspace.worldgen.PlanetCandidate;
import com.deepspace.worldgen.PlanetSectorGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic sector-based random-structure placement grid.
 *
 * Mirrors PlanetSectorGrid but for randomly placed NBT structures.
 *
 * Density: structures are ~5x rarer than celestial bodies.
 *   - PLANET_SECTOR_SIZE_CHUNKS = 32, PLANET_SPAWN_CHANCE_PCT = 25 → ~25% of 32-chunk sectors
 *   - STRUCTURE_SECTOR_SIZE_CHUNKS = 96, STRUCTURE_SPAWN_CHANCE_PCT = 5
 *     → 5% of 96-chunk sectors → net ~(5/100) * (32/96)^2 ≈ 0.55% relative density
 *     → ~5x rarer than planets ✓
 *
 * Bias: if the structure sector centre is within 256 blocks of any PlanetCandidate,
 * spawn chance is doubled (STRUCTURE_SPAWN_CHANCE_PCT * 2 → 10% in example)
 *
 * Determinism: pure function of (worldSeed, sectorX, sectorZ) — no mutable state,
 * thread-safe, produces identical results for identical inputs across server restarts.
 */
public final class StructureSectorGrid {

    private StructureSectorGrid() {}

    /**
     * Returns all StructureSectorCandidates whose void zone can reach chunk (cx, cz).
     * Thread-safe: pure function of inputs.
     *
     * @param worldSeed   World seed
     * @param cx          Chunk X of the chunk being generated
     * @param cz          Chunk Z of the chunk being generated
     * @return All nearby structure candidates relevant to this chunk
     */
    public static List<StructureSectorCandidate> resolve(long worldSeed, int cx, int cz) {
        DeepSpaceStructureRegistry registry = DeepSpaceStructureRegistry.getInstance();
        List<String> pool = registry.getPool();
        if (pool.isEmpty()) return new ArrayList<>();

        int S = DSConstants.STRUCTURE_SECTOR_SIZE_CHUNKS;
        int thisSectorX = Math.floorDiv(cx, S);
        int thisSectorZ = Math.floorDiv(cz, S);
        int searchR = DSConstants.STRUCTURE_SECTOR_SEARCH_RADIUS;

        List<StructureSectorCandidate> results = new ArrayList<>();

        for (int sx = thisSectorX - searchR; sx <= thisSectorX + searchR; sx++) {
            for (int sz = thisSectorZ - searchR; sz <= thisSectorZ + searchR; sz++) {
                StructureSectorCandidate candidate = resolveSector(worldSeed, sx, sz, pool);
                if (candidate == null) continue;

                // Include if the structure's void zone can reach this chunk
                double chunkCentreX = cx * 16.0 + 8.0;
                double chunkCentreZ = cz * 16.0 + 8.0;
                double dx = chunkCentreX - candidate.centerX;
                double dz = chunkCentreZ - candidate.centerZ;
                double distToChunk = Math.sqrt(dx * dx + dz * dz);

                if (distToChunk - 16 <= candidate.voidRadius) {
                    results.add(candidate);
                }
            }
        }
        return results;
    }

    /**
     * Deterministically resolve whether a structure exists in sector (sectorX, sectorZ)
     * and, if so, what its properties are.
     * Returns null if the sector is empty.
     *
     * @param worldSeed World seed
     * @param sectorX   Sector X coordinate
     * @param sectorZ   Sector Z coordinate
     * @param pool      Available NBT filenames (excluding crashed_ship.nbt)
     */
    public static StructureSectorCandidate resolveSector(long worldSeed, int sectorX, int sectorZ, List<String> pool) {
        if (pool.isEmpty()) return null;

        // Hash world seed with sector coords using different multipliers from PlanetSectorGrid
        long s = worldSeed
            ^ ((long) sectorX * 0x7E62A3B19F4D3C21L)
            ^ ((long) sectorZ * 0x3D5C9E2F1A8B7E0DL);
        s ^= s >>> 30; s *= 0xbf58476d1ce4e5b9L;
        s ^= s >>> 27; s *= 0x94d049bb133111ebL;
        s ^= s >>> 31;

        // Compute sector centre block coords for planet-proximity bias check
        int S = DSConstants.STRUCTURE_SECTOR_SIZE_CHUNKS;
        int sectorCentreChunkX = sectorX * S + S / 2;
        int sectorCentreChunkZ = sectorZ * S + S / 2;
        int sectorCentreX = sectorCentreChunkX * 16 + 8;
        int sectorCentreZ = sectorCentreChunkZ * 16 + 8;

        // Check for celestial body proximity bias (2x spawn chance if within 256 blocks)
        boolean nearPlanet = isNearPlanet(worldSeed, sectorCentreX, sectorCentreZ);
        int spawnChance = nearPlanet
                ? DSConstants.STRUCTURE_SPAWN_CHANCE_PCT * 2
                : DSConstants.STRUCTURE_SPAWN_CHANCE_PCT;

        // Spawn chance gate
        int chancePct = (int)(Math.abs(s) % 100);
        if (chancePct >= spawnChance) return null;

        // Second hash for position details
        long s2 = s ^ 0xA17F3C9E5B6D4218L;
        s2 ^= s2 >>> 30; s2 *= 0xbf58476d1ce4e5b9L; s2 ^= s2 >>> 27;

        // Place centre in inner half of sector
        int S2 = S / 2;
        int centerChunkX = sectorX * S + S2 / 2 + (int)(Math.abs(s2)      % S2);
        int centerChunkZ = sectorZ * S + S2 / 2 + (int)(Math.abs(s2 >> 8) % S2);

        int centerX = centerChunkX * 16 + 8;
        int centerZ = centerChunkZ * 16 + 8;

        // Y within asteroid band
        int centerY = DSConstants.ASTEROID_BAND_Y_MIN +
                (int)(Math.abs(s2 >> 16) % (DSConstants.ASTEROID_BAND_Y_MAX - DSConstants.ASTEROID_BAND_Y_MIN));
        centerY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 16,
                  Math.min(centerY, DSConstants.ASTEROID_BAND_Y_MAX - 16));

        // Select NBT filename deterministically
        int fileIdx = (int)(Math.abs(s2 >> 32) % pool.size());
        String nbtFilename = pool.get(fileIdx);

        // Compute void radius based on structure bounding box
        int[] size = DeepSpaceStructureRegistry.getInstance().getTemplateSize(nbtFilename);
        int maxDim = Math.max(size[0], Math.max(size[1], size[2]));
        double voidRadius = maxDim * DSConstants.STRUCTURE_VOID_RADIUS_MULTIPLIER
                + DSConstants.STRUCTURE_VOID_PADDING_BLOCKS;

        long sectorKey = packSectorKey(sectorX, sectorZ);

        return new StructureSectorCandidate(
                centerX, centerY, centerZ,
                nbtFilename,
                sectorX, sectorZ,
                s2,
                voidRadius,
                sectorKey
        );
    }

    /**
     * Check whether the given block coordinate is within 256 blocks of any
     * PlanetCandidate. Used to double spawn chance near celestial bodies.
     */
    private static boolean isNearPlanet(long worldSeed, int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        // Check a small radius of planet sectors around this point
        List<PlanetCandidate> nearby = PlanetSectorGrid.resolve(worldSeed, chunkX, chunkZ);
        for (PlanetCandidate planet : nearby) {
            double dx = blockX - planet.centerX;
            double dz = blockZ - planet.centerZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist <= DSConstants.STRUCTURE_CELESTIAL_BIAS_RADIUS_BLOCKS) return true;
        }
        return false;
    }

    /**
     * Pack (sectorX, sectorZ) into a single long for use as a map key.
     */
    public static long packSectorKey(int sectorX, int sectorZ) {
        return ((long) sectorX << 32) | (sectorZ & 0xFFFFFFFFL);
    }

    // -------------------------------------------------------------------------
    // StructureSectorCandidate inner record
    // -------------------------------------------------------------------------

    /**
     * A candidate structure placement resolved for a specific sector.
     * Immutable, thread-safe.
     */
    public static final class StructureSectorCandidate {
        /** Centre X in world-block coordinates. */
        public final int centerX;
        /** Centre Y in world-block coordinates (within asteroid band). */
        public final int centerY;
        /** Centre Z in world-block coordinates. */
        public final int centerZ;
        /** NBT filename of the structure to place. */
        public final String nbtFilename;
        /** Sector X coordinate. */
        public final int sectorX;
        /** Sector Z coordinate. */
        public final int sectorZ;
        /** Deterministic seed for this specific placement. */
        public final long seed;
        /**
         * Void exclusion radius in blocks around the structure centre.
         * No asteroids or celestial bodies may spawn within this radius.
         */
        public final double voidRadius;
        /** Packed sector key: (sectorX << 32) | sectorZ. For deduplication. */
        public final long sectorKey;

        public StructureSectorCandidate(
                int centerX, int centerY, int centerZ,
                String nbtFilename,
                int sectorX, int sectorZ,
                long seed,
                double voidRadius,
                long sectorKey) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.nbtFilename = nbtFilename;
            this.sectorX = sectorX;
            this.sectorZ = sectorZ;
            this.seed = seed;
            this.voidRadius = voidRadius;
            this.sectorKey = sectorKey;
        }

        @Override
        public String toString() {
            return "StructureSectorCandidate{" + nbtFilename
                   + " @ [" + centerX + "," + centerY + "," + centerZ + "]"
                   + " voidR=" + String.format("%.1f", voidRadius) + '}';
        }
    }
}
