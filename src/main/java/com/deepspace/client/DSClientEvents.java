package com.deepspace.client;

import com.deepspace.DSConstants;
import com.deepspace.blocks.ShipScannerBlockEntity;
import com.deepspace.entity.SpaceWhaleRenderer;
import com.deepspace.init.DSBlocks;
import com.deepspace.init.DSEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DSConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DSClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DSEntities.SPACE_WHALE.get(), SpaceWhaleRenderer::new);

        // BUG_14: register the cover overlay renderer so installed wireless energy
        // covers show their texture on the correct face of the ship scanner block.
        event.registerBlockEntityRenderer(
                DSBlocks.SHIP_SCANNER_BE.get(),
                ctx -> new DSCoverOverlayRenderer<ShipScannerBlockEntity>(ctx,
                        ShipScannerBlockEntity::getCoverHolder));
    }
}
