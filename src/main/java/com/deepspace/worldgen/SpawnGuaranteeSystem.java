package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.compat.FTBChunksCompat;
import com.deepspace.crash.CrashBubbleData;
import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.registry.AsteroidRegistry;
import com.deepspace.util.PersonalSeedHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

/**
 * Spawn Guarantee System -- prevents progression softlocks near the crash site.
 *
 * -- What is guaranteed ----------------------------------------------------
 *   Scans a 10-chunk radius (160 blocks) around the player's crash site.
 *   If any of the following are absent, injects one asteroid of that type:
 *
 *     * 2x ice asteroid       -- packed ice shell / ice core / water drilling vein
 *                               Critical for oxygen production chain:
 *                               ice -> water -> electrolyser -> oxygen
 *     * 1x iron asteroid      -- stone shell, iron/cobaltite/pyrite/magnetite ores
 *                               Needed for basic steel and machine casings
 *     * 1x copper asteroid    -- stone shell, copper/chalcopyrite/bornite ores
 *                               Needed for LV cables and circuits
 *     * 1x tin asteroid       -- stone shell, tin/cassiterite ores
 *                               Needed for solder and LV circuits
 *     * 1x coal asteroid      -- stone shell, coal/graphite ores
 *                               Needed for steam boilers and carbon-based recipes
 *
 * -- Placement spacing fix -------------------------------------------------
 *   Previously injected asteroids landed only 3-6 chunks (48-96 blocks) from
 *   the crash ship, causing visible clutter right at spawn.
 *
 *   Fix: guaranteed asteroids are placed at a much wider orbit around the
 *   crash site (MIN_INJECT_RADIUS ... MAX_INJECT_RADIUS chunks). Each injection
 *   is also placed in a different angular sector (sectors divide the circle
 *   evenly by injection index) so the five guaranteed resources are spread
 *   around the player in different directions, never bunched together.
 *
 *   A minimum separation check prevents two injected asteroids landing in
 *   adjacent chunks.
 */
public final class SpawnGuaranteeSystem {

    private SpawnGuaranteeSystem() {}

    private static final long GUARANTEE_SALT = 0xCAFEBABEDEADBEEFL;

    // -- Minimum separation between injected asteroids (chunks) --------------
    /** Injected asteroids must be at least this many chunks apart from each other.
     *  BUG_8: small (2) so all starter asteroids fit in the tight 2..6-chunk band. */
    private static final int MIN_INJECT_SEPARATION_CHUNKS = 2;

    // -- Placement radius band for guaranteed injections ----------------------
    /**
     * Minimum injection radius in chunks from crash site centre.
     * BUG_8: starter iron/copper/tin/coal/ice asteroids must be right next to
     * the crashed ship (within ~3 chunks), so place them in a 2..6-chunk band.
     */
    private static final int MIN_INJECT_RADIUS = 2;

    /** Maximum injection radius in chunks (6 chunks = 96 blocks). */
    private static final int MAX_INJECT_RADIUS = 6;

    private static final int RETRY_ATTEMPTS     = 12;   // more retries given wider search space
    private static final int SCAN_RADIUS_CHUNKS = 10;

    /**
     * Required asteroid categories -> minimum count inside bubble.
     * Each key matches ANY asteroid whose id contains that key string.
     */
    private static final Map<String, Integer> REQUIRED;
    static {
        REQUIRED = new LinkedHashMap<>();
        REQUIRED.put("ice_asteroid",  2);
        REQUIRED.put("iron_",         1);   // any stone-type variant of iron asteroid
        REQUIRED.put("copper_",       1);
        REQUIRED.put("tin_",          1);
        REQUIRED.put("coal_",         1);
    }

    // -- Public API ------------------------------------------------------------

    public static Set<ChunkPos> ensureGuarantees(ServerLevel level, CrashBubbleData bubble) {
        Map<String, Integer> present = scanForPresentAsteroids(level,
            bubble.centerChunkX, bubble.centerChunkZ);
        DeepSpaceMod.LOGGER.debug("[SpawnGuarantee] Present near crash site: {}", present);

        // Track positions of asteroids we inject this session so we can enforce
        // spacing between them (as well as spacing from the crash site itself).
        List<int[]> injectedPositions = new ArrayList<>();
        // BUG_2: collect every chunk we write so the caller can resend them.
        Set<ChunkPos> touched = new HashSet<>();

        int sectorIndex = 0;
        int totalSectors = REQUIRED.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> req : REQUIRED.entrySet()) {
            String key   = req.getKey();
            int needed   = req.getValue();
            int count    = present.getOrDefault(key, 0);

            for (int i = count; i < needed; i++) {
                String targetId = key.endsWith("_") ? key + "stone_asteroid" : key;

                boolean ok = injectGuaranteedAsteroid(
                    level, bubble, targetId, sectorIndex, totalSectors,
                    injectedPositions, touched);

                if (ok) {
                    DeepSpaceMod.LOGGER.info(
                        "[SpawnGuarantee] Injected {} (sector {}/{})",
                        targetId, sectorIndex + 1, totalSectors);
                } else {
                    DeepSpaceMod.LOGGER.warn(
                        "[SpawnGuarantee] Could not find suitable position for {} -- skipped",
                        targetId);
                }
                sectorIndex++;
            }
        }
        return touched;
    }

    // -- Scanning --------------------------------------------------------------

    private static Map<String, Integer> scanForPresentAsteroids(ServerLevel level,
                                                                   int cx, int cz) {
        Map<String, Integer> found = new HashMap<>();
        for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                int ccx = cx + dx, ccz = cz + dz;
                if (!level.hasChunk(ccx, ccz)) continue;
                identifyAsteroidInChunk(level.getChunk(ccx, ccz), found);
            }
        }
        return found;
    }

    private static void identifyAsteroidInChunk(LevelChunk chunk, Map<String, Integer> found) {
        int sx = chunk.getPos().getMinBlockX();
        int sz = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx += 4) {
            for (int lz = 0; lz < 16; lz += 4) {
                for (int y = DSConstants.ASTEROID_BAND_Y_MIN;
                         y <= DSConstants.ASTEROID_BAND_Y_MAX; y += 4) {
                    var rl = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                        .getKey(chunk.getBlockState(new BlockPos(sx+lx, y, sz+lz)).getBlock());
                    if (rl == null) continue;
                    String p = rl.getPath();

                    if (p.equals("packed_ice") || p.equals("ice"))
                        found.merge("ice_asteroid", 1, Integer::sum);
                    if (p.contains("_iron_ore") || p.equals("iron_block"))
                        found.merge("iron_", 1, Integer::sum);
                    if (p.contains("_copper_ore"))
                        found.merge("copper_", 1, Integer::sum);
                    if (p.contains("_tin_ore") || p.contains("_cassiterite_ore"))
                        found.merge("tin_", 1, Integer::sum);
                    if (p.equals("coal_ore") || p.contains("_coal_ore"))
                        found.merge("coal_", 1, Integer::sum);
                }
            }
        }
    }

    // -- Injection -------------------------------------------------------------

    /**
     * Inject one guaranteed asteroid, placed in the given angular sector at a
     * distance of MIN_INJECT_RADIUS ... MAX_INJECT_RADIUS chunks from the crash site.
     *
     * @param sectorIndex     Which sector (0-based) this injection occupies.
     * @param totalSectors    Total number of injections planned (divides the circle).
     * @param injectedSoFar   Positions of asteroids already injected this session.
     *                        Used to enforce inter-injection minimum separation.
     */
    private static boolean injectGuaranteedAsteroid(ServerLevel level,
                                                     CrashBubbleData bubble,
                                                     String asteroidId,
                                                     int sectorIndex,
                                                     int totalSectors,
                                                     List<int[]> injectedSoFar,
                                                     Set<ChunkPos> touched) {
        AsteroidDefinition def = AsteroidRegistry.getAsteroid(asteroidId);
        if (def == null) {
            DeepSpaceMod.LOGGER.error("[SpawnGuarantee] Unknown asteroid ID: {}", asteroidId);
            return false;
        }

        Random rng = new Random(
            bubble.personalSeed ^ (long) asteroidId.hashCode() ^ sectorIndex);

        // Angular sector centre for this injection.
        // Each injection is assigned a 1/totalSectors slice of the circle,
        // with a small random jitter within the slice so they don't form a
        // perfectly geometric pattern.
        double sectorWidth  = (2.0 * Math.PI) / Math.max(1, totalSectors);
        double sectorCentre = sectorIndex * sectorWidth;
        // Jitter: +-30% of the sector width
        double jitter = (rng.nextDouble() - 0.5) * sectorWidth * 0.6;
        double angle  = sectorCentre + jitter;

        int radiusRange = MAX_INJECT_RADIUS - MIN_INJECT_RADIUS;

        for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
            // Vary radius within the allowed band, with more attempts pushing outward
            double radiusFrac = (double) attempt / RETRY_ATTEMPTS;
            int radius = MIN_INJECT_RADIUS
                + (int)(radiusRange * (0.2 + radiusFrac * 0.8))
                + rng.nextInt(4) - 2; // +-2 chunk jitter
            radius = Math.max(MIN_INJECT_RADIUS, Math.min(MAX_INJECT_RADIUS, radius));

            // Also vary the angle slightly each retry to search around the sector
            double attemptAngle = angle + (rng.nextDouble() - 0.5) * sectorWidth * 0.3;

            int tcx = bubble.centerChunkX + (int)(radius * Math.cos(attemptAngle));
            int tcz = bubble.centerChunkZ + (int)(radius * Math.sin(attemptAngle));

            // Skip FTBChunks-claimed chunks
            if (FTBChunksCompat.isChunkClaimed(level, tcx, tcz)) continue;

            // -- Separation check: must be at least MIN_INJECT_SEPARATION_CHUNKS
            //    away from every previously injected asteroid this session ------
            boolean tooClose = false;
            for (int[] prev : injectedSoFar) {
                int ddx = Math.abs(tcx - prev[0]);
                int ddz = Math.abs(tcz - prev[1]);
                if (ddx < MIN_INJECT_SEPARATION_CHUNKS
                    && ddz < MIN_INJECT_SEPARATION_CHUNKS) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            // -- Place ---------------------------------------------------------
            int cx = tcx * 16 + 8;
            int cz = tcz * 16 + 8;
            int cy = DSConstants.ASTEROID_BAND_Y_MIN + 12
                + rng.nextInt(DSConstants.ASTEROID_BAND_Y_MAX
                              - DSConstants.ASTEROID_BAND_Y_MIN - 24);

            // BUG_1: never place a guaranteed asteroid where it would intersect
            // the floating ship. Use the max asteroid radius as a safe envelope;
            // a failed attempt makes the retry loop push further out.
            if (bubble.shipIntersectsSphere(cx, cy, cz,
                    DSConstants.ASTEROID_RADIUS_MAX, 8)) {
                continue;
            }

            long seed = PersonalSeedHelper.chunkSeed(bubble.personalSeed, tcx, tcz)
                ^ GUARANTEE_SALT;

            // Place asteroid across the 3x3 chunk grid it can straddle so every
            // chunk has the data; record them for the BUG_2 resend.
            ChunkAccess centerChunk = level.getChunk(tcx, tcz);
            AsteroidShape.place(centerChunk, def, cx, cy, cz, seed);
            touched.add(new ChunkPos(tcx, tcz));

            for (int ddx = -1; ddx <= 1; ddx++) {
                for (int ddz = -1; ddz <= 1; ddz++) {
                    if (ddx == 0 && ddz == 0) continue; // already placed center
                    ChunkAccess neighbourChunk = level.getChunk(tcx + ddx, tcz + ddz);
                    AsteroidShape.place(neighbourChunk, def, cx, cy, cz, seed);
                    touched.add(new ChunkPos(tcx + ddx, tcz + ddz));
                }
            }

            // Inject bedrock fluid vein for fluid-bearing asteroids (e.g. ice → water).
            // SpawnGuaranteeSystem runs on the main server thread so SavedData is safe.
            if (def.hasDrillingVein) {
                com.deepspace.compat.GTCEuCompat.placeFluidVein(level, def, cx, cz);
                DeepSpaceMod.LOGGER.info(
                    "[SpawnGuarantee] Injected fluid vein for {} at [{},{}] fluid={}",
                    def.id, cx, cz, def.fluid);
            }

            // Inject bedrock ore vein for all asteroids with GT ores.
            if (!def.ores.isEmpty()) {
                com.deepspace.compat.GTCEuCompat.placeOreVein(level, def, cx, cz);
                DeepSpaceMod.LOGGER.info(
                    "[SpawnGuarantee] Injected ore vein for {} at [{},{}]",
                    def.id, cx, cz);
            }

            // Record this position for inter-injection spacing
            injectedSoFar.add(new int[]{tcx, tcz});
            return true;
        }

        return false;
    }
}