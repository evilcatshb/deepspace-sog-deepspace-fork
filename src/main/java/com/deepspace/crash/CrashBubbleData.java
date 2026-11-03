package com.deepspace.crash;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Persisted data for one player's personal crash bubble.
 *
 * Stored in world/data/deepspace_world_data.dat via DSWorldData.
 *
 * shipOriginX/Y/Z is the south-west bottom corner of the placed structure
 * (the pos argument to placeInWorld).  The geometric centre of a 15x15 ship
 * is at (shipOriginX + 7, shipOriginY, shipOriginZ + 7).
 */
public final class CrashBubbleData {

    public final UUID playerUUID;
    /** Chunk X of crash site centre. */
    public final int centerChunkX;
    /** Chunk Z of crash site centre. */
    public final int centerChunkZ;
    /**
     * Block X of the SW bottom corner of the placed ship structure
     * (the pos passed to placeInWorld).
     */
    public final int shipOriginX;
    /**
     * Block Y of the bottom of the placed ship structure -- i.e. the Y of the
     * asteroid's top surface (ship floor sits flush here).
     * Populated correctly by CrashedShipPlacer after placement.
     * The allocator sets a placeholder of 64 which is updated on placement.
     */
    public final int shipOriginY;
    /**
     * Block Z of the SW bottom corner of the placed ship structure.
     */
    public final int shipOriginZ;
    /** Personal seed derived from worldSeed XOR playerUUID. */
    public final long personalSeed;
    /** Schema version for future migration. */
    public final int version;

    public static final int CURRENT_VERSION = 1;

    // Half-width/depth of the 15x15 ship footprint (integer centre offset)
    public static final int SHIP_HALF = 7;

    public CrashBubbleData(UUID playerUUID, int centerChunkX, int centerChunkZ,
                           int shipOriginX, int shipOriginY, int shipOriginZ,
                           long personalSeed) {
        this.playerUUID   = playerUUID;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.shipOriginX  = shipOriginX;
        this.shipOriginY  = shipOriginY;
        this.shipOriginZ  = shipOriginZ;
        this.personalSeed = personalSeed;
        this.version      = CURRENT_VERSION;
    }

    /**
     * Return a new CrashBubbleData identical to this one but with updated
     * ship origin coordinates.  Used by CrashedShipPlacer after placement
     * to record the actual surface Y (the allocator stores a placeholder Y=64).
     */
    public CrashBubbleData withShipOrigin(int x, int y, int z) {
        return new CrashBubbleData(
            playerUUID, centerChunkX, centerChunkZ, x, y, z, personalSeed);
    }

    /**
     * The horizontal centre of the ship footprint.
     * placeInWorld uses the SW corner as origin; the centre is offset by SHIP_HALF.
     */
    public int shipCentreX() { return shipOriginX + SHIP_HALF; }
    public int shipCentreZ() { return shipOriginZ + SHIP_HALF; }

    /**
     * Axis-aligned bounding box of the placed ship (+margin), as
     * {@code [minX,minY,minZ,maxX,maxY,maxZ]} in block coords. Asteroids must
     * never intersect this box (BUG_1).
     */
    public int[] shipAabb(int margin) {
        int w = CrashedShipPlacer.SHIP_W;
        int d = CrashedShipPlacer.SHIP_D;
        int h = CrashedShipPlacer.SHIP_INTERIOR_HEIGHT;
        return new int[] {
            shipOriginX - 1 - margin,
            shipOriginY - 2 - margin,
            shipOriginZ - 1 - margin,
            shipOriginX + w + 1 + margin,
            shipOriginY + h + 6 + margin,
            shipOriginZ + d + 1 + margin
        };
    }

    /** True if the sphere (cx,cy,cz,r) intersects this ship's AABB(+margin). */
    public boolean shipIntersectsSphere(int cx, int cy, int cz, int r, int margin) {
        int[] b = shipAabb(margin);
        double nx = Math.max(b[0], Math.min(cx, b[3]));
        double ny = Math.max(b[1], Math.min(cy, b[4]));
        double nz = Math.max(b[2], Math.min(cz, b[5]));
        double ddx = cx - nx, ddy = cy - ny, ddz = cz - nz;
        return ddx * ddx + ddy * ddy + ddz * ddz <= (double) r * r;
    }

    /** True if the AABB (axMin..axMax, ...) intersects this ship's AABB(+margin). */
    public boolean shipIntersectsAabb(int axMin, int ayMin, int azMin,
                                      int axMax, int ayMax, int azMax, int margin) {
        int[] b = shipAabb(margin);
        return axMax >= b[0] && axMin <= b[3]
            && ayMax >= b[1] && ayMin <= b[4]
            && azMax >= b[2] && azMin <= b[5];
    }

    // -- NBT serialisation ----------------------------------------------------

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid",  playerUUID);
        tag.putInt("cx",     centerChunkX);
        tag.putInt("cz",     centerChunkZ);
        tag.putInt("ox",     shipOriginX);
        tag.putInt("oy",     shipOriginY);
        tag.putInt("oz",     shipOriginZ);
        tag.putLong("seed",  personalSeed);
        tag.putInt("ver",    version);
        return tag;
    }

    public static CrashBubbleData fromNBT(CompoundTag tag) {
        return new CrashBubbleData(
            tag.getUUID("uuid"),
            tag.getInt("cx"),
            tag.getInt("cz"),
            tag.getInt("ox"),
            tag.getInt("oy"),
            tag.getInt("oz"),
            tag.getLong("seed"));
    }
}
