package com.deepspace.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SpaceWhaleRenderer extends GeoEntityRenderer<SpaceWhaleEntity> {

    private static final float SCALE = 8.0f;

    public SpaceWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new SpaceWhaleModel());
        this.shadowRadius = 5.0f;
    }

    @Override
    public void render(SpaceWhaleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
