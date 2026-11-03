package com.deepspace.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exotic Ice block -- found inside Zone 3 ice asteroids.
 *
 * Properties (deepspace-design.md ?10 / deepspace-other.md ?13):
 *   - Heater recipe: 1 Exotic Ice -> 16,000 mB water
 *   - Rare but reliable in Zone 3 ice zones
 *   - Rewards exploration (midgame logistics breakthrough)
 *   - Does NOT melt on its own (unlike vanilla ice) -- requires LV Heater
 *
 * The LV Heater recipe is registered via KubeJS in server_scripts.
 */
public class ExoticIceBlock extends Block {

    public ExoticIceBlock(Properties properties) {
        super(properties);
    }

    /** Slight transparency -- similar to packed ice. */
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.8f;
    }

    /** Does not melt from light sources. Only processable via GT Heater. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }
}
