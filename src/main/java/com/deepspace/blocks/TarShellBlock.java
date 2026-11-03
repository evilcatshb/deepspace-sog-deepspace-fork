package com.deepspace.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Tar Shell -- the dark carbonaceous outer crust of oil-bearing asteroids.
 * Signals the presence of a GT oil fluid vein below.
 */
public class TarShellBlock extends Block {

    public TarShellBlock(Properties properties) {
        super(properties);
    }

    /** Slightly sticky -- slower mining than stone. Handled by Properties.strength. */
}
