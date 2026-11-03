package com.deepspace.registry;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/**
 * Hardcoded registry of the 7 planet archetypes (one per shell type).
 * Initialised once during FMLCommonSetupEvent alongside AsteroidRegistry.load().
 *
 * Ice planets:     packed_ice shell, minecraft:water interior.
 * Tar planets:     tar_shell shell,  gtceu:heavy_oil interior.
 * All others:      shell block == inner block (solid rock throughout).
 */
public final class PlanetRegistry {

    private static List<PlanetDefinition> planets = List.of();
    private static int totalWeight = 0;

    private PlanetRegistry() {}

    public static void init() {
        List<PlanetDefinition> list = new ArrayList<>();

        // ---- STONE PLANET --------------------------------------------------
        list.add(new PlanetDefinition(
            "stone_planet", "stone",
            rl("minecraft:stone"), rl("minecraft:stone"),
            ores("gtceu:stone_iron_ore", "gtceu:stone_copper_ore", "gtceu:stone_gold_ore",
                 "gtceu:stone_coal_ore", "gtceu:stone_redstone_ore", "gtceu:stone_lapis_ore",
                 "gtceu:stone_diamond_ore", "gtceu:stone_emerald_ore",
                 "gtceu:stone_tin_ore", "gtceu:stone_nickel_ore", "gtceu:stone_zinc_ore",
                 "gtceu:stone_magnetite_ore", "gtceu:stone_ilmenite_ore"),
            25));

        // ---- DEEPSLATE PLANET ----------------------------------------------
        list.add(new PlanetDefinition(
            "deepslate_planet", "deepslate",
            rl("minecraft:deepslate"), rl("minecraft:deepslate"),
            ores("gtceu:deepslate_iron_ore", "gtceu:deepslate_copper_ore",
                 "gtceu:deepslate_gold_ore", "gtceu:deepslate_coal_ore",
                 "gtceu:deepslate_redstone_ore", "gtceu:deepslate_lapis_ore",
                 "gtceu:deepslate_diamond_ore", "gtceu:deepslate_emerald_ore",
                 "gtceu:deepslate_tin_ore", "gtceu:deepslate_nickel_ore",
                 "gtceu:deepslate_zinc_ore", "gtceu:deepslate_magnetite_ore"),
            25));

        // ---- GRANITE PLANET ------------------------------------------------
        list.add(new PlanetDefinition(
            "granite_planet", "granite",
            rl("minecraft:granite"), rl("minecraft:granite"),
            ores("gtceu:granite_monazite_ore", "gtceu:granite_neodymium_ore",
                 "gtceu:granite_bastnasite_ore", "gtceu:granite_thorium_ore",
                 "gtceu:granite_ilmenite_ore", "gtceu:granite_diamond_ore",
                 "gtceu:granite_platinum_ore", "gtceu:granite_iron_ore",
                 "gtceu:granite_gold_ore", "gtceu:granite_aluminium_ore",
                 "gtceu:granite_molybdenum_ore", "gtceu:granite_lithium_ore"),
            20));

        // ---- DIRT PLANET ---------------------------------------------------
        list.add(new PlanetDefinition(
            "dirt_planet", "dirt",
            rl("minecraft:coarse_dirt"), rl("minecraft:dirt"),
            ores("minecraft:coal_ore", "deepspace:fossil", "minecraft:bone_block",
                 "gtceu:stone_coal_ore"),
            15));

        // ---- RED GRANITE PLANET --------------------------------------------
        list.add(new PlanetDefinition(
            "red_granite_planet", "red_granite",
            rl("gtceu:red_granite"), rl("gtceu:red_granite"),
            ores("gtceu:red_granite_iron_ore", "gtceu:red_granite_copper_ore",
                 "gtceu:red_granite_gold_ore", "gtceu:red_granite_monazite_ore",
                 "gtceu:red_granite_neodymium_ore", "gtceu:red_granite_bastnasite_ore",
                 "gtceu:red_granite_thorium_ore", "gtceu:red_granite_ilmenite_ore",
                 "gtceu:red_granite_naquadah_ore", "gtceu:red_granite_uraninite_ore",
                 "gtceu:red_granite_iridium_ore", "gtceu:red_granite_osmium_ore"),
            20));

        // ---- ICE PLANET ----------------------------------------------------
        // Outer shell of packed_ice; hollow interior filled with water.
        // Ores scatter only within the shell layer itself (innerR = 0 for ore pass
        // is avoided — ores still scatter inside the shell volume via placeOres).
        list.add(new PlanetDefinition(
            "ice_planet", "ice",
            rl("minecraft:packed_ice"), rl("minecraft:water"),
            ores("deepspace:fossil", "minecraft:bone_block", "deepspace:exotic_ice"),
            10));

        // ---- TAR SHELL PLANET ----------------------------------------------
        // Outer crust of tar_shell; hollow interior filled with heavy oil fluid blocks.
        list.add(new PlanetDefinition(
            "tar_shell_planet", "tar_shell",
            rl("deepspace:tar_shell"), rl("gtceu:heavy_oil"),
            ores("gtceu:stone_coal_ore"),
            5));

        planets     = Collections.unmodifiableList(list);
        totalWeight = list.stream().mapToInt(p -> p.spawnWeight).sum();
    }

    /** Weighted-random selection seeded by sector seed. Deterministic. */
    public static PlanetDefinition select(long seed) {
        if (planets.isEmpty()) return null;
        long rng = seed ^ 0xDEADBEEFCAFEBABEL;
        rng ^= rng >>> 33; rng *= 0xff51afd7ed558ccdL;
        rng ^= rng >>> 33;
        int roll = (int)(Math.abs(rng) % totalWeight);
        int cumulative = 0;
        for (PlanetDefinition p : planets) {
            cumulative += p.spawnWeight;
            if (roll < cumulative) return p;
        }
        return planets.get(planets.size() - 1);
    }

    /** Total number of registered archetypes (useful for logging). */
    public static int count() { return planets.size(); }

    private static ResourceLocation rl(String s) { return new ResourceLocation(s); }

    private static List<ResourceLocation> ores(String... ids) {
        List<ResourceLocation> l = new ArrayList<>();
        for (String s : ids) l.add(new ResourceLocation(s));
        return l;
    }
}
