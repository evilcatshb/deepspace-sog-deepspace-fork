package com.deepspace.entity;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.entity.goal.MigrationCircleGoal;
import com.deepspace.entity.goal.SpaceWhaleChargeGoal;
import com.deepspace.entity.goal.SpaceWhaleTailSweepGoal;
import com.deepspace.entity.goal.SpaceWhaleWanderGoal;
import com.deepspace.init.DSEntities;
import com.deepspace.init.DSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SpaceWhaleEntity extends TamableAnimal implements GeoEntity {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Boolean> HAS_SADDLE =
        SynchedEntityData.defineId(SpaceWhaleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MIGRATING =
        SynchedEntityData.defineId(SpaceWhaleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOSTILE =
        SynchedEntityData.defineId(SpaceWhaleEntity.class, EntityDataSerializers.BOOLEAN);

    // ── Animations ───────────────────────────────────────────────────────────
    private static final RawAnimation SWIM_ANIM =
        RawAnimation.begin().thenLoop("animation.humpback.swim");
    private static final RawAnimation BREACH_ANIM =
        RawAnimation.begin().then("animation.humpback.breach", Animation.LoopType.PLAY_ONCE);

    // ── GeckoLib ─────────────────────────────────────────────────────────────
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ── Server-side state ────────────────────────────────────────────────────
    /** 30 s = 600 ticks between milkings. */
    private int milkCooldown = 0;
    private static final int MILK_COOLDOWN_TICKS = 600;

    private int regenTicker   = 0;
    private int breachCooldown = 0;

    // Migration params written by MigrationManager / restored from NBT
    public double migrationAngle     = 0.0;
    public double migrationCentreX   = 0.0;
    public double migrationCentreZ   = 0.0;
    public double migrationFlightY   = DSConstants.ASTEROID_BAND_Y_MAX + 30.0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public SpaceWhaleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.noCulling = true; // giant: skip frustum culling
    }

    // ── Attributes ────────────────────────────────────────────────────────────
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH,          400.0)
            .add(Attributes.MOVEMENT_SPEED,       0.18)
            .add(Attributes.FLYING_SPEED,         0.18)
            .add(Attributes.ATTACK_DAMAGE,        13.0)
            .add(Attributes.KNOCKBACK_RESISTANCE,  1.0)   // 100% knockback immune
            .add(Attributes.FOLLOW_RANGE,         64.0);
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanPassDoors(false);
        nav.setCanOpenDoors(false);
        return nav;
    }

    /**
     * BUG_1: Override to produce a directional AABB that covers the full whale model
     * including the head and tail (~30 blocks long), not just the 12-wide square that
     * vanilla EntityDimensions produces.
     *
     * Width stays at 12 blocks (half = 6). Length = 30 blocks (half = 15).
     * The AABB is axis-aligned, so we project the rotated rectangle corners to get
     * the tightest world-axis-aligned box that always contains the entire model.
     */
    @Override
    public AABB makeBoundingBox() {
        double halfW = getBbWidth() / 2.0;   // 6.0 — width as registered (12 / 2)
        double halfL = 15.0;                  // half of 30-block full model length
        double h     = getBbHeight();         // 6.0

        double yawRad = Math.toRadians(getYRot());
        double sin    = Math.abs(Math.sin(yawRad));
        double cos    = Math.abs(Math.cos(yawRad));

        // AABB extents of the rotated rectangle in XZ
        double extX = sin * halfL + cos * halfW;
        double extZ = cos * halfL + sin * halfW;

        double x = getX();
        double y = getY();
        double z = getZ();
        return new AABB(x - extX, y, z - extZ, x + extX, y + h, z + extZ);
    }

    // ── Synced-data helpers ───────────────────────────────────────────────────
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAS_SADDLE, false);
        this.entityData.define(MIGRATING,  false);
        this.entityData.define(HOSTILE,    false);
    }

    public boolean hasSaddle()           { return entityData.get(HAS_SADDLE); }
    public void    setSaddled(boolean b)  { entityData.set(HAS_SADDLE, b); }
    public boolean isMigrating()          { return entityData.get(MIGRATING); }
    public void    setMigrating(boolean b){ entityData.set(MIGRATING, b); }
    public boolean isHostile()            { return entityData.get(HOSTILE); }
    public void    setHostileMode(boolean b){ entityData.set(HOSTILE, b); }

    // ── Goals ─────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MigrationCircleGoal(this));
        goalSelector.addGoal(2, new SpaceWhaleChargeGoal(this, 10.0, 14, 16));
        goalSelector.addGoal(3, new SpaceWhaleTailSweepGoal(this));
        goalSelector.addGoal(4, new SpaceWhaleWanderGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() {
                return SpaceWhaleEntity.this.isHostile() && super.canUse();
            }
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;

        if (milkCooldown > 0) milkCooldown--;

        // HP regen: 1 HP / 3 s passive, 1 HP / 6 s aggressive
        regenTicker++;
        int regenInterval = isHostile() ? 120 : 60;
        if (regenTicker >= regenInterval) {
            regenTicker = 0;
            if (getHealth() < getMaxHealth()) heal(1.0f);
        }
        if (breachCooldown > 0) breachCooldown--;

        // Passive altitude floor enforcement
        // BUG_3: Skip floor enforcement when on a lead — the floor push fights the
        // lead physics and causes rapid drift away from the player, snapping the lead.
        if (!isHostile() && !isMigrating() && !isLeashed() && getTarget() == null
                && getY() < DSConstants.ASTEROID_BAND_Y_MAX + 10.0) {
            setDeltaMovement(getDeltaMovement().x, 0.15, getDeltaMovement().z);
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (source.is(DamageTypeTags.IS_FALL))  return false;
        if (source.is(DamageTypeTags.IS_FIRE))  return false;

        if (!source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            if (source.is(DamageTypeTags.IS_PROJECTILE) && amount <= 10.0f) return false;
            amount *= 0.6f;   // 40% cosmic skin reduction
        }

        if (source.getEntity() instanceof Player attacker && !isTame()) {
            setHostileMode(true);
            setTarget(attacker);
            breachCooldown = 0;
            // Cancel any wander/navigation so the whale doesn't keep drifting to its
            // old passive target (which would look like fleeing).
            getNavigation().stop();
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        if (super.isInvulnerableTo(src)) return true;
        return src.getEntity() instanceof SpaceWhaleEntity; // whales never harm each other
    }

    @Override protected void checkFallDamage(double y, boolean onGround, BlockState s, BlockPos p) {}
    @Override public boolean causeFallDamage(float dist, float mul, DamageSource src) { return false; }

    // ── Death drops ────────────────────────────────────────────────────────────
    @Override
    protected void dropEquipment() {
        super.dropEquipment();

        // Saddle
        if (hasSaddle()) spawnAtLocation(DSItems.SPACE_WHALE_SADDLE.get());

        if (!isBaby()) {
            // ── Adult ──────────────────────────────────────────────────────
            // Meat: 256-386 items (in stacks of 64)
            int totalMeat = 256 + random.nextInt(131);
            dropStacks(new ItemStack(DSItems.SPACE_WHALE_MEAT.get()), totalMeat);

            // Condensed star matter: 32-128
            dropCondensedStarMatter(32, 128);

            // REQ_1: Bone blocks: 128-256
            int boneBlocks = 128 + random.nextInt(129);
            dropStacks(new ItemStack(Items.BONE_BLOCK), boneBlocks);
        } else {
            // ── Baby ───────────────────────────────────────────────────────
            // Meat: 8-32
            int babyMeat = 8 + random.nextInt(25);
            spawnAtLocation(new ItemStack(DSItems.SPACE_WHALE_MEAT.get(), babyMeat));

            // Condensed star matter: 8-32
            dropCondensedStarMatter(8, 32);
        }
    }

    /** Helper: drops [min, max] condensed_star_matter in stacks of 64. */
    private void dropCondensedStarMatter(int min, int max) {
        var item = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation("kubejs", "condensed_star_matter"));
        if (item == null) {
            DeepSpaceMod.LOGGER.warn("[SpaceWhale] kubejs:condensed_star_matter not found");
            return;
        }
        int total = min + random.nextInt(max - min + 1);
        dropStacks(new ItemStack(item), total);
    }

    /** Splits [total] items into stacks of 64 and drops them. */
    private void dropStacks(ItemStack template, int total) {
        while (total > 0) {
            int n = Math.min(total, template.getMaxStackSize());
            ItemStack drop = template.copy();
            drop.setCount(n);
            spawnAtLocation(drop);
            total -= n;
        }
    }

    // ── Lead / leash ──────────────────────────────────────────────────────────

    /**
     * BUG_1: Vanilla Mob#tickLeash() breaks the lead when the entity is more than
     * 10 blocks from the holder.  For a 30-block whale scaled ×8 that threshold is
     * already exceeded the moment the lead is attached from anywhere but the dead
     * centre of the model.
     *
     * This override:
     *  • Raises the break threshold to 40 blocks so a player can comfortably lead
     *    the whale from any position around its body.
     *  • Applies a smooth pull force toward the holder once the lead goes taut
     *    (>8 blocks), so the whale actually follows instead of resisting.
     *  • Clamps vertical pull to 60 % so the whale doesn't slam into the player
     *    vertically.
     */
    @Override
    protected void tickLeash() {
        Entity holder = getLeashHolder();
        if (holder == null) return;

        if (!holder.isAlive()) {
            dropLeash(true, true);
            return;
        }

        double dist = distanceTo(holder);

        // Only break if genuinely out of range for a whale-sized creature
        if (dist > 40.0) {
            dropLeash(true, true);
            return;
        }

        // Apply a pull force once the lead goes taut
        if (dist > 8.0) {
            double dx = (holder.getX() - getX()) / dist;
            double dy = (holder.getY() - getY()) / dist;
            double dz = (holder.getZ() - getZ()) / dist;

            // Pull strength scales with how far over the taut-point we are,
            // capped so the whale doesn't rocket into the player
            double pull = Math.min((dist - 8.0) * 0.12, 0.55);
            setDeltaMovement(getDeltaMovement().add(
                dx * pull,
                dy * pull * 0.6,
                dz * pull));
        }
    }

    /**
     * BUG_1: Position the lead attachment point at the whale's snout rather than
     * the entity-origin (belly centre), so the rendered rope doesn't clip through
     * the model and players can intuitively attach leads near the head.
     */
    @Override
    public Vec3 getLeashOffset() {
        double yawRad = Math.toRadians(getYRot());
        // Forward vector in world XZ (-sin, cos for Minecraft's yaw convention)
        double fwdX = -Math.sin(yawRad) * 12.0;
        double fwdZ =  Math.cos(yawRad) * 12.0;
        return new Vec3(fwdX, getBbHeight() * 0.55, fwdZ);
    }

    // ── Riding / 3-D player control ────────────────────────────────────────────
    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return (hasSaddle() && isTame() && getFirstPassenger() instanceof Player p) ? p : null;
    }

    // canBeControlledByRider: not declared in supertype in 1.20.1; keep without @Override
    public boolean canBeControlledByRider() { return getControllingPassenger() != null; }

    @Override
    public void travel(Vec3 movement) {
        if (isVehicle() && getControllingPassenger() instanceof Player rider) {
            setYRot(rider.getYRot()); yRotO = rider.getYRot();
            setXRot(rider.getXRot() * 0.5f);
            yHeadRot = getYRot(); yHeadRotO = yHeadRot;

            float fwd    = rider.zza;
            float strafe = rider.xxa;
            // Sprint key (Ctrl) = fly up, Sneak (Shift) = fly down
            double vert  = rider.isSprinting() ? 1.0 : rider.isShiftKeyDown() ? -1.0 : 0.0;

            double spd  = getAttributeValue(Attributes.FLYING_SPEED) * 2.5;
            float  yawR = getYRot() * ((float) Math.PI / 180F);
            double mx   = (-Math.sin(yawR) * fwd + Math.cos(yawR) * strafe) * spd;
            double mz   = ( Math.cos(yawR) * fwd + Math.sin(yawR) * strafe) * spd;
            double my   = vert * spd * 0.8;

            // Enforce passive flight floor even while ridden
            // BUG_3: Skip when leashed so the floor doesn't fight the lead tension
            if (!isHostile() && !isLeashed() && getY() + my < DSConstants.ASTEROID_BAND_Y_MAX + 5) {
                my = Math.max(0, my);
            }

            setDeltaMovement(mx, my, mz);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.88));
            return;
        }
        super.travel(movement);
    }

    @Override
    public void positionRider(Entity rider, MoveFunction fn) {
        if (!hasPassenger(rider)) return;
        // BUG_2: Place rider 1 block above the body and shift forward toward the head
        // so the swimming animation doesn't clip through the player's camera.
        double forwardOffset = 6.0;  // blocks toward the head from entity centre
        double yawRad = Math.toRadians(getYRot());
        // Forward direction vector in XZ: (-sin(yaw), cos(yaw))
        double rx = getX() - Math.sin(yawRad) * forwardOffset;
        double ry = getY() + getPassengersRidingOffset() + rider.getMyRidingOffset() + 1.0;
        double rz = getZ() + Math.cos(yawRad) * forwardOffset;
        fn.accept(rider, rx, ry, rz);
        rider.setYRot(rider.getYRot() + (getYRot() - yRotO));
        rider.setYHeadRot(rider.getYRot());
    }

    // BUG_2: Return height that places rider's feet at the top of the visual body
    @Override public double getPassengersRidingOffset() { return getBbHeight() - 0.2; }

    // ── Player interaction ────────────────────────────────────────────────────
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ── Milking: empty bucket → condensed_star_matter_bucket ─────────────
        if (stack.is(Items.BUCKET)) {
            if (!level().isClientSide) {
                if (milkCooldown > 0) {
                    int secs = (milkCooldown + 19) / 20;
                    player.displayClientMessage(
                        Component.literal("The space whale isn't ready yet. Wait " + secs + "s.")
                            .withStyle(ChatFormatting.RED),
                        true);
                } else {
                    var csmBucket = ForgeRegistries.ITEMS.getValue(
                        new ResourceLocation("gtceu", "condensed_star_matter_bucket"));
                    if (csmBucket != null) {
                        // Consume the empty bucket in survival; skip in creative
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        // Always give the filled bucket regardless of game mode
                        ItemStack result = new ItemStack(csmBucket);
                        if (!player.getInventory().add(result)) player.drop(result, false);
                        milkCooldown = MILK_COOLDOWN_TICKS;
                        playSound(SoundEvents.COW_MILK, 1.0f, 0.8f + random.nextFloat() * 0.4f);
                    } else {
                        DeepSpaceMod.LOGGER.warn("[SpaceWhale] gtceu:condensed_star_matter_bucket not found");
                    }
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // ── Name tag on tamed whale ───────────────────────────────────────────
        if (stack.is(Items.NAME_TAG) && isTame() && isOwnedBy(player)) {
            return super.mobInteract(player, hand);   // vanilla nametag handling
        }

        // ── Taming with Eternal Singularity (MOD_5) ──────────────────────────
        if (!isTame()) {
            var singularity = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("avaritia", "eternal_singularity"));
            if (singularity != null && stack.is(singularity)) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    // Eternal Singularity is the hardest item in the game — taming always succeeds
                    tame(player);
                    setHostileMode(false);
                    setTarget(null);
                    level().broadcastEntityEvent(this, (byte) 7); // hearts
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }

        // ── Tamed owner only ──────────────────────────────────────────────────
        if (!isOwnedBy(player)) return InteractionResult.PASS;

        // Saddle application
        if (stack.is(DSItems.SPACE_WHALE_SADDLE.get()) && !hasSaddle()) {
            if (!level().isClientSide) {
                setSaddled(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Mount (shift to dismount is handled by vanilla)
        if (!player.isShiftKeyDown() && hasSaddle() && !isVehicle()) {
            player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    // ── Breeding ──────────────────────────────────────────────────────────────
    @Override
    public boolean isFood(ItemStack stack) {
        var cheese = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ad_astra", "cheese"));
        return cheese != null && stack.is(cheese);
    }

    @Nullable @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        SpaceWhaleEntity baby = DSEntities.SPACE_WHALE.get().create(level);
        if (baby != null && getOwner() instanceof Player p) baby.tame(p);
        return baby;
    }

    // ── Spawning ──────────────────────────────────────────────────────────────
    @Nullable @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff,
                                        MobSpawnType type, @Nullable SpawnGroupData gd,
                                        @Nullable CompoundTag tag) {
        gd = super.finalizeSpawn(level, diff, type, gd, tag);
        setNoGravity(true);
        if (getY() < DSConstants.ASTEROID_BAND_Y_MAX + 10)
            setPos(getX(), DSConstants.ASTEROID_BAND_Y_MAX + 20, getZ());
        return gd;
    }

    // checkSpawnObstruction / canBeNameTagged: not in supertype in 1.20.1 — no @Override
    public boolean checkSpawnObstruction(LevelAccessor l) { return true; }

    public boolean canBeNameTagged() { return isTame(); }

    // ── Misc ──────────────────────────────────────────────────────────────────
    @Override protected SoundEvent getAmbientSound()                    { return SoundEvents.DOLPHIN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s)        { return SoundEvents.DOLPHIN_HURT; }
    @Override protected SoundEvent getDeathSound()                     { return SoundEvents.DOLPHIN_DEATH; }
    @Override protected float getSoundVolume()                         { return 2.0f; }
    @Override protected void doPush(Entity e) { if (!(e instanceof SpaceWhaleEntity)) super.doPush(e); }
    @Override public boolean canAttack(LivingEntity t) { return !(t instanceof SpaceWhaleEntity) && super.canAttack(t); }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean isPushable()              { return false; }

    // ── NBT ───────────────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("HasSaddle",   hasSaddle());
        t.putBoolean("Migrating",   isMigrating());
        t.putBoolean("Hostile",     isHostile());
        t.putDouble("MigAngle",     migrationAngle);
        t.putDouble("MigCX",        migrationCentreX);
        t.putDouble("MigCZ",        migrationCentreZ);
        t.putDouble("MigY",         migrationFlightY);
        t.putInt("MilkCooldown",    milkCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setSaddled(t.getBoolean("HasSaddle"));
        setMigrating(t.getBoolean("Migrating"));
        setHostileMode(t.getBoolean("Hostile"));
        migrationAngle    = t.getDouble("MigAngle");
        migrationCentreX  = t.getDouble("MigCX");
        migrationCentreZ  = t.getDouble("MigCZ");
        migrationFlightY  = t.getDouble("MigY");
        milkCooldown      = t.getInt("MilkCooldown");
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "swim", 5, state -> {
            state.getController().setAnimation(SWIM_ANIM);
            return PlayState.CONTINUE;
        }));
        reg.add(new AnimationController<>(this, "breach", 0, state -> {
            if (isHostile() && breachCooldown <= 0) {
                breachCooldown = 200;
                state.getController().setAnimation(BREACH_ANIM);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
