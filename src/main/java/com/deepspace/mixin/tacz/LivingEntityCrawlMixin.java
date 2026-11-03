package com.deepspace.mixin.tacz;

import alpvax.mc.goprone.PlayerProneData;
import alpvax.mc.goprone.platform.service.Services;
import com.deepspace.DeepSpaceMod;
import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Server-side twin of LocalPlayerCrawlMixin.
 *
 * TACZ.LivingEntityCrawl#setCrawlPose() has the identical unconditional-clear
 * pattern as LocalPlayerCrawl (see that class's javadoc for the full root
 * cause), but this copy is driven by TACZ's *common* mixin
 * (com.tacz.guns.mixin.common.LivingEntityMixin), meaning it runs on the
 * server/logical side and governs the authoritative entity -- the one that
 * actually determines collision AABB / hitbox size, not just the visual
 * render pose.
 *
 * Without this half of the fix, GoProne's toggle looks correct client-side
 * (LocalPlayerCrawlMixin handles that) but the server-authoritative entity
 * still has its forced pose cleared every tick by this class, so the
 * player's hitbox never actually shrinks and they can't fit through a
 * 1-block gap.
 */
@Mixin(value = LivingEntityCrawl.class, remap = false)
public class LivingEntityCrawlMixin {

    @Redirect(
        method = "setCrawlPose",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setForcedPose(Lnet/minecraft/world/entity/Pose;)V"
        ),
        remap = false
    )
    private void deepspace$dontClearGoPronePose(Player player, Pose pose) {
        if (pose == null) {
            boolean goProneWantsProne = Services.PRONE.getProneData(player)
                    .map(PlayerProneData::shouldBeProne)
                    .orElse(false);

            if (goProneWantsProne) {
                DeepSpaceMod.LOGGER.debug(
                        "[TaczGoProneCompat] Skipped TACZ's server-side forced-pose clear for {} -- GoProne toggle is active.",
                        player.getGameProfile().getName()
                );
                return;
            }
        }
        player.setForcedPose(pose);
    }
}
