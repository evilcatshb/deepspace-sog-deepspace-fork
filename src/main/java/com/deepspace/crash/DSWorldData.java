package com.deepspace.crash;

import com.deepspace.DeepSpaceMod;
import com.deepspace.DSConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent world data for Deep Space.
 *
 * Saves to: world/data/deepspace_world_data.dat
 *
 * Contains:
 *  - Crash bubble registry (UUID -> CrashBubbleData)
 *  - Reserved spawn regions (chunk positions claimed by pending allocations)
 *  - Placed random structures (sectorKey -> PlacedStructureRecord): ensures each
 *    sector only ever spawns one structure, persisted across server restarts.
 */
public final class DSWorldData extends SavedData {

    private static final String DATA_NAME = "deepspace_world_data";

    /** All generated crash bubbles. ConcurrentHashMap for thread-safe reads. */
    private final Map<UUID, CrashBubbleData> bubbles = new ConcurrentHashMap<>();
    /** Chunk positions reserved for pending structure generation. */
    private final Set<Long> reservedChunkCentres = ConcurrentHashMap.newKeySet();
    /**
     * Placed random structures: sectorKey -> PlacedStructureRecord.
     * Ensures each sector only ever receives one random structure placement.
     */
    private final Map<Long, PlacedStructureRecord> placedStructures = new ConcurrentHashMap<>();

    // -- Factory --------------------------------------------------------------

    public static DSWorldData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            DSWorldData::load,
            DSWorldData::new,
            DATA_NAME);
    }

    // -- Crash bubble API -----------------------------------------------------

    public void addBubble(CrashBubbleData data) {
        bubbles.put(data.playerUUID, data);
        setDirty();
    }

    public CrashBubbleData getBubble(UUID playerUUID) {
        return bubbles.get(playerUUID);
    }

    public boolean hasBubble(UUID playerUUID) {
        return bubbles.containsKey(playerUUID);
    }

    public void reserveChunkCentre(int chunkX, int chunkZ) {
        reservedChunkCentres.add(packChunk(chunkX, chunkZ));
        setDirty();
    }

    public void releaseReservation(int chunkX, int chunkZ) {
        reservedChunkCentres.remove(packChunk(chunkX, chunkZ));
        setDirty();
    }

    public boolean isTooCloseToExistingBubble(int cx, int cz) {
        int minSep = DSConstants.MIN_CRASH_SITE_SEPARATION_CHUNKS;
        for (CrashBubbleData b : bubbles.values()) {
            int dx = Math.abs(b.centerChunkX - cx);
            int dz = Math.abs(b.centerChunkZ - cz);
            if (dx < minSep && dz < minSep) return true;
        }
        for (long packed : reservedChunkCentres) {
            int rx = (int)(packed >> 32);
            int rz = (int)(packed & 0xFFFFFFFFL);
            int dx = Math.abs(rx - cx);
            int dz = Math.abs(rz - cz);
            if (dx < minSep && dz < minSep) return true;
        }
        return false;
    }

    public int bubbleCount() { return bubbles.size(); }

    public java.util.Map<java.util.UUID, CrashBubbleData> getBubbles() {
        return java.util.Collections.unmodifiableMap(bubbles);
    }

    // -- Random structure persistence API ------------------------------------

    /**
     * Returns true if a structure has already been placed for this sector.
     * Used by DeepSpaceChunkGenerator to prevent duplicate placements.
     *
     * @param sectorKey Packed (sectorX, sectorZ) key from StructureSectorGrid.
     */
    public boolean hasStructureAtSector(long sectorKey) {
        return placedStructures.containsKey(sectorKey);
    }

    /**
     * Record that a random structure has been placed.
     * Called by StructurePlacer after successful worldgen placement.
     *
     * @param filename   NBT filename of the structure (e.g. "abandoned_station.nbt")
     * @param dimension  Dimension ID string (e.g. "deepspace:deep_space")
     * @param cx         Centre block X
     * @param cy         Centre block Y
     * @param cz         Centre block Z
     * @param sectorKey  Packed sector key from StructureSectorGrid.packSectorKey()
     */
    public void recordPlacedStructure(String filename, String dimension,
                                       int cx, int cy, int cz, long sectorKey) {
        placedStructures.put(sectorKey,
            new PlacedStructureRecord(filename, dimension, cx, cy, cz, sectorKey));
        setDirty();
    }

    /** Returns all placed random structures (for CC:Tweaked peripheral API, etc.). */
    public Collection<PlacedStructureRecord> getPlacedStructures() {
        return Collections.unmodifiableCollection(placedStructures.values());
    }

    // -- PlacedStructureRecord -----------------------------------------------

    public static final class PlacedStructureRecord {
        public final String filename;
        public final String dimension;
        public final int cx, cy, cz;
        public final long sectorKey;

        public PlacedStructureRecord(String filename, String dimension,
                                      int cx, int cy, int cz, long sectorKey) {
            this.filename  = filename;
            this.dimension = dimension;
            this.cx        = cx;
            this.cy        = cy;
            this.cz        = cz;
            this.sectorKey = sectorKey;
        }

        CompoundTag toNBT() {
            CompoundTag t = new CompoundTag();
            t.putString("file", filename);
            t.putString("dim",  dimension);
            t.putInt("cx", cx);
            t.putInt("cy", cy);
            t.putInt("cz", cz);
            t.putLong("sk", sectorKey);
            return t;
        }

        static PlacedStructureRecord fromNBT(CompoundTag t) {
            return new PlacedStructureRecord(
                t.getString("file"),
                t.getString("dim"),
                t.getInt("cx"),
                t.getInt("cy"),
                t.getInt("cz"),
                t.getLong("sk")
            );
        }
    }

    // -- SavedData serialisation ----------------------------------------------

    @Override
    public CompoundTag save(CompoundTag compound) {
        // Bubbles
        ListTag bubbleList = new ListTag();
        for (CrashBubbleData b : bubbles.values()) {
            bubbleList.add(b.toNBT());
        }
        compound.put("bubbles", bubbleList);

        // Reservations
        long[] reserved = reservedChunkCentres.stream().mapToLong(Long::longValue).toArray();
        compound.putLongArray("reserved", reserved);

        // Placed random structures
        ListTag structList = new ListTag();
        for (PlacedStructureRecord r : placedStructures.values()) {
            structList.add(r.toNBT());
        }
        compound.put("placed_structures", structList);

        return compound;
    }

    private static DSWorldData load(CompoundTag compound) {
        DSWorldData data = new DSWorldData();

        ListTag bubbleList = compound.getList("bubbles", Tag.TAG_COMPOUND);
        for (int i = 0; i < bubbleList.size(); i++) {
            try {
                CrashBubbleData b = CrashBubbleData.fromNBT(bubbleList.getCompound(i));
                data.bubbles.put(b.playerUUID, b);
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[DSWorldData] Skipping corrupt bubble entry #{}", i);
            }
        }

        long[] reserved = compound.getLongArray("reserved");
        for (long r : reserved) data.reservedChunkCentres.add(r);

        ListTag structList = compound.getList("placed_structures", Tag.TAG_COMPOUND);
        for (int i = 0; i < structList.size(); i++) {
            try {
                PlacedStructureRecord r = PlacedStructureRecord.fromNBT(structList.getCompound(i));
                data.placedStructures.put(r.sectorKey, r);
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[DSWorldData] Skipping corrupt structure record #{}", i);
            }
        }

        DeepSpaceMod.LOGGER.info(
            "[DSWorldData] Loaded {} crash bubbles, {} reservations, {} placed structures",
            data.bubbles.size(), data.reservedChunkCentres.size(),
            data.placedStructures.size());
        return data;
    }

    private static long packChunk(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
