package com.deepspace.entity;

import com.deepspace.DSConstants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SpaceWhaleModel extends GeoModel<SpaceWhaleEntity> {

    private static final ResourceLocation MODEL =
        new ResourceLocation(DSConstants.MODID, "geo/space_whale.geo.json");
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(DSConstants.MODID, "textures/entity/space_whale.png");
    // BUG_4: Alternate texture shown when a Space Whale Saddle is equipped
    private static final ResourceLocation TEXTURE_SADDLED =
        new ResourceLocation(DSConstants.MODID, "textures/entity/space_whale_saddled.png");
    private static final ResourceLocation ANIMATIONS =
        new ResourceLocation(DSConstants.MODID, "animations/space_whale.animation.json");

    @Override
    public ResourceLocation getModelResource(SpaceWhaleEntity entity) { return MODEL; }

    @Override
    public ResourceLocation getTextureResource(SpaceWhaleEntity entity) {
        // BUG_4: Switch to saddled texture when the whale has a saddle equipped
        return entity.hasSaddle() ? TEXTURE_SADDLED : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SpaceWhaleEntity entity) { return ANIMATIONS; }
}
