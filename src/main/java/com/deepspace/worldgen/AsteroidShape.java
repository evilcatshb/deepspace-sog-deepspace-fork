package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.registry.AsteroidKind;
import com.deepspace.worldgen.ore.ForgeOreResolver;
import com.deepspace.worldgen.ore.HostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds asteroid terrain. Two entry points:
 *
 *  - {@link #place} (legacy single-shape): used by SpawnGuaranteeSystem; builds
 *    a transient ELLIPSOID candidate and routes through {@link #placeCandidate}
 *    so guaranteed asteroids also get host-matched ores.
 *  - {@link #placeCandidate}: called once per overlapping chunk by the sector
 *    grid. The solid is a pure function of the candidate, so multi-chunk
 *    asteroids render seamlessly with no shared state.
 *
 * Ore system:
 *  - ROCK kind ignores the JSON ore list. Ores come from the host-matched
 *    {@code #forge:ores} resolver: every chosen material contributes its
 *    vanilla AND its GTCEU variant (~1:1) for the asteroid's rock host, so
 *    ore blocks always match the body (stone -> minecraft:iron_ore,
 *    deepslate -> minecraft:deepslate_iron_ore, granite -> gtceu:granite_*).
 *  - LAVA / OIL_TAR / FLUID_GAS / WATER_ICE / DIRT keep bespoke behaviour.
 */
public final class AsteroidShape {

    private static final int SHELL_THICKNESS = 2;

    private static final int SOLID_CORE_CHANCE = 6;          // 1-in-6 (ROCK only)
    private static final double CORE_RADIUS_MIN_FRAC = 0.30;
    private static final double CORE_RADIUS_MAX_FRAC = 0.55;
    private static final double CORE_FILL_MIN  = 0.75;
    private static final double CORE_FILL_MAX  = 1.00;

    private static final int CRATERS_PER_ASTEROID = 3;
    private static final int TUNNELS_PER_ASTEROID = 2;

    private AsteroidShape() {}

    // ------------------------------------------------------------------------
    // Legacy entry (SpawnGuaranteeSystem) -- builds an ellipsoid candidate
    // ------------------------------------------------------------------------

    public static void place(ChunkAccess chunk, AsteroidDefinition def,
                             int centreX, int centreY, int centreZ, long seed) {
        Random r = new Random(seed);
        int rMin = DSConstants.ASTEROID_RADIUS_MIN;
        int rMax = DSConstants.ASTEROID_RADIUS_MAX;
        int rx = rMin + r.nextInt(Math.max(1, rMax - rMin));
        int rz = rMin + r.nextInt(Math.max(1, rMax - rMin));
        int ry = Math.max(3, (int) ((rx + rz) * 0.5 * (0.3 + r.nextDouble() * 0.9)));

        int clampedY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + ry + 2,
                       Math.min(centreY, DSConstants.ASTEROID_BAND_Y_MAX - ry - 2));

        HostKey host = HostKey.fromBlock(def.primaryBlock());
        AsteroidCandidate cand = new AsteroidCandidate(
            centreX, clampedY, centreZ, rx, ry, rz,
            AsteroidShapeType.ELLIPSOID, new AsteroidShapeType.Params(),
            def, host, seed ^ 0xDEADBEEFL, seed ^ 0x5151A57E0DEFACEL,
            false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L,
            0, 0, 0, null, null, 0L, // no connection (legacy single asteroid)
            true); // forceDefOres: guaranteed asteroids keep JSON ores
        placeCandidate(chunk, cand);
    }

    // ------------------------------------------------------------------------
    // Shared core -- one chunk's slice of one asteroid
    // ------------------------------------------------------------------------

    public static void placeCandidate(ChunkAccess chunk, AsteroidCandidate cand) {
        AsteroidDefinition def = cand.def;
        Random rng = new Random(cand.shapeSeed);

        int centreX = cand.centerX, clampedY = cand.centerY, centreZ = cand.centerZ;
        int rx = cand.rx, ry = cand.ry, rz = cand.rz;

        BlockState shell = resolveBlock(def.outerShell, rng, Blocks.STONE.defaultBlockState());
        BlockState inner = resolveBlock(def.innerBlocks, rng, Blocks.STONE.defaultBlockState());

        BlockState innerBottom = inner, innerTop = inner;
        if (def.gravityLayered && def.innerBlocks.size() >= 2) {
            Block bB = ForgeRegistries.BLOCKS.getValue(def.innerBlocks.get(0));
            Block bT = ForgeRegistries.BLOCKS.getValue(def.innerBlocks.get(1));
            if (bB != null && bB != Blocks.AIR) innerBottom = bB.defaultBlockState();
            if (bT != null && bT != Blocks.AIR) innerTop = bT.defaultBlockState();
        }

        // WATER_ICE: blend packed_ice into the shell for visual variety.
        BlockState packedIce = state("minecraft:packed_ice");
        boolean iceBlend = def.kind == AsteroidKind.WATER_ICE && packedIce != null;

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        long nx = rng.nextLong(), ny = rng.nextLong(), nz = rng.nextLong();

        boolean hasSolidCore = def.kind == AsteroidKind.ROCK
            && (rng.nextInt(SOLID_CORE_CHANCE) == 0);
        double coreRx = 0, coreRy = 0, coreRz = 0, coreFill = 0;
        BlockState coreOre = Blocks.AIR.defaultBlockState();
        if (hasSolidCore) {
            double f = CORE_RADIUS_MIN_FRAC
                + rng.nextDouble() * (CORE_RADIUS_MAX_FRAC - CORE_RADIUS_MIN_FRAC);
            coreRx = rx * f; coreRy = ry * f; coreRz = rz * f;
            coreFill = CORE_FILL_MIN + rng.nextDouble() * (CORE_FILL_MAX - CORE_FILL_MIN);
            BlockState c = pickHostOre(cand, new Random(cand.oreSeed ^ 0xC04E5EEDL));
            if (c != null) coreOre = c;
        }

        double shellThreshold = computeShellThreshold(rx, ry, rz);
        AsteroidShapeType.Params sp = cand.shapeParams;

        for (int x = centreX - rx - 3; x <= centreX + rx + 3; x++) {
            if (x < chunkMinX || x >= chunkMinX + 16) continue;
            for (int z = centreZ - rz - 3; z <= centreZ + rz + 3; z++) {
                if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                for (int y = clampedY - ry - 2; y <= clampedY + ry + 2; y++) {
                    if (y < DSConstants.ASTEROID_BAND_Y_MIN
                        || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;

                    double normX = (double) (x - centreX) / rx;
                    double normY = (double) (y - clampedY) / ry;
                    double normZ = (double) (z - centreZ) / rz;
                    // BUG_10: coherent (smooth) warp -> a thick fuzzy shell,
                    // never isolated speckle cells.
                    double warp  = valueNoise(x, y, z, nx ^ ny ^ nz) * 0.18;
                    // BUG_10: column-coarse, y-independent -> FLAT_TOP mounds
                    // are vertically contiguous (no floating outcrop blocks).
                    double outcropN = cellNoise01(x >> 2, 0, z >> 2,
                        cand.shapeSeed ^ 0x0C0017L);

                    if (!cand.shape.inside(normX, normY, normZ, warp, outcropN, sp)) continue;
                    double dist = normX * normX + normY * normY + normZ * normZ - warp;

                    // BUG_6b: roughen the outer shell -- drop a few surface cells
                    // so no asteroid is a perfectly smooth solid. Never erode the
                    // interior/core/fluid body (keeps asteroids solid + walkable).
                    boolean surfaceCell = dist > shellThreshold && !def.isFluidAsteroid();
                    if (surfaceCell
                        && cellNoise01(x, y, z, cand.shapeSeed ^ 0xE0DE5L) * 100.0
                           < DSConstants.ASTEROID_EROSION_PCT) {
                        continue;
                    }

                    if (hasSolidCore && coreOre != Blocks.AIR.defaultBlockState()) {
                        double cnX = (double) (x - centreX) / coreRx;
                        double cnY = (double) (y - clampedY) / coreRy;
                        double cnZ = (double) (z - centreZ) / coreRz;
                        if (cnX * cnX + cnY * cnY + cnZ * cnZ <= 1.0
                            && rng.nextDouble() < coreFill) {
                            chunk.setBlockState(new BlockPos(x, y, z), coreOre, false);
                            continue;
                        }
                    }

                    BlockState layeredInner = def.gravityLayered
                        ? (y < clampedY ? innerBottom : innerTop) : inner;

                    BlockState st;
                    if (def.isFluidAsteroid() && dist <= shellThreshold) {
                        String fp = def.fluid != null ? def.fluid.getPath() : "";
                        String ip = def.innerBlocks.stream()
                            .map(ResourceLocation::getPath).findFirst().orElse("");
                        if (fp.contains("lava") || ip.contains("lava")) {
                            st = Blocks.LAVA.defaultBlockState();
                        } else if (!fp.isEmpty()) {
                            Block fb = ForgeRegistries.BLOCKS.getValue(def.fluid);
                            st = (fb != null && fb != Blocks.AIR)
                                ? fb.defaultBlockState() : inner;
                        } else {
                            st = layeredInner;
                        }
                    } else if (dist > shellThreshold) {
                        st = (iceBlend && ((x + y + z) & 3) == 0) ? packedIce : shell;
                    } else {
                        st = layeredInner;
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), st, false);
                }
            }
        }

        for (int c = 0; c < CRATERS_PER_ASTEROID; c++)
            carveCrater(chunk, centreX, clampedY, centreZ, rx, ry, rz, rng);
        for (int t = 0; t < TUNNELS_PER_ASTEROID; t++)
            carveTunnel(chunk, centreX, clampedY, centreZ, rx, ry, rz, rng);

        // BUG_11: round spherical impressions dished into the TOP face of
        // some asteroids (deterministic per-asteroid roll).
        if (!def.isFluidAsteroid()
            && cellNoise01(centreX, 0, centreZ, cand.shapeSeed ^ 0xC4A7E5L) * 100.0
               < DSConstants.ASTEROID_TOP_CRATER_CHANCE_PCT) {
            int topSurfaceY = (cand.shape == AsteroidShapeType.FLAT_TOP_PLATFORM)
                ? clampedY + (int) Math.round(ry * cand.shapeParams.flatTopNorm)
                : clampedY + ry;
            int nCr = 1 + rng.nextInt(Math.max(1, DSConstants.ASTEROID_TOP_CRATER_MAX));
            for (int c = 0; c < nCr; c++)
                carveTopCrater(chunk, centreX, topSurfaceY, centreZ, rx, rz, rng);
        }

        placeOres(chunk, cand, inner, shell);

        if (cand.connected)
            buildWalkway(chunk, cand, inner, shell);
    }

    // ------------------------------------------------------------------------
    // Ore placement (kind-aware)
    // ------------------------------------------------------------------------

    private static void placeOres(ChunkAccess chunk, AsteroidCandidate cand,
                                  BlockState inner, BlockState shell) {
        AsteroidDefinition def = cand.def;
        int budget = oreBudget(cand);
        Random rng = new Random(cand.oreSeed); // chunk-independent -> deterministic union

        // BUG_8: guaranteed starter asteroids keep their JSON ores, heavily
        // weighted toward the resource the asteroid id names (mostly iron/copper/…).
        if (cand.forceDefOres) {
            List<BlockState> pool = buildGuaranteedPool(def);
            if (!pool.isEmpty()) scatter(chunk, cand, pool, budget, rng);
            return;
        }

        switch (def.kind) {
            case ROCK: {
                List<BlockState> pool = buildHostOrePool(cand, rng);
                if (pool.isEmpty()) pool = resolvePool(def.ores); // safety fallback
                scatter(chunk, cand, pool, budget, rng);
                break;
            }
            case WATER_ICE: {
                List<BlockState> pool = mutableStates(
                    "deepspace:fossil", "minecraft:bone_block", "minecraft:packed_ice");
                pool.addAll(resolvePool(def.ores));
                scatter(chunk, cand, pool, budget / 4, rng);
                break;
            }
            case DIRT: {
                List<BlockState> pool = mutableStates(
                    "minecraft:coal_ore", "deepspace:fossil", "minecraft:bone_block");
                scatter(chunk, cand, pool, budget / 5, rng);
                break;
            }
            case LAVA:
            case OIL_TAR:
            case FLUID_GAS:
            case ALIEN:
            default: {
                List<BlockState> pool = resolvePool(def.ores);
                if (!pool.isEmpty()) scatter(chunk, cand, pool, budget / 4, rng);
                break;
            }
        }
    }

    /**
     * Host-matched #forge:ores pool: a varied per-asteroid subset of materials;
     * each contributes its vanilla AND gtceu variant for this rock host -> ~1:1
     * vanilla:GTCEU on stone/deepslate, host-correct gtceu-only elsewhere.
     */
    private static List<BlockState> buildHostOrePool(AsteroidCandidate cand, Random rng) {
        return hostOrePool(cand.host, rng);
    }

    /** Host-matched ore pool for a given host (BUG_5: null host -> empty). */
    private static List<BlockState> hostOrePool(HostKey host, Random rng) {
        List<BlockState> pool = new ArrayList<>();
        List<String> mats = new ArrayList<>(ForgeOreResolver.materialsFor(host));
        if (mats.isEmpty()) return pool;

        Collections.shuffle(mats, rng);
        int keep = Math.min(mats.size(), 2 + rng.nextInt(3)); // BUG_7: max 4 materials

        for (int i = 0; i < keep; i++) {
            String m = mats.get(i);
            Block v = ForgeOreResolver.vanilla(host, m);
            Block g = ForgeOreResolver.gt(host, m);
            int weight = 2 + rng.nextInt(5); // "different distributions" per asteroid
            for (int w = 0; w < weight; w++) {
                if (v != null) pool.add(v.defaultBlockState());
                if (g != null) pool.add(g.defaultBlockState());
                if (v == null && g == null) break;
            }
        }
        return pool;
    }

    private static BlockState pickHostOre(AsteroidCandidate cand, Random rng) {
        List<BlockState> p = buildHostOrePool(cand, rng);
        if (p.isEmpty()) p = resolvePool(cand.def.ores);
        return p.isEmpty() ? null : p.get(rng.nextInt(p.size()));
    }

    /** Spherical interior scatter, clipped to this chunk and the shape volume. */
    private static void scatter(ChunkAccess chunk, AsteroidCandidate cand,
                                List<BlockState> pool, int count, Random rng) {
        if (pool.isEmpty() || count <= 0) return;
        int cx = cand.centerX, cy = cand.centerY, cz = cand.centerZ;
        int rx = cand.rx, ry = cand.ry, rz = cand.rz;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        AsteroidShapeType.Params sp = cand.shapeParams;

        for (int i = 0; i < count; i++) {
            double t     = 0.10 + rng.nextDouble() * 0.75;
            double theta = rng.nextDouble() * Math.PI * 2.0;
            double phi   = Math.acos(1.0 - 2.0 * rng.nextDouble());
            double sx = t * Math.sin(phi) * Math.cos(theta);
            double sy = t * Math.cos(phi);
            double sz = t * Math.sin(phi) * Math.sin(theta);

            int ox = cx + (int) (rx * sx);
            int oy = cy + (int) (ry * sy);
            int oz = cz + (int) (rz * sz);

            if (ox < chunkMinX || ox >= chunkMinX + 16) continue;
            if (oz < chunkMinZ || oz >= chunkMinZ + 16) continue;
            if (oy < DSConstants.ASTEROID_BAND_Y_MIN
                || oy > DSConstants.ASTEROID_BAND_Y_MAX) continue;
            if (!cand.shape.inside(sx, sy, sz, 0.0, 1.0, sp)) continue;

            chunk.setBlockState(new BlockPos(ox, oy, oz),
                pool.get(rng.nextInt(pool.size())), false);
        }
    }

    /** Ore-cell budget ~ inner volume, so ore:rock ratio stays constant. */
    private static int oreBudget(AsteroidCandidate cand) {
        long v = (long) cand.rx * cand.ry * cand.rz;
        int n = (int) (v * DSConstants.ASTEROID_ORE_VOLUME_MULT);
        return Math.max(400, Math.min(n, 12000));
    }

    // ------------------------------------------------------------------------
    // Walkable connection neck
    // ------------------------------------------------------------------------

    /**
     * BUG_13: ore pool mixing BOTH connected asteroids' ores. Built in a
     * canonical (seed-sorted) order so both bridge endpoints produce the
     * identical list -> deterministic, seamless veins.
     */
    private static List<BlockState> buildWalkwayOrePool(AsteroidCandidate cand) {
        long sA = cand.oreSeed, sB = cand.linkOreSeed;
        HostKey hA = cand.host, hB = cand.linkHost;
        AsteroidDefinition dA = cand.def, dB = cand.linkDef;
        boolean aFirst = sA <= sB; // canonical order, role-independent
        List<BlockState> pool = new ArrayList<>();
        addSidePool(pool, aFirst ? hA : hB, aFirst ? sA : sB, aFirst ? dA : dB);
        addSidePool(pool, aFirst ? hB : hA, aFirst ? sB : sA, aFirst ? dB : dA);
        return pool;
    }

    private static void addSidePool(List<BlockState> pool, HostKey host,
                                    long oreSeed, AsteroidDefinition def) {
        List<BlockState> p = hostOrePool(host, new Random(oreSeed ^ 0x0DE51DEL));
        if (p.isEmpty() && def != null) p = resolvePool(def.ores);
        pool.addAll(p);
    }

    private static void buildWalkway(ChunkAccess chunk, AsteroidCandidate cand,
                                     BlockState inner, BlockState shell) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int x0 = cand.centerX, z0 = cand.centerZ;
        int x1 = cand.linkX,  z1 = cand.linkZ;

        // BUG_0: platform radius = average of the two asteroids' radii.
        int r = Math.max(2, cand.linkR);
        long bridgeSeed = cand.linkSeed; // shared so both endpoints match exactly

        // BUG_16: the bridge ramps from asteroid A's surface Y to B's surface Y.
        int aTop = cand.walkAtopY, bTop = cand.walkBtopY;
        // BUG_15: thickness = average of the two asteroids' heights.
        int thickness = Math.max(3, cand.walkHeight);

        // Ellipsoid coefficients for BOTH asteroids (BUG_3/BUG_12: clip to real
        // curvature; never extend the slab past either asteroid's surface).
        double aRx = Math.max(1, cand.rx), aRy = Math.max(1, cand.ry), aRz = Math.max(1, cand.rz);
        double bRx = Math.max(1, cand.linkRx), bRy = Math.max(1, cand.linkRy),
               bRz = Math.max(1, cand.linkRz);

        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        if (steps == 0) return;
        double clip = 1.0 + DSConstants.WALKWAY_WARP_MARGIN;

        // BUG_13: ore mix from both asteroids.
        List<BlockState> orePool = buildWalkwayOrePool(cand);

        for (int s = 0; s <= steps; s++) {
            double f = (double) s / steps;
            int bx = (int) Math.round(x0 + (x1 - x0) * f);
            int bz = (int) Math.round(z0 + (z1 - z0) * f);

            // BUG_16: gradual incline/decline -- centre-line top Y is the
            // linear ramp between the two asteroids' surface Ys.
            int rampTop = (int) Math.round(aTop + (bTop - aTop) * f);

            // BUG_4: organic, non-uniform cross-section.
            double taper  = 1.0 - 0.20 * Math.pow(2.0 * f - 1.0, 2.0); // ~0.8..1.0
            double rNoise = 0.62 + 0.38 * cellNoise01(bx, 0, bz, bridgeSeed ^ 0x4A11D7L);
            int rCol   = Math.max(2, (int) Math.round(r * taper * rNoise));
            int rColSq = rCol * rCol;

            for (int dx = -rCol; dx <= rCol; dx++) {
                for (int dz = -rCol; dz <= rCol; dz++) {
                    if (dx * dx + dz * dz > rColSq) continue;
                    int x = bx + dx, z = bz + dz;
                    if (x < chunkMinX || x >= chunkMinX + 16) continue;
                    if (z < chunkMinZ || z >= chunkMinZ + 16) continue;

                    // BUG_4: undulating top (±1) on the ramped surface.
                    int colTop = rampTop + (int) Math.round(
                        cellNoise01(x, 0, z, bridgeSeed ^ 0x70B5EEDL) * 2.0 - 1.0);
                    colTop = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 2,
                             Math.min(colTop, DSConstants.ASTEROID_BAND_Y_MAX - 1));
                    // BUG_15: full thickness column beneath the ramped top.
                    int colBot = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 1,
                                          colTop - thickness + 1);

                    for (int y = colBot; y <= colTop; y++) {
                        // BUG_3 + BUG_12: clip to each asteroid's max warped
                        // solid envelope so no bridge block pokes past the curve.
                        double eax = (x - cand.centerX) / aRx;
                        double eay = (y - cand.centerY) / aRy;
                        double eaz = (z - cand.centerZ) / aRz;
                        if (eax * eax + eay * eay + eaz * eaz <= clip) continue;
                        double ebx = (x - cand.linkX) / bRx;
                        double eby = (y - cand.linkCenterY) / bRy;
                        double ebz = (z - cand.linkZ) / bRz;
                        if (ebx * ebx + eby * eby + ebz * ebz <= clip) continue;

                        // Random block-removal noise (shared seed).
                        double er = (y == colTop)
                            ? DSConstants.ASTEROID_EROSION_PCT / 2.0  // keep top walkable-ish
                            : DSConstants.ASTEROID_EROSION_PCT;
                        if (cellNoise01(x, y, z, bridgeSeed) * 100.0 < er) continue;

                        BlockState st;
                        if (y == colTop) {
                            st = shell;
                        } else if (!orePool.isEmpty()
                            && cellNoise01(x, y, z, bridgeSeed ^ 0x05E11L) * 100.0
                               < DSConstants.WALKWAY_ORE_PCT) {
                            // BUG_13: interior ore vein from BOTH asteroids.
                            int idx = (int) (cellNoise01(x, y, z, bridgeSeed ^ 0x15E12L)
                                * orePool.size());
                            if (idx >= orePool.size()) idx = orePool.size() - 1;
                            st = orePool.get(idx);
                        } else {
                            st = inner;
                        }
                        chunk.setBlockState(new BlockPos(x, y, z), st, false);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Crater / tunnel carving (unchanged; already chunk-clip safe)
    // ------------------------------------------------------------------------

    /**
     * BUG_11: carve a near-vertical spherical impression into the asteroid's
     * top face. The sphere centre sits above the surface so only its lower
     * cap removes blocks -> a clean round bowl in the top.
     */
    private static void carveTopCrater(ChunkAccess chunk, int cx, int topY,
                                        int cz, int rx, int rz, Random rng) {
        int maxR = Math.max(3, Math.min(rx, rz));
        int craterR = Math.max(3, maxR / 2 + rng.nextInt(Math.max(1, maxR / 2)));
        int ox = cx + (rng.nextInt(maxR + 1) - maxR / 2);
        int oz = cz + (rng.nextInt(maxR + 1) - maxR / 2);
        int oy = topY + craterR / 2; // centre above surface -> dishes the top

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int rSq = craterR * craterR;

        for (int dx = -craterR; dx <= craterR; dx++) {
            for (int dy = -craterR; dy <= craterR; dy++) {
                for (int dz = -craterR; dz <= craterR; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) continue;
                    int x = ox + dx, y = oy + dy, z = oz + dz;
                    if (x < chunkMinX || x >= chunkMinX + 16) continue;
                    if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                    if (y < DSConstants.ASTEROID_BAND_Y_MIN
                        || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;
                    chunk.setBlockState(new BlockPos(x, y, z),
                        Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    private static void carveCrater(ChunkAccess chunk, int cx, int cy, int cz,
                                     int rx, int ry, int rz, Random rng) {
        double theta = rng.nextDouble() * Math.PI * 2;
        double phi   = rng.nextDouble() * Math.PI;
        int craterR  = 2 + rng.nextInt(4);

        int ox = cx + (int) (rx * Math.sin(phi) * Math.cos(theta));
        int oy = cy + (int) (ry * Math.cos(phi));
        int oz = cz + (int) (rz * Math.sin(phi) * Math.sin(theta));

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        for (int dx = -craterR; dx <= craterR; dx++) {
            for (int dy = -craterR; dy <= craterR; dy++) {
                for (int dz = -craterR; dz <= craterR; dz++) {
                    int x = ox + dx, y = oy + dy, z = oz + dz;
                    if (x < chunkMinX || x >= chunkMinX + 16) continue;
                    if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                    if (y < DSConstants.ASTEROID_BAND_Y_MIN
                        || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;
                    double d = (dx * dx + dy * dy * 0.7 + dz * dz)
                        / (double) (craterR * craterR);
                    if (d <= 1.0)
                        chunk.setBlockState(new BlockPos(x, y, z),
                            Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    private static void carveTunnel(ChunkAccess chunk, int cx, int cy, int cz,
                                     int rx, int ry, int rz, Random rng) {
        boolean vertical = rng.nextBoolean();
        int tunnelRadius = 1;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        if (vertical) {
            int tx = cx + rng.nextInt(Math.max(1, rx / 2)) * (rng.nextBoolean() ? 1 : -1);
            int tz = cz + rng.nextInt(Math.max(1, rz / 2)) * (rng.nextBoolean() ? 1 : -1);
            for (int y = cy - ry; y <= cy + ry; y++) {
                if (y < DSConstants.ASTEROID_BAND_Y_MIN
                    || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;
                for (int dx = -tunnelRadius; dx <= tunnelRadius; dx++) {
                    for (int dz = -tunnelRadius; dz <= tunnelRadius; dz++) {
                        int x = tx + dx, z = tz + dz;
                        if (x < chunkMinX || x >= chunkMinX + 16) continue;
                        if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                        chunk.setBlockState(new BlockPos(x, y, z),
                            Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        } else {
            boolean alongX = rng.nextBoolean();
            int ty = cy + rng.nextInt(Math.max(1, ry / 2)) * (rng.nextBoolean() ? 1 : -1);
            if (alongX) {
                int tz = cz + rng.nextInt(Math.max(1, rz / 2)) * (rng.nextBoolean() ? 1 : -1);
                for (int x = cx - rx; x <= cx + rx; x++) {
                    if (x < chunkMinX || x >= chunkMinX + 16) continue;
                    for (int dy = -tunnelRadius; dy <= tunnelRadius + 1; dy++) {
                        for (int dz = -tunnelRadius; dz <= tunnelRadius; dz++) {
                            int y = ty + dy, z = tz + dz;
                            if (y < DSConstants.ASTEROID_BAND_Y_MIN
                                || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;
                            if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                            chunk.setBlockState(new BlockPos(x, y, z),
                                Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            } else {
                int tx = cx + rng.nextInt(Math.max(1, rx / 2)) * (rng.nextBoolean() ? 1 : -1);
                for (int z = cz - rz; z <= cz + rz; z++) {
                    if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                    for (int dy = -tunnelRadius; dy <= tunnelRadius + 1; dy++) {
                        for (int dx = -tunnelRadius; dx <= tunnelRadius; dx++) {
                            int y = ty + dy, x = tx + dx;
                            if (y < DSConstants.ASTEROID_BAND_Y_MIN
                                || y > DSConstants.ASTEROID_BAND_Y_MAX) continue;
                            if (x < chunkMinX || x >= chunkMinX + 16) continue;
                            chunk.setBlockState(new BlockPos(x, y, z),
                                Blocks.AIR.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static List<BlockState> resolvePool(List<ResourceLocation> ores) {
        List<BlockState> l = new ArrayList<>();
        for (ResourceLocation rl : ores) {
            Block b = ForgeRegistries.BLOCKS.getValue(rl);
            if (b != null && b != Blocks.AIR) l.add(b.defaultBlockState());
        }
        return l;
    }

    private static List<BlockState> mutableStates(String... ids) {
        List<BlockState> l = new ArrayList<>();
        for (String id : ids) {
            BlockState s = state(id);
            if (s != null) l.add(s);
        }
        return l;
    }

    private static BlockState state(String id) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return (b != null && b != Blocks.AIR) ? b.defaultBlockState() : null;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /**
     * BUG_10: smooth coherent value noise in [-1,1) -- trilinear interpolation
     * of {@link #cellNoise01} lattice points on a 4-block grid. Deterministic
     * and chunk-independent, so the warped shell boundary is continuous (a
     * thick fuzzy shell) instead of speckled with isolated cells.
     */
    private static double valueNoise(int x, int y, int z, long seed) {
        final int S = 4;
        int x0 = Math.floorDiv(x, S), y0 = Math.floorDiv(y, S), z0 = Math.floorDiv(z, S);
        double tx = (x - x0 * S) / (double) S;
        double ty = (y - y0 * S) / (double) S;
        double tz = (z - z0 * S) / (double) S;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        tz = tz * tz * (3 - 2 * tz);
        double c000 = cellNoise01(x0,   y0,   z0,   seed);
        double c100 = cellNoise01(x0+1, y0,   z0,   seed);
        double c010 = cellNoise01(x0,   y0+1, z0,   seed);
        double c110 = cellNoise01(x0+1, y0+1, z0,   seed);
        double c001 = cellNoise01(x0,   y0,   z0+1, seed);
        double c101 = cellNoise01(x0+1, y0,   z0+1, seed);
        double c011 = cellNoise01(x0,   y0+1, z0+1, seed);
        double c111 = cellNoise01(x0+1, y0+1, z0+1, seed);
        double x00 = lerp(c000, c100, tx), x10 = lerp(c010, c110, tx);
        double x01 = lerp(c001, c101, tx), x11 = lerp(c011, c111, tx);
        double yA  = lerp(x00, x10, ty),   yB  = lerp(x01, x11, ty);
        return lerp(yA, yB, tz) * 2.0 - 1.0;
    }

    /** Deterministic, chunk-independent per-cell hash in [0,1). */
    private static double cellNoise01(int x, int y, int z, long seed) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (y * 0x6C62272E07BB0142L)
               ^ (z * 0xD1B54A32D192ED03L) ^ seed;
        h ^= h >>> 30; h *= 0xbf58476d1ce4e5b9L;
        h ^= h >>> 27; h *= 0x94d049bb133111ebL;
        h ^= h >>> 31;
        return (h & 0xFFFFL) / 65536.0;
    }

    /**
     * BUG_8: pool for guaranteed starter asteroids. Keeps the asteroid's JSON
     * ores but weights whichever ore matches the asteroid id's resource token
     * (e.g. "iron"/"copper"/"tin"/"coal" from {@code <token>_<rock>_asteroid})
     * heavily, so the body is mostly that ore.
     */
    private static List<BlockState> buildGuaranteedPool(AsteroidDefinition def) {
        List<BlockState> pool = new ArrayList<>();
        if (def.ores.isEmpty()) return pool;
        String token = def.id;
        int us = token.indexOf('_');
        if (us > 0) token = token.substring(0, us);
        for (ResourceLocation rl : def.ores) {
            Block b = ForgeRegistries.BLOCKS.getValue(rl);
            if (b == null || b == Blocks.AIR) continue;
            BlockState st = b.defaultBlockState();
            int w = rl.getPath().contains(token) ? 8 : 1;
            for (int i = 0; i < w; i++) pool.add(st);
        }
        return pool;
    }

    private static double computeShellThreshold(int rx, int ry, int rz) {
        int minR = Math.min(rx, Math.min(ry, rz));
        if (minR < 1) minR = 1;
        return Math.max(0.0, 1.0 - (double) SHELL_THICKNESS / minR);
    }

    private static BlockState resolveBlock(List<ResourceLocation> ids, Random rng,
                                            BlockState fallback) {
        if (ids.isEmpty()) return fallback;
        ResourceLocation rl = ids.get(rng.nextInt(ids.size()));
        Block block = ForgeRegistries.BLOCKS.getValue(rl);
        return (block != null && block != Blocks.AIR) ? block.defaultBlockState() : fallback;
    }
}
