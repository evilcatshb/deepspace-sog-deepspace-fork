package com.deepspace.entity;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.init.DSEntities;
import com.deepspace.init.DSDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles natural (persistent, non-migration) Space Whale spawning.
 *
 * Rules (from spec):
 *   • Only in deepspace:deep_space
 *   • > 10 000 blocks from dimension centre (0, 0)
 *   • Spawn locations seeded semi-deterministically by world seed
 *     (same seed → same locations; future API can predict them)
 *   • Pods of 2-3: 1-2 adults + occasional baby
 *   • Twice as rare as a randomly-placed NBT structure
 *     → sector chance ≈ STRUCTURE_SPAWN_CHANCE_PCT / 2
 *
 * Implementation strategy:
 *   Divide space into 128-chunk sectors. Each sector has at most one pod,
 *   determined purely by hashing (worldSeed, sectorX, sectorZ). We check
 *   whether the active players are near an un-spawned sector pod and spawn it.
 *
 *   Spawned sectors are tracked in-memory (non-persistent); pods re-spawn
 *   naturally each server session unless the whales are still alive.
 */
public class WhaleNaturalSpawnSystem {

    private static final int SECTOR_CHUNKS     = 128;    // 2048 blocks per sector
    private static final int SPAWN_CHANCE_PCT  = DSConstants.STRUCTURE_SPAWN_CHANCE_PCT / 2;
    private static final double MIN_DIST       = 10_000.0; // blocks from origin
    private static final double TRIGGER_DIST   = 256.0;    // blocks from player to spawn
    private static final double FLIGHT_Y       = DSConstants.ASTEROID_BAND_Y_MAX + 20.0;

    // Sectors where we've already spawned this session
    private final Set<Long> spawnedSectors = new HashSet<>();
    private int checkCooldown = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (checkCooldown-- > 0) return;
        checkCooldown = 100; // check every 5 seconds

        ServerLevel level = event.getServer().getLevel(DSDimensions.DEEP_SPACE);
        if (level == null) return;

        long worldSeed = level.getSeed();

        for (ServerPlayer player : level.players()) {
            double px = player.getX(), pz = player.getZ();
            // Skip players near the origin — whales don't spawn there
            if (Math.sqrt(px * px + pz * pz) < MIN_DIST - TRIGGER_DIST) continue;

            int playerSX = Math.floorDiv((int) px, SECTOR_CHUNKS * 16);
            int playerSZ = Math.floorDiv((int) pz, SECTOR_CHUNKS * 16);

            // Check the player's sector and adjacent sectors
            for (int dsx = -1; dsx <= 1; dsx++) {
                for (int dsz = -1; dsz <= 1; dsz++) {
                    int sx = playerSX + dsx;
                    int sz = playerSZ + dsz;
                    long sectorKey = ((long) sx << 32) | ((long) sz & 0xFFFFFFFFL);

                    if (spawnedSectors.contains(sectorKey)) continue;

                    // Deterministic chance gate
                    long hash = worldSeed
                        ^ ((long) sx * 0x9E3779B97F4A7C15L)
                        ^ ((long) sz * 0x6C62272E07BB0142L);
                    hash ^= hash >>> 30; hash *= 0xbf58476d1ce4e5b9L;
                    hash ^= hash >>> 27; hash *= 0x94d049bb133111ebL;
                    hash ^= hash >>> 31;

                    int pct = (int) (Math.abs(hash) % 100);
                    if (pct >= SPAWN_CHANCE_PCT) continue; // no pod in this sector

                    // Determine pod centre within sector
                    long h2 = hash ^ 0xDEADC0DEBEEFCAFEL;
                    h2 ^= h2 >>> 30; h2 *= 0xbf58476d1ce4e5b9L; h2 ^= h2 >>> 27;

                    int S = SECTOR_CHUNKS * 16;
                    double podX = (double)(sx * S) + (Math.abs(h2)      % S);
                    double podZ = (double)(sz * S) + (Math.abs(h2 >> 8) % S);

                    // Must be > MIN_DIST from origin
                    if (Math.sqrt(podX * podX + podZ * podZ) < MIN_DIST) continue;

                    // Must be within TRIGGER_DIST of the player
                    double dx = podX - px, dz = podZ - pz;
                    if (dx * dx + dz * dz > TRIGGER_DIST * TRIGGER_DIST) continue;

                    // Check no existing whale already in area
                    boolean alreadyLive = !level.getEntitiesOfClass(SpaceWhaleEntity.class,
                        new net.minecraft.world.phys.AABB(
                            podX - TRIGGER_DIST, FLIGHT_Y - 50, podZ - TRIGGER_DIST,
                            podX + TRIGGER_DIST, FLIGHT_Y + 50, podZ + TRIGGER_DIST))
                        .isEmpty();
                    if (alreadyLive) { spawnedSectors.add(sectorKey); continue; }

                    // ── Spawn the pod ──────────────────────────────────────────
                    int adults = 1 + (int)(Math.abs(h2 >> 16) % 2); // 1 or 2
                    boolean hasBaby = (Math.abs(h2 >> 18) % 3) == 0; // ~33% chance
                    int total = adults + (hasBaby ? 1 : 0);

                    for (int i = 0; i < total; i++) {
                        SpaceWhaleEntity whale = DSEntities.SPACE_WHALE.get().create(level);
                        if (whale == null) continue;
                        double spawnX = podX + (Math.abs(h2 >> (20 + i * 4)) % 32) - 16;
                        double spawnZ = podZ + (Math.abs(h2 >> (24 + i * 4)) % 32) - 16;
                        whale.setPos(spawnX, FLIGHT_Y, spawnZ);
                        whale.setNoGravity(true);
                        if (i >= adults) whale.setBaby(true); // last one is baby
                        whale.finalizeSpawn(level,
                            level.getCurrentDifficultyAt(whale.blockPosition()),
                            MobSpawnType.NATURAL, null, null);
                        level.addFreshEntity(whale);
                    }
                    spawnedSectors.add(sectorKey);
                    DeepSpaceMod.LOGGER.debug("[WhaleSpawn] Spawned pod ({} adults, baby={}) at ({}, {})",
                        adults, hasBaby, (int)podX, (int)podZ);
                }
            }
        }
    }
}
