package com.deepspace.registry;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * Immutable data object describing one planet archetype.
 * Parallels AsteroidDefinition — shared read-only across all worldgen threads.
 */
public final class PlanetDefinition {

    /** Unique string ID (e.g. "stone_planet", "ice_planet"). */
    public final String id;

    /** Logical shell category — used for ore selection logic. */
    public final String shellType; // "stone" | "deepslate" | "granite" | "dirt"
                                   // | "red_granite" | "ice" | "tar_shell"

    /** Primary outer-shell block (the visible surface). */
    public final ResourceLocation shellBlock;

    /**
     * Interior fill block (inside the shell layer).
     * For ice planets this is minecraft:water; for tar planets gtceu:heavy_oil.
     */
    public final ResourceLocation innerBlock;

    /**
     * Ores scattered throughout the interior volume.
     * Contextually matched to the shell type.
     */
    public final List<ResourceLocation> ores;

    /** Weighted selection probability (higher = more common). */
    public final int spawnWeight;

    public PlanetDefinition(String id, String shellType,
                             ResourceLocation shellBlock, ResourceLocation innerBlock,
                             List<ResourceLocation> ores, int spawnWeight) {
        this.id          = id;
        this.shellType   = shellType;
        this.shellBlock  = shellBlock;
        this.innerBlock  = innerBlock;
        this.ores        = List.copyOf(ores);
        this.spawnWeight = spawnWeight;
    }
}
