package com.deepspace.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Immutable data object representing one asteroid archetype from deepspace-registry.json.
 * Created once at load time, shared read-only across all threads.
 */
public final class AsteroidDefinition {

    public final String id;
    public final String zoneId;
    public final int spawnWeight;
    public final String category;

    /** Block IDs for the outer shell layer. */
    public final List<ResourceLocation> outerShell;
    /** Block IDs for the inner fill. */
    public final List<ResourceLocation> innerBlocks;
    /** Ore block IDs distributed inside the asteroid. */
    public final List<ResourceLocation> ores;

    /** Fluid ID for fluid-bearing asteroids, or null. */
    public final ResourceLocation fluid;
    /** Whether this asteroid has a GT fluid vein drilled below it. */
    public final boolean hasDrillingVein;
    /** Average fluid volume in mB for fluid asteroids. */
    public final long avgFluidAmountMb;

    /**
     * When true, the lower half of the asteroid (y < centreY) is filled with
     * innerBlocks[0] and the upper half (y >= centreY) is filled with
     * innerBlocks[1].  Used for stony asteroids where gravel must always sit
     * on top of stone so gravity does not cause it to fall into the void.
     */
    public final boolean gravityLayered;

    /** Behavioural class (rock vs lava/oil/gas/ice/dirt), resolved at load. */
    public final AsteroidKind kind;

    public AsteroidDefinition(
            String id,
            String zoneId,
            int spawnWeight,
            String category,
            List<ResourceLocation> outerShell,
            List<ResourceLocation> innerBlocks,
            List<ResourceLocation> ores,
            ResourceLocation fluid,
            boolean hasDrillingVein,
            long avgFluidAmountMb,
            boolean gravityLayered) {
        this.id = id;
        this.zoneId = zoneId;
        this.spawnWeight = spawnWeight;
        this.category = category;
        this.outerShell = List.copyOf(outerShell);
        this.innerBlocks = List.copyOf(innerBlocks);
        this.ores = List.copyOf(ores);
        this.fluid = fluid;
        this.hasDrillingVein = hasDrillingVein;
        this.avgFluidAmountMb = avgFluidAmountMb;
        this.gravityLayered = gravityLayered;
        this.kind = AsteroidKind.classify(category, this.outerShell, this.innerBlocks, fluid);
    }

    public boolean isFluidAsteroid() {
        return fluid != null;
    }

    /**
     * The block whose rock type the scattered ores must visually match.
     * Prefer the inner fill (what you mine through); fall back to the shell.
     */
    public ResourceLocation primaryBlock() {
        if (!innerBlocks.isEmpty()) return innerBlocks.get(0);
        if (!outerShell.isEmpty())  return outerShell.get(0);
        return new ResourceLocation("minecraft", "stone");
    }

    @Override
    public String toString() {
        return "Asteroid{" + id + ", zone=" + zoneId + ", weight=" + spawnWeight + "}";
    }
}
