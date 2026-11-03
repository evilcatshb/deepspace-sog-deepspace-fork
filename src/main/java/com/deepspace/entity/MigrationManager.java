package com.deepspace.entity;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.entity.goal.MigrationCircleGoal;
import com.deepspace.init.DSEntities;
import com.deepspace.init.DSDimensions;
import com.deepspace.worldgen.PlanetCandidate;
import com.deepspace.worldgen.PlanetSectorGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Manages Space Whale migration events.
 *
 * Migration fires at 12:00 AM and 12:00 PM real (wall-clock) time,
 * plus on demand via {@link #triggerMigration(MinecraftServer)}.
 *
 * Each migration (BUG_1 fix):
 *   1. Checks which chunks are loaded by online players in Deep Space.
 *   2. Spawns 2–3 whales at the southernmost loaded chunk boundary (+Z = south).
 *   3. Finds the celestial body/planet sphere nearest to any online player
 *      within their loaded chunks; if none is found there, searches a broader
 *      area around the players' average position.
 *   4. Assigns each whale a fair arc offset (2π/N) so they orbit as a halo
 *      around that planet, migrating north from the spawn point to reach it.
 *   5. Falls back to the original seed-based planet selection only when no
 *      players are online in the dimension.
 *
 * {@link #progressMigration(MinecraftServer)} immediately sends all circling
 * whales into their retreat phase (used by /spacewhale migrate progress).
 */
public class MigrationManager {

    public static final MigrationManager INSTANCE = new MigrationManager();

    // Checked every 20 ticks; only fire once per minute-window
    private int lastFiredHour   = -1;
    private boolean firedAM     = false;
    private boolean firedPM     = false;
    private int eventCounter    = 0;

    /** Server tick: poll wall-clock twice per day. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        LocalTime now = LocalTime.now();
        int hour   = now.getHour();
        int minute = now.getMinute();

        // Trigger at 00:00 and 12:00 (within the first minute of the hour)
        boolean isMidnight = (hour == 0);
        boolean isNoon     = (hour == 12);

        if (isMidnight && !firedAM && minute == 0) {
            firedAM = true;
            firedPM = false;
            triggerMigration(server);
        } else if (isNoon && !firedPM && minute == 0) {
            firedPM = true;
            firedAM = false;
            triggerMigration(server);
        } else if (!isMidnight && !isNoon) {
            // Reset flags outside trigger windows so next occurrence fires
            if (hour != 0)  firedAM = false;
            if (hour != 12) firedPM = false;
        }
    }

    // ── Core spawn logic ──────────────────────────────────────────────────────

    /** Public: spawns migration whales right now. Called from command or scheduled tick. */
    public void triggerMigration(MinecraftServer server) {
        ServerLevel level = server.getLevel(DSDimensions.DEEP_SPACE);
        if (level == null) {
            DeepSpaceMod.LOGGER.warn("[MigrationManager] Deep Space dimension not loaded — skipping migration");
            return;
        }

        long worldSeed = level.getSeed();
        // Each event gets a unique seed offset so planet selection varies across events
        long eventSeed = worldSeed ^ (long) (eventCounter++) * 0x9E3779B97F4A7C15L;
        eventSeed ^= eventSeed >>> 30; eventSeed *= 0xbf58476d1ce4e5b9L; eventSeed ^= eventSeed >>> 27;

        // ── BUG_1 FIX: Determine spawn position and target planet from loaded chunks ──
        //
        // When players are online in the Deep Space dimension we:
        //   a) Collect every chunk loaded by any online player (server view-distance
        //      radius around each player's chunk position).
        //   b) Use the SOUTHERNMOST of those chunks as the whale spawn boundary
        //      (+Z = south in Minecraft coordinates).
        //   c) Find every planet whose centre falls within those loaded chunks and
        //      pick the one NEAREST to any online player as the migration target.
        //   d) If no planet exists in loaded chunks, widen the search to a broader
        //      radius around the players' average position so the orbit target is
        //      still player-relevant.
        //   e) Only fall back to the original seed-based random planet near the
        //      origin when no players are online in the dimension at all.

        List<ServerPlayer> players = level.players();

        double spawnX;
        double spawnZ;
        PlanetCandidate planet;

        if (!players.isEmpty()) {
            int viewDist = server.getPlayerList().getViewDistance();
            int S = DSConstants.PLANET_SECTOR_SIZE_CHUNKS; // 32 chunks = 512 blocks per sector

            // Track southernmost (highest Z) loaded chunk and collect all planets
            // visible in any online player's loaded chunks.
            //
            // IMPORTANT: we search by SECTOR, not by individual chunk. Calling
            // PlanetSectorGrid.resolve() on random chunks only returns a planet when
            // the chunk falls inside the planet's void radius (~56–152 blocks). Since
            // sectors are 512 blocks wide, most individual chunks miss it entirely.
            // Resolving at the SECTOR-CENTER chunk guarantees the planet is found for
            // any sector that contains one, then we distance-filter against the player.
            int southmostChunkZ = Integer.MIN_VALUE;
            int southmostChunkX = 0;
            List<PlanetCandidate> loadedPlanets = new ArrayList<>();

            for (ServerPlayer player : players) {
                int pChunkX = player.chunkPosition().x;
                int pChunkZ = player.chunkPosition().z;
                double pBlockX = player.getX();
                double pBlockZ = player.getZ();

                // Southern edge of this player's loaded area
                int candidateSouthChunkZ = pChunkZ + viewDist;
                if (candidateSouthChunkZ > southmostChunkZ) {
                    southmostChunkZ = candidateSouthChunkZ;
                    southmostChunkX = pChunkX;
                }

                // Sectors whose planet spheres could reach into this player's view
                int playerSectorX = Math.floorDiv(pChunkX, S);
                int playerSectorZ = Math.floorDiv(pChunkZ, S);
                int viewDistSectors = (viewDist * 16 + DSConstants.PLANET_RADIUS_MAX) / (S * 16) + 2;

                for (int dsx = -viewDistSectors; dsx <= viewDistSectors; dsx++) {
                    for (int dsz = -viewDistSectors; dsz <= viewDistSectors; dsz++) {
                        int sx = playerSectorX + dsx;
                        int sz = playerSectorZ + dsz;
                        // Resolve at sector-center chunk so the planet is reliably returned
                        int cx = sx * S + S / 2;
                        int cz = sz * S + S / 2;
                        List<PlanetCandidate> nearby = PlanetSectorGrid.resolve(worldSeed, cx, cz);
                        for (PlanetCandidate p : nearby) {
                            // Keep only planets whose sphere actually reaches into loaded chunks
                            double dist = Math.sqrt(
                                Math.pow(p.centerX - pBlockX, 2) +
                                Math.pow(p.centerZ - pBlockZ, 2));
                            if (dist > viewDist * 16.0 + p.radius) continue;
                            // Deduplicate — multiple sectors may resolve the same planet
                            boolean dup = loadedPlanets.stream().anyMatch(c ->
                                Math.abs(c.centerX - p.centerX) < 100 &&
                                Math.abs(c.centerZ - p.centerZ) < 100);
                            if (!dup) loadedPlanets.add(p);
                        }
                    }
                }
            }

            // Spawn whales at the southernmost loaded chunk boundary
            spawnZ = southmostChunkZ * 16.0 + 8.0;
            spawnX = southmostChunkX * 16.0 + 8.0;

            if (!loadedPlanets.isEmpty()) {
                // Use the planet nearest to any online player that is in their loaded chunks
                planet = loadedPlanets.stream()
                    .min(Comparator.comparingDouble(p ->
                        players.stream()
                            .mapToDouble(pl -> {
                                double dx = p.centerX - pl.getX();
                                double dz = p.centerZ - pl.getZ();
                                return dx * dx + dz * dz;
                            })
                            .min().orElse(Double.MAX_VALUE)))
                    .orElse(null);
            } else {
                // No planet in loaded chunks — broader search near average player position
                planet = pickNearestPlanetToPlayers(worldSeed, players);
                if (planet == null) {
                    DeepSpaceMod.LOGGER.warn("[MigrationManager] No planet found near players — using dimension centre");
                }
            }
        } else {
            // No players online in the dimension — original seed-based fallback
            planet = pickMigrationPlanet(worldSeed, eventSeed);
            if (planet == null) {
                DeepSpaceMod.LOGGER.warn("[MigrationManager] No planet found for migration — using dimension centre");
            }
            double fallbackCX = (planet != null) ? planet.centerX : 0.0;
            double fallbackCZ = (planet != null) ? planet.centerZ : 0.0;
            // Original behaviour: spawn 16 chunks (256 blocks) south of the planet
            spawnX = fallbackCX;
            spawnZ = fallbackCZ + MigrationCircleGoal.ORBIT_RADIUS + 256.0;
        }

        double centreX = (planet != null) ? planet.centerX : spawnX;
        double centreZ = (planet != null) ? planet.centerZ : (spawnZ - MigrationCircleGoal.ORBIT_RADIUS - 256.0);
        double flightY = DSConstants.ASTEROID_BAND_Y_MAX + 30.0;

        // ── Pod size: 3-8 whales ──────────────────────────────────────────────
        Random rng = new Random(eventSeed);
        int podSize = 3 + rng.nextInt(6); // 3 to 8

        DeepSpaceMod.LOGGER.info("[MigrationManager] Migration event #{}: {} whales → planet ({}, {}), spawn ({}, {})",
            eventCounter - 1, podSize, (int) centreX, (int) centreZ, (int) spawnX, (int) spawnZ);

        List<SpaceWhaleEntity> pod = new ArrayList<>();

        for (int i = 0; i < podSize; i++) {
            SpaceWhaleEntity whale = DSEntities.SPACE_WHALE.get().create(level);
            if (whale == null) continue;

            // ── Fair arc offset: each whale gets 2π/N of the circle ───────────
            double angleOffset = (2.0 * Math.PI / podSize) * i;

            // ── Spawn at the southernmost loaded chunk with slight X spread ────
            double whaleSpawnX = spawnX + (rng.nextDouble() - 0.5) * 32.0;
            double whaleSpawnZ = spawnZ;
            double whaleSpawnY = flightY;

            whale.setPos(whaleSpawnX, whaleSpawnY, whaleSpawnZ);
            whale.setNoGravity(true);

            // Write migration params
            whale.migrationAngle   = angleOffset;
            whale.migrationCentreX = centreX;
            whale.migrationCentreZ = centreZ;
            whale.migrationFlightY = flightY;
            whale.setMigrating(true);

            // Finalize spawn (sets no-gravity, altitude clamp, etc.)
            whale.finalizeSpawn(level, level.getCurrentDifficultyAt(whale.blockPosition()),
                MobSpawnType.EVENT, null, null);

            level.addFreshEntity(whale);
            pod.add(whale);
        }

        DeepSpaceMod.LOGGER.info("[MigrationManager] Spawned {} migration whales.", pod.size());
    }

    // ── /spacewhale migrate progress ─────────────────────────────────────────

    /**
     * Immediately advances all currently circling migration whales to the retreat phase.
     * Called by /spacewhale migrate progress.
     */
    public int progressMigration(MinecraftServer server) {
        ServerLevel level = server.getLevel(DSDimensions.DEEP_SPACE);
        if (level == null) return 0;

        int count = 0;
        for (SpaceWhaleEntity whale : level.getEntitiesOfClass(SpaceWhaleEntity.class,
                net.minecraft.world.phys.AABB.ofSize(
                    net.minecraft.world.phys.Vec3.ZERO, 200_000, 2000, 200_000))) {
            if (!whale.isMigrating()) continue;
            // Find the MigrationCircleGoal and skip it to retreat
            for (var wrapped : whale.goalSelector.getAvailableGoals()) {
                if (wrapped.getGoal() instanceof MigrationCircleGoal mg) {
                    mg.skipToRetreat();
                    count++;
                }
            }
        }
        return count;
    }

    // ── Planet selection ──────────────────────────────────────────────────────

    /**
     * Fallback: picks a planet near the dimension origin when no players are online.
     * Uses eventSeed to vary which planet is chosen each time.
     */
    private static PlanetCandidate pickMigrationPlanet(long worldSeed, long eventSeed) {
        List<PlanetCandidate> candidates = new ArrayList<>();
        int searchR = 8; // sectors
        for (int sx = -searchR; sx <= searchR; sx++) {
            for (int sz = -searchR; sz <= searchR; sz++) {
                int cx = sx * DSConstants.PLANET_SECTOR_SIZE_CHUNKS
                       + DSConstants.PLANET_SECTOR_SIZE_CHUNKS / 2;
                int cz = sz * DSConstants.PLANET_SECTOR_SIZE_CHUNKS
                       + DSConstants.PLANET_SECTOR_SIZE_CHUNKS / 2;
                List<PlanetCandidate> nearby = PlanetSectorGrid.resolve(worldSeed, cx, cz);
                for (PlanetCandidate p : nearby) {
                    boolean dup = candidates.stream().anyMatch(c ->
                        Math.abs(c.centerX - p.centerX) < 100 &&
                        Math.abs(c.centerZ - p.centerZ) < 100);
                    if (!dup) candidates.add(p);
                }
            }
        }
        if (candidates.isEmpty()) return null;
        int idx = (int) (Math.abs(eventSeed) % candidates.size());
        return candidates.get(idx);
    }

    /**
     * BUG_1 FIX: Searches a wider area centred on the average position of online
     * players to find the nearest planet when none exists inside their loaded chunks.
     * Used as a secondary fallback when players are online but their view hasn't
     * loaded any planet sectors yet.
     */
    private static PlanetCandidate pickNearestPlanetToPlayers(long worldSeed,
                                                               List<ServerPlayer> players) {
        double avgX = players.stream().mapToDouble(ServerPlayer::getX).average().orElse(0.0);
        double avgZ = players.stream().mapToDouble(ServerPlayer::getZ).average().orElse(0.0);

        int S = DSConstants.PLANET_SECTOR_SIZE_CHUNKS;
        // Align to the sector the average player position falls in, then search outward
        int avgSectorX = Math.floorDiv((int) Math.floor(avgX / 16.0), S);
        int avgSectorZ = Math.floorDiv((int) Math.floor(avgZ / 16.0), S);
        int searchR = 12; // sectors

        List<PlanetCandidate> candidates = new ArrayList<>();
        for (int dsx = -searchR; dsx <= searchR; dsx++) {
            for (int dsz = -searchR; dsz <= searchR; dsz++) {
                int sx = avgSectorX + dsx;
                int sz = avgSectorZ + dsz;
                // Use sector-center chunk so resolve() reliably returns the planet
                int cx = sx * S + S / 2;
                int cz = sz * S + S / 2;
                List<PlanetCandidate> nearby = PlanetSectorGrid.resolve(worldSeed, cx, cz);
                for (PlanetCandidate p : nearby) {
                    boolean dup = candidates.stream().anyMatch(c ->
                        Math.abs(c.centerX - p.centerX) < 100 &&
                        Math.abs(c.centerZ - p.centerZ) < 100);
                    if (!dup) candidates.add(p);
                }
            }
        }

        if (candidates.isEmpty()) return null;

        return candidates.stream()
            .min(Comparator.comparingDouble(p -> {
                double dx = p.centerX - avgX;
                double dz = p.centerZ - avgZ;
                return dx * dx + dz * dz;
            }))
            .orElse(null);
    }
}