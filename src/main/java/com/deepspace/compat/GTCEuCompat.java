package com.deepspace.compat;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.registry.AsteroidDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import com.deepspace.worldgen.OreVeinRegenSystem;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * GregTech CEu Modern 7.5.x – bedrock fluid AND ore vein injection.
 *
 * ── FLUID VEIN PIPELINE ──────────────────────────────────────────────────
 *
 *  1. DATA-DRIVEN DEFINITIONS (primary)
 *     data/gtceu/fluid_veins/deepspace_*.json are loaded by GTCEu's
 *     BedrockFluidLoader at startup.  dimension_filter: ["deepspace:deep_space"]
 *     ensures they only appear in Deep Space.
 *
 *  2. DIRECT INJECTION (this class, placeFluidVein)
 *     On asteroid placement, FluidVeinWorldEntry entries are written directly
 *     into BedrockFluidVeinSavedData.veinFluids keyed by actual chunk coords
 *     (17×17 chunk area).  This guarantees the correct fluid at the asteroid.
 *
 * ── ORE VEIN PIPELINE ────────────────────────────────────────────────────
 *
 *  1. KUBEJS DEFINITIONS (primary)
 *     kubejs/server_scripts/ores/deepspace_bedrock_ore_veins.js registers all
 *     50 ore families via GTCEuServerEvents.bedrockOreVeins, which fires inside
 *     BedrockOreLoader.apply() — the officially supported GTCEu 7.5.x approach.
 *     Veins are keyed as "deepspace:deepspace_<material>" (DSConstants.MODID
 *     namespace), which is what findOreDefinition() constructs for lookup.
 *     REQUIRED CONFIG: config/gtceu/gtceu-common.toml [machines] doBedrockOres=true
 *
 *  2. DIRECT INJECTION (this class, placeOreVein)
 *     On asteroid placement, an OreVeinWorldEntry is written into
 *     BedrockOreVeinSavedData.veinOres keyed by the GT grid coordinate:
 *       gridX = Math.floorDiv(asteroidChunkX, bedrockOreDistance)
 *       gridZ = Math.floorDiv(asteroidChunkZ, bedrockOreDistance)
 *     This matches exactly how GT's own getOreVeinWorldEntry looks up entries,
 *     so the prospector (SPacketProspectBedrockOre) finds the vein correctly.
 *
 *  3. YIELD LIMITS
 *     oreYield=26–100 (seeded random per position) × MAX_OPERATIONS=160
 *     = 4,160–16,000 items total per vein.
 *     depleted_yield=0 means the vein truly empties.
 *
 *  4. REGENERATION (OreVeinRegenSystem)
 *     Every 6000 ticks (~5 min), operationsRemaining is restored by +6,
 *     capped at MAX_OPERATIONS.  Full regen from 0 takes ~100 min.
 *     The oreYield (items/op) and material composition never change on regen.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 *     applyBiomeDecoration runs on worldgen threads; SavedData is not safe
 *     there.  Both placeFluidVein and placeOreVein are deferred via
 *     sl.getServer().execute().  SpawnGuaranteeSystem runs on main thread
 *     and calls both methods directly.
 *
 * ── PERSISTENCE ──────────────────────────────────────────────────────────
 *     OreVeinWorldEntry serialises its definition by ResourceLocation key.
 *     The KubeJS bedrockOreVeins script ensures each key is always resolved
 *     on world reload (GTCEuServerEvents fires during every server resource load).
 */
public final class GTCEuCompat {

    private GTCEuCompat() {}

    private static final boolean GTCEU_LOADED = ModList.get().isLoaded("gtceu");

    // 7×7 chunk area around each asteroid centre
    // 8 → 17×17 = 289 chunks → ~272 block radius (~80% prospector hit rate)
    private static final int INJECT_RADIUS_CHUNKS = 8;

    private static final int DEFAULT_OPS_REMAINING = 100_000;

    // Yield variation ±40% seeded by position
    private static final double YIELD_VARIANCE = 0.40;

    // ---- Bedrock Ore Vein constants ----------------------------------------

    /** Minimum items-per-operation (gives 26×160 = 4,160 total ores). */
    private static final int ORE_YIELD_MIN = 26;
    /** Maximum items-per-operation (gives 100×160 = 16,000 total ores). */
    private static final int ORE_YIELD_MAX = 100;

    /** Default bedrockOreDistance grid size; read from config at runtime if possible. */
    /** Grid size 1 = per-chunk keys; requires bedrockOreDistance=1 in gtceu-common.toml. */
    private static final int DEFAULT_ORE_GRID_SIZE = 1;

    /**
     * Cache: primary-material-name → registered BedrockOreDefinition object.
     * Populated lazily; ensures we don't re-register on every asteroid placement.
     */
    private static final ConcurrentHashMap<String, Object> ORE_DEF_CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------

    public static void placeFluidVein(ServerLevel level, AsteroidDefinition asteroidDef,
                                       int centreX, int centreZ) {
        if (!GTCEU_LOADED) return;
        if (!asteroidDef.hasDrillingVein || asteroidDef.fluid == null) return;

        ResourceLocation fluidRl = asteroidDef.fluid;
        Fluid targetFluid = ForgeRegistries.FLUIDS.getValue(fluidRl);
        if (targetFluid == null || targetFluid == Fluids.EMPTY) {
            DeepSpaceMod.LOGGER.warn(
                "[GTCEuCompat] Unknown fluid '{}' for asteroid '{}' -- skipping",
                fluidRl, asteroidDef.id);
            return;
        }

        int baseYield = computeSemiRandomYield(asteroidDef, centreX, centreZ);
        int centreCx = centreX >> 4;
        int centreCz = centreZ >> 4;

        try {
            Class<?> savedDataClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData");
            Method getOrCreate = savedDataClass.getMethod("getOrCreate", ServerLevel.class);
            Object savedData = getOrCreate.invoke(null, level);

            if (savedData == null) {
                DeepSpaceMod.LOGGER.warn(
                    "[GTCEuCompat] BedrockFluidVeinSavedData.getOrCreate returned null for fluid '{}' -- " +
                    "ensure this is called on the main server thread!", fluidRl);
                return;
            }

            Field veinFluidsField = savedDataClass.getField("veinFluids");
            @SuppressWarnings("unchecked")
            HashMap<Object, Object> veinFluids =
                (HashMap<Object, Object>) veinFluidsField.get(savedData);

            // Find the data-driven definition (loaded from data/gtceu/fluid_veins/deepspace_*.json)
            Object def = findDefinitionForFluid(targetFluid, fluidRl);
            if (def == null) {
                DeepSpaceMod.LOGGER.warn(
                    "[GTCEuCompat] No BedrockFluidDefinition found for fluid '{}' " +
                    "(data/gtceu/fluid_veins/deepspace_{}.json may not have loaded yet) -- " +
                    "creating ad-hoc fallback",
                    fluidRl, fluidRl.getPath());
                def = createAdHocDefinition(targetFluid, asteroidDef, fluidRl);
                if (def == null) return;
            }

            String veinIdStr = getVeinId(def);
            DeepSpaceMod.LOGGER.debug(
                "[GTCEuCompat] Using definition '{}' for fluid '{}'", veinIdStr, fluidRl);

            // Inject into ACTUAL chunk coords in a 7×7 area
            int injected = 0, skipped = 0;
            for (int dx = -INJECT_RADIUS_CHUNKS; dx <= INJECT_RADIUS_CHUNKS; dx++) {
                for (int dz = -INJECT_RADIUS_CHUNKS; dz <= INJECT_RADIUS_CHUNKS; dz++) {
                    ChunkPos key = new ChunkPos(centreCx + dx, centreCz + dz);
                    if (veinFluids.containsKey(key)) {
                        skipped++;
                        continue;
                    }
                    int chunkYield = varyYield(baseYield,
                        (centreCx + dx) * 31 + (centreCz + dz));
                    Object entry = newFluidVeinWorldEntry(def, chunkYield,
                        DEFAULT_OPS_REMAINING);
                    if (entry == null) continue;
                    setVeinId(entry, veinIdStr);
                    veinFluids.put(key, entry);
                    injected++;
                }
            }

            try { savedDataClass.getMethod("setDirty").invoke(savedData); }
            catch (Exception ignored) {}

            int total = (2 * INJECT_RADIUS_CHUNKS + 1) * (2 * INJECT_RADIUS_CHUNKS + 1);
            DeepSpaceMod.LOGGER.info(
                "[GTCEuCompat] Fluid vein '{}' (fluid={} yield={}) injected={} skipped={} total={} " +
                "centreChunk=[{},{}]",
                veinIdStr, fluidRl, baseYield, injected, skipped, total, centreCx, centreCz);

        } catch (ClassNotFoundException e) {
            DeepSpaceMod.LOGGER.debug("[GTCEuCompat] BedrockFluidVeinSavedData not found");
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn(
                "[GTCEuCompat] Injection failed for fluid '{}': {} ({})",
                fluidRl, e.getClass().getSimpleName(), e.getMessage());
            DeepSpaceMod.LOGGER.debug("[GTCEuCompat] Stack:", e);
        }
    }

    // -------------------------------------------------------------------------
    // Yield math
    // -------------------------------------------------------------------------

    private static int computeSemiRandomYield(AsteroidDefinition def, int cx, int cz) {
        long avg = def.avgFluidAmountMb;
        double base = Math.min(2000.0, Math.max(50.0, avg / 1000.0));
        long seed = (long) cx * 0x6C62272E07BB0142L ^ (long) cz * 0x94D049BB133111EBL
                    ^ avg * 0x1234567890ABCDEFL;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        double rnd = ((seed >>> 11) & 0xFFFFF) / (double) 0xFFFFF;
        double factor = 1.0 - YIELD_VARIANCE + rnd * (2.0 * YIELD_VARIANCE);
        return (int) Math.round(base * factor);
    }

    private static int varyYield(int base, int salt) {
        long seed = (long) base * 0x9E3779B97F4A7C15L ^ (long) salt * 0x6C62272E07BB0142L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        double r = ((seed >>> 11) & 0xFFFFF) / (double) 0xFFFFF;
        return Math.max(10, (int) Math.round(base * (0.95 + r * 0.10)));
    }

    // -------------------------------------------------------------------------
    // Definition lookup
    // -------------------------------------------------------------------------

    /** Search GTRegistries for a definition whose storedFluid matches targetFluid. */
    private static Object findDefinitionForFluid(Fluid targetFluid,
                                                   ResourceLocation fluidRl) {
        try {
            Class<?> registriesClass = Class.forName(
                "com.gregtechceu.gtceu.api.registry.GTRegistries");
            Field bedrockField = registriesClass.getField("BEDROCK_FLUID_DEFINITIONS");
            Object registry = bedrockField.get(null);

            Class<?> defClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition");
            Method getStoredFluid = defClass.getMethod("getStoredFluid");

            @SuppressWarnings("unchecked")
            java.util.Set<Object> defs =
                (java.util.Set<Object>) registry.getClass()
                    .getMethod("values").invoke(registry);

            for (Object def : defs) {
                @SuppressWarnings("unchecked")
                Supplier<Fluid> supplier = (Supplier<Fluid>) getStoredFluid.invoke(def);
                if (supplier == null) continue;
                try {
                    Fluid f = supplier.get();
                    if (f != null && f.equals(targetFluid)) {
                        Object key = registry.getClass()
                            .getMethod("getKey", Object.class).invoke(registry, def);
                        DeepSpaceMod.LOGGER.debug(
                            "[GTCEuCompat] Found definition key='{}' for fluid '{}'",
                            key, fluidRl);
                        return def;
                    }
                } catch (Exception ignored) {}
            }

            DeepSpaceMod.LOGGER.debug(
                "[GTCEuCompat] No definition found for fluid '{}' in {} registered defs",
                fluidRl, defs.size());
            return null;
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[GTCEuCompat] findDefinitionForFluid error: {}",
                e.getMessage());
            return null;
        }
    }

    /** Fallback: create and register an ad-hoc definition if JSON wasn't loaded. */
    private static Object createAdHocDefinition(Fluid targetFluid,
                                                  AsteroidDefinition asteroidDef,
                                                  ResourceLocation fluidRl) {
        try {
            ResourceLocation id = new ResourceLocation(DSConstants.MODID,
                "deepspace_" + fluidRl.getNamespace() + "_" + fluidRl.getPath());

            Class<?> defClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition");
            Object builder = defClass.getMethod("builder", ResourceLocation.class)
                                     .invoke(null, id);

            Supplier<Fluid> fluidSupplier = () -> targetFluid;
            builderCall(builder, "fluid",         new Class[]{Supplier.class}, fluidSupplier);
            builderCall(builder, "weight",         new Class[]{int.class}, 100);

            long avg = asteroidDef.avgFluidAmountMb;
            int minY = (int) Math.max(10, avg / 5000);
            int maxY = (int) Math.min(2000, avg / 500);
            builderCall(builder, "yield",          new Class[]{int.class, int.class}, minY, maxY);
            builderCall(builder, "depletionAmount", new Class[]{int.class}, 1);
            builderCall(builder, "depletionChance", new Class[]{int.class}, 1);
            builderCall(builder, "depletedYield",   new Class[]{int.class}, 20);

            // null dimensions → accepted for all dimensions
            Field dimField = builder.getClass().getDeclaredField("dimensions");
            dimField.setAccessible(true);
            dimField.set(builder, null);

            Object def = builder.getClass().getMethod("build").invoke(builder);

            // Register so veinId lookups work on world reload
            try {
                Class<?> regClass = Class.forName(
                    "com.gregtechceu.gtceu.api.registry.GTRegistries");
                Object reg = regClass.getField("BEDROCK_FLUID_DEFINITIONS").get(null);
                reg.getClass().getMethod("register", Object.class, Object.class)
                   .invoke(reg, id, def);
                DeepSpaceMod.LOGGER.info(
                    "[GTCEuCompat] Registered ad-hoc definition '{}'", id);
            } catch (Exception ex) {
                DeepSpaceMod.LOGGER.debug(
                    "[GTCEuCompat] Could not register ad-hoc def: {}", ex.getMessage());
            }
            return def;
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[GTCEuCompat] createAdHocDefinition: {}",
                e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    private static Object newFluidVeinWorldEntry(Object def, int yield, int ops) {
        try {
            Class<?> entryClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.FluidVeinWorldEntry");
            Class<?> defClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition");
            Constructor<?> ctor = entryClass.getConstructor(defClass, int.class, int.class);
            return ctor.newInstance(def, yield, ops);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[GTCEuCompat] newFluidVeinWorldEntry: {}", e.getMessage());
            return null;
        }
    }

    private static void setVeinId(Object entry, String id) {
        try {
            Field f = entry.getClass().getDeclaredField("veinId");
            f.setAccessible(true);
            f.set(entry, id);
        } catch (Exception ignored) {}
    }

    private static String getVeinId(Object def) {
        try {
            Class<?> regClass = Class.forName("com.gregtechceu.gtceu.api.registry.GTRegistries");
            Object reg = regClass.getField("BEDROCK_FLUID_DEFINITIONS").get(null);
            Object rl = reg.getClass().getMethod("getKey", Object.class).invoke(reg, def);
            return rl != null ? rl.toString() : "deepspace:unknown_vein";
        } catch (Exception e) {
            return "deepspace:unknown_vein";
        }
    }

    private static void builderCall(Object builder, String method,
                                     Class<?>[] types, Object... args) {
        try {
            builder.getClass().getMethod(method, types).invoke(builder, args);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug(
                "[GTCEuCompat] builder.{}() failed: {}", method, e.getMessage());
        }
    }

    // =========================================================================
    // Bedrock Ore Vein injection
    // =========================================================================

    /**
     * Injects a GT bedrock ore vein beneath the given asteroid position.
     *
     * The vein is keyed by the GT ore-vein grid coordinate
     *   (Math.floorDiv(chunkX, bedrockOreDistance), Math.floorDiv(chunkZ, bedrockOreDistance))
     * so the prospector finds it when the player is anywhere within that grid cell.
     *
     * Yield:  oreYield(26–100, seeded) × MAX_OPERATIONS(160) = 4,160–16,000 items total
     * Regeneration: OreVeinRegenSystem ticks every 5 min, adds 6 ops/tick → full regen ~1h40m.
     *
     * Only called for asteroids that have at least one gtceu ore.
     */
    public static void placeOreVein(ServerLevel level, AsteroidDefinition asteroidDef,
                                     int centreX, int centreZ) {
        if (!GTCEU_LOADED) return;

        // Only inject for asteroids that have GT ores
        List<ResourceLocation> gtOres = new java.util.ArrayList<>();
        for (ResourceLocation rl : asteroidDef.ores) {
            if ("gtceu".equals(rl.getNamespace())) gtOres.add(rl);
        }
        if (gtOres.isEmpty()) return;

        String primaryMaterial = extractPrimaryMaterial(gtOres.get(0));
        if (primaryMaterial == null || primaryMaterial.isEmpty()) return;

        Object oreDef = findOreDefinition(primaryMaterial);
        if (oreDef == null) {
            DeepSpaceMod.LOGGER.warn(
                "[GTCEuCompat] No BedrockOreDefinition found for material '{}' (asteroid '{}') -- " +
                "ensure data/gtceu/bedrock_ore_veins/deepspace_{}.json is loaded",
                primaryMaterial, asteroidDef.id, primaryMaterial);
            return;
        }

        injectOreVein(level, oreDef, primaryMaterial, centreX, centreZ);
    }

    /**
     * Inject a GT bedrock ore vein for a directly-supplied material name.
     * Used by the new ROCK asteroid path, where the GT material is chosen by
     * {@code ForgeOreResolver.pickPrimaryGtMaterial(host, seed)} rather than
     * read from the (now bypassed) JSON ore list.
     */
    public static void placeOreVein(ServerLevel level, String primaryMaterial,
                                     int centreX, int centreZ) {
        if (!GTCEU_LOADED) return;
        if (primaryMaterial == null || primaryMaterial.isEmpty()) return;

        Object oreDef = findOreDefinition(primaryMaterial);
        if (oreDef == null) {
            DeepSpaceMod.LOGGER.warn(
                "[GTCEuCompat] No BedrockOreDefinition for material '{}' -- " +
                "ensure data/gtceu/bedrock_ore_veins/deepspace_{}.json is loaded",
                primaryMaterial, primaryMaterial);
            return;
        }
        injectOreVein(level, oreDef, primaryMaterial, centreX, centreZ);
    }

    /** Extracts the GT material name from an ore block ResourceLocation.
     *  e.g. gtceu:granite_iron_ore → "iron" */
    private static String extractPrimaryMaterial(ResourceLocation oreRl) {
        if (!"gtceu".equals(oreRl.getNamespace())) return null;
        String path = oreRl.getPath(); // e.g. "stone_iron_ore"
        String[] PREFIXES = {
            "red_granite_","deepslate_","andesite_","granite_","diorite_",
            "marble_","basalt_","tuff_","stone_"
        };
        for (String prefix : PREFIXES) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                break;
            }
        }
        if (path.endsWith("_ore")) {
            path = path.substring(0, path.length() - 4);
        }
        return path;
    }

    /**
     * Looks up a BedrockOreDefinition in GTRegistries$BEDROCK_ORE_DEFINITIONS.
     *
     * Definitions are registered by the KubeJS server script
     * (deepspace_bedrock_ore_veins.js) via GTCEuServerEvents.bedrockOreVeins.
     * Each vein is added with ID "deepspace:deepspace_<material>", so we look
     * up using DSConstants.MODID ("deepspace") as the namespace.
     *
     * Uses a local ConcurrentHashMap cache so each material is only looked up once
     * per world session (after which the definition object reference is stable).
     */
    private static Object findOreDefinition(String primaryMaterial) {
        Object cached = ORE_DEF_CACHE.get(primaryMaterial);
        if (cached != null) return cached;

        try {
            // KubeJS registers veins as "deepspace:deepspace_<material>"
            ResourceLocation id = new ResourceLocation(DSConstants.MODID,
                "deepspace_" + primaryMaterial);

            Class<?> regClass = Class.forName(
                "com.gregtechceu.gtceu.api.registry.GTRegistries");
            Object registry = regClass.getField("BEDROCK_ORE_DEFINITIONS").get(null);

            // GTRegistry.get(K) – K is ResourceLocation for GTRegistry$RL
            Method get = registry.getClass().getMethod("get", Object.class);
            Object def = get.invoke(registry, id);

            if (def != null) {
                ORE_DEF_CACHE.put(primaryMaterial, def);
                DeepSpaceMod.LOGGER.debug(
                    "[GTCEuCompat] Ore def cached: '{}' for material '{}'", id, primaryMaterial);
            } else {
                DeepSpaceMod.LOGGER.warn(
                    "[GTCEuCompat] Ore def not found: '{}' -- " +
                    "check kubejs/server_scripts/ores/deepspace_bedrock_ore_veins.js " +
                    "and that doBedrockOres=true in gtceu-common.toml",
                    id);
            }
            return def;

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[GTCEuCompat] findOreDefinition('{}') error: {} {}",
                primaryMaterial, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Reads bedrockOreDistance from ConfigHolder.
     *
     * In GTCEu 7.5.x the field is a toma-configuration Configurable<Integer>.
     * BedrockOreVeinSavedData accesses it via Configurable.getValue() which may
     * return the boxed Integer directly (toma stores it generically).
     * We try three strategies in order, defaulting to 16 if all fail.
     */
    private static int getBedrockOreGridSize() {
        try {
            Class<?> holderClass = Class.forName(
                "com.gregtechceu.gtceu.config.ConfigHolder");
            Object holder   = holderClass.getField("INSTANCE").get(null);
            Object worldgen = holder.getClass().getField("worldgen").get(holder);
            Object oreVeins = worldgen.getClass().getField("oreVeins").get(worldgen);
            Object configurable = oreVeins.getClass()
                .getField("bedrockOreDistance").get(oreVeins);

            // Strategy 1: toma Configurable has a public 'value' field (Integer due to erasure)
            try {
                Object v = configurable.getClass().getField("value").get(configurable);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Exception ignored) {}

            // Strategy 2: getValue() method on Configurable returns the wrapped value
            try {
                Method getValue = configurable.getClass().getMethod("getValue");
                Object v = getValue.invoke(configurable);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Exception ignored) {}

            // Strategy 3: get() method (some versions use plain get())
            try {
                Method get = configurable.getClass().getMethod("get");
                Object v = get.invoke(configurable);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}

        return DEFAULT_ORE_GRID_SIZE; // safe default (matches GT's own default of 16)
    }

    @SuppressWarnings("unchecked")
    private static void injectOreVein(ServerLevel level, Object oreDef,
                                       String primaryMaterial,
                                       int centreX, int centreZ) {
        try {
            Class<?> savedDataClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreVeinSavedData");
            Method getOrCreate = savedDataClass.getMethod("getOrCreate", ServerLevel.class);
            Object savedData = getOrCreate.invoke(null, level);
            if (savedData == null) {
                DeepSpaceMod.LOGGER.warn(
                    "[GTCEuCompat] BedrockOreVeinSavedData.getOrCreate returned null " +
                    "for ore vein '{}' -- ensure this runs on main server thread", primaryMaterial);
                return;
            }

            Field veinOresField = savedDataClass.getField("veinOres");
            HashMap<Object, Object> veinOres =
                (HashMap<Object, Object>) veinOresField.get(savedData);

            // Key the vein by the asteroid's exact chunk position so every asteroid gets its own
            // independent bedrock ore vein. Previously this used Math.floorDiv(chunkX, gridSize=16)
            // which bucketed the whole 16x16-chunk area into ONE shared cell, silently dropping
            // every asteroid after the first in each area.
            //
            // REQUIRED CONFIG: config/gtceu/gtceu-common.toml
            //   [machines]
            //     bedrockOreDistance = 1
            //
            // With bedrockOreDistance=1, GT's getOreVeinWorldEntry(chunkX,chunkZ) maps to
            // ChunkPos(chunkX, chunkZ) — an exact match for the key stored here.
            // The Bedrock Ore Miner and Prospector will correctly find the vein for each asteroid.
            int asteroidCX = centreX >> 4;
            int asteroidCZ = centreZ >> 4;

            net.minecraft.world.level.ChunkPos gridKey =
                new net.minecraft.world.level.ChunkPos(asteroidCX, asteroidCZ);

            if (veinOres.containsKey(gridKey)) {
                // Already injected for this chunk (e.g. SpawnGuaranteeSystem re-ran).
                DeepSpaceMod.LOGGER.debug(
                    "[GTCEuCompat] Chunk [{},{}] already has a bedrock ore vein -- skipping '{}'",
                    asteroidCX, asteroidCZ, primaryMaterial);
                return;
            }

            // Seeded-random oreYield per position: ORE_YIELD_MIN..ORE_YIELD_MAX
            // Total ores = oreYield × MAX_OPERATIONS → range 4,160–16,000
            int oreYield = computeSeedOreYield(asteroidCX, asteroidCZ);

            // Create OreVeinWorldEntry(def, oreYield, operationsRemaining)
            Object entry = newOreVeinWorldEntry(oreDef, oreYield,
                OreVeinRegenSystem.MAX_OPERATIONS);
            if (entry == null) return;

            veinOres.put(gridKey, entry);

            try { savedDataClass.getMethod("setDirty").invoke(savedData); }
            catch (Exception ignored) {}

            DeepSpaceMod.LOGGER.info(
                "[GTCEuCompat] Ore vein injected: material='{}' chunk=[{},{}] " +
                "oreYield={} ops={} totalOres={}",
                primaryMaterial, asteroidCX, asteroidCZ,
                oreYield, OreVeinRegenSystem.MAX_OPERATIONS,
                (long) oreYield * OreVeinRegenSystem.MAX_OPERATIONS);

        } catch (ClassNotFoundException e) {
            DeepSpaceMod.LOGGER.debug("[GTCEuCompat] BedrockOreVeinSavedData not found");
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn(
                "[GTCEuCompat] Ore vein injection failed for '{}': {} ({})",
                primaryMaterial, e.getClass().getSimpleName(), e.getMessage());
            DeepSpaceMod.LOGGER.debug("[GTCEuCompat] Stack:", e);
        }
    }

    private static Object newOreVeinWorldEntry(Object def, int oreYield, int ops) {
        try {
            Class<?> entryClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockore.OreVeinWorldEntry");
            Class<?> defClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition");
            Constructor<?> ctor = entryClass.getConstructor(defClass, int.class, int.class);
            return ctor.newInstance(def, oreYield, ops);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[GTCEuCompat] newOreVeinWorldEntry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deterministic seeded yield in [ORE_YIELD_MIN, ORE_YIELD_MAX] based on chunk position.
     * Same asteroid always gets the same yield across world restarts.
     * Total ores = result × MAX_OPERATIONS → range [4,160 – 16,000].
     */
    private static int computeSeedOreYield(int chunkX, int chunkZ) {
        long seed = (long) chunkX * 0x9E3779B97F4A7C15L
                  ^ (long) chunkZ * 0x6C62272E07BB0142L;
        seed ^= seed >>> 30;
        seed *= 0xbf58476d1ce4e5b9L;
        seed ^= seed >>> 27;
        seed *= 0x94d049bb133111ebL;
        seed ^= seed >>> 31;
        int range = ORE_YIELD_MAX - ORE_YIELD_MIN + 1;
        return ORE_YIELD_MIN + (int)(Math.abs(seed) % range);
    }

    // =========================================================================

    public static boolean isGTFluid(ResourceLocation rl) {
        if (!GTCEU_LOADED) return false;
        return "gtceu".equals(rl.getNamespace());
    }
}
