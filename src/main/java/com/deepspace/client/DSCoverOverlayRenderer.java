package com.deepspace.client;

import com.deepspace.compat.gtceu.DSCoverHolder;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Field;

/**
 * BUG_14: Renders GTCEu cover overlay textures on the Ship Scanner block faces.
 *
 * <p>Mirrors WarpDrive's {@code CoverOverlayRenderer} but lives in deepspace so
 * that deepspace does NOT need warpdrive as a compile-time dependency.
 *
 * @param <T> the block entity type hosting a DSCoverHolder
 */
@OnlyIn(Dist.CLIENT)
public class DSCoverOverlayRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    @FunctionalInterface
    public interface CoverHolderGetter<T extends BlockEntity> {
        DSCoverHolder get(T be);
    }

    private final CoverHolderGetter<T> holderGetter;

    public DSCoverOverlayRenderer(BlockEntityRendererProvider.Context ctx,
                                   CoverHolderGetter<T> holderGetter) {
        this.holderGetter = holderGetter;
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        DSCoverHolder holder = holderGetter.get(entity);
        if (holder == null) return;

        for (Direction face : GTUtil.DIRECTIONS) {
            CoverBehavior cover = holder.getCoverAtSide(face);
            if (cover == null) continue;
            TextureAtlasSprite sprite = getCoverSprite(cover);
            if (sprite == null) continue;
            renderFaceOverlay(poseStack, buffer, face, sprite, packedLight, packedOverlay);
        }
    }

    private static TextureAtlasSprite getCoverSprite(CoverBehavior cover) {
        try {
            var rendererSupplier = cover.coverDefinition.getCoverRenderer();
            if (rendererSupplier == null) return null;
            Object renderer = rendererSupplier.get();
            if (renderer == null) return null;
            for (Field f : renderer.getClass().getDeclaredFields()) {
                if (TextureAtlasSprite.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(renderer);
                    if (val instanceof TextureAtlasSprite sprite) return sprite;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void renderFaceOverlay(PoseStack poseStack, MultiBufferSource buffer,
                                           Direction face, TextureAtlasSprite sprite,
                                           int light, int overlay) {
        float offset = 1.001f / 16.0f;
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();
        Matrix3f norm = poseStack.last().normal();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        switch (face) {
            case UP    -> quad(consumer, mat, norm, light, overlay, 0,1+offset,0, 1,1+offset,0, 1,1+offset,1, 0,1+offset,1, u0,v0,u1,v1, 0,1,0);
            case DOWN  -> quad(consumer, mat, norm, light, overlay, 0,-offset,1, 1,-offset,1, 1,-offset,0, 0,-offset,0, u0,v0,u1,v1, 0,-1,0);
            case NORTH -> quad(consumer, mat, norm, light, overlay, 1,1,-offset, 0,1,-offset, 0,0,-offset, 1,0,-offset, u0,v0,u1,v1, 0,0,-1);
            case SOUTH -> quad(consumer, mat, norm, light, overlay, 0,1,1+offset, 1,1,1+offset, 1,0,1+offset, 0,0,1+offset, u0,v0,u1,v1, 0,0,1);
            case WEST  -> quad(consumer, mat, norm, light, overlay, -offset,1,0, -offset,1,1, -offset,0,1, -offset,0,0, u0,v0,u1,v1, -1,0,0);
            case EAST  -> quad(consumer, mat, norm, light, overlay, 1+offset,1,1, 1+offset,1,0, 1+offset,0,0, 1+offset,0,1, u0,v0,u1,v1, 1,0,0);
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer c, Matrix4f mat, Matrix3f norm, int light, int overlay,
                              float x0,float y0,float z0, float x1,float y1,float z1,
                              float x2,float y2,float z2, float x3,float y3,float z3,
                              float u0,float v0,float u1,float v1, float nx,float ny,float nz) {
        vertex(c,mat,norm,x3,y3,z3,u0,v1,light,overlay,nx,ny,nz);
        vertex(c,mat,norm,x2,y2,z2,u1,v1,light,overlay,nx,ny,nz);
        vertex(c,mat,norm,x1,y1,z1,u1,v0,light,overlay,nx,ny,nz);
        vertex(c,mat,norm,x0,y0,z0,u0,v0,light,overlay,nx,ny,nz);
    }

    private static void vertex(VertexConsumer c, Matrix4f mat, Matrix3f norm,
                                float x,float y,float z, float u,float v,
                                int light, int overlay, float nx,float ny,float nz) {
        c.vertex(mat,x,y,z).color(255,255,255,255).uv(u,v)
         .overlayCoords(overlay).uv2(light).normal(norm,nx,ny,nz).endVertex();
    }
}
