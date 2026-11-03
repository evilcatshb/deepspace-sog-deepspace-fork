package com.deepspace.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;

/**
 * Alien Stone -- dense crystalline crust found on Zone 4 naquadah asteroids.
 * High blast resistance. Requires diamond pickaxe or higher to mine.
 * Breaks in ~0.3 seconds (6 ticks) with a diamond-tier or better pickaxe.
 */
public class AlienStoneBlock extends Block {
    public AlienStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player,
                                    BlockGetter level, BlockPos pos) {
        if (player.getAbilities().instabuild) return 1.0f;

        ItemStack held = player.getMainHandItem();
        // Check if holding a pickaxe that can harvest this block
        if (!held.isEmpty() && held.isCorrectToolForDrops(state)) {
            return 1.0f / 6.0f; // 6 ticks = 0.3 s
        }
        // Wrong tool or empty hand — extremely slow (effectively unbreakable)
        return 1.0f / 100_000.0f;
    }
}
