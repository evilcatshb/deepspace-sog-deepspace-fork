package com.deepspace.blocks;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.api.IStructureRadiusMap;
import com.deepspace.compat.gtceu.DSCoverHolder;
import com.deepspace.init.DSBlocks;
import com.deepspace.structures.MobData;
import com.deepspace.structures.RegisteredStructure;
import com.deepspace.structures.StructureRadiusMapImpl;
import com.deepspace.worldgen.PlanetCandidate;
import com.deepspace.worldgen.PlanetSectorGrid;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Block entity for the Ship Scanner.
 *
 * <h3>Energy model (GT CEu)</h3>
 * Exposes {@link IEnergyContainer} so GT ZPM cables can push EU into the buffer.
 * Buffer = 4 ticks at ZPM tier. Accepts exactly 1 ampere of ZPM voltage.
 * {@link #hasSufficientPower()} consumes one tick of EU on each Lua call.
 *
 * <h3>Threading model</h3>
 * All Lua functions run on the server tick thread ({@code mainThread = true}).
 *
 * <ul>
 *   <li><b>Structures</b> — scan runs async ({@link CompletableFuture}); result cached in
 *       {@link AtomicReference}. First call returns stale/empty and kicks off the scan;
 *       subsequent calls return fresh results.</li>
 *   <li><b>Ships</b> — scan runs <em>synchronously</em> on the server thread. The chunk
 *       walk is cheap (HashMap iteration over already-loaded chunks); running it off-thread
 *       would race against main-thread mutations of {@code LevelChunk.getBlockEntities()}.
 *       Result is always fresh on the first call.</li>
 *   <li><b>Planets</b> — scan runs async; planet placement is pure deterministic math
 *       with no shared mutable state, so it is safe to run off-thread.</li>
 * </ul>
 *
 * <h3>Cache keys</h3>
 * Each scan type independently tracks its own {@code (dim, x, y, z, radius)} snapshot
 * so that completing one scan type cannot falsely suppress another's staleness check.
 * Ships additionally use a TTL ({@value #SHIP_SCAN_TTL_TICKS} ticks) so that ships
 * that warped into range are detected without the scanner moving.
 */
public class ShipScannerBlockEntity extends BlockEntity {

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final int DEFAULT_SCAN_RANGE = 2500;
    public static final int MAX_SCAN_RANGE     = 5000;

    /**
     * LV voltage (32 EU/t). The wireless energy cover supplies 1A @ LV = 32 EU/t.
     * BUG_17: was ZPM (131,072 EU/t) which required 3+ minutes of accumulation
     * at LV rate before hasSufficientPower() ever returned true.
     */
    private static final long LV_VOLTAGE = GTValues.V[GTValues.LV];

    /**
     * 4-tick buffer at LV rate (128 EU). Small enough that the cover fills it
     * in 4 ticks, preventing any delivery-jitter starvation.
     * BUG_17: was ZPM * 4 = 524,288 EU — took 16,384 ticks to fill via LV cover.
     */
    private static final long EU_BUFFER_SIZE = LV_VOLTAGE * 4;

    /**
     * Ticks before a ship-scan result is considered stale even if the scanner
     * hasn't moved. 200 ticks = 10 seconds at 20 TPS. Catches ships that warped.
     */
    private static final long SHIP_SCAN_TTL_TICKS = 200L;

    // ── Energy ────────────────────────────────────────────────────────────────

    private long euStored = 0L;

    private final IEnergyContainer energyContainer = new IEnergyContainer() {

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            if (voltage < LV_VOLTAGE) return 0;
            long canAccept = EU_BUFFER_SIZE - euStored;
            if (canAccept <= 0) return 0;
            long amperesAccepted = Math.min(amperage, 1L);
            long euAccepted = Math.min(voltage * amperesAccepted, canAccept);
            euStored = Math.min(euStored + euAccepted, EU_BUFFER_SIZE);
            setChanged();
            return amperesAccepted;
        }

        @Override public boolean inputsEnergy(Direction side) { return true; }

        @Override
        public long changeEnergy(long delta) {
            euStored = Math.max(0, Math.min(EU_BUFFER_SIZE, euStored + delta));
            setChanged();
            return euStored;
        }

        @Override public long getEnergyStored()   { return euStored; }
        @Override public long getEnergyCapacity() { return EU_BUFFER_SIZE; }
        @Override public long getInputVoltage()   { return LV_VOLTAGE; }
        @Override public long getInputAmperage()  { return 1L; }
    };

    private LazyOptional<IEnergyContainer> energyCap = LazyOptional.empty();

    // ── BUG_14: GTMThings Wireless Energy Cover support ───────────────────────
    // DSCoverHolder implements ICoverable (GTCEu) without any warpdrive dependency,
    // allowing GTMThings covers to be physically installed/removed by right-clicking
    // with a cover item / crowbar. The warpdrive MixinWirelessEnergyReceiveCover
    // detects ShipScannerBlockEntity via reflection and routes energy into our
    // existing IEnergyContainer (changeEnergy() accumulates euStored).
    @Nullable
    private DSCoverHolder coverHolder;
    private LazyOptional<com.gregtechceu.gtceu.api.capability.ICoverable> coverableCap =
        LazyOptional.empty();

    /**
     * Owner UUID stored at cover-attach time — already resolved to the FTB team UUID
     * (or the player's own UUID if not in a team). The warpdrive mixin reads this via
     * reflection to key the GTMThings wireless energy network lookup.
     */
    @Nullable
    private java.util.UUID wirelessOwnerUUID = null;

    public DSCoverHolder getCoverHolder() {
        if (coverHolder == null) {
            coverHolder = new DSCoverHolder(this);
            coverableCap = LazyOptional.of(() -> coverHolder);
        }
        return coverHolder;
    }

    @Nullable public java.util.UUID getWirelessOwnerUUID()      { return wirelessOwnerUUID; }
    public void setWirelessOwnerUUID(java.util.UUID uuid)       { wirelessOwnerUUID = uuid; setChanged(); }

    // ── Scan radius ───────────────────────────────────────────────────────────

    private int scanRadius = DEFAULT_SCAN_RANGE;

    // ── Cache keys & state — structures ───────────────────────────────────────
    // Each scan type has its OWN (dim, x, y, z, radius) snapshot so that a
    // completed scan for type A cannot suppress a staleness check for type B.

    private String lastStructureDim = null;
    private int    lastStructureX   = Integer.MIN_VALUE;
    private int    lastStructureY   = Integer.MIN_VALUE;
    private int    lastStructureZ   = Integer.MIN_VALUE;
    private int    lastStructureR   = -1;

    private final AtomicReference<String> cachedStructuresJson =
        new AtomicReference<>("{\"success\":true,\"scanning\":true,\"structures\":[]}");
    private final AtomicReference<String> cachedNearestJson    =
        new AtomicReference<>("{\"success\":true,\"scanning\":true,\"nearest\":null}");

    /**
     * Raw structure list kept in parallel with {@link #cachedStructuresJson} so the
     * Ship Scanner GUI can consume typed data without re-parsing JSON.
     * Updated by the async scan thread; read on the server thread when the GUI opens.
     */
    private final AtomicReference<List<RegisteredStructure>> cachedStructureList =
        new AtomicReference<>(List.of());

    private volatile boolean scanInProgress = false;

    // ── Cache keys & state — ships ─────────────────────────────────────────────

    private String lastShipDim      = null;
    private int    lastShipX        = Integer.MIN_VALUE;
    private int    lastShipY        = Integer.MIN_VALUE;
    private int    lastShipZ        = Integer.MIN_VALUE;
    private int    lastShipR        = -1;
    private long   lastShipScanTick = -1L;

    private final AtomicReference<String> cachedShipsJson      =
        new AtomicReference<>("{\"success\":true,\"scanning\":false,\"ships\":[]}");
    private final AtomicReference<String> cachedNearestShipJson =
        new AtomicReference<>("{\"success\":true,\"scanning\":false,\"nearest\":null}");

    // ── Cache keys & state — planets ──────────────────────────────────────────

    private String lastPlanetDim = null;
    private int    lastPlanetX   = Integer.MIN_VALUE;
    private int    lastPlanetY   = Integer.MIN_VALUE;
    private int    lastPlanetZ   = Integer.MIN_VALUE;
    private int    lastPlanetR   = -1;

    private final AtomicReference<String> cachedPlanetsJson       =
        new AtomicReference<>("{\"success\":true,\"scanning\":true,\"planets\":[]}");
    private final AtomicReference<String> cachedNearestPlanetJson =
        new AtomicReference<>("{\"success\":true,\"scanning\":true,\"nearest\":null}");

    private volatile boolean planetScanInProgress = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ShipScannerBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlocks.SHIP_SCANNER_BE.get(), pos, state);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   ShipScannerBlockEntity be) {
        // BUG_14: drive cover holder tick subscriptions so wireless energy covers
        // can pull EU from the GTMThings network every server tick.
        if (be.coverHolder != null) be.coverHolder.serverTick();
    }

    // ── Power ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} and consumes one LV tick of EU (32 EU) from the buffer.
     * Called from every Lua-exposed function before doing any work.
     * BUG_17: was checking/consuming ZPM_VOLTAGE (131,072 EU) which required
     * 3+ minutes to accumulate via a LV wireless energy cover.
     */
    public boolean hasSufficientPower() {
        if (euStored >= LV_VOLTAGE) {
            euStored -= LV_VOLTAGE;
            setChanged();
            return true;
        }
        return false;
    }

    // ── Scan radius ───────────────────────────────────────────────────────────

    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(MAX_SCAN_RANGE, radius));
    }

    public int getScanRadius() { return scanRadius; }

    // ─────────────────────────────────────────────────────────────────────────
    // FACTION INFERENCE (NBT-based, no chunk loading required)
    // ─────────────────────────────────────────────────────────────────────────

    /** Mirrors {@code ShipCoreBlockEntity.FACTION_NAMES} — must stay in sync. */
    private static final List<String> FACTION_NAMES =
            List.of("scav", "luna", "auto", "imax", "pirate", "republic", "collective");

    /**
     * Infers the dominant faction of a structure from its mob list without loading chunks.
     *
     * Checks, in order: (1) live entity instance tags via {@code entity.getTags()},
     * (2) tags baked into the NBT template ({@link MobData#nbtTags}),
     * (3) faction name substring in the entity_type registry key as a last resort.
     *
     * Returns {@code "neutral"} when no faction-affiliated entities are found.
     */
    private static String inferStructureFaction(List<MobData> mobs) {
        if (mobs == null || mobs.isEmpty()) return "neutral";
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (String f : FACTION_NAMES) counts.put(f, 0);

        for (MobData mob : mobs) {
            // 1. Live entity tags (chunk-loaded, runtime reference available)
            if (mob.entityBase != null) {
                for (String f : FACTION_NAMES) {
                    if (mob.entityBase.getTags().contains(f)) {
                        counts.merge(f, 1, Integer::sum);
                        break;
                    }
                }
                continue;
            }
            // 2. Tags baked into the NBT template
            if (mob.nbtTags != null) {
                for (String f : FACTION_NAMES) {
                    if (mob.nbtTags.contains(f)) {
                        counts.merge(f, 1, Integer::sum);
                        break;
                    }
                }
                continue;
            }
            // 3. Faction name in entity_type registry key (e.g. "deepspace:pirate_gunner")
            if (mob.entity_type != null) {
                String lower = mob.entity_type.toLowerCase(java.util.Locale.ROOT);
                for (String f : FACTION_NAMES) {
                    if (lower.contains(f)) {
                        counts.merge(f, 1, Integer::sum);
                        break;
                    }
                }
            }
        }

        String dominant = "neutral";
        int max = 0;
        for (String f : FACTION_NAMES) {
            int c = counts.getOrDefault(f, 0);
            if (c > max) { max = c; dominant = f; }
        }
        return dominant;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STRUCTURE API  (existing behaviour, preserved)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns cached JSON for {@code getStructuresInRadius}. Kicks off an async
     * scan if the cache is stale. Returns immediately with cached data.
     */
    public String getStructuresJson(int radius) {
        triggerStructureScanIfStale(radius);
        return cachedStructuresJson.get();
    }

    /**
     * Returns cached JSON for {@code findNearestStructure}. Kicks off an async
     * scan if the cache is stale. Returns immediately with cached data.
     */
    public String getNearestJson(int radius) {
        triggerStructureScanIfStale(radius);
        return cachedNearestJson.get();
    }

    private void triggerStructureScanIfStale(int radius) {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;

        String dim   = sl.dimension().location().toString();
        BlockPos pos = getBlockPos();

        boolean stale = !scanInProgress && (
            !dim.equals(lastStructureDim)
            || pos.getX() != lastStructureX
            || pos.getY() != lastStructureY
            || pos.getZ() != lastStructureZ
            || radius != lastStructureR
        );
        if (!stale) return;
        scanInProgress = true;

        final String   capDim    = dim;
        final long     seed      = sl.getSeed();
        final BlockPos center    = pos.immutable();
        final int      rad       = radius;

        CompletableFuture.runAsync(() -> {
            try {
                IStructureRadiusMap map = StructureRadiusMapImpl.INSTANCE;
                List<RegisteredStructure> structures =
                    map.getStructuresInRadius(sl, seed, center, rad);

                cachedStructuresJson.set(buildStructuresJson(structures));
                cachedNearestJson.set(buildNearestStructureJson(structures, center));
                cachedStructureList.set(structures != null ? structures : List.of());

                lastStructureDim = capDim;
                lastStructureX   = center.getX();
                lastStructureY   = center.getY();
                lastStructureZ   = center.getZ();
                lastStructureR   = rad;

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ShipScanner] Async structure scan failed: {}", e.getMessage());
            } finally {
                scanInProgress = false;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHIP API  (warpdrive:ship_core blocks)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns JSON of all {@code warpdrive:ship_core} blocks within radius,
     * sorted by distance ascending.
     *
     * <p>Ship scanning runs <em>synchronously</em> on the server thread.
     * {@code LevelChunk.getBlockEntities()} returns a plain {@link java.util.HashMap}
     * that is mutated by the main thread; iterating it off-thread causes
     * {@link java.util.ConcurrentModificationException}. Since the walk is cheap
     * (null-returning {@code getChunkNow} for unloaded chunks, fast HashMap iteration
     * for loaded ones), synchronous execution is safe and avoids the race entirely.
     *
     * <p>Results are always fresh on the first call. Cache is re-used until the
     * scanner moves, the radius changes, or {@value #SHIP_SCAN_TTL_TICKS} ticks
     * elapse (so ships that warped into range are detected).
     */
    public String getShipsJson(int radius) {
        refreshShipCacheIfStale(radius);
        return cachedShipsJson.get();
    }

    /** Returns JSON of the nearest {@code warpdrive:ship_core} within radius. */
    public String getNearestShipJson(int radius) {
        refreshShipCacheIfStale(radius);
        return cachedNearestShipJson.get();
    }

    /**
     * Performs the ship scan synchronously (called on the server thread via
     * {@code @LuaFunction(mainThread = true)}).
     */
    private void refreshShipCacheIfStale(int radius) {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;

        String   dim      = sl.dimension().location().toString();
        BlockPos pos      = getBlockPos();
        long     gameTick = sl.getGameTime();

        boolean stale = (
            !dim.equals(lastShipDim)
            || pos.getX() != lastShipX
            || pos.getY() != lastShipY
            || pos.getZ() != lastShipZ
            || radius != lastShipR
            || (gameTick - lastShipScanTick) >= SHIP_SCAN_TTL_TICKS
        );
        if (!stale) return;

        try {
            // Synchronous: safe on the main thread, avoids HashMap race
            List<ShipEntry> ships = scanLoadedShips(sl, pos.immutable(), radius);
            cachedShipsJson.set(buildShipsJson(ships));
            cachedNearestShipJson.set(buildNearestShipJson(ships));
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[ShipScanner] Ship scan failed: {}", e.getMessage());
            cachedShipsJson.set("{\"ERROR\":\"SCAN_FAILED\"}");
            cachedNearestShipJson.set("{\"ERROR\":\"SCAN_FAILED\"}");
        }

        lastShipDim      = dim;
        lastShipX        = pos.getX();
        lastShipY        = pos.getY();
        lastShipZ        = pos.getZ();
        lastShipR        = radius;
        lastShipScanTick = gameTick;
    }

    /**
     * Iterates all loaded chunks in the XZ bounding box and collects every
     * warpdrive {@code ShipCoreBlockEntity} within the 3-D sphere via reflection.
     * Results are sorted by distance ascending.
     *
     * <p>Must be called on the server thread. Uses {@code getChunkNow} so no
     * chunk generation is triggered; unloaded chunks are skipped in O(1).
     * Returns an empty list immediately if warpdrive is not loaded.
     */
    private static List<ShipEntry> scanLoadedShips(ServerLevel sl, BlockPos center, int radius) {
        initWarpdriveReflection();
        if (shipCoreClass == null) return List.of(); // warpdrive absent

        int chunkMinX = (center.getX() - radius) >> 4;
        int chunkMaxX = (center.getX() + radius) >> 4;
        int chunkMinZ = (center.getZ() - radius) >> 4;
        int chunkMaxZ = (center.getZ() + radius) >> 4;

        List<ShipEntry> found = new ArrayList<>();

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                ChunkAccess chunkAccess = sl.getChunkSource().getChunkNow(cx, cz);
                if (!(chunkAccess instanceof LevelChunk chunk)) continue;

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (!isShipCore(be)) continue;

                    BlockPos corePos = entry.getKey();
                    double   dist    = euclidean(center, corePos);
                    if (dist > radius) continue;

                    found.add(new ShipEntry(
                        corePos.getX(),
                        corePos.getY(),
                        corePos.getZ(),
                        (int) dist,
                        getShipName(be),
                        getShipFaction(be)
                    ));
                }
            }
        }

        found.sort(Comparator.comparingInt(e -> e.distance));
        return found;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLANET API  (celestial spheres via PlanetSectorGrid)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns cached JSON of all celestial sphere planets within radius,
     * sorted by distance ascending. Only valid inside {@code deepspace:deep_space}.
     */
    public String getPlanetsJson(int radius) {
        triggerPlanetScanIfStale(radius);
        return cachedPlanetsJson.get();
    }

    /** Returns cached JSON of the nearest planet within radius. */
    public String getNearestPlanetJson(int radius) {
        triggerPlanetScanIfStale(radius);
        return cachedNearestPlanetJson.get();
    }

    private void triggerPlanetScanIfStale(int radius) {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;

        String   dim = sl.dimension().location().toString();
        BlockPos pos = getBlockPos();

        boolean stale = !planetScanInProgress && (
            !dim.equals(lastPlanetDim)
            || pos.getX() != lastPlanetX
            || pos.getY() != lastPlanetY
            || pos.getZ() != lastPlanetZ
            || radius != lastPlanetR
        );
        if (!stale) return;
        planetScanInProgress = true;

        final String   capDim  = dim;
        final long     seed    = sl.getSeed();
        final BlockPos center  = pos.immutable();
        final int      rad     = radius;

        // Planet scan is pure deterministic math — no level access — safe off-thread.
        CompletableFuture.runAsync(() -> {
            try {
                List<PlanetEntry> planets = scanPlanets(seed, center, rad);

                cachedPlanetsJson.set(buildPlanetsJson(planets));
                cachedNearestPlanetJson.set(buildNearestPlanetJson(planets));

                lastPlanetDim = capDim;
                lastPlanetX   = center.getX();
                lastPlanetY   = center.getY();
                lastPlanetZ   = center.getZ();
                lastPlanetR   = rad;

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ShipScanner] Async planet scan failed: {}", e.getMessage());
                cachedPlanetsJson.set("{\"ERROR\":\"SCAN_FAILED\"}");
                cachedNearestPlanetJson.set("{\"ERROR\":\"SCAN_FAILED\"}");
            } finally {
                planetScanInProgress = false;
            }
        });
    }

    /**
     * Deterministically resolves all planet candidates within {@code radius} blocks
     * of {@code center}. Uses {@link PlanetSectorGrid#resolve} — no chunks are loaded.
     *
     * <p>Strategy: iterate all sectors whose XZ extent overlaps the search bounding box
     * (with a ±1 sector margin for safety). Query each sector's centre chunk through
     * {@code PlanetSectorGrid.resolve}, then deduplicate by {@code (centerX, centerZ)}
     * — at most one planet exists per sector. Finally filter by 3-D radius.
     */
    private static List<PlanetEntry> scanPlanets(long worldSeed, BlockPos center, int radius) {
        int S = DSConstants.PLANET_SECTOR_SIZE_CHUNKS; // chunks per sector side

        // Sector range covering the block-radius bounding box, +1 margin each side.
        int sMinX = Math.floorDiv(Math.floorDiv(center.getX() - radius, 16), S) - 1;
        int sMaxX = Math.floorDiv(Math.floorDiv(center.getX() + radius, 16), S) + 1;
        int sMinZ = Math.floorDiv(Math.floorDiv(center.getZ() - radius, 16), S) - 1;
        int sMaxZ = Math.floorDiv(Math.floorDiv(center.getZ() + radius, 16), S) + 1;

        Set<Long>         seen  = new HashSet<>();
        List<PlanetEntry> found = new ArrayList<>();

        for (int sx = sMinX; sx <= sMaxX; sx++) {
            for (int sz = sMinZ; sz <= sMaxZ; sz++) {
                // Centre chunk of this sector as the representative query point.
                // Math: floorDiv(sx*S + S/2, S) == sx, so resolve() sees sector sx as "this sector".
                int repCx = sx * S + S / 2;
                int repCz = sz * S + S / 2;

                List<PlanetCandidate> candidates =
                    PlanetSectorGrid.resolve(worldSeed, repCx, repCz);

                for (PlanetCandidate p : candidates) {
                    // Encode (centerX, centerZ) as a single long for dedup.
                    long key = ((long) p.centerX << 32) | (p.centerZ & 0xFFFFFFFFL);
                    if (!seen.add(key)) continue;

                    double dist = euclidean(center, p.centerX, p.centerY, p.centerZ);
                    if (dist > radius) continue;

                    found.add(new PlanetEntry(
                        p.centerX,
                        p.centerY,
                        p.centerZ,
                        p.radius,
                        p.def.shellBlock.toString(),
                        (int) dist
                    ));
                }
            }
        }

        found.sort(Comparator.comparingInt(e -> e.distance));
        return found;
    }

    // ── Warpdrive reflection ───────────────────────────────────────────────────
    // warpdrive is an optional runtime dependency with no compile-time JAR.
    // We resolve ShipCoreBlockEntity and its getShipName() method once via
    // reflection on the first ship scan, then cache the results for all future calls.

    /** Set to true once we have attempted to resolve the warpdrive classes. */
    private static boolean warpdriveReflectInit = false;

    /**
     * Cached {@code Class<ShipCoreBlockEntity>}. Null if warpdrive is absent or
     * reflection failed. Populated on first call to {@link #initWarpdriveReflection()}.
     */
    @Nullable
    private static Class<?> shipCoreClass = null;

    /**
     * Cached {@code ShipCoreBlockEntity#getShipName()} method handle.
     * Null when {@link #shipCoreClass} is null.
     */
    @Nullable
    private static Method getShipNameMethod = null;

    /**
     * Cached {@code ShipCoreBlockEntity#getFaction()} method handle.
     * Returns the ship's dominant faction string, e.g. {@code "pirate"} or {@code "neutral"}.
     * Null when {@link #shipCoreClass} is null or the warpdrive version predates faction support.
     */
    @Nullable
    private static Method getFactionMethod = null;

    /**
     * Cached {@code ShipCoreBlockEntity#getOwnerDisplayName(MinecraftServer)} handle.
     * Returns the FTBTeams team name when applicable, then the Minecraft username,
     * then a short UUID fallback, or {@code "Unowned"} if no owner is set.
     * Null when {@link #shipCoreClass} is null.
     */
    @Nullable
    private static Method getOwnerDisplayNameMethod = null;

    /**
     * Resolves warpdrive reflection handles on first call; no-op on subsequent calls.
     * Safe to call from the server thread.
     */
    private static void initWarpdriveReflection() {
        if (warpdriveReflectInit) return;
        warpdriveReflectInit = true;
        try {
            shipCoreClass    = Class.forName("com.warpdrive.blockentity.ShipCoreBlockEntity");
            getShipNameMethod = shipCoreClass.getMethod("getShipName");
            getOwnerDisplayNameMethod = shipCoreClass.getMethod(
                "getOwnerDisplayName", net.minecraft.server.MinecraftServer.class);
            try {
                getFactionMethod = shipCoreClass.getMethod("getFaction");
            } catch (NoSuchMethodException ignored) {
                // older warpdrive build without faction support — ok
            }
            DeepSpaceMod.LOGGER.info("[ShipScanner] Warpdrive reflection initialised.");
        } catch (ClassNotFoundException e) {
            DeepSpaceMod.LOGGER.info("[ShipScanner] Warpdrive not on classpath; ship scanning disabled.");
        } catch (NoSuchMethodException e) {
            DeepSpaceMod.LOGGER.warn("[ShipScanner] Warpdrive method not found: {}", e.getMessage());
        }
    }

    /** Returns true if {@code be} is a warpdrive ShipCoreBlockEntity. */
    private static boolean isShipCore(BlockEntity be) {
        return shipCoreClass != null && shipCoreClass.isInstance(be);
    }

    /**
     * Invokes {@code ShipCoreBlockEntity#getShipName()} on {@code be} via the cached
     * method handle. Returns {@code "UNKNOWN"} on any reflection error.
     */
    private static String getShipName(BlockEntity be) {
        try {
            Object result = getShipNameMethod.invoke(be);
            return result instanceof String s ? s : "UNKNOWN";
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[ShipScanner] getShipName() reflection failed: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Invokes {@code ShipCoreBlockEntity#getOwnerDisplayName(MinecraftServer)} via the
     * cached method handle. Returns the FTBTeams team name when applicable, otherwise
     * the Minecraft username, or {@code "Unowned"} on any error.
     */
    private static String getShipOwner(BlockEntity be, net.minecraft.server.MinecraftServer server) {
        try {
            if (getOwnerDisplayNameMethod == null) return "Unowned";
            Object result = getOwnerDisplayNameMethod.invoke(be, server);
            return result instanceof String s ? s : "Unowned";
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[ShipScanner] getOwnerDisplayName() reflection failed: {}", e.getMessage());
            return "Unowned";
        }
    }


    /**
     * Invokes {@code ShipCoreBlockEntity#getFaction()} via the cached method handle.
     * Returns {@code "neutral"} on any reflection error or if the warpdrive version
     * predates faction support.
     */
    private static String getShipFaction(BlockEntity be) {
        if (getFactionMethod == null) return "neutral";
        try {
            Object result = getFactionMethod.invoke(be);
            return result instanceof String s ? s : "neutral";
        } catch (Exception e) {
            return "neutral";
        }
    }

    // ── Internal DTOs ─────────────────────────────────────────────────────────

    private record ShipEntry(int x, int y, int z, int distance, String name, String faction) {}
    private record PlanetEntry(int x, int y, int z, int radius, String shellBlock, int distance) {}

    // ─────────────────────────────────────────────────────────────────────────
    // GUI SUPPORT  (Ship Scanner right-click GUI)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flat entry record consumed by {@link ShipScannerMenu} / {@link ShipScannerScreen}.
     *
     * <ul>
     *   <li>{@code type} — one of {@link #SHIP}, {@link #PLAYER}, {@link #STRUCTURE}.</li>
     *   <li>{@code owner} — non-empty only for SHIP entries; uses
     *       {@code ShipCoreBlockEntity#getOwnerDisplayName(MinecraftServer)} so FTBTeams
     *       team names are shown when applicable.</li>
     * </ul>
     */
    public record GuiEntry(byte type, String name, String owner, String faction,
                           int x, int y, int z, int distance) {
        public static final byte SHIP      = 0;
        public static final byte PLAYER    = 1;
        public static final byte STRUCTURE = 2;
    }

    /**
     * Collects all scan results for the Ship Scanner GUI and returns them sorted
     * by distance ascending. <b>Must be called on the server thread.</b>
     *
     * <ul>
     *   <li>Ships — scanned synchronously (safe on the server thread).</li>
     *   <li>Players — enumerated from {@code ServerLevel.players()} directly.</li>
     *   <li>Structures — served from the async cache; a rescan is triggered if
     *       stale, but the method does not block waiting for it.</li>
     * </ul>
     *
     * @param sl     The server level the scanner block lives in.
     * @param radius Search radius in blocks (GUI hard-caps this at 3000).
     * @return Mutable list of {@link GuiEntry} records, sorted by distance.
     */
    public List<GuiEntry> collectGuiEntries(ServerLevel sl, int radius) {
        List<GuiEntry> result = new ArrayList<>();
        BlockPos center = getBlockPos();
        net.minecraft.server.MinecraftServer server = sl.getServer();

        // 1. Ships (synchronous — safe and cheap on the server thread)
        initWarpdriveReflection();
        if (shipCoreClass != null) {
            try {
                List<ShipEntry> ships = scanLoadedShips(sl, center.immutable(), radius);
                for (ShipEntry s : ships) {
                    BlockEntity shipBe = sl.getBlockEntity(new BlockPos(s.x(), s.y(), s.z()));
                    String owner = (shipBe != null) ? getShipOwner(shipBe, server) : "Unowned";
                    String shipFaction = (shipBe != null) ? getShipFaction(shipBe) : "neutral";
                    result.add(new GuiEntry(GuiEntry.SHIP, s.name(), owner, shipFaction,
                        s.x(), s.y(), s.z(), s.distance()));
                }
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ShipScanner] GUI ship scan error: {}", e.getMessage());
            }
        }

        // 2. Players (always synchronous)
        for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
            double dx = p.getX() - center.getX();
            double dy = p.getY() - center.getY();
            double dz = p.getZ() - center.getZ();
            int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                result.add(new GuiEntry(GuiEntry.PLAYER, p.getName().getString(), "", "",
                    (int) p.getX(), (int) p.getY(), (int) p.getZ(), dist));
            }
        }

        // 3. Structures — trigger async rescan if stale, consume whatever is cached
        triggerStructureScanIfStale(radius);
        for (RegisteredStructure s : cachedStructureList.get()) {
            int dist = (int) euclidean(center, s);
            if (dist <= radius) {
                String name = s.template.nbtFilename;
                if (name.endsWith(".nbt")) name = name.substring(0, name.length() - 4);
                String structureFaction = inferStructureFaction(s.getMobs());
                result.add(new GuiEntry(GuiEntry.STRUCTURE, name, "", structureFaction,
                    s.location.x, s.location.y, s.location.z, dist));
            }
        }

        result.sort(Comparator.comparingInt(GuiEntry::distance));
        return result;
    }

    /**
     * Returns {@code true} if an async structure scan is currently running.
     * Used by the GUI to show a "Scanning structures…" notice in the footer.
     */
    public boolean isStructureScanInProgress() { return scanInProgress; }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    // ── Structures ────────────────────────────────────────────────────────────

    private static String buildStructuresJson(List<RegisteredStructure> structures) {
        if (structures == null || structures.isEmpty()) {
            return "{\"success\":true,\"scanning\":false,\"structures\":[]}";
        }
        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"structures\":[");
        boolean first = true;
        for (RegisteredStructure s : structures) {
            if (!first) sb.append(',');
            first = false;
            appendStructureObject(sb, s, -1);
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String buildNearestStructureJson(List<RegisteredStructure> structures,
                                                     BlockPos center) {
        if (structures == null || structures.isEmpty()) {
            return "{\"success\":true,\"scanning\":false,\"nearest\":null}";
        }
        RegisteredStructure nearest = structures.stream()
            .min(Comparator.comparingDouble(s -> euclidean(center, s)))
            .orElse(null);
        if (nearest == null) return "{\"success\":true,\"scanning\":false,\"nearest\":null}";

        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"nearest\":");
        appendStructureObject(sb, nearest, (int) euclidean(center, nearest));
        sb.append("}");
        return sb.toString();
    }

    private static void appendStructureObject(StringBuilder sb, RegisteredStructure s,
                                               int distance) {
        String type = s.template.nbtFilename;
        if (type.endsWith(".nbt")) type = type.substring(0, type.length() - 4);

        List<MobData> mobs = s.getMobs();
        String faction = inferStructureFaction(mobs);

        sb.append("{\"type\":").append(jsonString(type))
          .append(",\"faction\":").append(jsonString(faction))
          .append(",\"x\":").append(s.location.x)
          .append(",\"y\":").append(s.location.y)
          .append(",\"z\":").append(s.location.z);

        if (distance >= 0) sb.append(",\"distance\":").append(distance);

        sb.append(",\"entities\":[");
        if (mobs != null && !mobs.isEmpty()) {
            boolean first = true;
            for (MobData mob : mobs) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"type\":").append(jsonString(mob.entity_type))
                  .append(",\"x\":").append(mob.spawn_x)
                  .append(",\"y\":").append(mob.spawn_y)
                  .append(",\"z\":").append(mob.spawn_z)
                  .append(",\"health\":").append(resolveMobHealth(mob))
                  .append("}");
            }
        }
        sb.append("]}");
    }

    // ── Ships ─────────────────────────────────────────────────────────────────

    private static String buildShipsJson(List<ShipEntry> ships) {
        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"ships\":[");
        boolean first = true;
        for (ShipEntry s : ships) {
            if (!first) sb.append(',');
            first = false;
            appendShipObject(sb, s);
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String buildNearestShipJson(List<ShipEntry> ships) {
        if (ships.isEmpty()) return "{\"success\":true,\"scanning\":false,\"nearest\":null}";
        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"nearest\":");
        appendShipObject(sb, ships.get(0)); // list already sorted by distance ascending
        sb.append("}");
        return sb.toString();
    }

    /** Appends {@code {"name":..., "x":..., "y":..., "z":..., "distance":...}}. */
    private static void appendShipObject(StringBuilder sb, ShipEntry s) {
        sb.append("{\"name\":").append(jsonString(s.name))
          .append(",\"faction\":").append(jsonString(s.faction))
          .append(",\"x\":").append(s.x)
          .append(",\"y\":").append(s.y)
          .append(",\"z\":").append(s.z)
          .append(",\"distance\":").append(s.distance)
          .append("}");
    }

    // ── Planets ───────────────────────────────────────────────────────────────

    private static String buildPlanetsJson(List<PlanetEntry> planets) {
        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"planets\":[");
        boolean first = true;
        for (PlanetEntry p : planets) {
            if (!first) sb.append(',');
            first = false;
            appendPlanetObject(sb, p);
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String buildNearestPlanetJson(List<PlanetEntry> planets) {
        if (planets.isEmpty()) return "{\"success\":true,\"scanning\":false,\"nearest\":null}";
        StringBuilder sb = new StringBuilder("{\"success\":true,\"scanning\":false,\"nearest\":");
        appendPlanetObject(sb, planets.get(0)); // list already sorted by distance ascending
        sb.append("}");
        return sb.toString();
    }

    /** Appends {@code {"x":..., "y":..., "z":..., "radius":..., "shell_block":..., "distance":...}}. */
    private static void appendPlanetObject(StringBuilder sb, PlanetEntry p) {
        sb.append("{\"x\":").append(p.x)
          .append(",\"y\":").append(p.y)
          .append(",\"z\":").append(p.z)
          .append(",\"radius\":").append(p.radius)
          .append(",\"shell_block\":").append(jsonString(p.shellBlock))
          .append(",\"distance\":").append(p.distance)
          .append("}");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static float resolveMobHealth(MobData mob) {
        if (mob.entityLiving != null && !mob.entityLiving.isDeadOrDying()) {
            return mob.entityLiving.getHealth();
        }
        return (float) mob.health;
    }

    private static double euclidean(BlockPos center, RegisteredStructure s) {
        double dx = center.getX() - s.location.x;
        double dy = center.getY() - s.location.y;
        double dz = center.getZ() - s.location.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double euclidean(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double euclidean(BlockPos center, int x, int y, int z) {
        double dx = center.getX() - x;
        double dy = center.getY() - y;
        double dz = center.getZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // ── Capabilities ──────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        energyCap = LazyOptional.of(() -> energyContainer);
        // BUG_14: re-arm cover cap + fire onLoad on all installed covers.
        coverableCap = LazyOptional.of(() -> getCoverHolder());
        if (coverHolder != null) coverHolder.onLoad();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        coverableCap.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                       @Nullable Direction side) {
        if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER) return energyCap.cast();
        // BUG_14: expose ICoverable so GTMThings wireless covers can attach.
        if (cap == GTCapability.CAPABILITY_COVERABLE) {
            getCoverHolder();
            return coverableCap.cast();
        }
        return super.getCapability(cap, side);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EUStored",   euStored);
        tag.putInt("ScanRadius",  scanRadius);
        // Persist all six caches so results survive server restarts without re-scanning
        tag.putString("CachedStructures",    cachedStructuresJson.get());
        tag.putString("CachedNearest",       cachedNearestJson.get());
        tag.putString("CachedShips",         cachedShipsJson.get());
        tag.putString("CachedNearestShip",   cachedNearestShipJson.get());
        tag.putString("CachedPlanets",       cachedPlanetsJson.get());
        tag.putString("CachedNearestPlanet", cachedNearestPlanetJson.get());
        // BUG_14: persist wireless owner UUID and installed covers.
        if (wirelessOwnerUUID != null) tag.putUUID("WirelessOwner", wirelessOwnerUUID);
        if (coverHolder != null) coverHolder.saveToTag(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        euStored   = tag.getLong("EUStored");
        scanRadius = tag.contains("ScanRadius") ? tag.getInt("ScanRadius") : DEFAULT_SCAN_RANGE;
        if (tag.contains("CachedStructures"))    cachedStructuresJson.set(tag.getString("CachedStructures"));
        if (tag.contains("CachedNearest"))       cachedNearestJson.set(tag.getString("CachedNearest"));
        if (tag.contains("CachedShips"))         cachedShipsJson.set(tag.getString("CachedShips"));
        if (tag.contains("CachedNearestShip"))   cachedNearestShipJson.set(tag.getString("CachedNearestShip"));
        if (tag.contains("CachedPlanets"))       cachedPlanetsJson.set(tag.getString("CachedPlanets"));
        if (tag.contains("CachedNearestPlanet")) cachedNearestPlanetJson.set(tag.getString("CachedNearestPlanet"));
        // BUG_14: restore wireless owner UUID and installed covers.
        if (tag.hasUUID("WirelessOwner")) wirelessOwnerUUID = tag.getUUID("WirelessOwner");
        if (tag.contains("DSCovers")) getCoverHolder().loadFromTag(tag);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // BUG_14: unsubscribe cover ticks (does NOT drop items — handled by Block.playerWillDestroy)
        if (coverHolder != null) coverHolder.onRemoved();
    }

    // ── Client sync (BUG_16) ──────────────────────────────────────────────────
    // Without these three overrides, BlockEntity.getUpdatePacket() returns null,
    // so level.sendBlockUpdated() never delivers cover data to the client.
    // The client's DSCoverHolder stays empty → DSCoverOverlayRenderer renders nothing.
    // Reference: ShipCoreBlockEntity (warpdrive) implements the identical pattern.

    /**
     * Called by the server when the client first loads the chunk that contains
     * this block entity. Includes cover NBT so the overlay is visible immediately
     * on chunk load, not only after the next incremental update packet.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (coverHolder != null) coverHolder.saveToTag(tag);
        return tag;
    }

    /**
     * Returns an incremental update packet queued by
     * {@code ServerLevel.broadcastChanges()} every time
     * {@code DSCoverHolder.notifyBlockUpdate()} fires (cover add/remove).
     * The client receives this and calls {@link #handleUpdateTag}.
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Called on the CLIENT when a {@link ClientboundBlockEntityDataPacket} arrives.
     * Delegates to {@link #load} so the client-side cover holder is populated
     * and {@link com.deepspace.client.DSCoverOverlayRenderer} can resolve sprites.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
