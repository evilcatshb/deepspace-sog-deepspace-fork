package com.deepspace.mixin.tacz;

import alpvax.mc.goprone.PlayerProneData;
import alpvax.mc.goprone.platform.service.Services;
import com.deepspace.DeepSpaceMod;
import com.tacz.guns.client.gameplay.LocalPlayerCrawl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes GoProne's "Prone (Toggle)" keybind being silently overridden by TACZ.
 *
 * Root cause (confirmed via javap on the distributed TACZ jar):
 * TACZ.LocalPlayerCrawl#tickCrawl() calls setCrawlPose() every client tick,
 * unconditionally, regardless of whether the player is holding a gun. When
 * TACZ's own isCrawling flag is false (i.e. the player never used TACZ's own
 * crawl key), setCrawlPose() calls LocalPlayer#setForcedPose(null) every tick,
 * which clears the single global forced-pose slot on the entity -- including
 * whatever GoProne set moments earlier in the same tick sequence via
 * PlayerProneData#updateProneState() -> Services.PRONE.setForcedPose(player, Pose.SWIMMING).
 *
 * Since there's only one forced-pose slot and no cooperation between the two
 * mods, TACZ's later unconditional clear wins every tick, so GoProne's toggle
 * never becomes visible no matter what key it's bound to.
 *
 * Fix: redirect TACZ's setForcedPose(null) call so it's skipped whenever
 * GoProne's own PlayerProneData currently wants the player prone. TACZ's
 * setForcedPose(Pose.SWIMMING) call (its own active crawl) is untouched, so
 * TACZ's native crawl-while-aiming behaviour is unaffected.
 */
@Mixin(value = LocalPlayerCrawl.class, remap = false)
public class LocalPlayerCrawlMixin {

    @Redirect(
        method = "setCrawlPose",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;setForcedPose(Lnet/minecraft/world/entity/Pose;)V"
        ),
        remap = false
    )
    private void deepspace$dontClearGoPronePose(LocalPlayer player, Pose pose) {
        if (pose == null) {
            boolean goProneWantsProne = Services.PRONE.getProneData(player)
                    .map(PlayerProneData::shouldBeProne)
                    .orElse(false);

            if (goProneWantsProne) {
                // GoProne owns the forced pose right now -- don't let TACZ clear it.
                DeepSpaceMod.LOGGER.debug(
                        "[TaczGoProneCompat] Skipped TACZ's forced-pose clear for {} -- GoProne toggle is active.",
                        player.getGameProfile().getName()
                );
                return;
            }
        }
        player.setForcedPose(pose);
    }
}
