package com.deepspace.deathless;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * One pending deathless respawn.
 *
 * Captured the moment a tagged LivingEntity dies — holds everything needed
 * to reconstruct that entity later at the same position in the same dimension.
 */
public final class DeathlessEntry {

    /** Original entity's UUID at time of death (used as queue key, NOT reused on respawn). */
    public final UUID originalUuid;
    /** Registry name of the entity type, e.g. "minecraft:zombie". */
    public final String entityTypeId;
    /** Full entity NBT (the result of saveWithoutId). */
    public final CompoundTag entityNbt;
    /** Death position. */
    public final double x, y, z;
    /** Death rotation. */
    public final float yaw, pitch;
    /** Dimension registry name, e.g. "minecraft:overworld" or "deepspace:deep_space". */
    public final String dimensionId;
    /** Server tick (level.getGameTime + delay) at which the respawn should fire. */
    public final long respawnTick;

    public DeathlessEntry(UUID originalUuid,
                          String entityTypeId,
                          CompoundTag entityNbt,
                          double x, double y, double z,
                          float yaw, float pitch,
                          String dimensionId,
                          long respawnTick) {
        this.originalUuid = originalUuid;
        this.entityTypeId = entityTypeId;
        this.entityNbt    = entityNbt;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.dimensionId = dimensionId;
        this.respawnTick = respawnTick;
    }

    CompoundTag toNBT() {
        CompoundTag t = new CompoundTag();
        t.putUUID("uuid", originalUuid);
        t.putString("type", entityTypeId);
        t.put("nbt", entityNbt);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putFloat("yaw", yaw);
        t.putFloat("pitch", pitch);
        t.putString("dim", dimensionId);
        t.putLong("tick", respawnTick);
        return t;
    }

    static DeathlessEntry fromNBT(CompoundTag t) {
        return new DeathlessEntry(
            t.getUUID("uuid"),
            t.getString("type"),
            t.getCompound("nbt").copy(),
            t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
            t.getFloat("yaw"), t.getFloat("pitch"),
            t.getString("dim"),
            t.getLong("tick")
        );
    }
}
