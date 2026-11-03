package com.deepspace.compat.kubejs;

/**
 * LargeBreederReactorEnergyTracker — now a no-op.
 *
 * The Large Breeder Reactor uses vanilla GTCEu recipes for uranium consumption
 * and EU output.  No CC:Tweaked integration or custom tick logic is needed.
 *
 * This class is kept so the existing registration line in DeepSpaceMod.java
 * continues to compile without change:
 *
 *   forgeBus.register(new LargeBreederReactorEnergyTracker());
 *
 * You may safely remove both this class and that registration line if desired.
 */
public class LargeBreederReactorEnergyTracker {
    // intentionally empty — all reactor logic is handled by the GTCEu recipe system
}
