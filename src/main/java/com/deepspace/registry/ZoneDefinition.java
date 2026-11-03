package com.deepspace.registry;

import java.util.List;

/**
 * Immutable data object for one distance-based progression zone.
 */
public final class ZoneDefinition {

    public final String id;
    public final String name;
    public final int distanceMin;
    public final int distanceMax;
    public final List<String> tiers;

    public ZoneDefinition(String id, String name, int distanceMin, int distanceMax,
                          List<String> tiers) {
        this.id = id;
        this.name = name;
        this.distanceMin = distanceMin;
        this.distanceMax = distanceMax;
        this.tiers = List.copyOf(tiers);
    }

    /** Returns true if blockDistance falls within this zone. */
    public boolean contains(double blockDistance) {
        return blockDistance >= distanceMin && blockDistance < distanceMax;
    }
}
