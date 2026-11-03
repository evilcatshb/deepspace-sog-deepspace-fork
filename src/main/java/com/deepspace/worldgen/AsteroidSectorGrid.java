package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.registry.AsteroidRegistry;
import com.deepspace.worldgen.ore.HostKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic sector-based asteroid placement (mirrors PlanetSectorGrid).
 *
 * Space is divided into {@code ASTEROID_SECTOR_SIZE_CHUNKS}² grids; each sector
 * holds at most one asteroid, decided purely by hashing the effective seed
 * (world seed, or the player's personal crash-bubble seed) and sector coords —
 * no mutable shared state, safe on async worldgen threads.
 *
 * Asteroids may span several chunks and may bridge to a neighbour sector's
 * asteroid with a walkable neck; both endpoints derive identical connection
 * geometry from the sorted sector-pair hash, so the bridge is seamless.
 */
public final class AsteroidSectorGrid {

    private AsteroidSectorGrid() {}

    private static final int BAND_CENTRE_Y =
        (DSConstants.ASTEROID_BAND_Y_MIN + DSConstants.ASTEROID_BAND_Y_MAX) / 2;

    /** Raw (pre-connection) asteroid resolved from one sector. */
    private static final class Raw {
        final int cx, cy, cz, rx, ry, rz;
        final AsteroidShapeType shape;
        final AsteroidShapeType.Params params;
        final AsteroidDefinition def;
        final HostKey host;
        final long shapeSeed, oreSeed;
        Raw(int cx, int cy, int cz, int rx, int ry, int rz,
            AsteroidShapeType shape, AsteroidShapeType.Params params,
            AsteroidDefinition def, HostKey host, long shapeSeed, long oreSeed) {
            this.cx = cx; this.cy = cy; this.cz = cz;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.shape = shape; this.params = params;
            this.def = def; this.host = host;
            this.shapeSeed = shapeSeed; this.oreSeed = oreSeed;
        }
        int topY() { return cy + ry; }
        /** Highest solid surface Y for this shape (BUG_6 — flat tops sit low). */
        int effTopY() {
            if (shape == AsteroidShapeType.FLAT_TOP_PLATFORM)
                return cy + (int) Math.round(ry * params.flatTopNorm);
            return cy + ry;
        }
        int avgR() { return (int) Math.round((rx + rz) / 2.0); }
    }

    public static List<AsteroidCandidate> resolve(long seed, int cx, int cz) {
        int S = DSConstants.ASTEROID_SECTOR_SIZE_CHUNKS;
        int thisSx = Math.floorDiv(cx, S);
        int thisSz = Math.floorDiv(cz, S);
        int r = DSConstants.ASTEROID_SECTOR_SEARCH_RADIUS;

        List<AsteroidCandidate> out = new ArrayList<>();
        for (int sx = thisSx - r; sx <= thisSx + r; sx++) {
            for (int sz = thisSz - r; sz <= thisSz + r; sz++) {
                AsteroidCandidate c = resolveConnected(seed, sx, sz);
                if (c != null && c.reachesChunk(cx, cz)) out.add(c);
            }
        }
        return out;
    }

    private static AsteroidCandidate resolveConnected(long seed, int sx, int sz) {
        Raw a = rawSector(seed, sx, sz);
        if (a == null) return null;

        boolean connected = false;
        int linkX = 0, linkZ = 0, walkTopY = 0, walkBotY = 0, linkR = 0;
        int linkCy = 0, linkRx = 0, linkRy = 0, linkRz = 0;
        long linkSeed = 0L;
        int walkAtopY = 0, walkBtopY = 0, walkHeight = 0;
        AsteroidDefinition linkDef = null;
        HostKey linkHost = null;
        long linkOreSeed = 0L;

        outer:
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nsx = sx + dx, nsz = sz + dz;
                Raw b = rawSector(seed, nsx, nsz);
                if (b == null) continue;

                // BUG_1: only connect asteroids of the SAME primary block.
                if (!a.def.primaryBlock().equals(b.def.primaryBlock())) continue;

                double dXz = Math.hypot(a.cx - b.cx, a.cz - b.cz);
                double gap = dXz - a.rx - b.rx;
                if (gap > DSConstants.ASTEROID_CONNECT_MAX_GAP) continue;

                long ph = pairHash(seed, sx, sz, nsx, nsz);
                if ((int) (Math.abs(ph) % 100) < DSConstants.ASTEROID_CONNECT_CHANCE_PCT) {
                    connected = true;
                    linkX = b.cx; linkZ = b.cz;
                    linkCy = b.cy; linkRx = b.rx; linkRy = b.ry; linkRz = b.rz;
                    linkSeed = ph;
                    // BUG_16: ramp endpoints = each asteroid's real surface Y.
                    walkAtopY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 4,
                                Math.min(a.effTopY(),
                                         DSConstants.ASTEROID_BAND_Y_MAX - 1));
                    walkBtopY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 4,
                                Math.min(b.effTopY(),
                                         DSConstants.ASTEROID_BAND_Y_MAX - 1));
                    // BUG_15: walkway thickness = average of the two asteroids'
                    // vertical heights (height = 2*ry, so avg = a.ry + b.ry).
                    walkHeight = Math.max(3, a.ry + b.ry);
                    // Legacy band fields kept for AABB reach; ramp uses A/B tops.
                    walkTopY = Math.max(walkAtopY, walkBtopY);
                    walkBotY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + 1,
                               Math.min(Math.min(walkAtopY, walkBtopY) - walkHeight,
                                        walkTopY - 1));
                    // BUG_0: platform radius = average of the two asteroids' radii.
                    linkR = Math.max(2, (int) Math.round((a.avgR() + b.avgR()) / 2.0));
                    // BUG_13: neighbour ore identity for the mixed bridge vein.
                    linkDef = b.def; linkHost = b.host; linkOreSeed = b.oreSeed;
                    break outer;
                }
            }
        }

        return new AsteroidCandidate(a.cx, a.cy, a.cz, a.rx, a.ry, a.rz,
            a.shape, a.params, a.def, a.host, a.shapeSeed, a.oreSeed,
            connected, linkX, linkZ, walkTopY, linkR, walkBotY, linkCy,
            linkRx, linkRy, linkRz, linkSeed,
            walkAtopY, walkBtopY, walkHeight, linkDef, linkHost, linkOreSeed,
            false);
    }

    /** Symmetric per-pair hash (same for both endpoints, order-independent). */
    private static long pairHash(long seed, int ax, int az, int bx, int bz) {
        long k1 = (((long) ax) << 32) ^ (az & 0xffffffffL);
        long k2 = (((long) bx) << 32) ^ (bz & 0xffffffffL);
        long lo = Math.min(k1, k2), hi = Math.max(k1, k2);
        long h = seed ^ (lo * 0x9E3779B97F4A7C15L) ^ (hi * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        return h;
    }

    private static Raw rawSector(long seed, int sx, int sz) {
        long s = seed
            ^ ((long) sx * 0x9E3779B97F4A7C15L)
            ^ ((long) sz * 0x6C62272E07BB0142L);
        s ^= s >>> 30; s *= 0xbf58476d1ce4e5b9L;
        s ^= s >>> 27; s *= 0x94d049bb133111ebL;
        s ^= s >>> 31;

        if ((int) (Math.abs(s) % 100) >= DSConstants.ASTEROID_SECTOR_SPAWN_PCT)
            return null;

        long s2 = s ^ 0xDEADC0DEBEEFA1B2L;
        s2 ^= s2 >>> 30; s2 *= 0xbf58476d1ce4e5b9L; s2 ^= s2 >>> 27;

        AsteroidDefinition def = AsteroidRegistry.selectUniform(s);
        if (def == null) return null;

        int S = DSConstants.ASTEROID_SECTOR_SIZE_CHUNKS;
        int sectorBlocks = S * 16;
        int marginX = (int) (Math.abs(s2) % Math.max(1, sectorBlocks - 16)) + 8;
        int marginZ = (int) (Math.abs(s2 >> 12) % Math.max(1, sectorBlocks - 16)) + 8;
        int centerX = sx * sectorBlocks + marginX;
        int centerZ = sz * sectorBlocks + marginZ;

        int rMin = DSConstants.ASTEROID_RADIUS_MIN;
        int rMax = DSConstants.ASTEROID_RADIUS_MAX;
        int span = Math.max(1, rMax - rMin);
        int rx = rMin + (int) (Math.abs(s2 >> 20) % span);
        int rz = rMin + (int) (Math.abs(s2 >> 28) % span);
        int ry = Math.max(3, (int) ((rx + rz) * 0.5
            * (0.35 + ((Math.abs(s2 >> 36) % 100) / 100.0) * 0.85)));

        // BUG_2: flat platforms are now uncommon (~12%); size no longer forces them.
        AsteroidShapeType shape;
        AsteroidShapeType.Params params = new AsteroidShapeType.Params();
        int shapeRoll = (int) (Math.abs(s2 >> 44) % 100);
        if (shapeRoll < 40) {
            shape = AsteroidShapeType.ELLIPSOID;
        } else if (shapeRoll < 70) {
            shape = AsteroidShapeType.SPHERE;
            int rr = Math.max(rMin, (rx + rz) / 2);
            rx = rr; ry = rr; rz = rr;
        } else if (shapeRoll < 88) {
            shape = AsteroidShapeType.CRESCENT;
            params.biteOffX = 0.85 + ((Math.abs(s2 >> 50) % 40) / 100.0);
            params.biteR    = 0.80 + ((Math.abs(s2 >> 54) % 35) / 100.0);
        } else {
            shape = AsteroidShapeType.FLAT_TOP_PLATFORM;
            params.flatTopNorm = 0.05 + ((Math.abs(s2 >> 50) % 30) / 100.0);
        }

        int yVar = (int) (Math.abs(s2 >> 58) % 61) - 30;
        int centerY = Math.max(DSConstants.ASTEROID_BAND_Y_MIN + ry + 2,
                      Math.min(BAND_CENTRE_Y + yVar,
                               DSConstants.ASTEROID_BAND_Y_MAX - ry - 2));

        HostKey host = HostKey.fromBlock(def.primaryBlock());
        long shapeSeed = s ^ 0xDEADBEEFL;
        long oreSeed   = s ^ 0x5151A57E0DEFACEL;
        return new Raw(centerX, centerY, centerZ, rx, ry, rz,
            shape, params, def, host, shapeSeed, oreSeed);
    }
}
