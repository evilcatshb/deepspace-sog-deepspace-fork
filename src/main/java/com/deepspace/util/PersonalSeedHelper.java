package com.deepspace.util;

import java.util.UUID;

/**
 * Derives the personal seed for a player's crash bubble.
 *
 * Contract: F(worldSeed, playerUUID) must always return the same value.
 * Formula from deepspace-technical.md:
 *   personalSeed = worldSeed ^ playerUUID.getMostSignificantBits()
 */
public final class PersonalSeedHelper {

    private PersonalSeedHelper() {}

    /**
     * Derive a player's personal chunk-generation seed.
     *
     * @param worldSeed   The level's seed (from ServerLevel.getSeed())
     * @param playerUUID  The player's UUID
     * @return            Deterministic 64-bit seed unique to this player + world
     */
    public static long derive(long worldSeed, UUID playerUUID) {
        // XOR world seed with high bits of UUID
        long seed = worldSeed ^ playerUUID.getMostSignificantBits();
        // Mix in low bits for uniqueness when MSBs collide
        seed ^= playerUUID.getLeastSignificantBits() >>> 17;
        // Final avalanche mix (Murmur3-style finalizer)
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= seed >>> 33;
        return seed;
    }

    /**
     * Derive a deterministic chunk-local seed for asteroid selection.
     *
     * @param personalSeed  The player's personal seed (or world seed for shared zones)
     * @param chunkX        Chunk X
     * @param chunkZ        Chunk Z
     * @return              Seed for this specific chunk's asteroid roll
     */
    public static long chunkSeed(long personalSeed, int chunkX, int chunkZ) {
        long seed = personalSeed;
        seed ^= (long) chunkX * 0x9E3779B97F4A7C15L;
        seed ^= (long) chunkZ * 0x6C62272E07BB0142L;
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        return seed;
    }
}
