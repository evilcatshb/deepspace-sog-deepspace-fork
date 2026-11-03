package com.deepspace.deathless;

import com.deepspace.DeepSpaceMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent queue of pending deathless respawns.
 *
 * Saves to: world/data/deepspace_deathless_data.dat (overworld save folder).
 *
 * Contains a UUID-keyed map of {@link DeathlessEntry} so each dying entity
 * is only ever queued once, even if multiple LivingDeathEvents fire for it.
 */
public final class DeathlessWorldData extends SavedData {

    private static final String DATA_NAME = "deepspace_deathless_data";

    /** Pending respawns keyed by the original entity's UUID. */
    private final Map<UUID, DeathlessEntry> entries = new ConcurrentHashMap<>();

    // -- Factory --------------------------------------------------------------

    /** Always anchor on the overworld so a single shared queue serves every dimension. */
    public static DeathlessWorldData getOrCreate(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            // Fall back to the level passed if overworld unavailable (shouldn't happen).
            throw new IllegalStateException("Overworld not loaded — cannot access DeathlessWorldData");
        }
        return getOrCreate(overworld);
    }

    public static DeathlessWorldData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            DeathlessWorldData::load,
            DeathlessWorldData::new,
            DATA_NAME);
    }

    // -- Queue API ------------------------------------------------------------

    /**
     * Adds an entry only if no entry exists for that UUID yet.
     * Returns true if the entry was added, false if a duplicate was detected.
     */
    public boolean addIfAbsent(DeathlessEntry entry) {
        DeathlessEntry prev = entries.putIfAbsent(entry.originalUuid, entry);
        if (prev == null) {
            setDirty();
            return true;
        }
        return false;
    }

    /** Snapshot of all current entries — safe to iterate while {@link #remove} is called. */
    public Collection<DeathlessEntry> getAll() {
        return new ArrayList<>(entries.values());
    }

    public void remove(UUID originalUuid) {
        if (entries.remove(originalUuid) != null) {
            setDirty();
        }
    }

    public int size() {
        return entries.size();
    }

    // -- SavedData serialisation ---------------------------------------------

    @Override
    public CompoundTag save(CompoundTag compound) {
        ListTag list = new ListTag();
        for (DeathlessEntry e : entries.values()) {
            list.add(e.toNBT());
        }
        compound.put("entries", list);
        return compound;
    }

    private static DeathlessWorldData load(CompoundTag compound) {
        DeathlessWorldData data = new DeathlessWorldData();
        ListTag list = compound.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                DeathlessEntry e = DeathlessEntry.fromNBT(list.getCompound(i));
                data.entries.put(e.originalUuid, e);
            } catch (Exception ex) {
                DeepSpaceMod.LOGGER.warn("[DeathlessWorldData] Skipping corrupt entry #{}: {}",
                    i, ex.getMessage());
            }
        }
        DeepSpaceMod.LOGGER.info("[DeathlessWorldData] Loaded {} pending deathless respawns",
            data.entries.size());
        return data;
    }
}
