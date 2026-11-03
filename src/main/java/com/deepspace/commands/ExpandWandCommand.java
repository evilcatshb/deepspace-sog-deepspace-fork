package com.deepspace.commands;

import com.deepspace.DeepSpaceMod;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * /expandwand
 *
 * Takes the player's current WorldEdit wand selection and flood-fills outward
 * from every non-air block on the boundary of that selection, visiting all
 * 6-face-connected non-air blocks.  The final WorldEdit selection is updated
 * to the axis-aligned bounding box of every visited block, so the entire
 * connected solid structure ends up inside the new //wand region.
 *
 * Typical use-case:
 *   1. Use //wand and left/right-click to select a small piece of a celestial
 *      body (planet, asteroid, space station …).
 *   2. Run /expandwand — the selection expands to cover every block connected
 *      to that initial selection that isn't air, across as many chunks as needed.
 *
 * Safety limits
 *   - Hard cap of 10 000 000 blocks visited to prevent freezing the server on
 *     infinite or extremely large structures.
 *   - Only faces (6-connectivity) are used — diagonal blocks are NOT included.
 *
 * Permission: requires op-level 2.
 * Requires: WorldEdit mod present at runtime.
 */
public class ExpandWandCommand {

    private static final boolean WE_LOADED = ModList.get().isLoaded("worldedit");

    /** Maximum blocks the flood-fill will visit before aborting. */
    private static final int MAX_BLOCKS = 10_000_000;

    /** The six face-adjacent offsets (±X, ±Y, ±Z). */
    private static final int[][] FACES = {
        { 1, 0, 0}, {-1, 0, 0},
        { 0, 1, 0}, { 0,-1, 0},
        { 0, 0, 1}, { 0, 0,-1}
    };

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (!WE_LOADED) {
            DeepSpaceMod.LOGGER.warn("[ExpandWand] WorldEdit not loaded — /expandwand disabled.");
            return;
        }
        event.getDispatcher().register(
            Commands.literal("expandwand")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> execute(ctx.getSource()))
        );
        DeepSpaceMod.LOGGER.info("[ExpandWand] /expandwand registered.");
    }

    // -------------------------------------------------------------------------
    // Core logic
    // -------------------------------------------------------------------------

    private int execute(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[expandwand] Must be run by a player."));
            return 0;
        }

        ForgeWorldEdit fwe = ForgeWorldEdit.inst;
        if (fwe == null) {
            source.sendFailure(Component.literal("[expandwand] WorldEdit not initialised."));
            return 0;
        }

        LocalSession session = fwe.getSession(player);
        World weWorld = ForgeAdapter.adapt(player.serverLevel());

        // ── 1. Get the current selection ────────────────────────────────────
        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            source.sendFailure(Component.literal(
                    "[expandwand] No WorldEdit selection active. Use //wand to make a selection first."));
            return 0;
        }

        BlockVector3 selMin = region.getMinimumPoint();
        BlockVector3 selMax = region.getMaximumPoint();

        ServerLevel level = player.serverLevel();

        source.sendSystemMessage(Component.literal(String.format(
                "[expandwand] Starting flood-fill from selection [%d,%d,%d] → [%d,%d,%d] …",
                selMin.getBlockX(), selMin.getBlockY(), selMin.getBlockZ(),
                selMax.getBlockX(), selMax.getBlockY(), selMax.getBlockZ())));

        // ── 2. Seed the BFS with every non-air block inside the selection ───
        //       Using long-encoded positions for memory efficiency.
        Set<Long> visited = new HashSet<>();
        Deque<long[]> queue = new ArrayDeque<>(); // [encodedPos, x, y, z]

        for (int x = selMin.getBlockX(); x <= selMax.getBlockX(); x++) {
            for (int y = selMin.getBlockY(); y <= selMax.getBlockY(); y++) {
                for (int z = selMin.getBlockZ(); z <= selMax.getBlockZ(); z++) {
                    if (!isAir(level, x, y, z)) {
                        long key = encode(x, y, z);
                        if (visited.add(key)) {
                            queue.add(new long[]{key, x, y, z});
                        }
                    }
                }
            }
        }

        if (visited.isEmpty()) {
            source.sendFailure(Component.literal(
                    "[expandwand] The current selection contains only air — nothing to expand from."));
            return 0;
        }

        // ── 3. BFS flood-fill across connected non-air blocks ───────────────
        int minX = selMin.getBlockX(), minY = selMin.getBlockY(), minZ = selMin.getBlockZ();
        int maxX = selMax.getBlockX(), maxY = selMax.getBlockY(), maxZ = selMax.getBlockZ();
        boolean hitLimit = false;

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[1];
            int cy = (int) cur[2];
            int cz = (int) cur[3];

            // Update bounding box
            if (cx < minX) minX = cx;
            if (cy < minY) minY = cy;
            if (cz < minZ) minZ = cz;
            if (cx > maxX) maxX = cx;
            if (cy > maxY) maxY = cy;
            if (cz > maxZ) maxZ = cz;

            // Explore 6 faces
            for (int[] d : FACES) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                int nz = cz + d[2];

                long nkey = encode(nx, ny, nz);
                if (visited.contains(nkey)) continue;
                if (isAir(level, nx, ny, nz)) continue;

                visited.add(nkey);
                queue.add(new long[]{nkey, nx, ny, nz});

                if (visited.size() >= MAX_BLOCKS) {
                    hitLimit = true;
                    break;
                }
            }
            if (hitLimit) break;
        }

        if (hitLimit) {
            source.sendSystemMessage(Component.literal(
                    "§e[expandwand] Warning: hit the " + MAX_BLOCKS + "-block safety limit. "
                    + "The selection covers the explored region but may not include the entire structure."));
        }

        // ── 4. Update the WorldEdit selection ───────────────────────────────
        BlockVector3 newMin = BlockVector3.at(minX, minY, minZ);
        BlockVector3 newMax = BlockVector3.at(maxX, maxY, maxZ);

        CuboidRegionSelector selector = new CuboidRegionSelector(weWorld, newMin, newMax);
        session.setRegionSelector(weWorld, selector);
        selector.learnChanges();

        // Dispatch CUI update so the selection highlights immediately in-game
        selector.explainRegionAdjust(ForgeAdapter.adaptPlayer(player), session);

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        source.sendSystemMessage(Component.literal(
                "§a[expandwand] Done! Selection expanded to " + sizeX + "×" + sizeY + "×" + sizeZ + "."));
        source.sendSystemMessage(Component.literal(String.format(
                "§7  Blocks visited: §f%,d  §7| New bounds: §f[%d,%d,%d] → [%d,%d,%d]",
                visited.size(),
                minX, minY, minZ,
                maxX, maxY, maxZ)));

        DeepSpaceMod.LOGGER.info("[expandwand] Flood-fill complete: {} blocks visited, new AABB {}x{}x{} at [{},{},{}]",
                visited.size(), sizeX, sizeY, sizeZ, minX, minY, minZ);
        return 1;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the block at (x,y,z) should be treated as "empty" and
     * not followed by the flood-fill.  Currently: air, cave-air, void-air.
     */
    private static boolean isAir(ServerLevel level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.isAir();
    }

    /**
     * Encode (x,y,z) as a single long for fast set membership.
     * Y range: uses bits 0-11 (±2048), X and Z each use 26 bits (±33 million).
     * This covers the entire Minecraft world including build-height limits.
     */
    private static long encode(int x, int y, int z) {
        // Pack: x in bits 38..63, y in bits 12..37, z in bits 0..11 — actually
        // use a simpler safe scheme: shift into non-overlapping ranges.
        // Y: valid range −2048..2048 → 12 bits after +2048 offset
        // X, Z: valid range −30,000,000..30,000,000 → 26 bits each after offset
        long lx = (long)(x + 30_000_000) & 0x3FFFFFFL;  // 26 bits
        long ly = (long)(y + 2048)        & 0xFFFL;       // 12 bits
        long lz = (long)(z + 30_000_000) & 0x3FFFFFFL;  // 26 bits
        return (lx << 38) | (ly << 26) | lz;
    }
}
