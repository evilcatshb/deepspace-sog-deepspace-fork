package com.deepspace.structures;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class StructureLifecycleBootstrap {

    private StructureLifecycleBootstrap() {}

    public static void run(
            ServerLevel level,
            BlockPos origin,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {

        DeepSpaceMod.LOGGER.info("[StructureInit] Universal machine activation");

        List<BlockEntity> machines = new ArrayList<>();

        // PASS 1 — collect block entities
        for (int dx = -3; dx <= sizeX + 3; dx++)
        for (int dy = -1; dy <= sizeY + 3; dy++)
        for (int dz = -3; dz <= sizeZ + 3; dz++) {

            BlockPos pos = origin.offset(dx, dy, dz);
            BlockEntity be = level.getBlockEntity(pos);

            if (be != null)
                machines.add(be);
        }

        // PASS 2 — lifecycle restore
        for (BlockEntity be : machines) {
            try {

                if (!be.hasLevel())
                    be.setLevel(level);

                be.onLoad();

                be.invalidateCaps();
                be.reviveCaps();

            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn(
                        "[StructureInit] revive failed at {}",
                        be.getBlockPos()
                );
            }
        }

        // PASS 3 — neighbor rebuild
        for (BlockEntity be : machines) {

            BlockPos pos = be.getBlockPos();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            try {
                level.updateNeighborsAt(pos, block);
                level.sendBlockUpdated(pos, state, state, 3);
            } catch (Exception ignored) {}
        }

        // PASS 4 — delayed activation (CRITICAL)
        level.getServer().execute(() -> {

            for (BlockEntity be : machines) {
                try {
                    be.setChanged();

                    BlockPos pos = be.getBlockPos();
                    BlockState state = level.getBlockState(pos);

                    state.getBlock().onPlace(state, level, pos, state, false);

                } catch (Exception ignored) {}
            }

            DeepSpaceMod.LOGGER.info("[StructureInit] Machine activation complete");
        });
    }
}