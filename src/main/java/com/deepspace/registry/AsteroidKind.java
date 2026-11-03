package com.deepspace.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Behavioural class of an asteroid archetype, resolved once at registry load.
 *
 * Special kinds keep their bespoke shell / fluid / scatter exactly as before.
 * {@link #ROCK} (the ~310 rock-shell archetypes) instead ignores the JSON ore
 * list and is filled by the host-matched {@code #forge:ores} resolver.
 */
public enum AsteroidKind {
    /** Lava-filled interior. */
    LAVA,
    /** deepspace:tar_shell crust with an oil fluid interior. */
    OIL_TAR,
    /** Helium / hydrogen / methane / nitrogen (and any other) fluid asteroid. */
    FLUID_GAS,
    /** Ice / packed-ice / water bodies (fossils + bone scatter). */
    WATER_ICE,
    /** Solid minecraft:coarse_dirt body. */
    DIRT,
    /** Bespoke alien shell (deepspace:alien_stone / ae2:sky_stone) -- keeps its own ores. */
    ALIEN,
    /** Generic rock asteroid -> host-matched #forge:ores. */
    ROCK;

    private static boolean any(List<ResourceLocation> l, String contains) {
        if (l == null) return false;
        for (ResourceLocation rl : l) if (rl.toString().contains(contains)) return true;
        return false;
    }

    private static boolean eq(List<ResourceLocation> l, String id) {
        if (l == null) return false;
        for (ResourceLocation rl : l) if (rl.toString().equals(id)) return true;
        return false;
    }

    public static AsteroidKind classify(String category,
                                        List<ResourceLocation> outerShell,
                                        List<ResourceLocation> innerBlocks,
                                        ResourceLocation fluid) {
        String fluidPath = fluid != null ? fluid.getPath() : "";

        if (any(outerShell, "tar_shell"))                       return OIL_TAR;
        if (fluidPath.contains("lava") || any(innerBlocks, "lava")) return LAVA;

        boolean iceShell = eq(outerShell, "minecraft:ice")
                || eq(outerShell, "minecraft:packed_ice")
                || eq(outerShell, "minecraft:blue_ice");
        if ("icy".equals(category) || iceShell
                || fluidPath.equals("water") || fluidPath.equals("salt_water"))
            return WATER_ICE;

        if (eq(outerShell, "minecraft:coarse_dirt")
                || eq(innerBlocks, "minecraft:coarse_dirt"))
            return DIRT;

        if (fluid != null) return FLUID_GAS;

        if (any(outerShell, "alien_stone") || any(outerShell, "sky_stone")
                || any(innerBlocks, "alien_stone")) return ALIEN;

        return ROCK;
    }
}
