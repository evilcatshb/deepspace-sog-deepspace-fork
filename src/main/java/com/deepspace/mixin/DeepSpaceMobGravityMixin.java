package com.deepspace.mixin;

import com.deepspace.compat.WarpDriveBridge;
import com.deepspace.init.DSDimensions;
import earth.terrarium.adastra.api.events.AdAstraEvents;
import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import earth.terrarium.adastra.common.constants.PlanetConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Applies Ad Astra-style gravity physics, fire immunity and fall-damage
 * scaling to the four ground-mob types managed by
 * {@link com.deepspace.entity.navigation.MobNavigationPatcher}, but ONLY
 * while those entities are in the {@code deepspace:deep_space} dimension.
 *
 * WarpDrive's API is accessed exclusively via {@link WarpDriveBridge}
 * (reflection), so there is no compile-time dependency on the WarpDrive jar.
 *
 * <h3>Why a separate mixin instead of relying on Ad Astra's own LivingEntityMixin?</h3>
 * Deep Space has a raw gravity of {@code 0.1}, giving a normalised ratio of
 * {@code 0.1 / 9.807 ~ 0.010} -- well below Ad Astra's
 * {@code ZERO_GRAVITY_THRESHOLD} of {@code 0.05}.  Ad Astra therefore routes
 * all entities through the zero-gravity path, which contains the guard:
 * <pre>{@code if (!entity.isControlledByLocalInstance()) return;}</pre>
 * Server-side mobs always return {@code false} from that check, so no gravity
 * is ever applied, and without the old hover-mode workaround they simply drift.
 *
 * Inside a Gravity Normaliser set to {@code targetGravity = 1.0} the
 * position-local gravity ratio rises to {@code ~0.102 > 0.05}, so Ad Astra's
 * normal gravity path fires instead -- and that path works correctly for
 * server mobs.  The critical fix is therefore two-fold:
 *
 *  1. Remove hover mode from {@link com.deepspace.entity.navigation.DeepSpaceNavigation}.
 *  2. Add this mixin so that mobs outside a normaliser still feel a gentle
 *     downward pull instead of floating forever.
 */
@Mixin(LivingEntity.class)
public abstract class DeepSpaceMobGravityMixin extends Entity {

    public DeepSpaceMobGravityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ── Patched entity types (must match MobNavigationPatcher.PATCHED_TYPES) ──

    @Unique
    private static final Set<ResourceLocation> DEEPSPACE$PATCHED_TYPES = Set.of(
        new ResourceLocation("minecraft", "zombified_piglin"),
        new ResourceLocation("minecraft", "villager"),
        new ResourceLocation("minecraft", "iron_golem"),
        new ResourceLocation("minecraft", "pillager")
    );

    @Unique
    private boolean deepspace$isPatchedMob() {
        if (!DSDimensions.DEEP_SPACE.equals(level().dimension())) return false;
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(
                ((LivingEntity) (Object) this).getType());
        return typeKey != null && DEEPSPACE$PATCHED_TYPES.contains(typeKey);
    }

    // ── tick ─────────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    public void deepspace$tick(CallbackInfo ci) {
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!deepspace$isPatchedMob()) return;

        if (entity.tickCount % 20 == 0) {
            if (AdAstraEvents.OxygenTickEvent.fire(level, entity)) {
                OxygenApi.API.entityTick(level, entity);
            }
            if (AdAstraEvents.TemperatureTickEvent.fire(level, entity)) {
                TemperatureApi.API.entityTick(level, entity);
            }
        }
    }

    // ── travel ────────────────────────────────────────────────────────────────

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void deepspace$travel(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!deepspace$isPatchedMob()) return;

        float gravity = GravityApi.API.getGravity(this);

        var movementAffectingPos = getBlockPosBelowThatAffectsMyMovement();
        if (gravity <= PlanetConstants.ZERO_GRAVITY_THRESHOLD) {
            if (AdAstraEvents.ZeroGravityTickEvent.fire(level(), entity, travelVector, movementAffectingPos)) {
                GravityApi.API.entityTick(level(), entity, travelVector, movementAffectingPos);
                ci.cancel();
            }
        } else if (AdAstraEvents.GravityTickEvent.fire(level(), entity, travelVector, movementAffectingPos)) {
            if (this.isInWater()
                    || this.isInLava()
                    || entity.isFallFlying()
                    || entity.hasEffect(MobEffects.SLOW_FALLING)) {
                return;
            }
            float newGravity = 0.08f * gravity;
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.x(), velocity.y() + 0.08f - newGravity, velocity.z());
        }
    }

    // ── hurt ─────────────────────────────────────────────────────────────────

    /**
     * Suppresses fire damage in TRUE vacuum (no WarpDrive gas zone present).
     *
     * If any gas field (O2, air, or CO2) covers this position, fire is real
     * and can damage mobs. Only suppress in actual vacuum so the pure-O2
     * fire damage multiplier in WarpDriveCompat.onLivingHurt can still apply.
     *
     * WarpDrive API accessed via reflection through WarpDriveBridge. Falls
     * back to anyGas=false (suppress fire in vacuum) when WarpDrive is absent.
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void deepspace$hurt(DamageSource source, float amount,
                               CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!deepspace$isPatchedMob()) return;

        if (source.is(DamageTypeTags.IS_FIRE)) {
            // Only suppress fire damage in TRUE vacuum.
            // If any gas is present (O2, air, or CO2), fire can sustain itself
            // and damage mobs -- let it through so the O2-boost multiplier applies.
            boolean anyGas = WarpDriveBridge.hasAnyActiveOxygenField(level(), entity.blockPosition());
            if (!anyGas) {
                entity.setRemainingFireTicks(0);
                cir.setReturnValue(false);
            }
        }
    }

    // ── causeFallDamage ───────────────────────────────────────────────────────

    @ModifyVariable(
        method = "causeFallDamage",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true)
    private float deepspace$causeFallDamageMultiplier(float multiplier) {
        if (!deepspace$isPatchedMob()) return multiplier;
        return multiplier * GravityApi.API.getGravity(this);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void deepspace$causeFallDamageCancel(float fallDistance, float multiplier,
                                                 DamageSource source,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!deepspace$isPatchedMob()) return;
        if (!(fallDistance <= 3.0f / GravityApi.API.getGravity(this))) return;
        cir.setReturnValue(false);
    }
}
