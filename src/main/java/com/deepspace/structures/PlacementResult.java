package com.deepspace.structures;

/**
 * Result of a {@link StructurePlacer#placeStructure} call.
 *
 * Used for:
 *   - Chat feedback to the triggering player.
 *   - Logging with [DeepSpace_Structures] prefix.
 *   - FTB Quests success/fail callbacks.
 */
public enum PlacementResult {

    /** Structure was placed successfully. */
    SPAWNED,

    /** One or more chunks in the structure footprint are claimed by another player/team. */
    ERROR_CHUNK_CLAIMED,

    /**
     * No safe air-only volume was found within the search radius.
     * This can happen if the area around the target player is very dense
     * with blocks, or if the structure is extremely large.
     */
    ERROR_SAFE_LOCATION,

    /** The requested filename was not found in the registry. */
    ERROR_FILENAME_MISSING,

    /** The NBT data for the structure could not be loaded or parsed. */
    ERROR_INVALID_NBT,

    /**
     * An entity mod required by the structure is not loaded.
     * Placement continues, but entity/mob will be skipped.
     */
    ERROR_ENTITY_MOD_MISSING,

    /** The target player named in USERNAME argument is not online. */
    ERROR_PLAYER_OFFLINE,

    /** An unexpected exception occurred during placement. */
    ERROR_INTERNAL;

    /** True if placement succeeded (no error). */
    public boolean isSuccess() {
        return this == SPAWNED;
    }
}
