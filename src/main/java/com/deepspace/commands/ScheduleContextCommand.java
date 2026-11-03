package com.deepspace.commands;

import com.deepspace.DeepSpaceMod;
import com.deepspace.scheduler.ScheduleContextManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Privileged claimant-context scheduler used by DeepSpace PAXI systems. */
public final class ScheduleContextCommand {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("schedule_ctx")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("function")
                        .then(Commands.argument("function", ResourceLocationArgument.id())
                                .then(Commands.argument("time", TimeArgument.time())
                                        .executes(ctx -> schedule(ctx, true))
                                        .then(Commands.literal("replace").executes(ctx -> schedule(ctx, true)))
                                        .then(Commands.literal("append").executes(ctx -> schedule(ctx, false))))));
        event.getDispatcher().register(root);
        DeepSpaceMod.LOGGER.info("[ScheduleCtx] /schedule_ctx registered.");
    }

    private int schedule(CommandContext<CommandSourceStack> ctx, boolean replace) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer claimant)) {
            source.sendFailure(Component.literal("[schedule_ctx] This command requires a player executor; console cannot create a claimant-bound event."));
            return 0;
        }
        ResourceLocation functionId = ResourceLocationArgument.getId(ctx, "function");
        int delayTicks = ctx.getArgument("time", Integer.class);
        if (claimant.getServer().getFunctions().get(functionId).isEmpty()) {
            source.sendFailure(Component.literal("[schedule_ctx] Unknown function: " + functionId));
            return 0;
        }
        try {
            ScheduleContextManager.getOrCreate(claimant.getServer()).schedule(claimant, functionId, delayTicks, replace);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("[schedule_ctx] " + e.getMessage()));
            return 0;
        }
        return 1;
    }
}
