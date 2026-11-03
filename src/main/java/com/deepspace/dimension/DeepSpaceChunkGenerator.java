package com.deepspace.dimension;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.crash.CrashBubbleData;
import com.deepspace.crash.DSWorldData;
import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.registry.AsteroidKind;
import com.deepspace.structures.PlacementContext;
import com.deepspace.structures.StructurePlacer;
import com.deepspace.structures.StructureSectorGrid;
import com.deepspace.util.PersonalSeedHelper;
import com.deepspace.worldgen.AsteroidCandidate;
import com.deepspace.worldgen.AsteroidSectorGrid;
import com.deepspace.worldgen.AsteroidShape;
import com.deepspace.worldgen.PlanetCandidate;
import com.deepspace.worldgen.PlanetSectorGrid;
import com.deepspace.worldgen.PlanetShape;
import com.deepspace.worldgen.ore.ForgeOreResolver;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DeepSpaceChunkGenerator extends ChunkGenerator {

    public static final Codec<DeepSpaceChunkGenerator> CODEC =
        RecordCodecBuilder.create(inst -> inst.group(
            SpaceBiomeSource.CODEC.fieldOf("biome_source")
                .forGetter(g -> (SpaceBiomeSource) g.getBiomeSource())
        ).apply(inst, DeepSpaceChunkGenerator::new));


    public DeepSpaceChunkGenerator(SpaceBiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() { return CODEC; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Executor executor, Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                              RandomState randomState, ChunkAccess chunk) {}

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                              BiomeManager biomeManager, StructureManager structureManager,
                              ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk,
                                      StructureManager structureManager) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        try {
            long worldSeed = level.getSeed();

            // --- Structure pass (first, for void exclusion) -----------------
            List<StructureSectorGrid.StructureSectorCandidate> nearbyStructures =
                StructureSectorGrid.resolve(worldSeed, cx, cz);

            boolean insideStructureVoid = false;
            for (StructureSectorGrid.StructureSectorCandidate candidate : nearbyStructures) {
                double dx   = (cx * 16.0 + 8) - candidate.centerX;
                double dz   = (cz * 16.0 + 8) - candidate.centerZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist <= candidate.voidRadius) {
                    insideStructureVoid = true;
                }

                // Trigger placement for the owning chunk
                int structChunkX = candidate.centerX >> 4;
                int structChunkZ = candidate.centerZ >> 4;
                if (cx == structChunkX && cz == structChunkZ) {
                    placeStructureDeferred(level, candidate);
                }
            }

            // BUG_11: the structure void must NOT skip the planet pass, or a
            // structure's bounding box carves a slab out of any nearby planet.
            // Planets always generate; only the asteroid pass is suppressed.

            // --- Planet pass (always runs) ----------------------------------
            List<PlanetCandidate> nearbyPlanets =
                PlanetSectorGrid.resolve(worldSeed, cx, cz);

            boolean insidePlanetVoid = false;
            for (PlanetCandidate planet : nearbyPlanets) {
                double dx   = (cx * 16.0 + 8) - planet.centerX;
                double dz   = (cz * 16.0 + 8) - planet.centerZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist - 12 <= planet.radius) {
                    try {
                        PlanetShape.placeInChunk(chunk, planet.def,
                            planet.centerX, planet.centerY, planet.centerZ,
                            planet.radius, planet.shellThick, planet.seed);
                    } catch (Exception pe) {
                        DeepSpaceMod.LOGGER.error(
                            "[DeepSpace] Planet slice failed [{},{}]: {}",
                            cx, cz, pe.toString());
                    }
                }

                if (dist <= planet.voidRadius()) {
                    insidePlanetVoid = true;
                }
            }

            // --- Asteroid pass ----------------------------------------------
            if (!insidePlanetVoid && !insideStructureVoid) {
                placeAsteroidsInChunk(level, chunk, cx, cz);
            }

        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error(
                "[DeepSpace] Chunk gen error [{},{}]: {} -- leaving void", cx, cz, e.getMessage());
        }
    }

    /**
     * Defer random structure placement to the main server thread.
     * Placement needs entity spawning + SavedData access, not safe from worldgen threads.
     */
    private void placeStructureDeferred(
            WorldGenLevel level,
            StructureSectorGrid.StructureSectorCandidate candidate) {

        ServerLevel serverLevel = null;
        if (level instanceof ServerLevel sl) {
            serverLevel = sl;
        } else if (level instanceof WorldGenRegion wgr) {
            serverLevel = wgr.getLevel();
        }
        if (serverLevel == null) return;

        final ServerLevel sl     = serverLevel;
        final BlockPos origin    = new BlockPos(candidate.centerX, candidate.centerY, candidate.centerZ);
        final String   filename  = candidate.nbtFilename;
        final long     sectorKey = candidate.sectorKey;

        sl.getServer().execute(() -> {
            try {
                // Guard: only place once per sector (persisted across restarts)
                DSWorldData wd = DSWorldData.getOrCreate(sl);
                if (wd.hasStructureAtSector(sectorKey)) return;

                PlacementContext ctx = PlacementContext.worldgen();
                com.deepspace.structures.PlacementResult result =
                    StructurePlacer.placeStructure(sl, origin, filename, ctx);

                DeepSpaceMod.LOGGER.info(
                    "[DeepSpace] Random structure {} at [{},{},{}] -> {}",
                    filename, origin.getX(), origin.getY(), origin.getZ(), result.name());

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn(
                    "[DeepSpace] Random structure placement failed for {} at {}: {}",
                    filename, origin, e.getMessage());
            }
        });
    }

    private void placeAsteroidsInChunk(WorldGenLevel level, ChunkAccess chunk, int cx, int cz) {
        long effectiveSeed = level.getSeed();

        ServerLevel serverLevel = null;
        if (level instanceof ServerLevel sl) {
            serverLevel = sl;
        } else if (level instanceof WorldGenRegion wgr) {
            serverLevel = wgr.getLevel();
        }

        if (serverLevel != null) {
            CrashBubbleData nearest = findNearestBubble(serverLevel, cx, cz);
            if (nearest != null) effectiveSeed = nearest.personalSeed;
            if (com.deepspace.compat.FTBChunksCompat.isChunkClaimed(serverLevel, cx, cz)) return;
        }

        // Tags (#forge:ores) bind at server start; lazy-ensure for first-chunk safety.
        if (!ForgeOreResolver.ready()) {
            ForgeOreResolver.ensureBuilt(level.getLevel().registryAccess());
        }

        List<AsteroidCandidate> candidates =
            AsteroidSectorGrid.resolve(effectiveSeed, cx, cz);

        // BUG_1: regular asteroids must never intersect a crashed ship. This
        // also guarantees the ship is never embedded in a regular asteroid.
        java.util.Collection<CrashBubbleData> bubbles =
            serverLevel != null
                ? DSWorldData.getOrCreate(serverLevel).getBubbles().values()
                : java.util.List.<CrashBubbleData>of();

        for (AsteroidCandidate cand : candidates) {
            boolean hitsShip = false;
            for (CrashBubbleData b : bubbles) {
                if (b.shipIntersectsAabb(cand.minX(), cand.centerY - cand.ry, cand.minZ(),
                                         cand.maxX(), cand.centerY + cand.ry, cand.maxZ(), 4)) {
                    hitsShip = true;
                    break;
                }
            }
            if (hitsShip) continue;

            AsteroidShape.placeCandidate(chunk, cand);

            // Defer GT bedrock veins to the main thread, ONCE, from the chunk
            // that owns the asteroid centre (an asteroid spans several chunks).
            if (serverLevel == null) continue;
            int ownerCx = cand.centerX >> 4;
            int ownerCz = cand.centerZ >> 4;
            if (cx != ownerCx || cz != ownerCz) continue;

            final ServerLevel sl = serverLevel;
            final AsteroidDefinition def = cand.def;
            final int vx = cand.centerX, vz = cand.centerZ;

            if (def.hasDrillingVein) {
                sl.getServer().execute(() ->
                    com.deepspace.compat.GTCEuCompat.placeFluidVein(sl, def, vx, vz));
            }

            if (def.kind == AsteroidKind.ROCK) {
                final String mat = ForgeOreResolver
                    .pickPrimaryGtMaterial(cand.host, cand.oreSeed);
                if (mat != null) {
                    sl.getServer().execute(() ->
                        com.deepspace.compat.GTCEuCompat.placeOreVein(sl, mat, vx, vz));
                }
            } else if (!def.ores.isEmpty()) {
                sl.getServer().execute(() ->
                    com.deepspace.compat.GTCEuCompat.placeOreVein(sl, def, vx, vz));
            }
        }
    }

    private CrashBubbleData findNearestBubble(ServerLevel level, int cx, int cz) {
        DSWorldData wd = DSWorldData.getOrCreate(level);
        CrashBubbleData nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Map.Entry<UUID, CrashBubbleData> e : wd.getBubbles().entrySet()) {
            CrashBubbleData b = e.getValue();
            double dx = cx - b.centerChunkX, dz = cz - b.centerChunkZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < nearestDist) { nearestDist = dist; nearest = b; }
        }
        return nearest;
    }

    @Override public int getGenDepth()  { return 384; }
    @Override public int getSeaLevel()  { return -63; }
    @Override public int getMinY()      { return -64; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                              LevelHeightAccessor level, RandomState randomState) { return 0; }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
                                      RandomState randomState) {
        return new NoiseColumn(getMinY(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("DeepSpace asteroid generator v2.0");
    }
}
