package com.deepspace.registry;

import com.deepspace.DeepSpaceMod;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Central registry of asteroid archetypes and zones.
 *
 * Contract compliance:
 *  - MUST be immutable after load() completes.
 *  - MUST be lock-free (all fields final after init, no mutation).
 *  - MUST be thread-safe for reads.
 *
 * Loaded from: /data/deepspace/registry/deepspace-registry.json
 * (bundled inside the mod jar as a resource)
 */
public final class AsteroidRegistry {

    private static final Gson GSON = new Gson();

    // Immutable maps populated once by load()
    private static volatile Map<String, ZoneDefinition> zones = Map.of();
    private static volatile Map<String, AsteroidDefinition> asteroids = Map.of();
    // Zones in order of increasing distance (for fast lookup)
    private static volatile List<ZoneDefinition> orderedZones = List.of();
    // Flat immutable list of every archetype (for uniform, zone-free selection)
    private static volatile List<AsteroidDefinition> allList = List.of();
    // Asteroids grouped by zone id
    private static volatile Map<String, List<AsteroidDefinition>> asteroidsByZone = Map.of();
    // Total weight per zone (precomputed)
    private static volatile Map<String, Integer> totalWeightByZone = Map.of();

    private AsteroidRegistry() {}

    /**
     * Load the registry from the bundled JSON resource.
     * Called once during FMLCommonSetupEvent on the main thread.
     */
    public static void load() {
        try {
            InputStream is = AsteroidRegistry.class.getResourceAsStream(
                "/data/deepspace/registry/deepspace-registry.json");
            if (is == null) {
                DeepSpaceMod.LOGGER.error("[AsteroidRegistry] Registry JSON not found in jar!");
                return;
            }
            JsonObject root = GSON.fromJson(
                new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            parseRegistry(root);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[AsteroidRegistry] Failed to load registry", e);
        }
    }

    private static void parseRegistry(JsonObject root) {
        // -- Parse zones ------------------------------------------------------
        Map<String, ZoneDefinition> zoneMap = new LinkedHashMap<>();
        List<ZoneDefinition> zoneList = new ArrayList<>();
        JsonArray zonesArr = root.getAsJsonArray("zones");
        for (JsonElement el : zonesArr) {
            JsonObject z = el.getAsJsonObject();
            String id = z.get("id").getAsString();
            String name = z.get("name").getAsString();
            JsonArray dist = z.getAsJsonArray("distance_blocks");
            int dMin = dist.get(0).getAsInt();
            int dMax = dist.get(1).getAsInt();
            List<String> tiers = new ArrayList<>();
            for (JsonElement t : z.getAsJsonArray("tier")) tiers.add(t.getAsString());
            ZoneDefinition zd = new ZoneDefinition(id, name, dMin, dMax, tiers);
            zoneMap.put(id, zd);
            zoneList.add(zd);
        }
        // Sort by distance ascending
        zoneList.sort(Comparator.comparingInt(z -> z.distanceMin));

        // -- Parse asteroids --------------------------------------------------
        Map<String, AsteroidDefinition> asteroidMap = new LinkedHashMap<>();
        Map<String, List<AsteroidDefinition>> byZone = new HashMap<>();
        Map<String, Integer> weightByZone = new HashMap<>();

        JsonArray asteroidsArr = root.getAsJsonArray("asteroids");
        for (JsonElement el : asteroidsArr) {
            JsonObject a = el.getAsJsonObject();
            String id = a.get("id").getAsString();
            String zoneId = a.get("zone").getAsString();
            int weight = a.get("spawn_weight").getAsInt();
            String category = a.get("category").getAsString();

            List<ResourceLocation> outerShell = parseRLList(a, "outer_shell");
            List<ResourceLocation> innerBlocks = parseRLList(a, "inner_blocks");
            List<ResourceLocation> ores = parseRLList(a, "ores");

            ResourceLocation fluid = null;
            if (a.has("fluid") && !a.get("fluid").isJsonNull()) {
                String fs = a.get("fluid").getAsString();
                if (!fs.isEmpty()) fluid = new ResourceLocation(fs);
            }
            boolean hasDrillingVein = a.has("has_drilling_vein") &&
                a.get("has_drilling_vein").getAsBoolean();
            long avgFluid = a.has("avg_fluid_amount_mb") ?
                a.get("avg_fluid_amount_mb").getAsLong() : 0L;

            boolean gravityLayered = a.has("gravity_layered") &&
                a.get("gravity_layered").getAsBoolean();

            AsteroidDefinition def = new AsteroidDefinition(id, zoneId, weight, category,
                outerShell, innerBlocks, ores, fluid, hasDrillingVein, avgFluid, gravityLayered);

            asteroidMap.put(id, def);
            byZone.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(def);
            weightByZone.merge(zoneId, weight, Integer::sum);
        }

        // Freeze into unmodifiable structures
        zones = Collections.unmodifiableMap(zoneMap);
        orderedZones = Collections.unmodifiableList(zoneList);
        asteroids = Collections.unmodifiableMap(asteroidMap);
        allList = List.copyOf(asteroidMap.values());
        // Deep-freeze asteroid lists per zone
        Map<String, List<AsteroidDefinition>> frozenByZone = new HashMap<>();
        byZone.forEach((k, v) -> frozenByZone.put(k, Collections.unmodifiableList(v)));
        asteroidsByZone = Collections.unmodifiableMap(frozenByZone);
        totalWeightByZone = Collections.unmodifiableMap(weightByZone);

        DeepSpaceMod.LOGGER.info("[AsteroidRegistry] Loaded {} zones, {} asteroid types",
            zones.size(), asteroids.size());
    }

    private static List<ResourceLocation> parseRLList(JsonObject obj, String key) {
        List<ResourceLocation> list = new ArrayList<>();
        if (!obj.has(key) || obj.get(key).isJsonNull()) return list;
        JsonArray arr = obj.getAsJsonArray(key);
        for (JsonElement el : arr) {
            String s = el.getAsString();
            if (!s.isBlank()) list.add(new ResourceLocation(s));
        }
        return list;
    }

    // -- Public read API (thread-safe, lock-free) -----------------------------

    /** Resolve which zone a given block distance from crash site belongs to. */
    public static ZoneDefinition resolveZone(double blockDistance) {
        List<ZoneDefinition> oz = orderedZones;
        for (ZoneDefinition z : oz) {
            if (z.contains(blockDistance)) return z;
        }
        // Beyond zone 4: return the last zone
        return oz.isEmpty() ? null : oz.get(oz.size() - 1);
    }

    /**
     * Select an asteroid definition for the given zone using seeded RNG.
     * Weighted random selection -- deterministic for same seed.
     */
    public static AsteroidDefinition selectAsteroid(String zoneId, long seed) {
        List<AsteroidDefinition> pool = asteroidsByZone.get(zoneId);
        if (pool == null || pool.isEmpty()) return null;
        int totalWeight = totalWeightByZone.getOrDefault(zoneId, 0);
        if (totalWeight == 0) return null;

        // LCG seeded by chunk seed -- no Random object allocation on hot path
        long rng = seed ^ 0x6C62272E07BB0142L;
        rng ^= rng >>> 33; rng *= 0xff51afd7ed558ccdL;
        rng ^= rng >>> 33; rng *= 0xc4ceb9fe1a85ec53L;
        rng ^= rng >>> 33;
        int roll = (int) (Math.abs(rng) % totalWeight);

        int cumulative = 0;
        for (AsteroidDefinition def : pool) {
            cumulative += def.spawnWeight;
            if (roll < cumulative) return def;
        }
        return pool.get(pool.size() - 1);
    }

    /** Every asteroid archetype, frozen (uniform zone-free pool). */
    public static List<AsteroidDefinition> allAsteroids() {
        return allList;
    }

    /**
     * Uniformly select an archetype ignoring zone AND spawn_weight — every one
     * of the ~324 types is equally likely anywhere. Deterministic for a seed.
     */
    public static AsteroidDefinition selectUniform(long seed) {
        List<AsteroidDefinition> pool = allList;
        if (pool.isEmpty()) return null;
        long rng = seed ^ 0x6C62272E07BB0142L;
        rng ^= rng >>> 33; rng *= 0xff51afd7ed558ccdL;
        rng ^= rng >>> 33; rng *= 0xc4ceb9fe1a85ec53L;
        rng ^= rng >>> 33;
        return pool.get((int) (Math.abs(rng) % pool.size()));
    }

    /** Get all asteroids for a zone (for guarantee checking). */
    public static List<AsteroidDefinition> getAsteroidsForZone(String zoneId) {
        return asteroidsByZone.getOrDefault(zoneId, List.of());
    }

    /** Get a specific asteroid definition by ID. */
    public static AsteroidDefinition getAsteroid(String id) {
        return asteroids.get(id);
    }

    public static int asteroidCount() { return asteroids.size(); }
    public static int zoneCount()     { return zones.size(); }
}
