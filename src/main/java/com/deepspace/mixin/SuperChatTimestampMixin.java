package com.deepspace.mixin;

import com.deepspace.compat.superchat.SuperChatBfcrAdapter;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Super Chat 3.1.0 target: ChatInteractionHandler.onServerChat(ServerChatEvent).
 * Modifies only the verified PlayerList.m_240416_(Component, boolean) argument.
 * The original Super Chat rich Component is preserved as a child unchanged.
 */
@Mixin(targets = "net.mcreator.superchat.command.ChatInteractionHandler", remap = false)
public abstract class SuperChatTimestampMixin {
    @ModifyArg(
            method = "onServerChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V",
                    remap = false
            ),
            index = 0,
            remap = false
    )
    private static Component deepspace$wrapTimestamp(Component original) {
        return SuperChatBfcrAdapter.wrap(original);
    }
}
