package com.deepspace.events;

import com.deepspace.mixin.CapabilityProviderGatherInvoker;
import earth.terrarium.botarium.common.fluid.base.BotariumFluidItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Belt-and-suspenders safety net for the Botarium capability desync fix.
 *
 * When a player logs in, their entire inventory (including armor slots and
 * offhand) was just deserialized from NBT via {@code ItemStack.of(CompoundTag)}.
 * The {@code BotariumCapabilityRebuildMixin} already hooks that constructor,
 * but this handler ensures that even if an edge case slips through the mixin,
 * every BotariumFluidItem in the player's inventory has correct capabilities
 * before any game-tick logic runs.
 *
 * This also handles the case where the player's inventory was loaded from a
 * world save that was created before this fix was deployed.
 */
@Mod.EventBusSubscriber(modid = "deepspace")
public class BotariumCapabilityLoginHandler {

    private static final Logger LOGGER = LogManager.getLogger("deepspace");

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Inventory inv = player.getInventory();
        int rebuilt = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BotariumFluidItem<?>)) continue;

            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("BotariumData", 10)) continue;

            // Force capability rebuild: invalidate → revive → gather fresh
            CapabilityProviderGatherInvoker invoker =
                (CapabilityProviderGatherInvoker) (Object) stack;
            invoker.deepspace$invalidateCaps();
            invoker.deepspace$reviveCaps();
            invoker.deepspace$gatherCapabilities(
                () -> stack.getItem().initCapabilities(stack, null)
            );
            rebuilt++;
        }

        if (rebuilt > 0) {
            LOGGER.info("[DeepSpace] Rebuilt Botarium capabilities for {} item(s) in {}'s inventory on login.",
                rebuilt, player.getName().getString());
        }
    }
}
