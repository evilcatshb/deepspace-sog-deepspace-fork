package com.deepspace.deathless;

import com.deepspace.DeepSpaceMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Periodically restores the deathless tag on faction traders.
 *
 * The faction PAXI datapack can remove "deathless" from trader entities
 * during load-time normalization. DeepSpace requires traders to participate
 * in the deathless respawn system, so this server-side enforcer restores the
 * tag whenever a loaded trader is found without it.
 */
public final class TraderDeathlessEnforcer {

    /** Check once per second (20 ticks). */
    private static final int CHECK_INTERVAL_TICKS = 20;
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!entity.getTags().contains("trader")) continue;
                if (entity.getTags().contains(DeathlessDeathHandler.DEATHLESS_TAG)) continue;

                entity.addTag(DeathlessDeathHandler.DEATHLESS_TAG);
                DeepSpaceMod.LOGGER.debug(
                    "[Deathless] Restored deathless tag on trader {} ({}) in {}",
                    entity.getUUID(), entity.getType(), level.dimension().location()
                );
            }
        }
    }
}
