package com.deepspace.api;

import com.deepspace.structures.RegisteredStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import java.util.List;
import java.util.Optional;

/**
 * Public API interface for querying randomly placed NBT structures in Deep Space.
 *
 * Allows future mods (e.g. CC:Tweaked peripherals) to retrieve structure locations
 * deterministically, including structures on ungenerated chunks.
 *
 * Access the singleton via:
 *   com.deepspace.structures.StructureRadiusMapImpl.INSTANCE
 */
public interface IStructureRadiusMap {

    /**
     * Returns ALL registered random structures within a radius.
     *
     * MUST include:
     *  - already generated structures (loaded chunks)
     *  - structures that WILL generate in future chunks (deterministic prediction)
     *
     * Results MUST be deterministic based on world seed.
     *
     * @param level      Level accessor (dimension source)
     * @param worldSeed  The world seed (from level.getSeed())
     * @param center     Search origin
     * @param radius     Search radius in blocks
     * @return list of structures guaranteed to exist or will generate
     */
    List<RegisteredStructure> getStructuresInRadius(
            LevelAccessor level,
            long worldSeed,
            BlockPos center,
            int radius
    );

    /**
     * Deterministically calculates structures WITHOUT requiring chunks
     * to be generated or loaded.
     *
     * Used by map previews, scanners, quests, CC:Tweaked peripherals, etc.
     *
     * @param dimension  Dimension ResourceLocation (e.g. "deepspace:deep_space")
     * @param worldSeed  The world seed
     * @param center     Search origin
     * @param radius     Search radius in blocks
     * @return list of structures that will generate at these positions
     */
    List<RegisteredStructure> predictStructures(
            ResourceLocation dimension,
            long worldSeed,
            BlockPos center,
            int radius
    );

    /**
     * Returns structures currently loaded in memory.
     * (Already generated chunks only)
     */
    List<RegisteredStructure> getLoadedStructures(ServerLevel level);

    /**
     * Returns nearest structure to position.
     *
     * @param level       Level accessor
     * @param origin      Search origin
     * @param maxDistance Maximum search distance in blocks
     * @return Optional containing nearest structure, or empty if none found
     */
    Optional<RegisteredStructure> findNearestStructure(
            LevelAccessor level,
            BlockPos origin,
            int maxDistance
    );

    /**
     * Returns structures belonging to a specific template (.nbt filename).
     *
     * @param templateId ResourceLocation with path = filename without .nbt extension,
     *                   e.g. new ResourceLocation("deepspace", "abandoned_station")
     */
    List<RegisteredStructure> getStructuresByTemplate(
            ResourceLocation templateId
    );

    /**
     * Called by worldgen / StructurePlacer when a structure is placed.
     * Implementation must register structure in the realtime map.
     */
    void registerStructure(RegisteredStructure structure);

    /**
     * Called when a structure is unloaded or removed.
     */
    void unregisterStructure(RegisteredStructure structure);

    /**
     * Performs realtime mob validation.
     *
     * Implementation MUST:
     *  - iterate MobData LivingEntity references
     *  - remove mobs that are confirmed dead (isDeadOrDying() returns true and entity is loaded)
     *  - keep mobs that merely despawned temporarily (chunk unloaded)
     *
     * Expected to run periodically (server tick task, ~every 100 ticks).
     */
    void tickMobValidation(ServerLevel level);
}
