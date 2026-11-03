package com.deepspace.worldgen;

import com.deepspace.DSConstants;
import com.deepspace.registry.PlanetDefinition;

/**
 * A resolved planet instance for one chunk-generation pass.
 * Created transiently inside PlanetSectorGrid.resolve() — never persisted.
 * All fields are final; safe to share across worldgen threads (read-only).
 */
public final class PlanetCandidate {

    public final int centerX;     // world block X of sphere centre
    public final int centerY;     // world block Y of sphere centre
    public final int centerZ;     // world block Z of sphere centre
    public final int radius;      // sphere radius in blocks
    public final int shellThick;  // shell thickness in blocks
    public final PlanetDefinition def;
    public final long seed;       // per-planet seed for ore scatter

    public PlanetCandidate(int centerX, int centerY, int centerZ,
                            int radius, int shellThick,
                            PlanetDefinition def, long seed) {
        this.centerX   = centerX;
        this.centerY   = centerY;
        this.centerZ   = centerZ;
        this.radius    = radius;
        this.shellThick = shellThick;
        this.def       = def;
        this.seed      = seed;
    }

    /**
     * Returns the void-clear-zone radius.
     * No asteroids spawn within this distance from the planet centre.
     */
    public double voidRadius() {
        return radius * DSConstants.PLANET_VOID_RADIUS_MULTIPLIER
               + DSConstants.PLANET_VOID_PADDING_BLOCKS;
    }
}
