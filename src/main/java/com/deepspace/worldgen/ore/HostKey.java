package com.deepspace.worldgen.ore;

import net.minecraft.resources.ResourceLocation;

/**
 * The rock "host" an ore variant is embedded in. Used to keep every scattered
 * ore visually matched to an asteroid / planet's primary block (e.g. a
 * deepslate asteroid only ever gets {@code minecraft:deepslate_*_ore} /
 * {@code gtceu:deepslate_*_ore}).
 *
 * Ordering of {@link #GT_PREFIXES} matches the authoritative list in
 * GTCEuCompat.extractPrimaryMaterial — {@code red_granite_} MUST be tested
 * before {@code granite_}, {@code deepslate_} before any other.
 */
public enum HostKey {
    STONE("stone_"),
    DEEPSLATE("deepslate_"),
    GRANITE("granite_"),
    DIORITE("diorite_"),
    ANDESITE("andesite_"),
    TUFF("tuff_"),
    MARBLE("marble_"),
    BASALT("basalt_"),
    RED_GRANITE("red_granite_");

    /** GTCEu ore-block path prefix for this host (e.g. {@code deepslate_}). */
    public final String gtPrefix;

    HostKey(String gtPrefix) {
        this.gtPrefix = gtPrefix;
    }

    /** Prefix scan order: longest / most specific first. */
    public static final HostKey[] GT_PREFIXES = {
        RED_GRANITE, DEEPSLATE, ANDESITE, GRANITE, DIORITE, MARBLE, BASALT, TUFF, STONE
    };

    /**
     * Map an asteroid / planet primary block id to its ore host, or
     * {@code null} when the host cannot be determined. Callers MUST treat a
     * null host as "no host-matched ores" (BUG_5) rather than guessing stone.
     */
    public static HostKey fromBlock(ResourceLocation primaryBlock) {
        if (primaryBlock == null) return null;
        String ns = primaryBlock.getNamespace();
        String p  = primaryBlock.getPath();

        if ("minecraft".equals(ns)) {
            switch (p) {
                case "stone":                return STONE;
                case "deepslate":            return DEEPSLATE;
                case "granite":              return GRANITE;
                case "diorite":              return DIORITE;
                case "andesite":             return ANDESITE;
                case "tuff":                 return TUFF;
                case "basalt":
                case "smooth_basalt":        return BASALT;
                default:                     return null;
            }
        }
        if ("gtceu".equals(ns)) {
            if (p.contains("red_granite")) return RED_GRANITE;
            if (p.contains("marble"))      return MARBLE;
            if (p.contains("basalt"))      return BASALT;
        }
        return null;
    }
}
