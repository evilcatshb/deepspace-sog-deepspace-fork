package com.deepspace.commands;

import com.deepspace.DSConstants;
import com.deepspace.entity.MigrationManager;
import com.deepspace.entity.SpaceWhaleEntity;
import com.deepspace.entity.goal.MigrationCircleGoal;
import com.deepspace.init.DSEntities;
import com.deepspace.init.DSDimensions;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * /spacewhale spawn           – spawns one Space Whale at the op's position
 * /spacewhale migrate         – manually triggers the migration event
 * /spacewhale migrate progress – immediately advances circling whales to retreat
 */
public class SpaceWhaleCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spacewhale")
                .requires(src -> src.hasPermission(2)) // OP level 2

                // /spacewhale spawn
                .then(Commands.literal("spawn")
                    .executes(ctx -> {
                        CommandSourceStack src = ctx.getSource();
                        ServerLevel level = src.getLevel();
                        Vec3 pos = src.getPosition();

                        SpaceWhaleEntity whale = DSEntities.SPACE_WHALE.get().create(level);
                        if (whale == null) {
                            src.sendFailure(Component.literal("[SpaceWhale] Failed to create entity."));
                            return 0;
                        }

                        double spawnY = Math.max(pos.y, DSConstants.ASTEROID_BAND_Y_MAX + 20.0);
                        whale.setPos(pos.x, spawnY, pos.z);
                        whale.setNoGravity(true);
                        whale.finalizeSpawn(level,
                            level.getCurrentDifficultyAt(whale.blockPosition()),
                            MobSpawnType.COMMAND, null, null);
                        level.addFreshEntity(whale);

                        src.sendSuccess(() -> Component.literal(
                            "Spawned Space Whale at " +
                            (int)pos.x + ", " + (int)spawnY + ", " + (int)pos.z)
                            .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))

                // /spacewhale migrate
                .then(Commands.literal("migrate")
                    .executes(ctx -> {
                        CommandSourceStack src = ctx.getSource();
                        MigrationManager.INSTANCE.triggerMigration(src.getServer());
                        src.sendSuccess(() -> Component.literal(
                            "Migration event triggered. Whales spawning from the south.")
                            .withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    })

                    // /spacewhale migrate progress
                    .then(Commands.literal("progress")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            int count = MigrationManager.INSTANCE.progressMigration(src.getServer());
                            if (count == 0) {
                                src.sendSuccess(() -> Component.literal(
                                    "No migrating Space Whales found to advance.")
                                    .withStyle(ChatFormatting.YELLOW), false);
                            } else {
                                src.sendSuccess(() -> Component.empty()
                                    .append(Component.literal("Sent ").withStyle(ChatFormatting.GREEN))
                                    .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(" migration whale(s) into retreat phase.").withStyle(ChatFormatting.GREEN)), true);
                            }
                            return count;
                        })))
        );
    }
}
