package com.deepspace.compat.superchat;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discord Integration 3.0.7.1 bridge to its existing chat(ServerChatEvent).
 * It does not post Forge events, broadcast Minecraft chat, or reformat Discord.
 */
public final class SuperChatDiscordIntegrationBridge {
    private static final Logger LOGGER = LogManager.getLogger("deepspace-superchat-compat");
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile Method chatMethod;

    private SuperChatDiscordIntegrationBridge() {}

    public static void invoke(ServerChatEvent event) {
        if (!ModList.get().isLoaded("dcintegration")) return;
        try {
            Method method = resolve();
            Object mod = ModList.get().getModObjectById("dcintegration").orElse(null);
            if (mod == null) throw new IllegalStateException("Forge returned no dcIntegration mod object");
            method.invoke(mod, event);
        } catch (Throwable t) {
            if (WARNED.compareAndSet(false, true)) {
                LOGGER.warn("[SuperChatCompat] Could not invoke Discord Integration chat bridge; Super Chat/Minecraft chat remains unchanged.", t);
            }
        }
    }

    private static Method resolve() throws ReflectiveOperationException {
        Method cached = chatMethod;
        if (cached != null) return cached;
        Class<?> type = Class.forName("de.erdbeerbaerlp.dcintegration.forge.DiscordIntegrationMod", false,
                SuperChatDiscordIntegrationBridge.class.getClassLoader());
        Method method = type.getMethod("chat", ServerChatEvent.class);
        chatMethod = method;
        return method;
    }
}
