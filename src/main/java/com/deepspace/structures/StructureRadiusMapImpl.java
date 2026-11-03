package com.deepspace.structures;

import com.deepspace.DeepSpaceMod;
import com.deepspace.api.IStructureRadiusMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Singleton implementation of {@link IStructureRadiusMap}.
 *
 * Access via {@code StructureRadiusMapImpl.INSTANCE}.
 *
 * Thread-safe: loadedStructures is a ConcurrentHashMap. Mob validation runs
 * on the server tick thread.
 */
public final class StructureRadiusMapImpl implements IStructureRadiusMap {

    /** Public singleton. */
    public static final IStructureRadiusMap INSTANCE = new StructureRadiusMapImpl();

    /** dimension-id -> list of registered structures in that dimension. */
    private final ConcurrentHashMap<String, List<RegisteredStructure>> loadedStructures =
            new ConcurrentHashMap<>();

    /** Track last mob-validation tick to run every ~100 ticks. */
    private int tickCounter = 0;

    private StructureRadiusMapImpl() {}

    // -------------------------------------------------------------------------
    // IStructureRadiusMap
    // -------------------------------------------------------------------------

    @Override
    public List<RegisteredStructure> getStructuresInRadius(
            LevelAccessor level,
            long worldSeed,
            BlockPos center,
            int radius) {

        String dimId = level.dimensionType().toString();
        // Use dimension key if available from ServerLevel
        if (level instanceof ServerLevel sl) {
            dimId = sl.dimension().location().toString();
        }

        // Loaded (already generated) structures in this dimension
        List<RegisteredStructure> loaded = getLoadedStructuresForDim(dimId, center, radius);

        // Predicted (not yet generated) structures — dedup by sectorKey
        Set<Long> loadedSectorKeys = loaded.stream()
                .map(s -> s.sectorKey)
                .collect(Collectors.toSet());

        ResourceLocation dimRL;
        try {
            dimRL = new ResourceLocation(dimId);
        } catch (Exception e) {
            dimRL = new ResourceLocation("deepspace", "deep_space");
        }

        List<RegisteredStructure> predicted = predictStructures(dimRL, worldSeed, center, radius);
        List<RegisteredStructure> combined = new ArrayList<>(loaded);
        for (RegisteredStructure pred : predicted) {
            if (!loadedSectorKeys.contains(pred.sectorKey)) {
                combined.add(pred);
            }
        }
        return combined;
    }

    @Override
    public List<RegisteredStructure> predictStructures(
            ResourceLocation dimension,
            long worldSeed,
            BlockPos center,
            int radius) {

        List<RegisteredStructure> results = new ArrayList<>();
        DeepSpaceStructureRegistry registry = DeepSpaceStructureRegistry.getInstance();
        List<String> pool = registry.getPool();
        if (pool.isEmpty()) return results;

        // Determine which sectors overlap the search radius
        int S = com.deepspace.DSConstants.STRUCTURE_SECTOR_SIZE_CHUNKS * 16; // sector size in blocks
        int minSX = Math.floorDiv(center.getX() - radius, S) - 1;
        int maxSX = Math.floorDiv(center.getX() + radius, S) + 1;
        int minSZ = Math.floorDiv(center.getZ() - radius, S) - 1;
        int maxSZ = Math.floorDiv(center.getZ() + radius, S) + 1;

        Set<Long> seen = new HashSet<>();

        for (int sx = minSX; sx <= maxSX; sx++) {
            for (int sz = minSZ; sz <= maxSZ; sz++) {
                StructureSectorGrid.StructureSectorCandidate candidate =
                        StructureSectorGrid.resolveSector(worldSeed, sx, sz, pool);
                if (candidate == null) continue;

                // Distance from center
                double dx = candidate.centerX - center.getX();
                double dz = candidate.centerZ - center.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;

                if (!seen.add(candidate.sectorKey)) continue;

                // Build a predicted RegisteredStructure (no real bounds yet)
                StructureTemplateEntry entry = registry.getEntry(candidate.nbtFilename);
                if (entry == null) entry = new StructureTemplateEntry(candidate.nbtFilename, true);

                StructureLocation loc = new StructureLocation(
                        dimension.toString(),
                        candidate.centerX,
                        candidate.centerY,
                        candidate.centerZ
                );

                // Populate mob list from the NBT template so scanners report entities
                // for structures that have not been chunk-loaded (and thus not spawned) yet.
                CompoundTag templateNbt = registry.getTemplate(candidate.nbtFilename);
                if (templateNbt != null) {
                    loc.mobs.addAll(readEntitiesFromNbt(templateNbt));
                }

                // Estimate bounds from cached template size
                int[] size = registry.getTemplateSize(candidate.nbtFilename);
                net.minecraft.world.level.levelgen.structure.BoundingBox estimatedBounds = null;
                if (size[0] > 0) {
                    estimatedBounds = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                            candidate.centerX - size[0] / 2,
                            candidate.centerY - size[1] / 2,
                            candidate.centerZ - size[2] / 2,
                            candidate.centerX + size[0] / 2,
                            candidate.centerY + size[1] / 2,
                            candidate.centerZ + size[2] / 2
                    );
                }

                results.add(new RegisteredStructure(loc, entry, estimatedBounds, candidate.sectorKey));
            }
        }
        return results;
    }

    @Override
    public List<RegisteredStructure> getLoadedStructures(ServerLevel level) {
        String dimId = level.dimension().location().toString();
        return getLoadedStructuresForDim(dimId, null, Integer.MAX_VALUE);
    }

    @Override
    public Optional<RegisteredStructure> findNearestStructure(
            LevelAccessor level,
            BlockPos origin,
            int maxDistance) {

        String dimId;
        long worldSeed = 0L;
        if (level instanceof ServerLevel sl) {
            dimId = sl.dimension().location().toString();
            worldSeed = sl.getSeed();
        } else {
            dimId = "deepspace:deep_space";
        }

        List<RegisteredStructure> candidates;
        try {
            ResourceLocation dimRL = new ResourceLocation(dimId);
            candidates = getStructuresInRadius(level, worldSeed, origin, maxDistance);
        } catch (Exception e) {
            candidates = new ArrayList<>();
        }

        return candidates.stream()
                .min(Comparator.comparingDouble(s -> {
                    double dx = s.location.x - origin.getX();
                    double dy = s.location.y - origin.getY();
                    double dz = s.location.z - origin.getZ();
                    return dx * dx + dy * dy + dz * dz;
                }));
    }

    @Override
    public List<RegisteredStructure> getStructuresByTemplate(ResourceLocation templateId) {
        String filename = templateId.getPath() + ".nbt";
        List<RegisteredStructure> results = new ArrayList<>();
        for (List<RegisteredStructure> list : loadedStructures.values()) {
            for (RegisteredStructure s : list) {
                if (s.template.nbtFilename.equals(filename)) {
                    results.add(s);
                }
            }
        }
        return results;
    }

    @Override
    public void registerStructure(RegisteredStructure structure) {
        String dimId = structure.location.dimensionName;
        loadedStructures.computeIfAbsent(dimId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(structure);
        DeepSpaceMod.LOGGER.debug("[StructureRadiusMapImpl] Registered {} in {}", structure, dimId);
    }

    @Override
    public void unregisterStructure(RegisteredStructure structure) {
        String dimId = structure.location.dimensionName;
        List<RegisteredStructure> list = loadedStructures.get(dimId);
        if (list != null) {
            list.remove(structure);
        }
    }

    @Override
    public void tickMobValidation(ServerLevel level) {
        tickCounter++;
        if (tickCounter < 100) return;
        tickCounter = 0;

        String dimId = level.dimension().location().toString();
        List<RegisteredStructure> structs = loadedStructures.get(dimId);
        if (structs == null) return;

        synchronized (structs) {
            for (RegisteredStructure structure : structs) {
                List<MobData> mobs = structure.location.mobs;
                mobs.removeIf(mob -> {
                    // Keep mob if no entity reference
                    if (mob.entityBase == null && mob.entityLiving == null) return false;

                    net.minecraft.world.entity.Entity ent =
                            mob.entityLiving != null ? mob.entityLiving : mob.entityBase;

                    // Only remove if entity is confirmed loaded AND dead
                    // If chunk is unloaded, entity.isAddedToWorld() returns false — keep the mob
                    if (!ent.isAddedToWorld()) return false;

                    if (ent.isRemoved()) {
                        DeepSpaceMod.LOGGER.debug(
                                "[StructureRadiusMapImpl] Removing dead mob {} from structure {}",
                                mob.entity_type, structure.template.nbtFilename);
                        return true;
                    }
                    if (mob.entityLiving != null && mob.entityLiving.isDeadOrDying()) {
                        DeepSpaceMod.LOGGER.debug(
                                "[StructureRadiusMapImpl] Removing dying mob {} from structure {}",
                                mob.entity_type, structure.template.nbtFilename);
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private List<RegisteredStructure> getLoadedStructuresForDim(
            String dimId, BlockPos center, int radius) {
        List<RegisteredStructure> list = loadedStructures.get(dimId);
        if (list == null) return new ArrayList<>();

        if (center == null || radius == Integer.MAX_VALUE) {
            return new ArrayList<>(list);
        }

        List<RegisteredStructure> result = new ArrayList<>();
        int r2 = radius * radius;
        for (RegisteredStructure s : list) {
            int dx = s.location.x - center.getX();
            int dz = s.location.z - center.getZ();
            if (dx * dx + dz * dz <= r2) {
                result.add(s);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // NBT entity parsing (predicted / unloaded structures)
    // -------------------------------------------------------------------------

    /**
     * Parses entity definitions from a raw structure NBT template and returns
     * {@link MobData} instances populated with relative spawn position, entity
     * type, and NBT-stored health.
     *
     * {@code entityLiving} and {@code entityBase} are {@code null} because the
     * structure has not been placed yet — callers (e.g.
     * {@link ShipScannerBlockEntity#resolveMobHealth}) fall back to the stored
     * {@code health} field when the live reference is absent.
     *
     * @param structureNbt  The raw {@link CompoundTag} of the structure template.
     * @return              Possibly-empty list; never {@code null}.
     */
    private static List<MobData> readEntitiesFromNbt(CompoundTag structureNbt) {
        List<MobData> result = new ArrayList<>();
        if (!structureNbt.contains("entities", Tag.TAG_LIST)) return result;

        ListTag entitiesTag = structureNbt.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entitiesTag.size(); i++) {
            CompoundTag entityEntry = entitiesTag.getCompound(i);
            try {
                // "pos" — 3 doubles relative to the NBT template origin, matching
                //          the relX/relY/relZ convention used by StructurePlacer.
                ListTag posTag = entityEntry.getList("pos", Tag.TAG_DOUBLE);
                if (posTag.size() < 3) continue;

                int relX = (int) posTag.getDouble(0);
                int relY = (int) posTag.getDouble(1);
                int relZ = (int) posTag.getDouble(2);

                // "nbt" — full serialised entity data; "id" is the registry key.
                CompoundTag entityNbt = entityEntry.getCompound("nbt");
                String typeId = entityNbt.getString("id");
                if (typeId.isEmpty()) continue;

                // "Health" is a float in the same units as LivingEntity.getHealth(),
                // so resolveMobHealth() will return the correct value for both
                // predicted and live entities.
                float health = entityNbt.contains("Health", Tag.TAG_FLOAT)
                        ? entityNbt.getFloat("Health")
                        : 20.0f;

                // Extract instance tags baked into the template at structure save time.
                // These include faction tags like "pirate", "scav", etc.
                List<String> tags = new ArrayList<>();
                if (entityNbt.contains("Tags", Tag.TAG_LIST)) {
                    ListTag tagsNbt = entityNbt.getList("Tags", Tag.TAG_STRING);
                    for (int t = 0; t < tagsNbt.size(); t++) tags.add(tagsNbt.getString(t));
                }

                MobData mob = new MobData(
                        relX, relY, relZ,
                        (int) health,
                        typeId,
                        null,   // StructureEntityInfo — not available without a placed template
                        null,   // entityLiving       — not yet spawned
                        null    // entityBase         — not yet spawned
                );
                mob.nbtTags = tags.isEmpty() ? null : tags;
                result.add(mob);
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.debug(
                        "[StructureRadiusMapImpl] Could not parse entity #{} from NBT template: {}",
                        i, e.getMessage());
            }
        }
        return result;
    }
}
