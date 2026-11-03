package com.deepspace.commands;

import com.deepspace.structures.RegisteredStructure;
import com.deepspace.structures.StructureRadiusMapImpl;
import com.deepspace.api.IStructureRadiusMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;
import java.util.Optional;

public class StructureCommand {

    private static final int DEFAULT_RADIUS = 10000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ds")
                .then(Commands.literal("structure")
                    // Admin-only
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("near")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                int radius = IntegerArgumentType.getInteger(context, "radius");
                                return executeNear(context.getSource(), radius);
                            })
                        )
                        .executes(context -> executeNear(context.getSource(), DEFAULT_RADIUS))
                    )
                    .then(Commands.literal("list")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                int radius = IntegerArgumentType.getInteger(context, "radius");
                                return executeList(context.getSource(), radius);
                            })
                        )
                        .executes(context -> executeList(context.getSource(), DEFAULT_RADIUS))
                    )
                )
        );
    }

    private static int executeNear(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[DS] Command can only be run by a player.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        if (level == null) {
            source.sendFailure(Component.literal("[DS] Deep Space dimension is not loaded.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();

        Optional<RegisteredStructure> nearest = StructureRadiusMapImpl.INSTANCE.findNearestStructure(
            level, playerPos, radius
        );

        if (nearest.isPresent()) {
            RegisteredStructure structure = nearest.get();
            BlockPos structurePos = new BlockPos(structure.location.x, structure.location.y, structure.location.z);
            double distance = Math.sqrt(playerPos.distSqr(structurePos));

            Component coords = Component.literal("(" + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ() + ")")
                .withStyle(Style.EMPTY
                    .withColor(TextColor.fromRgb(0xFF55FF)) // pink
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/execute in deepspace:deep_space run tp @s "
                        + structurePos.getX() + " " + structurePos.getY() + " " + structurePos.getZ()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to teleport")))
                );

            Component message = Component.literal("[DS] Closest structure: ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))) // green
                .append(coords)
                .append(Component.literal(" distance " + (int) distance + " " + structure.template.nbtFilename)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))));

            source.sendSuccess(() -> message, false);
        } else {
            source.sendSuccess(() -> Component.literal("[DS] No structure found within " + radius + " blocks.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))), false);
        }

        return 1;
    }

    private static int executeList(CommandSourceStack source, int radius) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[DS] Command can only be run by a player.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        if (level == null) {
            source.sendFailure(Component.literal("[DS] Deep Space dimension is not loaded.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))));
            return 0;
        }

        List<RegisteredStructure> structures = StructureRadiusMapImpl.INSTANCE.getStructuresInRadius(
            level, level.getSeed(), player.blockPosition(), radius
        );

        if (structures.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[DS] No structures found within " + radius + " blocks.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))), false);
            return 0;
        }

        for (RegisteredStructure structure : structures) {
            BlockPos pos = new BlockPos(structure.location.x, structure.location.y, structure.location.z);
            double distance = Math.sqrt(player.blockPosition().distSqr(pos));

            Component coords = Component.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")")
                .withStyle(Style.EMPTY
                    .withColor(TextColor.fromRgb(0xFF55FF)) // pink
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/execute in deepspace:deep_space run tp @s "
                        + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to teleport")))
                );

            Component message = Component.literal("[DS] Structure: ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))) // green
                .append(coords)
                .append(Component.literal(" distance " + (int) distance + " " + structure.template.nbtFilename)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))); // green

            source.sendSuccess(() -> message, false);
        }

        return 1;
    }
}