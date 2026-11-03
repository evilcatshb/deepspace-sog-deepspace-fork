package com.deepspace.structures;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A fully-registered structure instance — combines placement location, the
 * template metadata, and the bounding box of the placed structure.
 *
 * Instances are created either:
 *   - By {@link StructurePlacer} after a successful placement (worldgen or /importship).
 *   - Synthetically by {@link StructureRadiusMapImpl#predictStructures} for chunks not
 *     yet generated (the location and template are known deterministically; bounds are
 *     estimated from cached template sizes).
 */
public class RegisteredStructure {

    /** Placed world-space location + living mob list. */
    public final StructureLocation location;

    /** NBT source metadata (filename + compile/runtime flag). */
    public final StructureTemplateEntry template;

    /**
     * Axis-aligned bounding box of the placed structure in world space.
     * Null for predicted (not-yet-placed) structures — callers must handle null.
     */
    public BoundingBox bounds;

    /**
     * Sector key used by {@link StructureSectorGrid}.
     * Packed as {@code (sectorX << 32) | (sectorZ & 0xFFFFFFFFL)}.
     * Allows deduplication when merging loaded + predicted structure lists.
     */
    public final long sectorKey;

    public RegisteredStructure(StructureLocation location,
                                StructureTemplateEntry template,
                                BoundingBox bounds,
                                long sectorKey) {
        this.location  = location;
        this.template  = template;
        this.bounds    = bounds;
        this.sectorKey = sectorKey;
    }

    /** Convenience accessor — delegates to location. */
    public List<MobData> getMobs() {
        return location.mobs;
    }

    @Override
    public String toString() {
        return "RegisteredStructure{" + template.nbtFilename
               + " @ [" + location.x + "," + location.y + "," + location.z + "]"
               + " dim=" + location.dimensionName + '}';
    }
}
