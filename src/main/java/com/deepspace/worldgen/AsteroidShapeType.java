package com.deepspace.worldgen;

import com.deepspace.DSConstants;

/**
 * Asteroid silhouette, evaluated in normalised ellipsoid space
 * ({@code n = (pos - centre) / radius}, so the unit ellipsoid is |n|² ≤ 1).
 *
 * All tests are pure functions of the normalised position, a surface-noise
 * warp term and a per-cell hash noise, so every chunk overlapping a multi-chunk
 * asteroid computes the exact same solid with no shared state.
 */
public enum AsteroidShapeType {
    /** Triaxial ellipsoid (legacy look). */
    ELLIPSOID,
    /** True sphere (sector grid assigns equal radii). */
    SPHERE,
    /** Rounded bottom; flat walkable top at {@code p.flatTopNorm} with
     *  CONNECTED rocky mounds rising from it (BUG_10 — never floating). */
    FLAT_TOP_PLATFORM,
    /** Ellipsoid with a horizontal spherical bite removed. */
    CRESCENT;

    /** Extra per-asteroid shape parameters (normalised units). */
    public static final class Params {
        /** Flat-top cut height in normalised Y (top is sliced at this plane). */
        public double flatTopNorm = 0.2;
        /** Probability (0-1) a cell just above the cut survives as outcrop;
         *  tapers to ~0 toward the top of the ellipsoid. */
        public double outcropChance = DSConstants.FLAT_TOP_OUTCROP_PCT / 100.0;
        /** Crescent bite centre offset (normalised X/Z) and radius. */
        public double biteOffX = 1.05, biteOffZ = 0.0, biteR = 0.95;
    }

    /**
     * @param noise deterministic COLUMN-coarse, y-independent hash in [0,1)
     *              (chunk-independent) — same value for every Y in an (x,z)
     *              column so FLAT_TOP mounds are vertically contiguous.
     */
    public boolean inside(double nx, double ny, double nz,
                          double warp, double noise, Params p) {
        double d = nx * nx + ny * ny + nz * nz - warp;
        if (d > 1.0) return false;

        switch (this) {
            case FLAT_TOP_PLATFORM:
                if (ny <= p.flatTopNorm) return true;          // solid platform body
                // BUG_10: above the flat plane keep a CONNECTED mound. The mound
                // top for this column is fixed (noise depends only on x,z), so
                // every Y from the plane up to it is solid — never a floater.
                double moundTop = p.flatTopNorm
                    + (1.0 - p.flatTopNorm) * p.outcropChance * noise;
                return ny <= moundTop;
            case CRESCENT: {
                double bx = nx - p.biteOffX;
                double bz = nz - p.biteOffZ;
                double bite = bx * bx + ny * ny + bz * bz;
                return bite > p.biteR * p.biteR;
            }
            case ELLIPSOID:
            case SPHERE:
            default:
                return true;
        }
    }
}
