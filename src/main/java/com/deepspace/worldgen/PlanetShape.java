package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.registry.PlanetDefinition;
import com.deepspace.worldgen.ore.ForgeOreResolver;
import com.deepspace.worldgen.ore.HostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Fills a single chunk with the portion of a planet sphere that intersects it.
 *
 *   1. Clip the sphere bbox to the chunk's column range.
 *   2. shellBlock in the shell region, innerBlock (solid / fluid) in the core.
 *   3. Ore core: a dense host-matched ore body from the centre out to
 *      {@code radius - coreInset}, where coreInset is random per-planet.
 *   4. Vein shell: from just below the surface down to the ore core, large
 *      dense host-matched ore vein blobs (denser / bigger than asteroids).
 *
 * All ore host blocks match the planet's shell block (stone / deepslate /
 * granite / red_granite use the {@code #forge:ores} resolver, vanilla+GTCEU
 * ~1:1; non-rock shells — ice / tar / dirt — keep their archetype ore list).
 *
 * Determinism: ore decisions are pure functions of the planet seed and world
 * position, so multi-chunk planets stay seamless with no shared state.
 */
public final class PlanetShape {

    private PlanetShape() {}

    public static void placeInChunk(ChunkAccess chunk, PlanetDefinition def,
                                    int centerX, int centerY, int centerZ,
                                    int radius, int shellThick, long seed) {

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        int worldMinY = chunk.getMinBuildHeight();
        int worldMaxY = chunk.getMaxBuildHeight() - 1;
        int bboxMinX = centerX - radius;
        int bboxMaxX = centerX + radius;
        int bboxMinY = Math.max(centerY - radius, worldMinY);
        int bboxMaxY = Math.min(centerY + radius, worldMaxY);
        int bboxMinZ = centerZ - radius;
        int bboxMaxZ = centerZ + radius;

        int loX = Math.max(bboxMinX, chunkMinX);
        int hiX = Math.min(bboxMaxX, chunkMaxX);
        int loZ = Math.max(bboxMinZ, chunkMinZ);
        int hiZ = Math.min(bboxMaxZ, chunkMaxZ);
        if (loX > hiX || loZ > hiZ) return;

        BlockState shellState = resolveBlock(def.shellBlock);
        if (shellState == null) shellState = Blocks.STONE.defaultBlockState();
        BlockState innerState = resolveBlockOrFluid(def.innerBlock);
        if (innerState == null) innerState = shellState;

        double rSq      = (double) radius * radius;
        double innerR   = radius - shellThick;
        double innerRSq = innerR * innerR;

        // Random-per-planet core depth: core fills out to (radius - coreInset).
        int insetMin = DSConstants.PLANET_CORE_INSET_MIN;
        int insetMax = DSConstants.PLANET_CORE_INSET_MAX;
        int coreInset = insetMin + (int) (Math.abs(mix(seed, 0x1)) % (insetMax - insetMin + 1));
        double coreR   = Math.max(2.0, radius - coreInset);
        double coreRSq = coreR * coreR;
        double skinR   = radius - DSConstants.PLANET_SURFACE_SKIN_BLOCKS;
        double skinRSq = skinR * skinR;

        // BUG_9: ore logic is best-effort. Any failure here must NEVER stop the
        // sphere from filling, or the planet shows missing chunks.
        List<BlockState> orePool;
        try {
            orePool = buildOrePool(def, seed);
        } catch (Exception e) {
            orePool = new ArrayList<>();
        }
        boolean haveOre = orePool != null && !orePool.isEmpty();

        // --- Main fill + dense ore core -------------------------------------
        for (int x = loX; x <= hiX; x++) {
            double dx = x - centerX;
            double dxSq = dx * dx;
            for (int z = loZ; z <= hiZ; z++) {
                double dz = z - centerZ;
                double dxzSq = dxSq + dz * dz;
                if (dxzSq > rSq) continue;

                for (int y = bboxMinY; y <= bboxMaxY; y++) {
                    double dy   = y - centerY;
                    double dist = dxzSq + dy * dy;
                    if (dist > rSq) continue;

                    BlockState state = (dist >= innerRSq) ? shellState : innerState;

                    if (haveOre && dist <= coreRSq
                        && cellHash(x, y, z, seed) < DSConstants.PLANET_CORE_ORE_FILL) {
                        state = orePool.get(
                            (int) (Math.abs(cellPick(x, y, z, seed)) % orePool.size()));
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), state, false);
                }
            }
        }

        // --- Vein shell: just below surface down to the ore core ------------
        if (haveOre) {
            try {
                placeVeinShell(chunk, def, centerX, centerY, centerZ,
                               coreR, skinR, coreRSq, skinRSq, orePool, seed,
                               chunkMinX, chunkMinZ);
            } catch (Exception ignored) { /* sphere already filled; ore best-effort */ }
        }
    }

    /**
     * Large dense ore vein blobs scattered through the band
     * {@code coreR < dist <= skinR} (i.e. from the ore core up to just under
     * the surface skin). Bigger and denser than asteroid scatter.
     */
    private static void placeVeinShell(ChunkAccess chunk, PlanetDefinition def,
                                       int cx, int cy, int cz,
                                       double coreR, double skinR,
                                       double coreRSq, double skinRSq,
                                       List<BlockState> orePool, long seed,
                                       int chunkMinX, int chunkMinZ) {
        Random rng = new Random(seed ^ 0xC0FFEEFACEBEEFL);
        int veinSize = DSConstants.PLANET_SHELL_VEIN_SIZE;
        double bandLo = Math.max(coreR, 1.0);
        double bandHi = Math.max(skinR, bandLo + 1.0);

        for (int i = 0; i < DSConstants.PLANET_SHELL_VEIN_COUNT; i++) {
            double rr    = bandLo + rng.nextDouble() * (bandHi - bandLo);
            double theta = rng.nextDouble() * Math.PI * 2.0;
            double phi   = Math.acos(1.0 - 2.0 * rng.nextDouble());
            int bx = cx + (int) (rr * Math.sin(phi) * Math.cos(theta));
            int by = cy + (int) (rr * Math.cos(phi));
            int bz = cz + (int) (rr * Math.sin(phi) * Math.sin(theta));

            BlockState ore = orePool.get(rng.nextInt(orePool.size()));
            int vr = 2 + rng.nextInt(Math.max(1, veinSize - 1));

            for (int ddx = -vr; ddx <= vr; ddx++) {
                int x = bx + ddx;
                if (x < chunkMinX || x >= chunkMinX + 16) continue;
                for (int ddy = -vr; ddy <= vr; ddy++) {
                    int y = by + ddy;
                    if (y < -64 || y > 319) continue;
                    for (int ddz = -vr; ddz <= vr; ddz++) {
                        int z = bz + ddz;
                        if (z < chunkMinZ || z >= chunkMinZ + 16) continue;
                        if (ddx * ddx + ddy * ddy + ddz * ddz > vr * vr) continue;
                        double d = (double) (x - cx) * (x - cx)
                                 + (double) (y - cy) * (y - cy)
                                 + (double) (z - cz) * (z - cz);
                        if (d <= coreRSq || d > skinRSq) continue;
                        chunk.setBlockState(new BlockPos(x, y, z), ore, false);
                    }
                }
            }
        }
    }

    /**
     * Rock planets (stone/deepslate/granite/red_granite) -> host-matched
     * #forge:ores, vanilla + GTCEU ~1:1. Non-rock shells (ice/tar/dirt) keep
     * the archetype's own ore list.
     */
    private static List<BlockState> buildOrePool(PlanetDefinition def, long seed) {
        List<BlockState> pool = new ArrayList<>();
        HostKey host = rockHost(def.shellBlock);

        if (host != null && ForgeOreResolver.ready()) {
            List<String> mats = new ArrayList<>(ForgeOreResolver.materialsFor(host));
            if (!mats.isEmpty()) {
                Random rng = new Random(seed ^ 0xA11CE0FFL);
                Collections.shuffle(mats, rng);
                int keep = Math.max(4, (int) (mats.size() * (0.5 + rng.nextDouble() * 0.5)));
                keep = Math.min(keep, mats.size());
                for (int i = 0; i < keep; i++) {
                    String m = mats.get(i);
                    Block v = ForgeOreResolver.vanilla(host, m);
                    Block g = ForgeOreResolver.gt(host, m);
                    int w = 2 + rng.nextInt(4);
                    for (int k = 0; k < w; k++) {
                        if (v != null) pool.add(v.defaultBlockState());
                        if (g != null) pool.add(g.defaultBlockState());
                        if (v == null && g == null) break;
                    }
                }
            }
        }

        if (pool.isEmpty()) { // ice / tar / dirt, or resolver not ready
            for (ResourceLocation rl : def.ores) {
                Block b = ForgeRegistries.BLOCKS.getValue(rl);
                if (b != null && b != Blocks.AIR) pool.add(b.defaultBlockState());
            }
        }
        return pool;
    }

    /** Rock host for the resolver, or null for non-rock shells. */
    private static HostKey rockHost(ResourceLocation shell) {
        if (shell == null) return null;
        String ns = shell.getNamespace(), p = shell.getPath();
        if ("minecraft".equals(ns)) {
            switch (p) {
                case "stone": case "deepslate": case "granite":
                case "diorite": case "andesite": case "tuff": case "basalt":
                    return HostKey.fromBlock(shell);
                default: return null; // packed_ice / coarse_dirt / …
            }
        }
        if ("gtceu".equals(ns) && (p.contains("red_granite") || p.contains("marble")))
            return HostKey.fromBlock(shell);
        return null;
    }

    // -- Deterministic per-cell hashes (chunk-independent) --------------------

    private static long mix(long a, long b) {
        long h = a ^ (b * 0x9E3779B97F4A7C15L);
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    private static double cellHash(int x, int y, int z, long seed) {
        long h = mix(seed, (x * 0x9E3779B97F4A7C15L)
            ^ (y * 0x6C62272E07BB0142L) ^ (z * 0xD1B54A32D192ED03L));
        return (h & 0xFFFFL) / 65536.0;
    }

    private static long cellPick(int x, int y, int z, long seed) {
        return mix(seed ^ 0x5DEECE66DL,
            (x * 0xD1B54A32D192ED03L) ^ (y * 0x9E3779B97F4A7C15L)
            ^ (z * 0x6C62272E07BB0142L));
    }

    // -- Block resolution ----------------------------------------------------

    private static BlockState resolveBlock(ResourceLocation rl) {
        if (rl == null) return null;
        Block b = ForgeRegistries.BLOCKS.getValue(rl);
        return (b != null && b != Blocks.AIR) ? b.defaultBlockState() : null;
    }

    private static BlockState resolveBlockOrFluid(ResourceLocation rl) {
        if (rl == null) return null;
        Block b = ForgeRegistries.BLOCKS.getValue(rl);
        if (b != null && b != Blocks.AIR) return b.defaultBlockState();
        Fluid f = ForgeRegistries.FLUIDS.getValue(rl);
        if (f != null && f != Fluids.EMPTY) {
            Block fluidBlock = f.defaultFluidState().createLegacyBlock().getBlock();
            if (fluidBlock != Blocks.AIR) return fluidBlock.defaultBlockState();
        }
        return null;
    }
}
