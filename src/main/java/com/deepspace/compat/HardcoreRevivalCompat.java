package com.deepspace.compat;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.blay09.mods.hardcorerevival.capability.HardcoreRevivalData;
import net.blay09.mods.hardcorerevival.config.HardcoreRevivalConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Bridges ichttt's First Aid mod with Hardcore Revival.
 *
 * First Aid cancels LivingHurtEvent (LOWEST priority), which prevents
 * LivingDamageEvent from ever firing. HC Revival only listens to
 * LivingDamageEvent (via Balm), so it never activates. When First Aid
 * kills the player via setHealth(0), LivingDeathEvent fires — we intercept
 * it here and hand off to HC Revival's knockout mechanic.
 */
@Mod.EventBusSubscriber(modid = DSConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HardcoreRevivalCompat {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeathFirstAidBridge(LivingDeathEvent event) {
        if (!ModList.get().isLoaded("hardcorerevival")) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) return;

        // Let HC Revival's own timer-death (not_rescued_in_time) through
        ResourceLocation damageTypeId = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getKey(event.getSource().type());
        if (damageTypeId != null && "hardcorerevival".equals(damageTypeId.getNamespace())) return;

        HardcoreRevivalData data = HardcoreRevival.getRevivalData(player);
        if (data.isKnockedOut()) {
            // Player already downed — First Aid keeps re-killing via continued damage ticks.
            // Cancel this death to hold them on the HC Revival downed screen.
            event.setCanceled(true);
            player.setHealth(1.0f);
            return;
        }

        // Respect HC Revival's instantDeathSources config
        String damageIdStr = damageTypeId != null ? damageTypeId.toString() : "";
        if (HardcoreRevivalConfig.getActive().instantDeathSources.contains(damageIdStr)) return;

        // Respect HC Revival's instantDeathEntityTypes config
        var attacker = event.getSource().getEntity();
        if (attacker != null) {
            var attackerTypeId = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType());
            if (attackerTypeId != null && HardcoreRevivalConfig.getActive()
                    .instantDeathEntityTypes.contains(attackerTypeId.toString())) return;
        }

        // Respect HC Revival's singleplayer / lonely-multiplayer config
        var server = player.getServer();
        if (server != null) {
            if (HardcoreRevivalConfig.getActive().disableInSingleplayer && server.isSingleplayer()) return;
            if (HardcoreRevivalConfig.getActive().disableInLonelyMultiplayer
                    && !server.isSingleplayer() && server.getPlayerCount() == 1) return;
        }

        // Cancel vanilla death and hand off to HC Revival
        event.setCanceled(true);
        player.setHealth(1.0f);  // First Aid set health to 0; reset to prevent death-loop
        HardcoreRevival.getManager().knockout(player, event.getSource());

        DeepSpaceMod.LOGGER.debug("[HardcoreRevivalCompat] Bridged First Aid death -> HC Revival knockout for {}",
                player.getName().getString());
    }
}
