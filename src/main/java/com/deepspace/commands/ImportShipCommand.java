package com.deepspace.commands;

import com.deepspace.DeepSpaceMod;
import com.deepspace.structures.DeepSpaceStructureRegistry;
import com.deepspace.structures.PlacementContext;
import com.deepspace.structures.PlacementResult;
import com.deepspace.structures.StructurePlacer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * /importship [FILENAME] [USERNAME]
 *
 * Admin/op-only command (also usable by FTB Quests with elevated permissions,
 * and from server console — but USERNAME is required from console).
 *
 * Places the named .nbt structure at the nearest safe location to the target
 * player (USERNAME argument) or the executing player.
 *
 * FILENAME autocompletes from all loaded NBT filenames (compile-time + runtime).
 * USERNAME autocompletes from all online player names.
 *
 * Result message is always sent to the USERNAME target player (if online).
 *
 * Usage examples:
 *   /importship abandoned_station.nbt
 *   /importship trader_outpost.nbt BOB
 *   (from console): /importship wreck_small.nbt ALICE
 */
public class ImportShipCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> cmd =
            Commands.literal("importship")
                // Require op level 2 (admin). FTB Quests uses elevated source which also passes.
                .requires(src -> src.hasPermission(2))
                // /importship <filename>
                .then(Commands.argument("filename", StringArgumentType.word())
                    .suggests(this::suggestFilenames)
                    // /importship <filename> <target>
                    // EntityArgument.player() accepts plain names AND selectors (@p, @r, @a[...])
                    // so FTB Quests can pass @p and Brigadier will resolve it correctly.
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> executeWithTarget(ctx,
                                StringArgumentType.getString(ctx, "filename"),
                                EntityArgument.getPlayer(ctx, "target")))
                    )
                    // /importship <filename> (no target)
                    .executes(ctx -> executeNoTarget(ctx,
                            StringArgumentType.getString(ctx, "filename")))
                );

        event.getDispatcher().register(cmd);
        DeepSpaceMod.LOGGER.info("[ImportShip] /importship registered.");
    }

    // -------------------------------------------------------------------------
    // Suggest filenames from the registry
    // -------------------------------------------------------------------------

    private CompletableFuture<Suggestions> suggestFilenames(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : DeepSpaceStructureRegistry.getInstance().getAllNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    // -------------------------------------------------------------------------
    // Execution: with explicit target (name or selector like @p)
    // -------------------------------------------------------------------------

    private int executeWithTarget(
            CommandContext<CommandSourceStack> ctx,
            String filename,
            ServerPlayer targetPlayer) {

        CommandSourceStack src = ctx.getSource();
        String targetUsername = targetPlayer.getName().getString();
        ServerLevel level = targetPlayer.serverLevel();
        BlockPos origin = targetPlayer.blockPosition();

        return doPlace(src, filename, targetUsername, level, origin, targetPlayer);
    }

    // -------------------------------------------------------------------------
    // Execution: no USERNAME argument
    // -------------------------------------------------------------------------

    private int executeNoTarget(
            CommandContext<CommandSourceStack> ctx,
            String filename) {

        CommandSourceStack src = ctx.getSource();

        // Console requires a USERNAME
        if (!(src.getEntity() instanceof ServerPlayer)) {
            src.sendFailure(Component.literal(
                    "[importship] When run from server console, the USERNAME argument is required. " +
                    "Usage: /importship <filename> <username>"));
            return 0;
        }

        ServerPlayer caller = (ServerPlayer) src.getEntity();
        String username = caller.getName().getString();
        ServerLevel level = src.getLevel();
        // Use execution context position so /execute positioned ~ ~20 ~ works
        BlockPos origin = BlockPos.containing(src.getPosition());

        return doPlace(src, filename, username, level, origin, caller);
    }

    // -------------------------------------------------------------------------
    // Shared placement logic
    // -------------------------------------------------------------------------

    private int doPlace(
            CommandSourceStack src,
            String filename,
            String targetUsername,
            ServerLevel level,
            BlockPos origin,
            ServerPlayer targetPlayer) {

        // Validate filename
        if (!DeepSpaceStructureRegistry.getInstance().has(filename)) {
            String msg = "[DeepSpace] [StructurePlacer] " + filename + " for " + targetUsername
                    + " @ " + level.dimension().location() + " [" + origin.getX() + "," + origin.getY() + "," + origin.getZ() + "]"
                    + " - RESULT: " + PlacementResult.ERROR_FILENAME_MISSING.name();
            DeepSpaceMod.LOGGER.warn("[ImportShip] Unknown file: {}. {}", filename, msg);
            return 0;
        }

        // Build context: find target player's team members for FTB claim checking
        UUID targetUUID = targetPlayer != null ? targetPlayer.getUUID() : null;
        Set<UUID> allowedOwners = buildAllowedOwners(targetPlayer);

        PlacementContext context = PlacementContext.spawned(
                src.getEntity() instanceof ServerPlayer sp ? sp.getName().getString() : "console",
                targetUUID,
                allowedOwners
        );

        DeepSpaceMod.LOGGER.info("[ImportShip] Placing {} near {} ...", filename, targetUsername);

        // Run on server thread (commands already run there, but placement can be slow)
        PlacementResult result = StructurePlacer.placeStructure(level, origin, filename, context);

        // Log result to server console only
        if (targetPlayer != null) {
            StructurePlacer.sendResultMessage(level, filename, targetUsername, origin, result);
        }

        if (result.isSuccess()) {
            DeepSpaceMod.LOGGER.info("[ImportShip] {} placed successfully near {}.", filename, targetUsername);
            return 1;
        } else {
            DeepSpaceMod.LOGGER.warn("[ImportShip] Placement failed for {} near {}: {}",
                    filename, targetUsername, result.name());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build the set of UUIDs allowed to own FTBChunks claims in the placement footprint.
     * Includes the target player themselves plus their FTB team members (via reflection).
     */
    private static Set<UUID> buildAllowedOwners(ServerPlayer targetPlayer) {
        if (targetPlayer == null) return Collections.emptySet();
        Set<UUID> owners = new HashSet<>();
        owners.add(targetPlayer.getUUID());
        // Try to add FTB team members via reflection
        try {
            Class<?> teamsApiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            java.lang.reflect.Method apiMethod = teamsApiClass.getMethod("api");
            Object api = apiMethod.invoke(null);

            java.lang.reflect.Method getTeamMethod = api.getClass().getMethod("getPlayerTeam",
                    java.util.UUID.class);
            Object team = getTeamMethod.invoke(api, targetPlayer.getUUID());
            if (team != null) {
                java.lang.reflect.Method getMembers = team.getClass().getMethod("getMembers");
                Object memberSet = getMembers.invoke(team);
                if (memberSet instanceof Set<?> ms) {
                    for (Object member : ms) {
                        if (member instanceof UUID uid) owners.add(uid);
                    }
                }
            }
        } catch (Exception ignored) {
            // FTBTeams not loaded or API changed — just use the player alone
        }
        return owners;
    }
}
