package com.deepspace.structures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Holds data for a single mob/entity that was spawned as part of a
 * randomly placed (or imported) NBT structure.
 *
 * entityLiving / entityBase are runtime references — they are null before
 * the structure is placed, and may become null again if the chunk is unloaded
 * (but the mob is not dead).  Dead mobs are periodically pruned by
 * StructureRadiusMapImpl.tickMobValidation().
 */
public class MobData {

    /** The structure this mob belongs to (set after registration). */
    public RegisteredStructure parentStructure;

    /** Spawn position inside the structure (relative to the structure's NBT origin). */
    public int spawn_x;
    public int spawn_y;
    public int spawn_z;

    /** Last-known health when the entity was spawned or last checked. */
    public int health;

    /** Entity type name (e.g. "Villager", "Cow", "Pig"). */
    public String entity_type;

    /** Vanilla structure entity storage data — contains full serialised NBT. */
    public StructureEntityInfo entityInfo;

    /** Runtime living entity reference (null if not yet placed or chunk unloaded). */
    public LivingEntity entityLiving;

    /** Runtime base entity reference (null if not yet placed or chunk unloaded). */
    public Entity entityBase;

    /**
     * Instance tags baked into the NBT template at structure-file creation time.
     * Populated by {@link com.deepspace.structures.StructureRadiusMapImpl#readEntitiesFromNbt}.
     * Null when created via {@link com.deepspace.structures.StructurePlacer} (live spawn path).
     */
    public @Nullable java.util.List<String> nbtTags;

    public MobData(
            int spawn_x,
            int spawn_y,
            int spawn_z,
            int health,
            String entity_type,
            StructureEntityInfo entityInfo,
            LivingEntity entityLiving,
            Entity entityBase
    ) {
        this.spawn_x      = spawn_x;
        this.spawn_y      = spawn_y;
        this.spawn_z      = spawn_z;
        this.health       = health;
        this.entity_type  = entity_type;
        this.entityInfo   = entityInfo;
        this.entityLiving = entityLiving;
        this.entityBase   = entityBase;
    }
}
