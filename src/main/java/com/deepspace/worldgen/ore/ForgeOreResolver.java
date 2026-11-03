package com.deepspace.worldgen.ore;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Resolves the {@code #forge:ores} block tag (and {@code forge:ores/<material>}
 * subtags) into a host-matched ore lookup at runtime.
 *
 * For every ore block found in the tag we classify it as {@code (HostKey,
 * material, side)} where side is either the "vanilla" (non-GregTech) variant or
 * the GregTech variant. Asteroid / planet generation then asks for the variant
 * whose host matches the body's primary block, guaranteeing ores always match
 * the rock they sit in, while mixing vanilla + GTCEU ~1:1.
 *
 * Contract: built once after tags load ({@code TagsUpdatedEvent}); the result is
 * an immutable snapshot published through a volatile reference, so worldgen
 * threads read it lock-free (same model as AsteroidRegistry).
 */
public final class ForgeOreResolver {

    private ForgeOreResolver() {}

    /** Immutable resolved snapshot. */
    private static final class Data {
        final Map<HostKey, Map<String, Block>> vanilla;
        final Map<HostKey, Map<String, Block>> gt;
        final Map<HostKey, List<String>> materials;
        Data(Map<HostKey, Map<String, Block>> v,
             Map<HostKey, Map<String, Block>> g,
             Map<HostKey, List<String>> m) {
            this.vanilla = v; this.gt = g; this.materials = m;
        }
    }

    private static volatile Data DATA = null;

    public static boolean ready() { return DATA != null; }

    /**
     * (Re)build from the block registry's tags. Safe to call repeatedly
     * (TagsUpdatedEvent fires on load and on /reload).
     */
    public static void build(RegistryAccess ra) {
        try {
            Registry<Block> reg = ra.registryOrThrow(Registries.BLOCK);

            Map<HostKey, Map<String, Block>> vanilla = new EnumMap<>(HostKey.class);
            Map<HostKey, Map<String, Block>> gt      = new EnumMap<>(HostKey.class);
            for (HostKey h : HostKey.values()) {
                vanilla.put(h, new HashMap<>());
                gt.put(h, new HashMap<>());
            }

            reg.getTagNames()
               .filter(tk -> {
                   ResourceLocation l = tk.location();
                   return "forge".equals(l.getNamespace())
                       && (l.getPath().equals("ores") || l.getPath().startsWith("ores/"));
               })
               .forEach(tk -> reg.getTag(tk).ifPresent(set -> set.forEach(holder -> {
                   Block block = holder.value();
                   ResourceLocation rl = reg.getKey(block);
                   if (rl == null || block == Blocks.AIR) return;
                   classify(rl, block, vanilla, gt);
               })));

            Map<HostKey, List<String>> materials = new EnumMap<>(HostKey.class);
            int totalV = 0, totalG = 0;
            for (HostKey h : HostKey.values()) {
                Set<String> mats = new TreeSet<>();
                mats.addAll(vanilla.get(h).keySet());
                mats.addAll(gt.get(h).keySet());
                materials.put(h, List.copyOf(mats));
                totalV += vanilla.get(h).size();
                totalG += gt.get(h).size();
            }

            Map<HostKey, Map<String, Block>> fv = new EnumMap<>(HostKey.class);
            Map<HostKey, Map<String, Block>> fg = new EnumMap<>(HostKey.class);
            for (HostKey h : HostKey.values()) {
                fv.put(h, Map.copyOf(vanilla.get(h)));
                fg.put(h, Map.copyOf(gt.get(h)));
            }

            DATA = new Data(Collections.unmodifiableMap(fv),
                            Collections.unmodifiableMap(fg),
                            Collections.unmodifiableMap(materials));

            StringBuilder sb = new StringBuilder();
            for (HostKey h : HostKey.values())
                sb.append(' ').append(h).append('=').append(materials.get(h).size());
            DeepSpaceMod.LOGGER.info(
                "[ForgeOreResolver] Built: {} vanilla + {} gtceu ore variants; materials per host:{}",
                totalV, totalG, sb);

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[ForgeOreResolver] build failed", e);
        }
    }

    /** Lazy fallback so first-chunk worldgen never sees a null snapshot. */
    public static void ensureBuilt(RegistryAccess ra) {
        if (DATA == null) {
            synchronized (ForgeOreResolver.class) {
                if (DATA == null) build(ra);
            }
        }
    }

    /** Mods whose ores must never be used (BUG_4). */
    private static final Set<String> EXCLUDED_NS = Set.of(
        "ad_astra", "ad_astra_giselle_addon", "ad_extendra", "draconicevolution");

    /**
     * Register an ore only if its host is one of the 9 supported rock types
     * (stone, deepslate, granite, tuff, diorite, andesite, red_granite, marble,
     * basalt). Anything whose host cannot be resolved is excluded (BUG_5):
     * vanilla contributes only stone/deepslate hosts, gtceu only its known host
     * prefixes, and every other namespace is dropped entirely.
     */
    private static void classify(ResourceLocation rl, Block block,
                                 Map<HostKey, Map<String, Block>> vanilla,
                                 Map<HostKey, Map<String, Block>> gt) {
        String ns = rl.getNamespace();
        String p  = rl.getPath();

        if (EXCLUDED_NS.contains(ns)) return; // BUG_4

        if ("gtceu".equals(ns)) {
            for (HostKey h : HostKey.GT_PREFIXES) { // 9 supported hosts (incl. basalt)
                if (p.startsWith(h.gtPrefix)) {
                    String m = strip(p.substring(h.gtPrefix.length()));
                    if (!m.isEmpty()) gt.get(h).putIfAbsent(m, block);
                    return;
                }
            }
            return; // gtceu ore with an unknown host prefix -> exclude (BUG_5)
        }

        if ("minecraft".equals(ns)) {
            // Vanilla only has stone- and deepslate-hosted ore variants.
            if (p.startsWith("nether_") || p.equals("ancient_debris")) return;
            if (p.startsWith("deepslate_") && p.endsWith("_ore")) {
                String m = strip(p.substring("deepslate_".length()));
                if (!m.isEmpty()) vanilla.get(HostKey.DEEPSLATE).putIfAbsent(m, block);
                return;
            }
            if (p.endsWith("_ore")) {
                String m = strip(p);
                if (!m.isEmpty()) vanilla.get(HostKey.STONE).putIfAbsent(m, block);
            }
            return;
        }

        // BUG_5: any other namespace -> host undeterminable -> excluded.
    }

    private static String strip(String s) {
        return s.endsWith("_ore") ? s.substring(0, s.length() - 4) : s;
    }

    // -- Read API (lock-free) -------------------------------------------------

    public static List<String> materialsFor(HostKey host) {
        Data d = DATA;
        if (d == null || host == null) return List.of();
        return d.materials.getOrDefault(host, List.of());
    }

    public static Block vanilla(HostKey host, String material) {
        Data d = DATA;
        if (d == null || host == null) return null;
        Map<String, Block> m = d.vanilla.get(host);
        return m == null ? null : m.get(material);
    }

    public static Block gt(HostKey host, String material) {
        Data d = DATA;
        if (d == null || host == null) return null;
        Map<String, Block> m = d.gt.get(host);
        return m == null ? null : m.get(material);
    }

    /**
     * A material that has a GTCEU variant for this host (used to drive the GT
     * bedrock ore vein), or null if none — deterministic for a given seed.
     */
    public static String pickPrimaryGtMaterial(HostKey host, long seed) {
        Data d = DATA;
        if (d == null || host == null) return null;
        Map<String, Block> g = d.gt.get(host);
        if (g == null || g.isEmpty()) return null; // BUG_5: no wrong-host fallback
        List<String> keys = new ArrayList<>(g.keySet());
        Collections.sort(keys);
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        return keys.get((int) (Math.abs(h) % keys.size()));
    }
}
