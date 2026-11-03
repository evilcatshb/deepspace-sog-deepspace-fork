package com.deepspace.mixin;

import com.deepspace.compat.superchat.SuperChatDiscordIntegrationBridge;
import net.minecraftforge.event.ServerChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Super Chat 3.1.0 target: invoke Discord Integration 3.0.7.1 after the
 * verified Minecraft broadcast, before Super Chat cancels ServerChatEvent.
 * DI's existing chat method owns all Discord formatting and sending.
 */
@Mixin(targets = "net.mcreator.superchat.command.ChatInteractionHandler", remap = false)
public abstract class SuperChatDiscordIntegrationMixin {
    @Inject(
            method = "onServerChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = false
    )
    private static void deepspace$discordAfterBroadcast(ServerChatEvent event, CallbackInfo ci) {
        SuperChatDiscordIntegrationBridge.invoke(event);
    }
}
