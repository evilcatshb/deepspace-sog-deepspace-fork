package com.deepspace.structures;

import com.deepspace.util.StructurePlacementFixer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.deepspace.structures.StructureLifecycleBootstrap;
import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.compat.FTBChunksCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Unified structure placement engine.
 *
 * Used by:
 *  - CrashedShipPlacer (first-spawn ship)
 *  - DeepSpaceChunkGenerator (random worldgen structures)
 *  - ImportShipCommand (/importship admin command)
 *
 * Handles:
 *  - Loading NBT from registry (compile-time or runtime)
 *  - Safe-location spiral search with configurable bounding box
 *  - FTB Chunks claim checking (for /importship)
 *  - Block placement via MC StructureTemplate
 *  - Entity/NPC spawning with full NBT preservation
 *  - GregTech pipe/cable visual sync
 *  - GregTech multiblock machine initialisation
 *  - Registering placed structures in StructureRadiusMapImpl
 */
public final class StructurePlacer {

    private StructurePlacer() {}

    /** Padding added on all sides of the structure bounding box for safe-location search. */
    private static final int SAFE_LOCATION_PADDING = 4;

    /** Maximum spiral search radius for safe location (in blocks). */
    private static final int MAX_SPIRAL_SEARCH_RADIUS = 512;

    /** Spiral step size for safe-location search. */
    private static final int SPIRAL_STEP = 8;

    // Reflection cache for GT pipe syncNow
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, java.lang.reflect.Method>
            SYNC_NOW_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Main entry point: place a named structure near the given origin.
     *
     * For worldgen placements, origin is the deterministic sector position.
     * For /importship, origin is the player's current position; a safe location
     * is found by spiral search around that origin.
     *
     * @param level     The server level to place in
     * @param origin    Desired centre position (will spiral-search from here)
     * @param filename  NBT filename from the registry (e.g. "abandoned_station.nbt")
     * @param context   Placement context (worldgen vs spawned, target player info)
     * @return PlacementResult describing success or failure
     */
    public static PlacementResult placeStructure(
            ServerLevel level,
            BlockPos origin,
            String filename,
            PlacementContext context) {

        // --- 1. Get NBT from registry ---
        DeepSpaceStructureRegistry registry = DeepSpaceStructureRegistry.getInstance();
        CompoundTag nbt = registry.getTemplate(filename);
        if (nbt == null) {
            DeepSpaceMod.LOGGER.warn("[StructurePlacer] Structure not found in registry: {}", filename);
            return PlacementResult.ERROR_FILENAME_MISSING;
        }

        // --- 2. Load into MC StructureTemplate ---
        StructureTemplate template;
        try {
            template = loadTemplateFromNbt(level, nbt, filename);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[StructurePlacer] Failed to load StructureTemplate for {}: {}", filename, e.getMessage());
            return PlacementResult.ERROR_INVALID_NBT;
        }

        net.minecraft.core.Vec3i size = template.getSize();
        int sizeX = size.getX(), sizeY = size.getY(), sizeZ = size.getZ();

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            DeepSpaceMod.LOGGER.warn("[StructurePlacer] Zero-size template for {}: {}x{}x{}", filename, sizeX, sizeY, sizeZ);
            return PlacementResult.ERROR_INVALID_NBT;
        }

        // --- 3. Find safe placement location ---
        BlockPos safeOrigin;
        if (context.isWorldgen) {
            // For worldgen, use origin directly (no safe-location search needed;
            // void exclusion is already enforced by the sector grid)
            safeOrigin = origin;
        } else if (level.dimension().equals(DSConstants.DEEP_SPACE_KEY)) {
            // Deep Space dimension: find a clear void pocket above the asteroid band (unchanged behaviour).
            safeOrigin = findSafeLocation(level, origin, sizeX, sizeY, sizeZ, context);
            if (safeOrigin == null) {
                DeepSpaceMod.LOGGER.warn("[StructurePlacer] No safe location found near {} for {}", origin, filename);
                return PlacementResult.ERROR_SAFE_LOCATION;
            }
        } else {
            // Any other dimension (e.g. Overworld): snap to ground instead of searching for void.
            safeOrigin = findGroundLevelLocation(level, origin, sizeX, sizeY, sizeZ);
            if (safeOrigin == null) {
                DeepSpaceMod.LOGGER.warn("[StructurePlacer] No ground location found near {} for {}", origin, filename);
                return PlacementResult.ERROR_SAFE_LOCATION;
            }
        }

        // --- 4. FTB Chunks claim check (admin/importship only) ---
        if (context.isSpawned) {
            PlacementResult claimResult = checkFTBChunksClaims(level, safeOrigin, sizeX, sizeZ, context);
            if (claimResult != null) return claimResult;
        }

        // --- 5. Place blocks ---
        // IMPORTANT: ignoreEntities(true) so placeInWorld only places blocks.
        // All entity/mob/NPC spawning is handled exclusively by spawnEntities() below.
        // Without this, placeInWorld would spawn entities AND spawnEntities() would
        // spawn them again, producing duplicates of every mob/NPC in the structure.
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);

        // Force-load every chunk spanned by the structure BEFORE calling placeInWorld.
        //
        // WHY THIS IS REQUIRED:
        // StructureTemplate.placeInWorld() calls level.setBlock() for each block in the
        // template. Vanilla's setBlock() silently no-ops when the target chunk is not
        // loaded (it receives an EmptyLevelChunk and discards the write). For small
        // structures this is rarely a problem because nearby chunks are already loaded,
        // but for large structures — e.g. a 12-chunk-wide celestial sphere (~144 chunks)
        // — virtually none of the chunks will be loaded, and most blocks are silently
        // dropped, producing a partial or empty result with no error logged.
        //
        // level.getChunk(cx, cz) forces a synchronous load from disk (or generates the
        // chunk if it has never existed). This is safe to call on the server thread.
        {
            int minCx = safeOrigin.getX() >> 4;
            int maxCx = (safeOrigin.getX() + sizeX - 1) >> 4;
            int minCz = safeOrigin.getZ() >> 4;
            int maxCz = (safeOrigin.getZ() + sizeZ - 1) >> 4;
            int chunkCount = (maxCx - minCx + 1) * (maxCz - minCz + 1);
            DeepSpaceMod.LOGGER.info("[StructurePlacer] Pre-loading {} chunks for {} ({}x{} chunk footprint)",
                    chunkCount, filename, maxCx - minCx + 1, maxCz - minCz + 1);
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    level.getChunk(cx, cz);
                }
            }
        }

        try {
            template.placeInWorld(level, safeOrigin, safeOrigin, settings, level.getRandom(), 2);
            DeepSpaceMod.LOGGER.info("[StructurePlacer] Placed {} at {}", filename, safeOrigin);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[StructurePlacer] Block placement failed for {} at {}: {}", filename, safeOrigin, e.getMessage());
            return PlacementResult.ERROR_INTERNAL;
        }

                // ---------------------------------------------------------------------
                // Post-placement refresh pass (fix GTCEu pipe visual connections)
                // ---------------------------------------------------------------------
                // BUG FIX: must pass safeOrigin (actual placement corner), NOT origin
                // (the player's raw position). When /importship spiral-searches for a
                // safe location, safeOrigin differs from origin by an arbitrary offset.
                // Passing origin here made refreshArea() operate on the wrong volume,
                // leaving the actual structure completely unrefreshed.
                BoundingBox box = template.getBoundingBox(settings, safeOrigin);
                BlockPos min = new BlockPos(box.minX(), box.minY(), box.minZ());
                BlockPos max = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
                StructurePlacementFixer.refreshArea(level, min, max);

                // BUG FIX: reset Lootr crate identities so re-importing the same
                // structure doesn't alias the first copy's Lootr save-data (empty
                // crates + ReportedException NPE on container click). See
                // resetLootrCrates() below for details.
                resetLootrCrates(level, safeOrigin, sizeX, sizeY, sizeZ);

        // --- 6. Spawn entities / NPCs ---
        List<MobData> spawnedMobs = spawnEntities(level, safeOrigin, nbt);

        // --- 7. GregTech pipe/cable sync ---
        reinitPipeConnections(level, safeOrigin, sizeX, sizeY, sizeZ);

                //  7.5 universal machine initialisation
                StructureLifecycleBootstrap.run(
                                level,
                                safeOrigin,
                                sizeX,
                                sizeY,
                                sizeZ
                );

        // --- 8. Register in StructureRadiusMap ---
        StructureTemplateEntry entry = registry.getEntry(filename);
        if (entry == null) entry = new StructureTemplateEntry(filename, true);

        StructureLocation loc = new StructureLocation(
                level.dimension().location().toString(),
                safeOrigin.getX() + sizeX / 2,
                safeOrigin.getY() + sizeY / 2,
                safeOrigin.getZ() + sizeZ / 2
        );
        loc.mobs.addAll(spawnedMobs);

        BoundingBox bounds = new BoundingBox(
                safeOrigin.getX(), safeOrigin.getY(), safeOrigin.getZ(),
                safeOrigin.getX() + sizeX - 1,
                safeOrigin.getY() + sizeY - 1,
                safeOrigin.getZ() + sizeZ - 1
        );

        long sectorKey = computeSectorKey(safeOrigin);
        RegisteredStructure registered = new RegisteredStructure(loc, entry, bounds, sectorKey);
        for (MobData mob : spawnedMobs) mob.parentStructure = registered;

        StructureRadiusMapImpl.INSTANCE.registerStructure(registered);

        // --- 9. Persist in world data ---
        try {
            com.deepspace.crash.DSWorldData.getOrCreate(level)
                    .recordPlacedStructure(filename, level.dimension().location().toString(),
                            safeOrigin.getX() + sizeX / 2,
                            safeOrigin.getY() + sizeY / 2,
                            safeOrigin.getZ() + sizeZ / 2,
                            sectorKey);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug("[StructurePlacer] Could not persist structure record: {}", e.getMessage());
        }

        return PlacementResult.SPAWNED;
    }

    // -------------------------------------------------------------------------
    // Safe-location search (spiral)
    // -------------------------------------------------------------------------

    /**
     * Spiral search outward from origin for a location where the padded cuboid
     * [sizeX+2*pad, sizeY+2*pad, sizeZ+2*pad] contains only air/void blocks.
     * Returns null if no safe location found within MAX_SPIRAL_SEARCH_RADIUS.
     *
     * BUG_12 FIX: The original code clamped Y to [ASTEROID_BAND_Y_MIN+pad, ASTEROID_BAND_Y_MAX-sizeY-pad].
     * For a large ship (sizeY≈60) that's Y≤56 — deep in the dense asteroid zone where isCuboidClear()
     * almost never succeeds, exhausting all 1000+ candidates and timing out. The correct behaviour is
     * to search ABOVE the asteroid band (Y>ASTEROID_BAND_Y_MAX) where deep space is empty void, so the
     * first XZ candidate always produces a clear volume immediately.
     */
    private static BlockPos findSafeLocation(
            ServerLevel level, BlockPos origin,
            int sizeX, int sizeY, int sizeZ,
            PlacementContext context) {

        int padX = sizeX + SAFE_LOCATION_PADDING * 2;
        int padY = sizeY + SAFE_LOCATION_PADDING * 2;
        int padZ = sizeZ + SAFE_LOCATION_PADDING * 2;

        // BUG_12: Target Y ABOVE the asteroid band. Above Y=ASTEROID_BAND_Y_MAX (120) the
        // deep-space dimension is empty void, so isCuboidClear() returns true immediately.
        // We try a small range of Y values starting from just above the band so the ship
        // lands at a predictable, accessible height for the player.
        int minSafeY = DSConstants.ASTEROID_BAND_Y_MAX + SAFE_LOCATION_PADDING;        // 124
        int maxSafeY = level.getMaxBuildHeight() - sizeY - SAFE_LOCATION_PADDING - 1;  // near build limit
        // If the origin is already comfortably above the band, prefer Y near the origin.
        if (origin.getY() > minSafeY) {
            minSafeY = Math.min(origin.getY(), maxSafeY);
        }

        // Spiral in XZ plane — for each radius ring try several Y offsets above the band.
        for (int radius = 0; radius <= MAX_SPIRAL_SEARCH_RADIUS; radius += SPIRAL_STEP) {
            List<BlockPos> candidates = getSpiralRing(origin, radius);
            for (BlockPos candidate : candidates) {
                // Try a small set of Y levels above the asteroid band.
                for (int yOffset = 0; yOffset <= 48; yOffset += 8) {
                    int y = Math.min(minSafeY + yOffset, maxSafeY);
                    if (y < 0) continue;

                    BlockPos testPos = new BlockPos(candidate.getX(), y, candidate.getZ());

                    // Clearance envelope starts SAFE_LOCATION_PADDING before structure origin.
                    BlockPos checkCorner = testPos.offset(
                            -SAFE_LOCATION_PADDING, -SAFE_LOCATION_PADDING, -SAFE_LOCATION_PADDING);

                    if (isCuboidClear(level, checkCorner, padX, padY, padZ)) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Ground-snap search for non-Deep-Space dimensions (e.g. Overworld).
     *
     * Spiral-searches outward from origin in the XZ plane. For each candidate
     * column, uses the level's heightmap to find the Y position directly above
     * the highest solid block ("MOTION_BLOCKING_NO_LEAVES" ignores leaves so we
     * don't snap to the top of a tree canopy). That Y is exactly one block above
     * the ground surface, so placing the structure there means its bottom layer
     * rests directly on the ground (touching it, not floating).
     *
     * A horizontal-padded clearance check (same padding as the deep-space path)
     * is used to avoid embedding the structure into a hillside or existing build;
     * the check starts at ground level and extends upward through the full
     * structure height, so terrain below the footprint is not required to be air.
     */
    private static BlockPos findGroundLevelLocation(
            ServerLevel level, BlockPos origin,
            int sizeX, int sizeY, int sizeZ) {

        int padX = sizeX + SAFE_LOCATION_PADDING * 2;
        int padZ = sizeZ + SAFE_LOCATION_PADDING * 2;

        for (int radius = 0; radius <= MAX_SPIRAL_SEARCH_RADIUS; radius += SPIRAL_STEP) {
            List<BlockPos> candidates = getSpiralRing(origin, radius);
            for (BlockPos candidate : candidates) {
                int groundY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        candidate.getX(), candidate.getZ());

                if (groundY <= level.getMinBuildHeight() || groundY + sizeY >= level.getMaxBuildHeight()) {
                    continue;
                }

                BlockPos testPos = new BlockPos(candidate.getX(), groundY, candidate.getZ());

                // Only pad horizontally; vertically we only require the structure's
                // own height to be clear above the ground (terrain below is expected).
                BlockPos checkCorner = testPos.offset(-SAFE_LOCATION_PADDING, 0, -SAFE_LOCATION_PADDING);

                if (isCuboidClear(level, checkCorner, padX, sizeY, padZ)) {
                    return testPos;
                }
            }
        }
        return null;
    }

    /** Generate a ring of candidate positions at the given radius. */
    private static List<BlockPos> getSpiralRing(BlockPos centre, int radius) {
        List<BlockPos> ring = new ArrayList<>();
        if (radius == 0) {
            ring.add(centre);
            return ring;
        }
        // Walk the perimeter of the square ring
        for (int dx = -radius; dx <= radius; dx += SPIRAL_STEP) {
            ring.add(centre.offset(dx, 0, -radius));
            ring.add(centre.offset(dx, 0, radius));
        }
        for (int dz = -radius + SPIRAL_STEP; dz < radius; dz += SPIRAL_STEP) {
            ring.add(centre.offset(-radius, 0, dz));
            ring.add(centre.offset(radius, 0, dz));
        }
        return ring;
    }

    /** Returns true if the entire cuboid (padX × padY × padZ) centred at pos is air/void. */
    private static boolean isCuboidClear(ServerLevel level, BlockPos corner, int padX, int padY, int padZ) {
        int minX = corner.getX(), minY = corner.getY(), minZ = corner.getZ();
        int maxX = minX + padX - 1, maxY = minY + padY - 1, maxZ = minZ + padZ - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir() && !state.is(Blocks.VOID_AIR)) return false;
                }
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // FTB Chunks claim check
    // -------------------------------------------------------------------------

    private static PlacementResult checkFTBChunksClaims(
            ServerLevel level, BlockPos origin,
            int sizeX, int sizeZ,
            PlacementContext context) {

        int minCx = origin.getX() >> 4;
        int maxCx = (origin.getX() + sizeX - 1) >> 4;
        int minCz = origin.getZ() >> 4;
        int maxCz = (origin.getZ() + sizeZ - 1) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (FTBChunksCompat.isChunkClaimedByOther(level, cx, cz, context.allowedChunkOwners, context.targetPlayerUUID)) {
                    DeepSpaceMod.LOGGER.warn("[StructurePlacer] Chunk [{},{}] claimed by another player.", cx, cz);
                    return PlacementResult.ERROR_CHUNK_CLAIMED;
                }
            }
        }
        return null; // null means no claim conflict
    }

    // -------------------------------------------------------------------------
    // Entity / NPC spawning
    // -------------------------------------------------------------------------

    /**
     * Spawn entities from the NBT "entities" list tag.
     * Entities are placed at their relative offset from the structure origin.
     * Full NBT is applied to preserve trades, health, attributes, etc.
     *
     * Bug-fix notes:
     *   Bug 1 – Item frame item duplication:
     *     Item frames store their held item in the entity NBT ("Item" tag).
     *     If we call entity.load() and then addFreshEntity(), vanilla internally
     *     tries to call dropItem() during finalizeSpawn for frames, causing the
     *     held item to also appear on the ground.  We prevent this by calling
     *     setItem() directly after loading instead of relying on the auto-drop path.
     *     We achieve this by stripping the "Item" tag before load() and re-applying
     *     it afterwards via the ItemFrame API.
     *
     *   Bug 2 – Mob duplication:
     *     Fixed upstream: StructurePlaceSettings.setIgnoreEntities(true) ensures
     *     placeInWorld() never spawns entities; this method is the sole spawn path.
     *
     *   Bug 3 – Stale pathfinding destination:
     *     Entity NBT exported mid-movement contains Brain memories, path data, and
     *     patrol targets from the old world position.  We strip these fields before
     *     calling entity.load() so every mob/NPC starts with a clean navigation
     *     state relative to its new spawn location.
     */
    private static List<MobData> spawnEntities(ServerLevel level, BlockPos origin, CompoundTag structureNbt) {
        List<MobData> result = new ArrayList<>();
                // 🔍 HARD CHECK: does the root even contain "entities"?
                if (!structureNbt.contains("entities", Tag.TAG_LIST)) {
                        DeepSpaceMod.LOGGER.error("[SPAWN] NBT DOES NOT CONTAIN 'entities' TAG");
                        DeepSpaceMod.LOGGER.error("[SPAWN] Root keys: {}", structureNbt.getAllKeys());
                        return result;
                }
        ListTag entitiesTag = structureNbt.getList("entities", Tag.TAG_COMPOUND);
                // 🔍 CORE PIPELINE CHECKPOINT
                DeepSpaceMod.LOGGER.error("[SPAWN] entitiesTag size = {}", entitiesTag.size());
                if (entitiesTag.isEmpty()) {
                        DeepSpaceMod.LOGGER.error("[SPAWN] entitiesTag is EMPTY — entities were lost BEFORE spawn stage");
                }
          // 🔍 Inspect first few entries (avoid log spam)
                for (int i = 0; i < Math.min(entitiesTag.size(), 5); i++) {
                        CompoundTag entityEntry = entitiesTag.getCompound(i);

                        DeepSpaceMod.LOGGER.error("[SPAWN] --- Entity Entry #{} ---", i);
                        DeepSpaceMod.LOGGER.error("[SPAWN] Keys: {}", entityEntry.getAllKeys());

                        if (entityEntry.contains("nbt", Tag.TAG_COMPOUND)) {
                                CompoundTag nbt = entityEntry.getCompound("nbt");

                                DeepSpaceMod.LOGGER.error("[SPAWN] NBT Keys: {}", nbt.getAllKeys());
                                DeepSpaceMod.LOGGER.error("[SPAWN] ID: '{}'", nbt.getString("id"));
                        } else {
                                DeepSpaceMod.LOGGER.error("[SPAWN] MISSING 'nbt' TAG");
                        }

                        if (entityEntry.contains("pos", Tag.TAG_LIST)) {
                                ListTag pos = entityEntry.getList("pos", Tag.TAG_DOUBLE);
                                DeepSpaceMod.LOGGER.error("[SPAWN] pos = [{}, {}, {}]",
                                                pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
                        } else {
                                DeepSpaceMod.LOGGER.error("[SPAWN] MISSING 'pos' TAG");
                        }
                }

        for (int i = 0; i < entitiesTag.size(); i++) {
            CompoundTag entityEntry = entitiesTag.getCompound(i);
            try {
                // "pos" is a ListTag of 3 doubles (relative to structure origin)
                ListTag posTag = entityEntry.getList("pos", Tag.TAG_DOUBLE);
                double relX = posTag.getDouble(0);
                double relY = posTag.getDouble(1);
                double relZ = posTag.getDouble(2);

                // "nbt" is the full entity NBT — work on a mutable copy
                CompoundTag entityNbt = entityEntry.getCompound("nbt").copy();

                String typeId = entityNbt.getString("id");
                if (typeId.isEmpty()) continue;

                ResourceLocation entityTypeRL = new ResourceLocation(typeId);
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityTypeRL);
                if (entityType == null) {
                    DeepSpaceMod.LOGGER.warn("[StructurePlacer] Unknown entity type '{}' in structure, skipping.", typeId);
                    continue;
                }

                // Absolute world position
                double worldX = origin.getX() + relX;
                double worldY = origin.getY() + relY;
                double worldZ = origin.getZ() + relZ;

                // ── Bug 3 fix: strip stale navigation / memory data ──────────
                // These fields encode the mob's position/destination at export time
                // and cause it to navigate toward inaccessible old coordinates.
                stripStaleNavigationData(entityNbt);

                // ── Bug 1 fix: extract item-frame held item before load() ────
                // Item frames store their item as "Item" in their NBT.
                // Loading with that tag present causes vanilla to drop a copy of
                // the item on the ground during addFreshEntity().  Save it, strip
                // it, load the frame, then restore it via the API.
                CompoundTag heldItemNbt = null;
                boolean isItemFrame = typeId.equals("minecraft:item_frame")
                        || typeId.equals("minecraft:glow_item_frame");
                if (isItemFrame && entityNbt.contains("Item", Tag.TAG_COMPOUND)) {
                    heldItemNbt = entityNbt.getCompound("Item").copy();
                    entityNbt.remove("Item");
                }

                // Update position in NBT so entity.load() places it correctly
                ListTag posList = new ListTag();
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(worldX));
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(worldY));
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(worldZ));
                entityNbt.put("Pos", posList);

                // Create and load entity
                Entity entity = entityType.create(level);
                if (entity == null) continue;

                                // 🔴 CRITICAL FIX: remove UUID so Minecraft assigns a new one
                                entityNbt.remove("UUID");
                                entityNbt.remove("UUIDMost");
                                entityNbt.remove("UUIDLeast");

                //entity.load(entityNbt);
                                entity.deserializeNBT(entityNbt);
                entity.setPos(worldX, worldY, worldZ);

                // Restore rotation
                ListTag rotTag = entityNbt.getList("Rotation", Tag.TAG_FLOAT);
                if (rotTag.size() >= 2) {
                    entity.setYRot(rotTag.getFloat(0));
                    entity.setXRot(rotTag.getFloat(1));
                }

                boolean spawned = level.addFreshEntity(entity);
                if (!spawned) {
                    DeepSpaceMod.LOGGER.warn("[StructurePlacer] Failed to spawn entity {} at [{},{},{}]",
                            typeId, worldX, worldY, worldZ);
                    continue;
                }

                // ── Bug 1 fix part 2: restore held item via ItemFrame API ────
                if (isItemFrame && heldItemNbt != null && entity instanceof net.minecraft.world.entity.decoration.ItemFrame frame) {
                    try {
                        net.minecraft.world.item.ItemStack heldStack =
                            net.minecraft.world.item.ItemStack.of(heldItemNbt);
                        if (!heldStack.isEmpty()) {
                            frame.setItem(heldStack, false); // false = don't drop/play sound
                        }
                    } catch (Exception ex) {
                        DeepSpaceMod.LOGGER.warn("[StructurePlacer] Could not restore item frame item: {}", ex.getMessage());
                    }
                }

                DeepSpaceMod.LOGGER.debug("[StructurePlacer] Spawned {} at [{},{},{}]", typeId, (int)worldX, (int)worldY, (int)worldZ);

                MobData mobData = new MobData(
                        (int) relX, (int) relY, (int) relZ,
                        entity instanceof LivingEntity le ? (int) le.getHealth() : 20,
                        typeId,
                        null,
                        entity instanceof LivingEntity le ? le : null,
                        entity
                );
                result.add(mobData);

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[StructurePlacer] Failed to spawn entity #{}: {}", i, e.getMessage());
            }
        }
        return result;
    }

    /**
     * Bug 3 fix: Remove all navigation/memory fields from entity NBT that encode
     * coordinates from the world at export time.  Without this, mobs start
     * pathfinding toward their old unreachable destination (visible as legs
     * spinning while they push against a wall).
     *
     * Fields removed:
     *   Brain.memories   – villager job/home/meeting-point memories
     *   PatrolTarget     – pillager patrol waypoints
     *   patrol_target    – alternate key used by some mobs
     *   LeashTarget      – leash anchor position
     *   SleepingX/Y/Z    – bed position for villagers
     *   WanderTarget     – fox / animal wander goal
     */
    private static void stripStaleNavigationData(CompoundTag nbt) {
        // Brain memories contain BlockPos references to home/work/meeting points
        if (nbt.contains("Brain", Tag.TAG_COMPOUND)) {
            CompoundTag brain = nbt.getCompound("Brain");
            if (brain.contains("memories", Tag.TAG_COMPOUND)) {
                brain.put("memories", new CompoundTag()); // clear all memories
            }
        }
        // Patrol target (pillagers, vindicators)
        nbt.remove("PatrolTarget");
        nbt.remove("patrol_target");
        // Leash anchor (goats, animals on leads)
        nbt.remove("LeashTarget");
        // Villager sleeping position
        nbt.remove("SleepingX");
        nbt.remove("SleepingY");
        nbt.remove("SleepingZ");
        // Fox/animal wander goal target
        nbt.remove("WanderTarget");
        // Clear any explicit motion vector so the mob doesn't start flying sideways
        if (nbt.contains("Motion", Tag.TAG_LIST)) {
            ListTag motion = new ListTag();
            motion.add(net.minecraft.nbt.DoubleTag.valueOf(0));
            motion.add(net.minecraft.nbt.DoubleTag.valueOf(0));
            motion.add(net.minecraft.nbt.DoubleTag.valueOf(0));
            nbt.put("Motion", motion);
        }
    }

    // -------------------------------------------------------------------------
    // GregTech pipe/cable sync
    // -------------------------------------------------------------------------

    /**
     * BFS-scan for GT pipe/cable block entities in the structure footprint and
     * enqueue them for visual sync (same approach as CrashedShipPlacer).
     */
    private static void reinitPipeConnections(ServerLevel level, BlockPos origin,
                                               int sizeX, int sizeY, int sizeZ) {
        try {
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();

            for (int dx = -1; dx <= sizeX + 1; dx++) {
                for (int dy = -1; dy <= sizeY + 1; dy++) {
                    for (int dz = -1; dz <= sizeZ + 1; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        var be = level.getBlockEntity(pos);
                        if (be instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?>) {
                            queue.add(pos);
                        }
                    }
                }
            }

            if (queue.isEmpty()) return;

            Set<BlockPos> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                if (!visited.add(pos)) continue;
                var be = level.getBlockEntity(pos);
                if (!(be instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?>)) continue;
                be.setChanged();
                // Mark neighbors for re-connection
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    var nbe = level.getBlockEntity(neighbor);
                    if (nbe instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?>
                            && !visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            DeepSpaceMod.LOGGER.info("[StructurePlacer] Pipe BFS complete for structure at {}", origin);
        } catch (Throwable t) {
            // GT not loaded or other issue — non-fatal
            DeepSpaceMod.LOGGER.debug("[StructurePlacer] GT pipe sync skipped: {}", t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // NBT -> StructureTemplate loading
    // -------------------------------------------------------------------------

    /**
     * Load a StructureTemplate from a raw CompoundTag.
     * Uses the level's StructureTemplateManager to create and populate the template.
     */
    private static StructureTemplate loadTemplateFromNbt(ServerLevel level, CompoundTag nbt, String filename) {
        // Defensive: strip any embedded EnderIO conduit BE NBT that may be
        // inconsistent with the runtime EnderIO version.  Without this, the
        // capacitor bank / machine that ticks next to such a conduit crashes
        // the server with IllegalStateException("no node matching original
        // type") from ConduitBundle.getNodeFor.  Mutates `nbt` in place;
        // sanitization is idempotent so repeated placements are fine.
        int stripped = com.deepspace.compat.enderio.EnderIOConduitSanitizer
                .sanitizeStructureNbt(nbt);
        if (stripped > 0) {
            DeepSpaceMod.LOGGER.info(
                "[StructurePlacer] {} - sanitized {} EnderIO conduit BE NBT entries (preventing 'no node matching original type' crash)",
                filename, stripped);
        }

        StructureTemplateManager mgr = level.getStructureManager();
        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
        return template;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static long computeSectorKey(BlockPos pos) {
        int S = DSConstants.STRUCTURE_SECTOR_SIZE_CHUNKS * 16;
        int sectorX = Math.floorDiv(pos.getX(), S);
        int sectorZ = Math.floorDiv(pos.getZ(), S);
        return ((long) sectorX << 32) | (sectorZ & 0xFFFFFFFFL);
    }

    /**
     * Send a formatted placement result message to a player.
     * Used by /importship to always notify the USERNAME target player.
     *
     * Format: "[DeepSpace] [StructurePlacer] <filename> for <username> @ <dim> [x,y,z] - RESULT: <result>"
     */
    public static void sendResultMessage(ServerLevel level, String filename,
                                          String targetUsername, BlockPos pos,
                                          PlacementResult result) {
        String dimName = level.dimension().location().toString();
        String message = String.format(
                "[DeepSpace] [StructurePlacer] %s for %s @ %s [%d,%d,%d] - RESULT: %s",
                filename, targetUsername, dimName,
                pos.getX(), pos.getY(), pos.getZ(),
                result.name()
                
        );

        // Log to server console only — do not send to the player's chat
        DeepSpaceMod.LOGGER.info("{}", message);
    }

    // -------------------------------------------------------------------------
    // Lootr crate identity reset (soft dependency, reflection-based)
    // -------------------------------------------------------------------------
    //
    // BUG FIX: /importship pastes structures via StructureTemplate.placeInWorld(),
    // which deserializes each block entity's NBT verbatim -- including Lootr's
    // "tileId" (UUID) field baked into the .nbt at authoring time. Lootr keys its
    // per-player loot rolls off that tileId, NOT off world position. Since every
    // placement of the same structure file carries the SAME baked-in tileId, the
    // second (and every subsequent) /importship of the same ship produces Lootr
    // crates that alias the FIRST ship's Lootr save-data -- resulting in empty/
    // broken inventories and a ReportedException NPE on container click (Lootr's
    // container menu reads state keyed by a tileId that no longer matches this
    // block's actual dimension/position).
    //
    // Fix: after placement, walk the structure's footprint and reset every Lootr
    // block entity's identity:
    //   - tileId  -> null, so ILootBlockEntity#getTileId() lazily assigns a fresh
    //                UUID.randomUUID() the next time it's accessed
    //   - openers -> cleared, so the new crate isn't considered "already opened"
    //                by players who looted the original ship
    //   - opened  -> false, via the public setOpened() API method
    //
    // Implemented via reflection so Lootr remains a soft/optional dependency,
    // matching the existing FTBTeams reflection pattern in ImportShipCommand.
    // No-ops entirely (and only pays the Class.forName cost once) if Lootr is
    // not installed.

    private static final String LOOTR_MARKER_INTERFACE = "noobanidus.mods.lootr.api.blockentity.ILootBlockEntity";
    private static volatile Boolean lootrAvailable = null; // null = not yet checked
    private static Class<?> lootrInterfaceClass;
    private static java.lang.reflect.Method lootrSetOpenedMethod;
    private static java.lang.reflect.Method lootrGetOpenersMethod;

    private static boolean ensureLootrReflectionInit() {
        if (lootrAvailable != null) return lootrAvailable;
        synchronized (StructurePlacer.class) {
            if (lootrAvailable != null) return lootrAvailable;
            try {
                lootrInterfaceClass = Class.forName(LOOTR_MARKER_INTERFACE);
                lootrSetOpenedMethod = lootrInterfaceClass.getMethod("setOpened", boolean.class);
                lootrGetOpenersMethod = lootrInterfaceClass.getMethod("getOpeners");
                lootrAvailable = true;
                DeepSpaceMod.LOGGER.info("[StructurePlacer] Lootr detected -- crate identity reset enabled for /importship.");
            } catch (Throwable t) {
                lootrAvailable = false; // Lootr not installed -- fine, no-op forever after this.
            }
            return lootrAvailable;
        }
    }

    private static void resetLootrCrates(ServerLevel level, BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        if (!ensureLootrReflectionInit()) return;

        int resetCount = 0;
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dy = 0; dy < sizeY; dy++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    var be = level.getBlockEntity(pos);
                    if (be == null || !lootrInterfaceClass.isInstance(be)) continue;

                    try {
                        // Clear "who has opened this crate" tracking -- public API, no reflection needed.
                        Object openers = lootrGetOpenersMethod.invoke(be);
                        if (openers instanceof java.util.Set<?> set) {
                            set.clear();
                        }
                        lootrSetOpenedMethod.invoke(be, false);

                        // Force a brand-new identity so this crate no longer aliases the
                        // save-data entry of any other placed copy of this structure.
                        // "tileId" is declared directly on the concrete Lootr BE class
                        // (or an ancestor like LootrChestBlockEntity), so walk the hierarchy.
                        java.lang.reflect.Field tileIdField = findFieldInHierarchy(be.getClass(), "tileId");
                        if (tileIdField != null) {
                            tileIdField.setAccessible(true);
                            tileIdField.set(be, null); // getTileId() lazily assigns UUID.randomUUID() on next access
                        }

                        be.setChanged();
                        resetCount++;
                    } catch (Throwable t) {
                        DeepSpaceMod.LOGGER.warn("[StructurePlacer] Failed to reset Lootr crate identity at {}: {}", pos, t.getMessage());
                    }
                }
            }
        }

        if (resetCount > 0) {
            DeepSpaceMod.LOGGER.info("[StructurePlacer] Reset {} Lootr crate identit{} in structure footprint at {}",
                    resetCount, resetCount == 1 ? "y" : "ies", origin);
        }
    }

    /** Walk up the class hierarchy (stopping below Object) looking for a declared field by name. */
    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        return null;
    }
}