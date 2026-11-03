package com.deepspace.compat.superchat;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Super Chat 3.1.0 compatibility orchestration.
 * The verified handler is ChatInteractionHandler.onServerChat(ServerChatEvent).
 * HIGHEST is mandatory so this does not depend on NORMAL registration order.
 */
@Mod.EventBusSubscriber(modid = "deepspace", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SuperChatDiscordCompat {
    private static final Logger LOGGER = LogManager.getLogger("deepspace-superchat-compat");
    private static final AtomicBoolean HANDLER_WARNED = new AtomicBoolean();
    private static volatile Method superChatHandler;

    private SuperChatDiscordCompat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        if (event.isCanceled() || !ModList.get().isLoaded("super_chat")) return;
        try {
            Method method = resolveSuperChatHandler();
            if (method == null) return;
            method.invoke(null, event);
        } catch (Throwable t) {
            warnHandler(t);
        }
    }

    private static Method resolveSuperChatHandler() throws ReflectiveOperationException {
        Method cached = superChatHandler;
        if (cached != null) return cached;
        Class<?> type = Class.forName("net.mcreator.superchat.command.ChatInteractionHandler", false,
                SuperChatDiscordCompat.class.getClassLoader());
        Method method = type.getMethod("onServerChat", ServerChatEvent.class);
        superChatHandler = method;
        return method;
    }

    private static void warnHandler(Throwable t) {
        if (HANDLER_WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[SuperChatCompat] Could not invoke verified Super Chat handler; leaving normal Forge chat flow intact.", t);
        }
    }
}
