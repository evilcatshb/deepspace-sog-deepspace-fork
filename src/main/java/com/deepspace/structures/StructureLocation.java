package com.deepspace.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the placed world-space location of a registered structure.
 *
 * Named "StructureLocation" to avoid conflict with Minecraft's
 * {@link net.minecraft.resources.ResourceLocation}.
 *
 * The {@code mobs} list is kept up-to-date in realtime by
 * {@link StructureRadiusMapImpl#tickMobValidation(net.minecraft.server.level.ServerLevel)}.
 */
public class StructureLocation {

    /** Dimension ID, e.g. "deepspace:deep_space". */
    public String dimensionName;

    /** World-space X coordinate of the structure centre. */
    public int x;

    /** World-space Y coordinate of the structure centre. */
    public int y;

    /** World-space Z coordinate of the structure centre. */
    public int z;

    /**
     * Living mobs that were spawned inside this structure and are still alive.
     *
     * Thread-safe list: worldgen and tick events run on different threads.
     * Dead / confirmed-dead mobs are removed by tickMobValidation().
     */
    public List<MobData> mobs = new CopyOnWriteArrayList<>();

    public StructureLocation(String dimensionName, int x, int y, int z) {
        this.dimensionName = dimensionName;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
