package com.deepspace.compat.enderio;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defensive sanitization of EnderIO conduit data that crashes the server.
 *
 * SYMPTOM:
 *   java.lang.IllegalStateException: no node matching original type
 *     at com.enderio.conduits.common.conduit.ConduitBundle.getNodeFor(ConduitBundle.java:303)
 *     -> ConduitBlockEntity.getCapability
 *     -> CapacitorBank.serverTick (pushEnergy to neighbour)
 *
 * CAUSE:
 *   When DeepSpace places a structure NBT (e.g. collective_dome_astroid.nbt) that
 *   contains EnderIO conduit_bundle block entities, the embedded BE NBT can be
 *   inconsistent with the runtime EnderIO version (duplicate / missing
 *   type<->node mappings).  EnderIO loads the bundle without validation but
 *   throws as soon as a neighbour calls getCapability on it during its tick.
 *
 * FIX (two prongs):
 *   1. Pre-placement: strip every per-block "nbt" sub-tag whose palette state
 *      Name starts with "enderio:conduit".  The block still places, but EnderIO
 *      creates a fresh empty bundle on first access -- no broken nodes.
 *
 *   2. Runtime cleanup (deep_space dimension only): on chunk load, probe every
 *      "enderio:conduit*" block entity by calling getCapability with each
 *      direction / each common capability.  If any throws IllegalStateException,
 *      the bundle is broken and we replace the block with air.  Restricted to
 *      Deep Space so we never disturb player-built conduits in other dimensions.
 */
public final class EnderIOConduitSanitizer {

    private static final String ENDERIO_CONDUIT_PREFIX = "enderio:conduit";

    private static final Capability<?>[] PROBE_CAPS = new Capability<?>[] {
        ForgeCapabilities.ENERGY,
        ForgeCapabilities.ITEM_HANDLER,
        ForgeCapabilities.FLUID_HANDLER
    };

    // -------------------------------------------------------------------------
    // Pre-placement sanitization
    // -------------------------------------------------------------------------

    /**
     * Strip BE NBT for every block whose palette state Name starts with
     * "enderio:conduit".  Mutates the given CompoundTag in place; idempotent.
     * Safe to call regardless of whether EnderIO is loaded.
     *
     * @return number of stripped block-entity NBT tags
     */
    public static int sanitizeStructureNbt(CompoundTag nbt) {
        if (nbt == null) return 0;

        Set<Integer> conduitStateIndices = new HashSet<>();

        // Single-palette form: "palette" : ListTag<CompoundTag>
        if (nbt.contains("palette", Tag.TAG_LIST)) {
            collectConduitStateIndices(nbt.getList("palette", Tag.TAG_COMPOUND), conduitStateIndices);
        }
        // Multi-palette form: "palettes" : ListTag<ListTag<CompoundTag>>
        // (all palette variants share the same state indices, so reading the
        //  first variant is sufficient).
        if (nbt.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = nbt.getList("palettes", Tag.TAG_LIST);
            if (!palettes.isEmpty()) {
                collectConduitStateIndices(palettes.getList(0), conduitStateIndices);
            }
        }

        if (conduitStateIndices.isEmpty()) return 0;

        int stripped = 0;
        if (nbt.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag block = blocks.getCompound(i);
                int state = block.getInt("state");
                if (conduitStateIndices.contains(state) && block.contains("nbt", Tag.TAG_COMPOUND)) {
                    block.remove("nbt");
                    stripped++;
                }
            }
        }
        return stripped;
    }

    private static void collectConduitStateIndices(ListTag palette, Set<Integer> out) {
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompound(i);
            String name = entry.getString("Name");
            if (name.startsWith(ENDERIO_CONDUIT_PREFIX)) {
                out.add(i);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Runtime cleanup (chunk-load handler)
    // -------------------------------------------------------------------------

    /**
     * On chunk load in the Deep Space dimension, schedule a deferred scan of
     * the loaded chunk for broken EnderIO conduit block entities.  We defer to
     * the next server tick to ensure all BEs are fully constructed and to
     * keep the work off the chunk-load path.
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!DeepSpaceMod.ENDERIO_LOADED) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!DSConstants.DEEP_SPACE_KEY.equals(level.dimension())) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        server.execute(() -> sanitizeLoadedChunk(level, chunk));
    }

    private static void sanitizeLoadedChunk(ServerLevel level, LevelChunk chunk) {
        // Snapshot to avoid ConcurrentModificationException if removeBlock mutates the BE map.
        List<BlockEntity> candidates = new ArrayList<>();
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            if (key != null && key.toString().startsWith(ENDERIO_CONDUIT_PREFIX)) {
                candidates.add(be);
            }
        }
        if (candidates.isEmpty()) return;

        for (BlockEntity be : candidates) {
            if (be.isRemoved()) continue;
            if (isBroken(be)) {
                BlockPos pos = be.getBlockPos();
                DeepSpaceMod.LOGGER.warn(
                    "[EnderIOConduitSanitizer] Removing broken EnderIO conduit at {} in {} (NBT load corruption -- 'no node matching original type')",
                    pos, level.dimension().location());
                level.removeBlock(pos, false);
            }
        }
    }

    private static boolean isBroken(BlockEntity be) {
        for (Direction dir : Direction.values()) {
            for (Capability<?> cap : PROBE_CAPS) {
                try {
                    be.getCapability(cap, dir).resolve();
                } catch (IllegalStateException ex) {
                    return true;
                } catch (Throwable ignored) {
                    // Other failures: don't blame the BE; treat as healthy.
                }
            }
        }
        // Also probe with null direction (some queries pass null)
        for (Capability<?> cap : PROBE_CAPS) {
            try {
                be.getCapability(cap, null).resolve();
            } catch (IllegalStateException ex) {
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
