package com.deepspace.structures;

import com.deepspace.DeepSpaceMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Registry for all loaded structure NBT files.
 *
 * Sources:
 *  1. Compile-time: data/deepspace/structures/*.nbt (resource manager)
 *  2. Runtime:      config/deepspace/structures/*.nbt (filesystem)
 *
 * Runtime files with the same filename as compile-time files OVERRIDE them.
 * This allows server admins to customise crashed_ship.nbt and other structures.
 *
 * crashed_ship.nbt is excluded from the random generation pool — it is only
 * used when a new player first joins the server.
 *
 * Hot-reload: implements ResourceManagerReloadListener so /reload refreshes
 * compile-time structures while also re-scanning the config directory.
 *
 * Lifecycle:
 *  - Register as a ResourceManagerReloadListener in DeepSpaceMod.
 *  - Call loadRuntimeStructures() during FMLCommonSetupEvent.
 *  - The resource manager reload handles compile-time files.
 */
public final class DeepSpaceStructureRegistry implements ResourceManagerReloadListener {

    private static final DeepSpaceStructureRegistry INSTANCE = new DeepSpaceStructureRegistry();

    /** Filename -> loaded NBT CompoundTag. Runtime files override compile-time on same name. */
    private final ConcurrentHashMap<String, CompoundTag> loaded = new ConcurrentHashMap<>();

    /** Filename -> StructureTemplateEntry metadata. */
    private final ConcurrentHashMap<String, StructureTemplateEntry> entries = new ConcurrentHashMap<>();

    /** Cached template sizes (x,y,z) for bounding-box computations. Key = filename. */
    private final ConcurrentHashMap<String, int[]> templateSizes = new ConcurrentHashMap<>();

    /** The name of the crashed ship structure — excluded from random generation. */
    public static final String CRASHED_SHIP_FILENAME = "crashed_ship.nbt";

	/**
	 * Root folder used for compile-time NBT structure discovery.
	 *
	 * NOTE (MC 1.20+):
	 * ResourceManager#listResources MUST NOT receive a path ending
	 * with a trailing slash. Mojang added strict validation which
	 * throws IllegalArgumentException if '/' is present at the end.
	 *
	 * Correct example:
	 *   data/deepspace/structures/<file>.nbt
	 */
	private static final String RESOURCE_PREFIX = "structures";

    /** Config directory path: config/deepspace/structures/ */
    private Path configDir;

    private DeepSpaceStructureRegistry() {}

    public static DeepSpaceStructureRegistry getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // ResourceManagerReloadListener
    // -------------------------------------------------------------------------

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        // Clear compile-time entries (runtime entries are re-loaded separately)
        Set<String> runtimeFilenames = entries.entrySet().stream()
                .filter(e -> !e.getValue().compileTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        loaded.clear();
        entries.clear();
        templateSizes.clear();

        // Reload compile-time structures from resource manager
        loadCompileTimeStructures(manager);

        // Re-load runtime structures (they override compile-time on same filename)
        loadRuntimeStructures();

        DeepSpaceMod.LOGGER.info("[DeepSpace_Structures] Reload complete: {} structures loaded ({} in random pool).",
                loaded.size(), getPool().size());
    }

    // -------------------------------------------------------------------------
    // Compile-time loading (resource manager)
    // -------------------------------------------------------------------------

    private void loadCompileTimeStructures(ResourceManager manager) {
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources =
        manager.listResources(RESOURCE_PREFIX, loc ->
                loc.getPath().endsWith(".nbt") && loc.getNamespace().equals("deepspace")
        );

        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> entry : resources.entrySet()) {
            ResourceLocation loc = entry.getKey();
            String filename = extractFilename(loc.getPath());
            if (filename == null) continue;

            // Skip if runtime version already loaded (runtime overrides compile-time)
            if (entries.containsKey(filename) && !entries.get(filename).compileTime) continue;

            try (InputStream is = entry.getValue().open()) {
                CompoundTag nbt = readNbtGzip(is);
                if (nbt != null) {
                    loaded.put(filename, nbt);
                    entries.put(filename, new StructureTemplateEntry(filename, true));
                    cacheTemplateSize(filename, nbt);
                    DeepSpaceMod.LOGGER.info("[DeepSpace_Structures] [INFO] Loaded compile-time: {} (namespace={})",
        filename, loc.getNamespace());
                }
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[DeepSpace_Structures] Skipping invalid compile-time NBT {}: {}", filename, e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Runtime loading (config directory)
    // -------------------------------------------------------------------------

    /**
     * Called during FMLCommonSetupEvent and on /reload.
     * Scans config/deepspace/structures/ for .nbt files.
     */
    public void loadRuntimeStructures() {
        if (configDir == null) {
            configDir = FMLPaths.CONFIGDIR.get().resolve("deepspace").resolve("structures");
        }

        // Create directory tree if missing
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            DeepSpaceMod.LOGGER.warn("[DeepSpace_Structures] Could not create config dir {}: {}", configDir, e.getMessage());
        }

        if (!Files.isDirectory(configDir)) return;

        try {
            Files.walk(configDir)
                    .filter(p -> p.toString().endsWith(".nbt"))
                    .filter(Files::isRegularFile)
                    .forEach(this::loadRuntimeFile);
        } catch (IOException e) {
            DeepSpaceMod.LOGGER.warn("[DeepSpace_Structures] Error scanning config dir: {}", e.getMessage());
        }
    }

    private void loadRuntimeFile(Path file) {
        String filename = file.getFileName().toString();
        try (InputStream is = Files.newInputStream(file)) {
            CompoundTag nbt = readNbtGzip(is);
            if (nbt != null) {
                loaded.put(filename, nbt);
                entries.put(filename, new StructureTemplateEntry(filename, false));
                cacheTemplateSize(filename, nbt);
                DeepSpaceMod.LOGGER.info("[DeepSpace_Structures] [INFO] Loaded runtime: {}", filename);
            }
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[DeepSpace_Structures] Skipping invalid runtime NBT {}: {}", filename, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all filenames (for /importship autocomplete).
     */
    public List<String> getAllNames() {
        return new ArrayList<>(loaded.keySet());
    }

    /**
     * Returns the random-generation pool: all filenames EXCLUDING:
     *   - crashed_ship.nbt  (placed once per player via CrashedShipPlacer)
     *   - any file whose name starts with "shop_"  (manual /importship placement only)
     *
     * Files excluded here still appear in /importship autocomplete via getAllNames().
     */
    public List<String> getPool() {
        return loaded.keySet().stream()
                .filter(name -> !name.equals(CRASHED_SHIP_FILENAME))
                .filter(name -> !name.startsWith("shop_"))
                .collect(Collectors.toList());
    }

    /**
     * Returns the loaded NBT CompoundTag for the given filename, or null if not found.
     */
    @Nullable
    public CompoundTag getTemplate(String filename) {
        return loaded.get(filename);
    }

    /**
     * Returns the StructureTemplateEntry for the given filename, or null.
     */
    @Nullable
    public StructureTemplateEntry getEntry(String filename) {
        return entries.get(filename);
    }

    /**
     * Returns true if the registry has the named structure.
     */
    public boolean has(String filename) {
        return loaded.containsKey(filename);
    }

    /**
     * Returns the cached bounding box size [sizeX, sizeY, sizeZ] for the named structure,
     * or [0,0,0] if not available.
     */
    public int[] getTemplateSize(String filename) {
        int[] size = templateSizes.get(filename);
        return size != null ? size : new int[]{0, 0, 0};
    }

    /**
     * Number of loaded structures (including crashed_ship.nbt).
     */
    public int count() {
        return loaded.size();
    }

    /**
     * Returns the config directory for runtime structures.
     */
    public Path getConfigDir() {
        if (configDir == null) {
            configDir = FMLPaths.CONFIGDIR.get().resolve("deepspace").resolve("structures");
        }
        return configDir;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Read a gzip-compressed Minecraft NBT file.
     * Returns null on failure (non-fatal, logs warning).
     */
    @Nullable
    private static CompoundTag readNbtGzip(InputStream is) {
        try (GZIPInputStream gzip = new GZIPInputStream(is);
             DataInputStream dis = new DataInputStream(gzip)) {
            return NbtIo.read(dis);
        } catch (Exception e) {
            // Try reading as uncompressed NBT (some tools don't gzip)
            try {
                // Re-read - can't seek, so only works if caller retries
                // Just return null; the caller will log and skip
                return null;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Cache the [sizeX, sizeY, sizeZ] dimensions from the NBT "size" tag.
     */
    private void cacheTemplateSize(String filename, CompoundTag nbt) {
        try {
            if (nbt.contains("size")) {
                net.minecraft.nbt.ListTag sizeTag = nbt.getList("size", net.minecraft.nbt.Tag.TAG_INT);
                if (sizeTag.size() >= 3) {
                    templateSizes.put(filename, new int[]{
                            sizeTag.getInt(0),
                            sizeTag.getInt(1),
                            sizeTag.getInt(2)
                    });
                }
            }
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[DeepSpace_Structures] Could not read size for {}: {}", filename, e.getMessage());
        }
    }

    /**
     * Extract filename from a resource path like "deepspace:structures/abandoned_station.nbt"
     */
    @Nullable
    private static String extractFilename(String path) {
        int slash = path.lastIndexOf('/');
        String name = (slash >= 0) ? path.substring(slash + 1) : path;
        return name.endsWith(".nbt") ? name : null;
    }
}
