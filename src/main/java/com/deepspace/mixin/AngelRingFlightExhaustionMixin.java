package com.deepspace.mixin;

import com.deepspace.compat.AngelRingFlightExhaustionCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import toughasnails.api.thirst.ThirstHelper;

/**
 * Applies matching hunger and Tough As Nails thirst exhaustion during Angel Ring flight.
 * Flight movement uses the existing 0.075 exhaustion per 3D block rate. Stationary
 * flight has a minimum equivalent of 0.05 blocks per tick, i.e. 0.075 per second.
 * Both systems are updated directly exactly once; causeFoodExhaustion is deliberately
 * not used because Tough As Nails hooks that method and would double-count thirst.
 */
@Mixin(Player.class)
public abstract class AngelRingFlightExhaustionMixin {
    private static final double FLIGHT_EXHAUSTION_PER_BLOCK = 0.075D;
    private static final double MINIMUM_FLIGHT_BLOCKS_PER_TICK = 1.0D / 20.0D;

    @Inject(method = "checkMovementStatistics(DDD)V", at = @At("RETURN"))
    private void deepspace$addAngelRingFlightExhaustion(double dx, double dy, double dz, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        GameType gameType = player.gameMode.getGameModeForPlayer();
        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) return;

        // This hook replaces only the missing ordinary-flight exhaustion path.
        // Avoid movement states that have their own vanilla exhaustion behavior.
        if (player.isPassenger()
                || player.isSwimming()
                || player.isEyeInFluid(FluidTags.WATER)
                || player.isInWater()
                || player.onClimbable()
                || player.onGround()
                || player.isFallFlying()) {
            return;
        }

        if (!player.getAbilities().mayfly || !player.getAbilities().flying) return;
        if (!AngelRingFlightExhaustionCompat.isAngelRingEquipped(player)) return;

        // One tick represents 1/20 second. max() preserves the movement-based rate
        // while adding a minimum cost for hovering/AFK flight, without summing both.
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(distance) || distance < 0.0D) return;
        float charge = (float) (Math.max(distance, MINIMUM_FLIGHT_BLOCKS_PER_TICK)
                * FLIGHT_EXHAUSTION_PER_BLOCK);

        // If TAN is unavailable/disabled, fail closed rather than silently making
        // hunger and thirst unequal. TAN is compileOnly and is never bundled here.
        if (!AngelRingFlightExhaustionCompat.isThirstIntegrationAvailable()) return;
        try {
            toughasnails.api.thirst.IThirst thirst = ThirstHelper.getThirst(player);
            if (thirst == null) return;
            thirst.addExhaustion(charge);
            player.getFoodData().addExhaustion(charge);
        } catch (LinkageError | RuntimeException ignored) {
            // Optional compatibility must not crash the server if TAN's API changes.
        }
    }
}


