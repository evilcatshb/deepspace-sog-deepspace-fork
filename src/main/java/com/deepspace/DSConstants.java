package com.deepspace;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * All compile-time constants for Deep Space.
 * Values derived from bytecode analysis of dependency jars.
 */
public final class DSConstants {

    private DSConstants() {}

    // --- Mod identity --------------------------------------------------------
    public static final String MODID = DeepSpaceMod.MODID;

    // --- Dimension -----------------------------------------------------------
    public static final ResourceLocation DEEP_SPACE_RL = new ResourceLocation(MODID, "deep_space");
    public static final ResourceKey<Level> DEEP_SPACE_KEY =
        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, DEEP_SPACE_RL);

    // --- Worldgen geometry ---------------------------------------------------
    /** Bottom of asteroid band (Y). */
    public static final int ASTEROID_BAND_Y_MIN = 0;
    /** Top of asteroid band (Y). */
    public static final int ASTEROID_BAND_Y_MAX = 120;
    /** Guaranteed empty construction space starts here. */
    public static final int CONSTRUCTION_ZONE_Y = 160;
    /** Minimum blocks between asteroid centres. */
    public static final int MIN_ASTEROID_SPACING = 48;
    /** Nominal asteroid centre spacing (blocks). */
    public static final int AVG_ASTEROID_SPACING = 128;

    // --- Crash bubble --------------------------------------------------------
    /** Radius of personal crash bubble in chunks. */
    public static final int BUBBLE_RADIUS_CHUNKS = 15;
    /** Minimum chunk distance between two crash-site centres. */
    public static final int MIN_CRASH_SITE_SEPARATION_CHUNKS = 120;
    /** How far (blocks) to scan for guaranteed spawn resources. */
    public static final int SPAWN_GUARANTEE_SEARCH_RADIUS = 1200;

    // --- Zone distances (blocks from crash site) -----------------------------
    public static final int ZONE1_MIN =     300;
    public static final int ZONE1_MAX =    1500;
    public static final int ZONE2_MIN =    1500;
    public static final int ZONE2_MAX =    5000;
    public static final int ZONE3_MIN =    5000;
    public static final int ZONE3_MAX =   15000;
    public static final int ZONE4_MIN =   15000;

    // --- Ad Astra physics (from bytecode) ------------------------------------
    /** Gravity multiplier (Moon-like, from PlanetConstants). 1.622 m/s2 ~= 0.2 x Earth */
    public static final float DEEP_SPACE_GRAVITY = 1.622f;
    /** Temperature of deep space in degrees C (extreme cold). */
    public static final short DEEP_SPACE_TEMPERATURE = -200;
    /** Solar power units (no sun). */
    public static final int DEEP_SPACE_SOLAR_POWER = 0;

    // --- Water->Ice vacuum conversion -----------------------------------------
    /** Ticks before exposed water freezes in vacuum. */
    public static final int VACUUM_FREEZE_TICKS = 5;

    // --- Breeder reactor (ZPM-tier) ------------------------------------------
    /** Burn time in ticks for one uranium ingot. 1728000 t = 24 h real time. */
    public static final long BREEDER_BURN_TICKS = 1728000L;
    /** Starter uranium ingots in crash ship. Covers ~2 weeks. */
    public static final int BREEDER_STARTER_INGOTS = 14;

    // --- NBT save data keys --------------------------------------------------
    public static final String NBT_BUBBLES_FILE   = "deepspace_bubbles";
    public static final String NBT_CRASH_SITES    = "deepspace_crash_sites";
    public static final String NBT_RESERVED_REGIONS = "deepspace_reserved";

    // --- Structure path ------------------------------------------------------
    public static final ResourceLocation CRASHED_SHIP_STRUCTURE =
        new ResourceLocation(MODID, "crashed_ship");

    // --- Celestial Bodies (Planetary Spheres) --------------------------------

    /** Grid size in chunks for planet sector lookup. One planet can spawn per sector.
     *  16 chunks @ 25% chance => ~1:4 odds of a celestial sphere per 16-chunk grid. */
    public static final int PLANET_SECTOR_SIZE_CHUNKS = 16;   // 256 blocks

    /** Percentage chance (0-100) that a sector contains a planet. */
    public static final int PLANET_SPAWN_CHANCE_PCT = 25;

    /** Minimum planet radius in blocks (2 chunks = 32 blocks diameter). */
    public static final int PLANET_RADIUS_MIN = 16;

    /** Maximum planet radius in blocks (10 chunks = 160 blocks diameter). */
    public static final int PLANET_RADIUS_MAX = 80;

    /** Shell thickness in blocks (inner face to outer surface). */
    public static final int PLANET_SHELL_THICKNESS_MIN = 8;
    public static final int PLANET_SHELL_THICKNESS_MAX = 16;

    /**
     * The void clear-zone radius = planetRadius * this multiplier + PLANET_VOID_PADDING_BLOCKS.
     * Asteroid generation is suppressed within this radius.
     */
    public static final double PLANET_VOID_RADIUS_MULTIPLIER = 1.5;
    public static final int    PLANET_VOID_PADDING_BLOCKS    = 32;

    /**
     * How many neighbouring sectors to check when resolving planet influence.
     * Must be >= ceil(PLANET_RADIUS_MAX / PLANET_SECTOR_SIZE_CHUNKS) + 1.
     * With max radius 80 / sector 512 this is 1 — but use 2 for safety margin.
     */
    public static final int PLANET_SECTOR_SEARCH_RADIUS = 2;

    /** Number of ore scatter attempts per planet (much higher than asteroid). */
    public static final int PLANET_ORE_SCATTER_COUNT = 2000;

    // --- Random Structures ---------------------------------------------------

    /**
     * Grid size in chunks for structure sector lookup.
     * 96 chunks = 1536 blocks. Combined with 30% chance gives ~10x rarer than planets.
     */
    public static final int STRUCTURE_SECTOR_SIZE_CHUNKS = 96;

    /** Percentage chance (0-100) that a sector contains a structure. */
    public static final int STRUCTURE_SPAWN_CHANCE_PCT = (int)(PLANET_SPAWN_CHANCE_PCT * 0.75); // 75% as likely as planets

    /**
     * How many neighbouring sectors to check when resolving structure influence.
     * Larger structures need a bigger search radius.
     */
    public static final int STRUCTURE_SECTOR_SEARCH_RADIUS = 3;

    /**
     * Distance in blocks from a celestial body centre within which structures
     * have doubled spawn chance.
     */
    public static final int STRUCTURE_CELESTIAL_BIAS_RADIUS_BLOCKS = 256;

    /**
     * Minimum void padding in blocks around any structure bounding box.
     * Added on top of the radius-multiplier calculation.
     */
    public static final int STRUCTURE_VOID_PADDING_BLOCKS = 32;

    /**
     * Void radius = max(sizeX, sizeY, sizeZ) * multiplier + STRUCTURE_VOID_PADDING_BLOCKS.
     */
    public static final double STRUCTURE_VOID_RADIUS_MULTIPLIER = 1.5;

    // --- Asteroid sector grid (replaces per-chunk zone placement) ------------

    /** Grid size in chunks for asteroid sector lookup. 3 chunks = 48 blocks. */
    public static final int ASTEROID_SECTOR_SIZE_CHUNKS = 3;

    /** Percentage chance (0-100) that a sector contains an asteroid. (~25% denser than 70.) */
    public static final int ASTEROID_SECTOR_SPAWN_PCT = 88;

    /** Probability (0-100) a near-surface asteroid/walkway cell is eroded away
     *  so no shape is perfectly smooth. Deep interior/core is never eroded. */
    public static final int ASTEROID_EROSION_PCT = 6;

    /** Probability (0-100) an above-the-flat-plane cell survives as a rocky
     *  outcrop on FLAT_TOP_PLATFORM asteroids (sparser with height). */
    public static final int FLAT_TOP_OUTCROP_PCT = 28;

    /**
     * Neighbour sectors scanned when resolving asteroid influence.
     * Must cover the largest asteroid reach plus connection necks:
     * ceil((RADIUS_MAX + warp + bridge) / (SECTOR_SIZE*16)) + 1.
     */
    public static final int ASTEROID_SECTOR_SEARCH_RADIUS = 2;

    /** Minimum / maximum asteroid radius in blocks. 16 -> ~32-block (2x2 chunk) bodies. */
    public static final int ASTEROID_RADIUS_MIN = 6;
    public static final int ASTEROID_RADIUS_MAX = 16;

    /**
     * Ore cells scattered per asteroid = inner ellipsoid volume * this multiplier,
     * split ~1:1 vanilla:GTCEU. Tuned so a max-size asteroid yields the same
     * ~2k-8k ore cells the vanilla-present design intends today; larger asteroids
     * therefore scale ore at a constant ore:rock ratio.
     */
    public static final double ASTEROID_ORE_VOLUME_MULT = 1.6;

    /** Probability (0-100) that two neighbouring asteroids connect with a walkable neck. */
    public static final int ASTEROID_CONNECT_CHANCE_PCT = 30;

    /** Max surface-to-surface gap (blocks) for a connection to be considered. */
    public static final int ASTEROID_CONNECT_MAX_GAP = 24;

    /**
     * Extra normalised-distance margin added to the walkway's ellipsoid clip so
     * the bridge never enters an asteroid's MAX possible solid envelope. Must be
     * >= the surface-warp amplitude (0.18) plus erosion slack (BUG_3).
     */
    public static final double WALKWAY_WARP_MARGIN = 0.22;

    /** Chance (0-100) an asteroid has spherical crater(s) dished into its top. */
    public static final int ASTEROID_TOP_CRATER_CHANCE_PCT = 35;
    /** Max top-face craters on an asteroid that rolls one (BUG_11). */
    public static final int ASTEROID_TOP_CRATER_MAX = 2;

    /** Probability (0-100) an interior walkway cell is an ore from the two
     *  connected asteroids' mixed ore set (BUG_13). */
    public static final int WALKWAY_ORE_PCT = 6;

    // --- Planet ore core / vein-shell upgrade --------------------------------

    /**
     * Planet ore core radius = radius - randomInset, where randomInset is drawn
     * per-planet from [MIN, MAX] (so the core sits a random depth below the
     * surface). The band from just under the surface down to the core boundary
     * is the high-density vein shell.
     */
    public static final int PLANET_CORE_INSET_MIN = 6;
    public static final int PLANET_CORE_INSET_MAX = 28;

    /** Outermost blocks kept as clean shell surface (veins start just below). */
    public static final int PLANET_SURFACE_SKIN_BLOCKS = 2;

    /** Fraction (0-1) of core cells replaced with host-matched ore (dense solid core). */
    public static final double PLANET_CORE_ORE_FILL = 0.5;

    /** Vein-shell blob count and radius -- larger / denser than asteroid scatter.
     *  BUG_14: ~10x more veins than before (260 -> 2600). */
    public static final int PLANET_SHELL_VEIN_COUNT = 2600;
    public static final int PLANET_SHELL_VEIN_SIZE  = 5;
}
