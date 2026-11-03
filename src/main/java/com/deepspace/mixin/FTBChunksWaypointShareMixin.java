package com.deepspace.mixin;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbchunks.net.ShareWaypointPacket;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes FTB Chunks 2001.3.6 waypoint-share commands for waypoint names that
 * require quoting (for example, names containing spaces).
 *
 * FTB Chunks 2001.3.6 builds:
 *   /ftbchunks waypoint add-dim <name> <x> <y> <z> <dimension> white true
 * without escaping/quoting <name>. This lambda runs in the server-side
 * ShareWaypointPacket handler, where the clickable chat command is created.
 *
 * Brigadier's StringArgumentType.escapeIfRequired() produces the exact
 * quoted/escaped representation accepted by StringArgumentType.string().
 */
@Mixin(ShareWaypointPacket.class)
public abstract class FTBChunksWaypointShareMixin {

    @Shadow(remap = false)
    @Final
    private String name;

    @Inject(
            method = "lambda$handle$1",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void deepspace$fixWaypointShareCommand(
            String cords,
            String dim,
            Style style,
            CallbackInfoReturnable<Style> cir
    ) {
        String command = "/ftbchunks waypoint add-dim "
                + StringArgumentType.escapeIfRequired(name)
                + " "
                + cords
                + " "
                + dim
                + " white true";

        cir.setReturnValue(
                style.withClickEvent(
                        new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                command
                        )
                )
        );
    }
}
