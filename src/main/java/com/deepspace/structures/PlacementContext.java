package com.deepspace.structures;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Set;

/**
 * Describes who triggered a structure placement and why.
 *
 * Used by {@link StructurePlacer} to decide:
 *   - Whether to check FTB Chunks claims.
 *   - Which player to send result messages to.
 *   - Whether to skip the safe-location search (worldgen always searches).
 */
public class PlacementContext {

    /** True when the structure is placed during chunk generation. */
    public final boolean isWorldgen;

    /**
     * True when the structure is placed manually (via /importship, a quest,
     * or any other non-worldgen trigger).
     */
    public final boolean isSpawned;

    /**
     * Username of the player who triggered placement, or null for worldgen.
     * When set, result messages are sent to this player's chat.
     */
    @Nullable
    public final String spawnerUsername;

    /**
     * UUID of the player who is the "target" of the import — i.e. the USERNAME
     * argument of /importship.  Structure is placed near this player, and chunks
     * claimed by this player (or their FTB team) are allowed.
     * Null if not applicable (worldgen, or console without a USERNAME argument).
     */
    @Nullable
    public final UUID targetPlayerUUID;

    /**
     * UUIDs of all players who are allowed to have their claimed chunks used as
     * placement targets (the target player + their FTB team members).
     * Populated by ImportShipCommand after FTBTeams lookup.
     */
    @Nullable
    public final Set<UUID> allowedChunkOwners;

    // -------------------------------------------------------------------------

    public PlacementContext(boolean isWorldgen, boolean isSpawned,
                             @Nullable String spawnerUsername,
                             @Nullable UUID targetPlayerUUID,
                             @Nullable Set<UUID> allowedChunkOwners) {
        this.isWorldgen        = isWorldgen;
        this.isSpawned         = isSpawned;
        this.spawnerUsername   = spawnerUsername;
        this.targetPlayerUUID  = targetPlayerUUID;
        this.allowedChunkOwners = allowedChunkOwners;
    }

    // -------------------------------------------------------------------------
    // Convenience factories
    // -------------------------------------------------------------------------

    /** Use for chunk-generation placements (no FTB claim check, no chat messages). */
    public static PlacementContext worldgen() {
        return new PlacementContext(true, false, null, null, null);
    }

    /**
     * Use for /importship or quest-triggered placements.
     *
     * @param spawnerUsername  Username of the op/admin/quest that ran the command.
     * @param targetPlayerUUID UUID of the USERNAME argument player (may equal spawner).
     * @param allowedOwners    Set of UUIDs allowed to own chunks in the footprint.
     */
    public static PlacementContext spawned(@Nullable String spawnerUsername,
                                            @Nullable UUID targetPlayerUUID,
                                            @Nullable Set<UUID> allowedOwners) {
        return new PlacementContext(false, true, spawnerUsername, targetPlayerUUID, allowedOwners);
    }
}
