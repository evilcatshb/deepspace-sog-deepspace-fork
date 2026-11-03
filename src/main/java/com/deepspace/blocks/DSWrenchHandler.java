package com.deepspace.blocks;

import com.deepspace.DSConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Enforces wrench-only breaking for DeepSpace ship component blocks.
 * Creative-mode players are always exempt.
 */
@Mod.EventBusSubscriber(modid = DSConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DSWrenchHandler {

    public static final TagKey<Item> WRENCH_TAG =
        ItemTags.create(new ResourceLocation("forge", "tools/wrench"));

    /**
     * Per-tick destroy progress for wrench-breakable blocks.
     *
     * @param player the player breaking the block
     * @param wrenchTicks number of ticks to break with wrench (60 = 3s)
     */
    public static float destroyProgress(net.minecraft.world.entity.player.Player player, int wrenchTicks) {
        if (player.getAbilities().instabuild) return 1.0f;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.is(WRENCH_TAG)) return 1.0f / (float) wrenchTicks;
        return 1.0f / 100_000.0f; // near-zero; BreakEvent cancels the actual break
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Block block = event.getState().getBlock();

        if (!isWrenchOnlyBlock(block)) return;

        // Creative mode always allowed
        if (event.getPlayer().getAbilities().instabuild) return;

        ItemStack held = event.getPlayer().getMainHandItem();
        if (!held.isEmpty() && held.is(WRENCH_TAG)) return;

        event.setCanceled(true);
        event.getPlayer().sendSystemMessage(Component.literal(
            "\u00a7c[DeepSpace] \u00a7fThis block requires a \u00a7eWrench\u00a7f (forge:tools/wrench) to remove."));
    }

    private static boolean isWrenchOnlyBlock(Block block) {
        return block instanceof ShipScannerBlock;
    }
}
