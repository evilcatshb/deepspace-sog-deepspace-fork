package com.deepspace.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Set;

public class CommandLoginGuard {

    private static final Logger LOGGER = LogManager.getLogger("deepspace.CommandLoginGuard");

    /** Commands offline players may use before completing /login */
    private static final Set<String> WHITELIST = Set.of("login", "register", "auth");

    // Lazy-init handle to OfflineAuthHandler's private static Set<String> loggedIn
    private static volatile Field loggedInField;
    private static volatile boolean reflectionFailed;

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        // TrueUUID assigns real Mojang UUIDs (version 4) on successful Mojang auth.
        // Offline / name-based UUIDs are version 3. Premium players skip the guard.
        if (player.getUUID().version() != 3) return;

        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        String cmdRoot = input.split("\\s+")[0].toLowerCase();
        if (WHITELIST.contains(cmdRoot)) return;

        if (isOfflineAuthAuthenticated(player)) return;

        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(
                "§c[Auth] You must §f/login <password> §cbefore using commands."));
    }

    /**
     * Checks offlineauth's private static Set<String> loggedIn field via reflection.
     * Returns true (allow) when the player's name is present in that set.
     * NOTE: isOfflinePlayer(player) is NOT used here — that method checks
     * TrueuuidApi.isPremium() permanently and is unrelated to session login state.
     * Fails open if offlineauth is absent or reflection fails.
     */
    @SuppressWarnings("unchecked")
    private static boolean isOfflineAuthAuthenticated(ServerPlayer player) {
        if (reflectionFailed) return true;
        try {
            if (loggedInField == null) {
                synchronized (CommandLoginGuard.class) {
                    if (loggedInField == null) {
                        Class<?> cls = Class.forName("cn.alini.offlineauth.OfflineAuthHandler");
                        Field f = cls.getDeclaredField("loggedIn");
                        f.setAccessible(true);
                        loggedInField = f;
                    }
                }
            }
            Set<String> loggedIn = (Set<String>) loggedInField.get(null);
            return loggedIn.contains(player.getName().getString());
        } catch (Exception e) {
            LOGGER.warn("[CommandLoginGuard] offlineauth reflection failed (failing open): {}", e.getMessage());
            reflectionFailed = true;
            return true;
        }
    }
}
