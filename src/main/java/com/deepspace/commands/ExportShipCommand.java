package com.deepspace.commands;

import com.deepspace.DeepSpaceMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * /exportship [FILENAME]
 *
 * Op-only command (level 4). Exports a WorldEdit selection to a Minecraft 1.20.1
 * structure NBT file, INCLUDING all entities/NPCs within the selection bounds.
 *
 * Entity NBT is fully preserved: VillagerData, Offers (trades), health, equipment,
 * rotation, UUID, custom names, and all other attributes.
 *
 * Usage:
 *   /exportship               -- saves as crashed_ship_<timestamp>.nbt
 *   /exportship myship        -- saves as myship.nbt
 *   /exportship myship.nbt    -- same, explicit extension
 *
 * Output: <world>/deepspace_exports/<filename>.nbt
 */
public class ExportShipCommand {

    private static final boolean WE_LOADED = ModList.get().isLoaded("worldedit");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (!WE_LOADED) {
            DeepSpaceMod.LOGGER.warn("[ExportShip] WorldEdit not loaded -- /exportship disabled.");
            return;
        }
        event.getDispatcher().register(
            Commands.literal("exportship")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> execute(ctx.getSource(), null))
                .then(Commands.argument("filename", StringArgumentType.word())
                    .executes(ctx -> execute(ctx.getSource(),
                            StringArgumentType.getString(ctx, "filename"))))
        );
        DeepSpaceMod.LOGGER.info("[ExportShip] /exportship registered.");
    }

    private int execute(CommandSourceStack source, String filenameArg) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[exportship] Must be run by a player."));
            return 0;
        }

        ForgeWorldEdit fwe = ForgeWorldEdit.inst;
        if (fwe == null) {
            source.sendFailure(Component.literal("[exportship] WorldEdit not initialised."));
            return 0;
        }

        LocalSession session = fwe.getSession(player);
        World weWorld = ForgeAdapter.adapt(player.serverLevel());

        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            source.sendFailure(Component.literal(
                    "[exportship] No WorldEdit selection active. Use the wand to select your ship first."));
            return 0;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int sizeX = max.getBlockX() - min.getBlockX() + 1;
        int sizeY = max.getBlockY() - min.getBlockY() + 1;
        int sizeZ = max.getBlockZ() - min.getBlockZ() + 1;

        source.sendSystemMessage(Component.literal(String.format(
                "[exportship] Exporting %dx%dx%d selection ...", sizeX, sizeY, sizeZ)));

        ServerLevel level = player.serverLevel();

        // Build palette
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
        palette.add(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        paletteIndex.put(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 0);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (!paletteIndex.containsKey(state)) {
                        paletteIndex.put(state, palette.size());
                        palette.add(state);
                    }
                }
            }
        }

        // Build block entries
        ListTag blocksTag = new ListTag();
        int blockCount = 0, beCount = 0;
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(worldPos);
                    int rx = x - min.getBlockX();
                    int ry = y - min.getBlockY();
                    int rz = z - min.getBlockZ();

                    CompoundTag entry = new CompoundTag();
                    ListTag posList = new ListTag();
                    posList.add(IntTag.valueOf(rx));
                    posList.add(IntTag.valueOf(ry));
                    posList.add(IntTag.valueOf(rz));
                    entry.put("pos", posList);
                    entry.putInt("state", paletteIndex.get(state));

                    BlockEntity be = level.getBlockEntity(worldPos);
                    if (be != null) {
                        CompoundTag beNbt = be.saveWithFullMetadata();
                        beNbt.remove("x"); beNbt.remove("y"); beNbt.remove("z");
                        entry.put("nbt", beNbt);
                        beCount++;
                    }
                    blocksTag.add(entry);
                    blockCount++;
                }
            }
        }

        // Build entities tag — full NBT for all non-player entities in selection
        ListTag entitiesTag = buildEntitiesTag(level, min, max);
        int entityCount = entitiesTag.size();

		DeepSpaceMod.LOGGER.info("[EXPORT] Entities exported: {}", entitiesTag.size());

		for (int i = 0; i < entitiesTag.size(); i++) {
			CompoundTag e = entitiesTag.getCompound(i);
			CompoundTag nbt = e.getCompound("nbt");

			DeepSpaceMod.LOGGER.info("[EXPORT] Entity #{} id={}", i, nbt.getString("id"));
		}

        // Build palette NBT
        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            CompoundTag stateTag = new CompoundTag();
            var blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            stateTag.putString("Name", blockId != null ? blockId.toString() : "minecraft:air");
            if (!state.getProperties().isEmpty()) {
                CompoundTag propsTag = new CompoundTag();
                for (Property<?> prop : state.getProperties()) {
                    propsTag.putString(prop.getName(), getPropertyValue(state, prop));
                }
                stateTag.put("Properties", propsTag);
            }
            paletteTag.add(stateTag);
        }

        // Assemble root
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3120);
        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(sizeX));
        sizeTag.add(IntTag.valueOf(sizeY));
        sizeTag.add(IntTag.valueOf(sizeZ));
        root.put("size", sizeTag);
        root.put("palette", paletteTag);
        root.put("blocks", blocksTag);
        root.put("entities", entitiesTag);

        // Determine filename
        String baseName;
        if (filenameArg != null && !filenameArg.isBlank()) {
            baseName = Path.of(filenameArg).getFileName().toString();
            if (!baseName.endsWith(".nbt")) baseName += ".nbt";
        } else {
            baseName = "crashed_ship_" + LocalDateTime.now().format(TS) + ".nbt";
        }
        String latestName = baseName.replace(".nbt", "_latest.nbt");

        // Write output
        Path outDir = level.getServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("deepspace_exports");
        try { Files.createDirectories(outDir); }
        catch (IOException e) {
            source.sendFailure(Component.literal("[exportship] Could not create output dir: " + e.getMessage()));
            return 0;
        }

        Path outFile = outDir.resolve(baseName);
        Path latestFile = outDir.resolve(latestName);
        try {
            writeGzippedNbt(root, outFile);
            writeGzippedNbt(root, latestFile);
        } catch (IOException e) {
            source.sendFailure(Component.literal("[exportship] Write failed: " + e.getMessage()));
            return 0;
        }

        source.sendSystemMessage(Component.literal("\u00a7a[exportship] Export complete!"));
        source.sendSystemMessage(Component.literal(
                "\u00a77  Blocks: \u00a7f" + blockCount
                + " \u00a77| Block entities: \u00a7f" + beCount
                + " \u00a77| Entities/NPCs: \u00a7f" + entityCount
                + " \u00a77| Palette: \u00a7f" + palette.size()));
        source.sendSystemMessage(Component.literal("\u00a77  Size: \u00a7f" + sizeX + "x" + sizeY + "x" + sizeZ));
        source.sendSystemMessage(Component.literal("\u00a77  Origin: \u00a7f" + fmt(min)));
        source.sendSystemMessage(Component.literal("\u00a7e  Saved to:"));
        source.sendSystemMessage(Component.literal("\u00a7f    " + outFile.toAbsolutePath()));
        source.sendSystemMessage(Component.literal("\u00a7f    " + latestFile.toAbsolutePath()));
        source.sendSystemMessage(Component.literal(
                "\u00a77  To use as a random structure, copy to config/deepspace/structures/ and run /reload"));
        if (entityCount > 0) {
            source.sendSystemMessage(Component.literal(
                    "\u00a7a  " + entityCount + " entities/NPCs exported with full NBT (trades, health, rotation, etc.)"));
        }
        DeepSpaceMod.LOGGER.info("[exportship] Exported {} blocks ({} BEs, {} entities, {} palette) to {}",
                blockCount, beCount, entityCount, palette.size(), outFile);
        return 1;
    }

    /**
     * Collect all non-player entities within the selection AABB.
     * Saves full NBT including VillagerData, Offers, health, equipment, rotation.
     * Format: entities ListTag with each entry: {pos:[d,d,d], blockPos:[i,i,i], nbt:{...}}
     */
    private static ListTag buildEntitiesTag(ServerLevel level, BlockVector3 min, BlockVector3 max) {
        ListTag result = new ListTag();

        double minX = min.getBlockX(), minY = min.getBlockY(), minZ = min.getBlockZ();
        double maxXd = max.getBlockX() + 1.0, maxYd = max.getBlockY() + 1.0, maxZd = max.getBlockZ() + 1.0;

        AABB box = new AABB(minX, minY, minZ, maxXd, maxYd, maxZd);

        List<Entity> entities = level.getEntities((Entity) null, box, e -> !(e instanceof Player));

        for (Entity entity : entities) {
            try {
				CompoundTag entityNbt = new CompoundTag();
				entity.saveWithoutId(entityNbt);

				// Manually ensure ID is present (CRITICAL)
				entityNbt.putString("id",
					ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());

                // Store relative position as doubles (exact sub-block precision preserved)
                double relX = entity.getX() - minX;
                double relY = entity.getY() - minY;
                double relZ = entity.getZ() - minZ;

                CompoundTag entry = new CompoundTag();

                ListTag posTag = new ListTag();
                posTag.add(DoubleTag.valueOf(relX));
                posTag.add(DoubleTag.valueOf(relY));
                posTag.add(DoubleTag.valueOf(relZ));
                entry.put("pos", posTag);

                ListTag bpTag = new ListTag();
                bpTag.add(IntTag.valueOf((int) Math.floor(entity.getX()) - min.getBlockX()));
                bpTag.add(IntTag.valueOf((int) Math.floor(entity.getY()) - min.getBlockY()));
                bpTag.add(IntTag.valueOf((int) Math.floor(entity.getZ()) - min.getBlockZ()));
                entry.put("blockPos", bpTag);

                entry.put("nbt", entityNbt);
                result.add(entry);

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ExportShip] Failed to export entity {}: {}",
                        entity.getType().toShortString(), e.getMessage());
            }
        }
        return result;
    }

    private static void writeGzippedNbt(CompoundTag tag, Path path) throws IOException {
        try (var out = new java.util.zip.GZIPOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            NbtIo.write(tag, new DataOutputStream(out));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> String getPropertyValue(BlockState state, Property<T> prop) {
        return prop.getName(state.getValue(prop));
    }

    private static String fmt(BlockVector3 v) {
        return String.format("[%d, %d, %d]", v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }
}
