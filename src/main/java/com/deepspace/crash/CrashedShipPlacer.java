package com.deepspace.crash;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.blocks.BreederReactorBlockEntity;
import com.deepspace.compat.FTBChunksCompat;
import com.deepspace.init.DSBlocks;
import com.deepspace.registry.AsteroidDefinition;
import com.deepspace.registry.AsteroidRegistry;
import com.deepspace.util.PersonalSeedHelper;
import com.deepspace.worldgen.AsteroidShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;



import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

// GTCEu pipe/cable block entity base
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;

/**
 * CrashedShipPlacer: loads crashed_ship.nbt and guarantees pipe/cable visual sync.
 */
public final class CrashedShipPlacer {

    public static final int SHIP_W = 15;
    public static final int SHIP_D = 15;
    public static final int SHIP_INTERIOR_HEIGHT = 9;

    private static final int STARTER_ASTEROID_RADIUS = 14;

    /** Pending pipe sync queue */
    private static final ConcurrentLinkedQueue<PendingPipeSync> PENDING_SYNCS =
            new ConcurrentLinkedQueue<>();

    /** 300 ticks timeout for visual pipe sync */
    private static final int SYNC_TIMEOUT_TICKS = 300;

    // Cached reflection handles -- looked up once on first use, never again.
    // Avoids getDeclaredMethod + setAccessible overhead on every tick/pipe.
    private static volatile java.lang.reflect.Method cachedGetPlayers = null;
    private static volatile boolean getPlayersFailed = false;
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, java.lang.reflect.Method>
        cachedSyncNow = new java.util.concurrent.ConcurrentHashMap<>();

	// reinit ad astra machines etc Universal Strategy
	private static void universalMachineInit(ServerLevel level, BlockPos origin) {

		DeepSpaceMod.LOGGER.info("[StructureInit] Starting universal machine activation");

		List<BlockEntity> machines = new ArrayList<>();

		// ---- PASS 1: Collect block entities ----
		for (int dx = -3; dx <= SHIP_W + 3; dx++)
		for (int dy = -1; dy <= SHIP_INTERIOR_HEIGHT + 3; dy++)
		for (int dz = -3; dz <= SHIP_D + 3; dz++) {

			BlockPos pos = origin.offset(dx, dy, dz);
			BlockEntity be = level.getBlockEntity(pos);

			if (be != null) {
				machines.add(be);
			}
		}

		// ---- PASS 2: Force lifecycle restore ----
		for (BlockEntity be : machines) {
			try {
				be.setLevel(level);

				// critical for Ad Astra / IF / Mekanism
				if (!be.hasLevel()) {
					be.setLevel(level);
				}
				be.onLoad();

				// rebuild Forge + mod capabilities
				be.invalidateCaps();
				be.reviveCaps();

			} catch (Exception e) {
				DeepSpaceMod.LOGGER.warn(
					"[StructureInit] BE revive failed at {}",
					be.getBlockPos()
				);
			}
		}

		// ---- PASS 3: Neighbor updates (network rebuilds) ----
		for (BlockEntity be : machines) {

			BlockPos pos = be.getBlockPos();
			BlockState state = level.getBlockState(pos);
			Block block = state.getBlock();

			try {
				level.updateNeighborsAt(pos, block);

				// Create + Mekanism rely on this specifically
				level.sendBlockUpdated(pos, state, state, 3);

			} catch (Exception ignored) {}
		}

		// ---- PASS 4: Delayed activation tick ----
		// MANY mods finalize setup one tick later
		level.getServer().execute(() -> {

			for (BlockEntity be : machines) {
				try {
					be.setChanged();

					BlockPos pos = be.getBlockPos();
					BlockState state = level.getBlockState(pos);

					// simulate placement refresh safely
					state.getBlock().onPlace(state, level, pos, state, false);

				} catch (Exception ignored) {}
			}

			DeepSpaceMod.LOGGER.info("[StructureInit] Machine activation complete");
		});
	}

    // ---------------------- PendingPipeSync record ----------------------
    private record PendingPipeSync(
            ServerLevel level,
            Map<BlockPos, Set<ChunkPos>> pipesByChunk,
            int startTick
    ) {}

    // ---------------------- Public entry point ----------------------
    /**
     * Places the crashed ship as a FLOATING wreck in open space (BUG_1: no
     * starter asteroid beneath it, no footprint carving — the ship and
     * asteroids must never intersect). Returns the set of chunks the ship
     * occupies so the caller can force-generate + resend them (BUG_2).
     */
    public static java.util.Set<ChunkPos> place(ServerLevel level, CrashBubbleData bubble) {
        java.util.Set<ChunkPos> touched = new java.util.HashSet<>();

        // Force-load chunks around crash site
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                level.getChunk(bubble.centerChunkX + dx, bubble.centerChunkZ + dz);

        int baseX = bubble.centerChunkX * 16 + 8;
        int baseZ = bubble.centerChunkZ * 16 + 8;

        // Float the ship at a deterministic band Y. No asteroid is placed under
        // it; asteroid generation excludes the ship AABB so nothing intersects.
        int shipY = (DSConstants.ASTEROID_BAND_Y_MIN + DSConstants.ASTEROID_BAND_Y_MAX) / 2;
        BlockPos origin = new BlockPos(baseX, shipY, baseZ);

        // Record the ship's chunk span for resend (footprint + small margin).
        for (int x = origin.getX() - 2; x <= origin.getX() + SHIP_W + 2; x += 4)
            for (int z = origin.getZ() - 2; z <= origin.getZ() + SHIP_D + 2; z += 4)
                touched.add(new ChunkPos(x >> 4, z >> 4));

        if (isShipFootprintClaimed(level, origin)) {
            DeepSpaceMod.LOGGER.error(
                    "[CrashedShipPlacer] Ship footprint at {} overlaps a claimed chunk -- aborting.",
                    origin);
            return touched;
        }

        StructureTemplateManager mgr = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = mgr.get(DSConstants.CRASHED_SHIP_STRUCTURE);

        if (templateOpt.isEmpty()) {
            DeepSpaceMod.LOGGER.warn("[CrashedShipPlacer] crashed_ship.nbt not found -- fallback");
            placeBareFallback(level, origin, bubble);
            recordUpdatedBubble(level, bubble, origin);
            return touched;
        }

        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings();

        try {
            template.placeInWorld(level, origin, origin, settings,
                    level.getRandom(), 2);
            DeepSpaceMod.LOGGER.info("[CrashedShipPlacer] Placed floating ship at {}", origin);
            reinitPipeConnections(level, origin);

            universalMachineInit(level, origin);

            initMachines(level, origin);
            MeDriveInitialiser.injectCells(level, origin);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[CrashedShipPlacer] Placement failed: {}", e.getMessage());
            placeBareFallback(level, origin, bubble);
        }

        recordUpdatedBubble(level, bubble, origin);
        return touched;
    }

    // ---------------------- Overworld ground placement ----------------------
    /**
     * Places the crashed ship in the Overworld with its hull resting flush on
     * the terrain — zero-gap, not floating.
     *
     * Ground detection scans every column under the 15x15 footprint using
     * {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} and takes the HIGHEST
     * surface found. The ship's Y origin (its bottom layer) is set to that
     * height. Every column that sits lower than that chosen height is then
     * padded up to it with stone, so there is no air pocket anywhere under the
     * hull -- the ship never floats over a dip, and never gets buried by a
     * rise, it simply sits on a levelled pad flush with the highest point of
     * the ground it landed on.
     *
     * Returns the set of chunks the ship occupies so the caller can
     * force-generate + resend them (same BUG_2 fix as {@link #place}).
     */
    public static java.util.Set<ChunkPos> placeOnGround(ServerLevel level, CrashBubbleData bubble) {
        java.util.Set<ChunkPos> touched = new java.util.HashSet<>();

        // Force-load chunks around crash site
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                level.getChunk(bubble.centerChunkX + dx, bubble.centerChunkZ + dz);

        int baseX = bubble.centerChunkX * 16 + 8;
        int baseZ = bubble.centerChunkZ * 16 + 8;

        // Scan the whole footprint and use the highest surface found -- this
        // guarantees the ship never gets buried by a hill/tree/rock that only
        // exists at one corner.
        int groundY = Integer.MIN_VALUE;
        for (int x = baseX; x < baseX + SHIP_W; x++) {
            for (int z = baseZ; z < baseZ + SHIP_D; z++) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (h > groundY) groundY = h;
            }
        }

        int shipY = groundY;
        BlockPos origin = new BlockPos(baseX, shipY, baseZ);

        // Pad every column under the footprint up to shipY-1 with stone so
        // there is no air gap anywhere between the hull's bottom layer and
        // the terrain -- fills dips/valleys under the ship rather than
        // leaving it flush only at the single highest point.
        for (int x = baseX; x < baseX + SHIP_W; x++) {
            for (int z = baseZ; z < baseZ + SHIP_D; z++) {
                int colTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int y = colTop; y < shipY; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }

        // Record the ship's chunk span for resend (footprint + small margin).
        for (int x = origin.getX() - 2; x <= origin.getX() + SHIP_W + 2; x += 4)
            for (int z = origin.getZ() - 2; z <= origin.getZ() + SHIP_D + 2; z += 4)
                touched.add(new ChunkPos(x >> 4, z >> 4));

        if (isShipFootprintClaimed(level, origin)) {
            DeepSpaceMod.LOGGER.error(
                    "[CrashedShipPlacer] Ship footprint at {} overlaps a claimed chunk -- aborting.",
                    origin);
            return touched;
        }

        StructureTemplateManager mgr = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = mgr.get(DSConstants.CRASHED_SHIP_STRUCTURE);

        if (templateOpt.isEmpty()) {
            DeepSpaceMod.LOGGER.warn("[CrashedShipPlacer] crashed_ship.nbt not found -- fallback");
            placeBareFallback(level, origin, bubble);
            recordUpdatedBubble(level, bubble, origin);
            return touched;
        }

        StructureTemplate template = templateOpt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings();

        try {
            template.placeInWorld(level, origin, origin, settings,
                    level.getRandom(), 2);
            DeepSpaceMod.LOGGER.info(
                    "[CrashedShipPlacer] Placed grounded ship at {} (groundY={})", origin, groundY);
            reinitPipeConnections(level, origin);

            universalMachineInit(level, origin);

            initMachines(level, origin);
            MeDriveInitialiser.injectCells(level, origin);
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.error("[CrashedShipPlacer] Placement failed: {}", e.getMessage());
            placeBareFallback(level, origin, bubble);
        }

        recordUpdatedBubble(level, bubble, origin);
        return touched;
    }

    /** Store the true SW-corner ship origin so shipCentre/shipAabb are correct. */
    private static void recordUpdatedBubble(ServerLevel level, CrashBubbleData bubble,
                                            BlockPos origin) {
        CrashBubbleData updated = new CrashBubbleData(
                bubble.playerUUID,
                bubble.centerChunkX,
                bubble.centerChunkZ,
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                bubble.personalSeed);
        DSWorldData.getOrCreate(level).addBubble(updated);
        DeepSpaceMod.LOGGER.info(
                "[CrashedShipPlacer] Bubble ship origin [{},{},{}] (floating)",
                updated.shipOriginX, updated.shipOriginY, updated.shipOriginZ);
    }

    // ---------------------- Starter asteroid ----------------------
    private static void guaranteeStarterAsteroid(ServerLevel level,
                                                 CrashBubbleData bubble,
                                                 int cx, int cz) {
        AsteroidDefinition def = AsteroidRegistry.getAsteroid("stone_common");
        if (def == null) {
            var zone1 = AsteroidRegistry.getAsteroidsForZone("zone_1");
            if (!zone1.isEmpty()) def = zone1.get(0);
        }
        if (def == null) {
            DeepSpaceMod.LOGGER.error("[CrashedShipPlacer] Cannot find starter asteroid definition");
            return;
        }

        int asteroidCentreY = Math.max(
                DSConstants.ASTEROID_BAND_Y_MIN + STARTER_ASTEROID_RADIUS + 5,
                Math.min(65, DSConstants.ASTEROID_BAND_Y_MAX - STARTER_ASTEROID_RADIUS - 5));

        long seed = PersonalSeedHelper.chunkSeed(bubble.personalSeed, bubble.centerChunkX,
                bubble.centerChunkZ) ^ 0x5741CEDA5E201D1L;

        ChunkAccess chunk = level.getChunk(bubble.centerChunkX, bubble.centerChunkZ);
        AsteroidShape.place(chunk, def, cx, asteroidCentreY, cz, seed);

        for (int ddx = -1; ddx <= 1; ddx++) {
            for (int ddz = -1; ddz <= 1; ddz++) {
                if (ddx == 0 && ddz == 0) continue;
                ChunkAccess neighbour = level.getChunk(
                        bubble.centerChunkX + ddx, bubble.centerChunkZ + ddz);
                AsteroidShape.place(neighbour, def, cx, asteroidCentreY, cz, seed);
            }
        }

        DeepSpaceMod.LOGGER.info(
                "[CrashedShipPlacer] Starter asteroid placed: centre [{},{},{}] r={}",
                cx, asteroidCentreY, cz, STARTER_ASTEROID_RADIUS);

        // Always place a copper-ore asteroid within 4 chunks of the crash site.
        placeStarterCopperAsteroid(level, bubble);
    }

    /**
     * Places a large stone asteroid whose interior is mostly {@code minecraft:copper_ore}
     * at a deterministic but pseudo-random position within a 4-chunk (64-block) radius
     * of the player's crashed ship centre.
     *
     * Composition:
     *  outer shell  → minecraft:stone       (identical to a normal stone asteroid)
     *  inner fill   → minecraft:copper_ore  (majority of the asteroid volume)
     *  scattered    → gtceu copper / chalcopyrite / bornite ore variants
     *
     * Placement:
     *  The angular direction and distance (32–60 blocks) are derived from the player's
     *  personal seed, making the position unique per player but fully deterministic on
     *  re-load.  The 3×3 chunk grid call mirrors guaranteeStarterAsteroid, ensuring the
     *  asteroid is rendered correctly regardless of which chunk boundary it straddles.
     */
    private static void placeStarterCopperAsteroid(ServerLevel level, CrashBubbleData bubble) {

        // Stone outer shell + copper_ore inner fill so the bulk of the volume is ore.
        AsteroidDefinition copperDef = new AsteroidDefinition(
            "starter_copper",
            "zone_0",
            0,          // not placed by random spawn weight — explicit only
            "copper",
            List.of(new ResourceLocation("minecraft", "stone")),
            List.of(new ResourceLocation("minecraft", "copper_ore")),
            List.of(
                new ResourceLocation("gtceu", "stone_copper_ore"),
                new ResourceLocation("gtceu", "stone_chalcopyrite_ore"),
                new ResourceLocation("gtceu", "stone_bornite_ore")
            ),
            null,   // no fluid vein
            false,
            0L,
            false   // not gravity-layered
        );

        // Derive a stable seed from the player's personal seed.
        long copperSeed = PersonalSeedHelper.chunkSeed(
            bubble.personalSeed, bubble.centerChunkX, bubble.centerChunkZ)
            ^ 0xC09927A5E4B11C03L;

        // Use the seed to deterministically pick an angle and a distance within
        // 4 chunks (64 blocks).  Minimum 32 blocks keeps it clear of the stone
        // starter asteroid at the ship centre; maximum 60 blocks stays inside
        // the 4-chunk boundary with margin for the asteroid radius.
        Random rng = new Random(copperSeed);
        double angle      = rng.nextDouble() * 2.0 * Math.PI;
        int    distBlocks = 32 + rng.nextInt(29); // [32, 60]

        int shipCentreX = bubble.centerChunkX * 16 + 8;
        int shipCentreZ = bubble.centerChunkZ * 16 + 8;

        int copperX = shipCentreX + (int) (Math.cos(angle) * distBlocks);
        int copperZ = shipCentreZ + (int) (Math.sin(angle) * distBlocks);
        int copperY = Math.max(
            DSConstants.ASTEROID_BAND_Y_MIN + 10,
            Math.min(65, DSConstants.ASTEROID_BAND_Y_MAX - 10));

        // AsteroidShape.place() operates per-chunk. Call it for every chunk in the
        // 3×3 grid around the asteroid centre — mirroring guaranteeStarterAsteroid —
        // so no part of the asteroid is silently clipped at a chunk boundary.
        int centreChunkX = copperX >> 4;
        int centreChunkZ = copperZ >> 4;

        for (int ddx = -1; ddx <= 1; ddx++) {
            for (int ddz = -1; ddz <= 1; ddz++) {
                ChunkAccess chunk = level.getChunk(centreChunkX + ddx, centreChunkZ + ddz);
                AsteroidShape.place(chunk, copperDef, copperX, copperY, copperZ, copperSeed);
            }
        }

        DeepSpaceMod.LOGGER.info(
            "[CrashedShipPlacer] Starter copper asteroid placed at [{},{},{}] " +
            "(angle {}deg / {} blocks from ship centre)",
            copperX, copperY, copperZ,
            (int) Math.toDegrees(angle), distBlocks);
    }

    // ---------------------- Claim check ----------------------
    private static boolean isShipFootprintClaimed(ServerLevel level, BlockPos origin) {
        int minCx = origin.getX() >> 4;
        int maxCx = (origin.getX() + SHIP_W - 1) >> 4;
        int minCz = origin.getZ() >> 4;
        int maxCz = (origin.getZ() + SHIP_D - 1) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (FTBChunksCompat.isChunkClaimed(level, cx, cz)) {
                    DeepSpaceMod.LOGGER.warn("[CrashedShipPlacer] Chunk [{},{}] is claimed.", cx, cz);
                    return true;
                }
            }
        }
        return false;
    }

	 // ---------------------- Pipe Visual Sync ----------------------
	@SubscribeEvent
	public void onServerTickEnd(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || PENDING_SYNCS.isEmpty()) return;

		List<PendingPipeSync> batch = new ArrayList<>();
		PendingPipeSync p;
		while ((p = PENDING_SYNCS.poll()) != null) batch.add(p);

		for (PendingPipeSync pending : batch) {
			int currentTick = pending.level().getServer().getTickCount();
			int elapsed = currentTick - pending.startTick();

			if (elapsed < 3) { // wait for chunk loading
				PENDING_SYNCS.offer(pending);
				continue;
			}
			if (elapsed >= SYNC_TIMEOUT_TICKS) continue;

			Map<BlockPos, Set<ChunkPos>> remaining = new HashMap<>();

			for (var entry : pending.pipesByChunk().entrySet()) {
				BlockPos pos = entry.getKey();
				Set<ChunkPos> relevantChunks = entry.getValue();

				// Step 1: check that all relevant chunks are loaded and tracked by a player
				boolean allChunksReady = true;
				for (ChunkPos cp : relevantChunks) {
					if (getPlayersTrackingChunk(pending.level(), cp).isEmpty()) {
						allChunksReady = false;
						break;
					}
				}
				if (!allChunksReady) {
					remaining.put(pos, relevantChunks);
					continue;
				}

				// Step 2: sync the pipe
				var be = pending.level().getBlockEntity(pos);
				if (be == null) continue;

				try {
					java.lang.reflect.Method syncNow = cachedSyncNow.computeIfAbsent(
						be.getClass(), cls -> {
							try {
								java.lang.reflect.Method m = cls.getMethod("syncNow", boolean.class);
								m.setAccessible(true);
								return m;
							} catch (NoSuchMethodException ex) {
								return null;
							}
						});
					if (syncNow != null) syncNow.invoke(be, true);
				} catch (Exception e) {
					DeepSpaceMod.LOGGER.warn("[CrashedShipPlacer] syncNow failed at {}: {}", pos, e.getMessage());
					remaining.put(pos, relevantChunks);
				}
			}

			// Step 3: retry remaining pipes next tick
			if (!remaining.isEmpty()) {
				PENDING_SYNCS.offer(new PendingPipeSync(pending.level(), remaining, pending.startTick()));
				DeepSpaceMod.LOGGER.debug("[CrashedShipPlacer] {} pipes waiting (elapsed={})", remaining.size(), elapsed);
			} else {
				DeepSpaceMod.LOGGER.info("[CrashedShipPlacer] All pipes visually synced at tick={} elapsed={}", currentTick, elapsed);
			}
		}
	}

    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> getPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos) {
        if (getPlayersFailed) return level.players();
        try {
            var chunkMap = level.getChunkSource().chunkMap;
            if (cachedGetPlayers == null) {
                // First call: look up and cache the Method
                java.lang.reflect.Method m = chunkMap.getClass()
                    .getDeclaredMethod("getPlayers", ChunkPos.class, boolean.class);
                m.setAccessible(true);
                cachedGetPlayers = m;
            }
            var stream = (java.util.stream.Stream<?>) cachedGetPlayers.invoke(chunkMap, chunkPos, false);
            return (List<ServerPlayer>) stream.collect(Collectors.toList());
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.debug(
                    "[CrashedShipPlacer] getPlayers reflection failed ({}), falling back to all players",
                    e.getMessage());
            getPlayersFailed = true;
            cachedGetPlayers = null;
            return level.players();
        }
    }

	// ---------------------- Bulletproof Pipe/Cable Sync ----------------------
	private static void reinitPipeConnections(ServerLevel level, BlockPos origin) {
		Map<BlockPos, Set<ChunkPos>> pipeToChunks = new HashMap<>();
		int currentTick = level.getServer().getTickCount();

		// BFS queue of pipe positions to trace recursively
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();

		// Step 1: enqueue all pipes in ship footprint
		for (int dx = -2; dx <= SHIP_W + 2; dx++) {
			for (int dy = 0; dy <= SHIP_INTERIOR_HEIGHT + 2; dy++) {
				for (int dz = -2; dz <= SHIP_D + 2; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					var be = level.getBlockEntity(pos);
					if (be instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?>) {
						queue.add(pos);
					}
				}
			}
		}

		Set<BlockPos> visited = new HashSet<>();

		// Step 2: BFS traversal across all connected pipes
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			if (!visited.add(pos)) continue; // already processed

			var be = level.getBlockEntity(pos);
			if (!(be instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?>)) continue;

			// Step 2a: collect all chunks touched by this pipe
			Set<ChunkPos> relevantChunks = new HashSet<>();
			relevantChunks.add(new ChunkPos(pos));
			for (Direction dir : Direction.values()) relevantChunks.add(new ChunkPos(pos.relative(dir)));
			pipeToChunks.put(pos.immutable(), relevantChunks);

			// Step 2b: enqueue connected pipe neighbours (no neighborChanged --
			// calling neighborChanged here triggers an O(N) GTCEu pipe-network
			// rebuild for every pipe × 6 directions, turning this BFS into an
			// O(N²) main-thread stall.  Visual sync is handled by the
			// PENDING_SYNCS queue; setChanged() is sufficient to mark the BE dirty.)
			for (Direction dir : Direction.values()) {
				BlockPos neighbor = pos.relative(dir);
				var neighborBE = level.getBlockEntity(neighbor);
				if (neighborBE instanceof com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity<?, ?> && !visited.contains(neighbor))
					queue.add(neighbor);
			}

			be.setChanged();
		}

		DeepSpaceMod.LOGGER.info("[CrashedShipPlacer] Collected {} pipes across {} chunks at {} tick={}",
				pipeToChunks.size(),
				pipeToChunks.values().stream().flatMap(Set::stream).distinct().count(),
				origin, currentTick);

		// Step 3: enqueue PendingPipeSync for visual sync
		PENDING_SYNCS.offer(new PendingPipeSync(level, pipeToChunks, currentTick));
	}
	
    private static void initMachines(ServerLevel level, BlockPos origin) {
        for (int dx = -1; dx <= SHIP_W + 1; dx++)
            for (int dy = 0; dy <= SHIP_INTERIOR_HEIGHT + 2; dy++)
                for (int dz = -1; dz <= SHIP_D + 1; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    var be = level.getBlockEntity(p);
                    if (be instanceof BreederReactorBlockEntity reactor) {
                        reactor.initializeWithStarterFuel(DSConstants.BREEDER_STARTER_INGOTS);
                        DeepSpaceMod.LOGGER.info("[CrashedShipPlacer] Reactor at {}", p);
                    }
                }
    }

    private static void placeBareFallback(ServerLevel level, BlockPos origin, CrashBubbleData bubble) {
        int minCx = (origin.getX() - 2) >> 4, maxCx = (origin.getX() + 2) >> 4;
        int minCz = (origin.getZ() - 2) >> 4, maxCz = (origin.getZ() + 2) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++)
                if (FTBChunksCompat.isChunkClaimed(level, cx, cz)) return;

        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++)
                level.setBlock(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);

        level.setBlock(origin, DSBlocks.BREEDER_REACTOR.get().defaultBlockState(), 3);

        var be = level.getBlockEntity(origin);
        if (be instanceof BreederReactorBlockEntity reactor)
            reactor.initializeWithStarterFuel(DSConstants.BREEDER_STARTER_INGOTS);

        CrashBubbleData updated = new CrashBubbleData(
                bubble.playerUUID, bubble.centerChunkX, bubble.centerChunkZ,
                origin.getX(), origin.getY(), origin.getZ(), bubble.personalSeed);
        DSWorldData.getOrCreate(level).addBubble(updated);
    }
}
