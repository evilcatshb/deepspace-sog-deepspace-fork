package com.deepspace.events;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Previously blocked overworld/nether access. Overworld is now a permitted
 * WarpDrive destination so this handler is intentionally inert.
 */
public class OverworldBlocker {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Overworld access is now permitted — players may travel there via WarpDrive.
    }
}
