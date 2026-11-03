package com.deepspace.blocks;

import com.deepspace.DSConstants;
import com.deepspace.DeepSpaceMod;
import com.deepspace.init.DSBlocks;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Small Breeder Reactor block entity.
 *
 * Spec:
 *   Tier: ZPM  |  Output: 16A ZPM  |  Fuel: 1 uranium ingot per 1,728,000 ticks @ 100%
 *
 * Output throttling (outputPercent, 0-100):
 *   - Energy produced per tick = GT_EU_PER_TICK * outputPercent / 100
 *   - Fuel burn rate scales with outputPercent:
 *       100% -> 1 ingot per 1,728,000 ticks (24 h)
 *        50% -> 1 ingot per 3,456,000 ticks (48 h)
 *        25% -> 1 ingot per 6,912,000 ticks (96 h)
 *   - Implemented via integer burnAccumulator (no floating-point drift).
 *   - outputPercent persisted in NBT ("output_percent") and synced to client
 *     via ContainerData slot [3].
 *
 * Item inventory:
 *   Slot 0: uranium ingot input -- accessible from ALL sides.
 *
 * ContainerData (4 slots, server to client every tick):
 *   [0] = fuelRemainingTicks & 0xFFFF
 *   [1] = (fuelRemainingTicks >> 16) & 0xFFFF
 *   [2] = (fuelRemainingTicks >> 32) & 0xFFFF
 *   [3] = outputPercent (0-100)
 */
public class BreederReactorBlockEntity extends BlockEntity implements MenuProvider {

    // -- GT ZPM constants ---------------------------------------------------
    private static final int  GT_TIER        = GTValues.ZPM;
    private static final long GT_VOLTAGE     = GTValues.V[GT_TIER];   // 131,072 EU/t
    private static final long GT_AMPERAGE    = 16L;
    private static final long GT_EU_PER_TICK = GT_VOLTAGE * GT_AMPERAGE; // 2,097,152

	// UV tier for uranium-235
	private static final int UV_TIER = GTValues.UV;
	private static final long UV_VOLTAGE = GTValues.V[UV_TIER]; // 524_288 EU/t
	private static final long UV_AMPERAGE = 16L;
	private static final long UV_EU_PER_TICK = UV_VOLTAGE * UV_AMPERAGE; // 8,388,608

	// ZPM tier for thorium (same voltage as U-238, double the amperage)
	private static final long THORIUM_VOLTAGE = GT_VOLTAGE; // 131,072 EU/t (ZPM)
	private static final long THORIUM_AMPERAGE = 32L;
	private static final long THORIUM_EU_PER_TICK = THORIUM_VOLTAGE * THORIUM_AMPERAGE; // 4,194,304

    // -- FE constants -------------------------------------------------------
    private static final long FE_PER_EU   = 4L;
    private static final long FE_PER_TICK = GT_EU_PER_TICK * FE_PER_EU; // 8,388,608
    private static final int  FE_BUFFER   = (int) Math.min(FE_PER_TICK * 200L, Integer.MAX_VALUE);
    // NOTE: buffer/rate sizing below is intentionally based on the larger
    // UV-tier FE rate so U-235 operation never gets throttled by the FE side.
    private static final long FE_PER_TICK_MAX = FE_PER_TICK * 4; // matches UV feThisTick scale

    // Uranium ingot resource location
    private static final ResourceLocation URANIUM_INGOT_RL = new ResourceLocation("gtceu", "uranium_ingot");
	// Uranium 235 resource location
	private static final ResourceLocation URANIUM_235_INGOT_RL = new ResourceLocation("gtceu", "uranium_235_ingot");
	// Thorium ingot resource location
	private static final ResourceLocation THORIUM_INGOT_RL = new ResourceLocation("gtceu", "thorium_ingot");



    // -- State --------------------------------------------------------------
    private long fuelRemainingTicks = 0L;
    private long euBuffered         = 0L;
    /**
     * Amps actually delivered to the GT power network on the most-recently-completed
     * server tick.  Updated by pushGTEU() every tick; reset to 0 at the start of
     * each push so a tick with no consumers (or no buffered EU) correctly shows 0.
     * Not persisted -- it is a live measurement, not saved state.
     */
    private long lastTickAmpsOutput   = 0L;

    /**
     * Voltage/amperage the reactor is CURRENTLY offering to the GT network,
     * in EU/t and amps respectively. This tracks whichever fuel is presently
     * loaded/burning:
     *   - Uranium-238 -> GT_VOLTAGE / GT_AMPERAGE (ZPM, 131,072 EU/t)
     *   - Uranium-235 -> UV_VOLTAGE / UV_AMPERAGE (524,288 EU/t)
     *
     * THIS MUST STAY IN SYNC with the voltage tier actually used to fill
     * euBuffered on the same tick. Previously pushGTEU() and the capability's
     * getOutputVoltage()/getOutputAmperage() were hard-coded to GT_VOLTAGE/
     * GT_AMPERAGE (ZPM) even while burning U-235 (which buffers at UV rate).
     * That mismatch meant:
     *   1) Any cable/hatch that checks output voltage before accepting saw a
     *      voltage that didn't match what was really being offered.
     *   2) maxAmps in pushGTEU() was computed as euBuffered / GT_VOLTAGE
     *      (wrong divisor for UV-buffered energy), and acceptEnergyFromNetwork
     *      was called with the wrong voltage entirely -- most GT cables/hatches
     *      reject packets whose voltage doesn't match what they expect, so the
     *      push silently failed every tick and euBuffered just filled to cap
     *      while zero EU ever reached connected cables.
     *
     * Persisted in NBT so a reloaded chunk reports the correct rated voltage
     * even before the next tryConsumeIngot()/serverTick() cycle runs.
     */
    private long currentOutputVoltage  = GT_VOLTAGE;
    private long currentOutputAmperage = GT_AMPERAGE;

    /**
     * Output throttle (0-100 inclusive; default 100).
     * Persisted in NBT as "output_percent".
     * Synced to the client via ContainerData slot [3].
     */
    private int outputPercent = 100;

    /**
     * Fractional burn accumulator (not persisted; ephemeral rounding loss on
     * restart is at most 1 tick -- negligible vs 1,728,000 ticks per ingot).
     *
     * Each game tick we add outputPercent to this value.  When it reaches >= 100
     * we decrement fuelRemainingTicks by (accumulator / 100) and keep the
     * remainder.  This gives exact integer arithmetic at any throttle level.
     */
    private int burnAccumulator = 0;

    // -- Item handler (uranium ingot slot) -----------------------------------
    private final ItemStackHandler fuelHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isUraniumFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> fuelHandler);

    // -- ContainerData (fuel + outputPercent sync) --------------------------
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (fuelRemainingTicks & 0xFFFFL);
                case 1 -> (int) ((fuelRemainingTicks >> 16) & 0xFFFFL);
                case 2 -> (int) ((fuelRemainingTicks >> 32) & 0xFFFFL);
                case 3 -> outputPercent;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> fuelRemainingTicks = (fuelRemainingTicks & ~0xFFFFL) | (value & 0xFFFFL);
                case 1 -> fuelRemainingTicks = (fuelRemainingTicks & ~0xFFFF0000L) | ((long)(value & 0xFFFF) << 16);
                case 2 -> fuelRemainingTicks = (fuelRemainingTicks & ~0xFFFF00000000L) | ((long)(value & 0xFFFF) << 32);
                case 3 -> outputPercent = Math.max(0, Math.min(100, value));
            }
        }

        @Override
        public int getCount() { return 4; }
    };

    // -- FE capability ------------------------------------------------------
    private final EnergyStorage feBuffer = new EnergyStorage(
            FE_BUFFER, 0, (int) Math.min(FE_PER_TICK_MAX, Integer.MAX_VALUE)) {
        @Override public boolean canReceive() { return false; }
    };
    private final LazyOptional<IEnergyStorage> feCap = LazyOptional.of(() -> feBuffer);

    // -- GT EU capability ---------------------------------------------------
    private final IEnergyContainer gtContainer = new IEnergyContainer() {
        @Override public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) { return 0L; }
        @Override public boolean inputsEnergy(Direction side)  { return false; }
        @Override public boolean outputsEnergy(Direction side) { return true; }
        @Override public long changeEnergy(long delta) {
            long prev = euBuffered;
            euBuffered = Math.max(0, Math.min(euBuffered + delta, getEnergyCapacity()));
            return euBuffered - prev;
        }
        @Override public long getEnergyStored()   { return euBuffered; }
        // Capacity sized for the larger UV-tier packet so U-235 operation is
        // never artificially capped by a ZPM-sized buffer.
        @Override public long getEnergyCapacity() { return UV_EU_PER_TICK * 200L; }
        @Override public long getInputVoltage()   { return 0L; }
        @Override public long getInputAmperage()  { return 0L; }
        // These now reflect whichever tier is actually buffered/being pushed,
        // instead of being hard-coded to ZPM regardless of active fuel.
        @Override public long getOutputVoltage()  { return currentOutputVoltage; }
        @Override public long getOutputAmperage() { return currentOutputAmperage; }
    };
    private final LazyOptional<IEnergyContainer> gtCap = LazyOptional.of(() -> gtContainer);

    // -- Constructor --------------------------------------------------------
    public BreederReactorBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlocks.BREEDER_REACTOR_BE.get(), pos, state);
    }

    // -- MenuProvider -------------------------------------------------------

    @Override
    @Nonnull
    public Component getDisplayName() {
        return Component.translatable("block.deepspace.breeder_reactor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @Nonnull Inventory playerInv, @Nonnull Player player) {
        return new BreederReactorMenu(windowId, playerInv, this, containerData);
    }

    public ContainerData getContainerData() { return containerData; }
    public ItemStackHandler getFuelHandler() { return fuelHandler; }

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5,
                                     worldPosition.getY() + 0.5,
                                     worldPosition.getZ() + 0.5) <= 64.0;
    }

    // -- Output percent API -------------------------------------------------

    /**
     * Returns the current output throttle (0-100 inclusive).
     * 100 = full output (24 h per ingot).
     *  50 = half output (48 h per ingot).
     *   0 = idle -- no energy, no fuel burn.
     */
    public int getOutputPercent() {
        return outputPercent;
    }

    /**
     * Sets the output throttle.
     *
     * @param percent 0-100 inclusive.  Values outside this range are clamped.
     */
    public void setOutputPercent(int percent) {
        this.outputPercent = Math.max(0, Math.min(100, percent));
        // Reset accumulator so fractional state does not carry over
        this.burnAccumulator = 0;
        setChanged();
    }

    // -- Server tick --------------------------------------------------------

	public static void serverTick(Level level, BlockPos pos, BlockState state,
								   BreederReactorBlockEntity be) {
		boolean wasActive = state.getValue(BreederReactorBlock.ACTIVE);

		// Always try to queue the next ingot when fuel is empty
		be.tryConsumeIngot();

		// Deactivate if out of fuel or throttled to 0%
		if (be.fuelRemainingTicks <= 0 || be.outputPercent == 0) {
			if (wasActive)
				level.setBlock(pos, state.setValue(BreederReactorBlock.ACTIVE, false), 3);
			return;
		}

		// -- Fractional fuel burn -------------------------------------------
		be.burnAccumulator += be.outputPercent;
		if (be.burnAccumulator >= 100) {
			long ticksToBurn = be.burnAccumulator / 100;
			be.burnAccumulator -= (int)(ticksToBurn * 100);
			be.fuelRemainingTicks = Math.max(0, be.fuelRemainingTicks - ticksToBurn);
		}
		be.setChanged();

		// -- Determine which fuel is currently active -----------------------
		ItemStack currentFuel = be.fuelHandler.getStackInSlot(0);

		long euThisTick;
		long feThisTick;

		if (isUranium235(currentFuel)) {
			// Uranium-235 → 16A @ UV tier
			euThisTick = UV_EU_PER_TICK * be.outputPercent / 100;
			feThisTick = FE_PER_TICK * 4 *  be.outputPercent / 100;
			be.currentOutputVoltage  = UV_VOLTAGE;
			be.currentOutputAmperage = UV_AMPERAGE;
		} else if (isThorium(currentFuel)) {
			// Thorium → 32A @ ZPM tier
			euThisTick = THORIUM_EU_PER_TICK * be.outputPercent / 100;
			feThisTick = FE_PER_TICK * 2 * be.outputPercent / 100;
			be.currentOutputVoltage  = THORIUM_VOLTAGE;
			be.currentOutputAmperage = THORIUM_AMPERAGE;
		} else {
			// Uranium-238 → 16A @ ZPM tier (existing behavior)
			euThisTick = GT_EU_PER_TICK * be.outputPercent / 100;
			feThisTick = FE_PER_TICK * be.outputPercent / 100;
			be.currentOutputVoltage  = GT_VOLTAGE;
			be.currentOutputAmperage = GT_AMPERAGE;
		}

		// -- Apply energy to buffers ----------------------------------------
		be.feBuffer.receiveEnergy((int) Math.min(feThisTick, Integer.MAX_VALUE), false);
		be.gtContainer.changeEnergy(euThisTick);

		// Push energy to neighbors
		pushFE(level, pos, be);
		pushGTEU(level, pos, be);

		if (!wasActive)
			level.setBlock(pos, state.setValue(BreederReactorBlock.ACTIVE, true), 3);
	}

    /**
     * Consume exactly ONE uranium ingot when fuel is depleted.
     * Only fires when fuelRemainingTicks <= 0.
     */
	private void tryConsumeIngot() {
		if (fuelRemainingTicks > 0) return;

		ItemStack stack = fuelHandler.getStackInSlot(0);
		if (stack.isEmpty() || !isUraniumFuel(stack)) return;

		boolean is235 = isUranium235(stack);
		boolean isThoriumFuel = isThorium(stack);
		fuelHandler.extractItem(0, 1, false);

		fuelRemainingTicks += DSConstants.BREEDER_BURN_TICKS; // ticks

		// Keep the advertised voltage tier in sync with the ingot we just
		// queued up, so getOutputVoltage()/getOutputAmperage() (and the next
		// pushGTEU() call) already reflect the correct tier even before the
		// energy math below runs this same tick.
		if (is235) {
			currentOutputVoltage  = UV_VOLTAGE;
			currentOutputAmperage = UV_AMPERAGE;
		} else if (isThoriumFuel) {
			currentOutputVoltage  = THORIUM_VOLTAGE;
			currentOutputAmperage = THORIUM_AMPERAGE;
		} else {
			currentOutputVoltage  = GT_VOLTAGE;
			currentOutputAmperage = GT_AMPERAGE;
		}

		setChanged();
		DeepSpaceMod.LOGGER.info(
			"[BreederReactor] Consumed {} at {} ({} queued), fuel={}t (~{}h) @ {}%",
			ForgeRegistries.ITEMS.getKey(stack.getItem()),
			worldPosition,
			fuelHandler.getStackInSlot(0).getCount(),
			fuelRemainingTicks,
			fuelRemainingTicks / 20 / 3600,
			outputPercent
		);
	}

    // -- Push FE to neighbours -----------------------------------------------

    private static void pushFE(Level level, BlockPos pos, BreederReactorBlockEntity be) {
        if (be.feBuffer.getEnergyStored() <= 0) return;
        for (Direction dir : Direction.values()) {
            BlockEntity n = level.getBlockEntity(pos.relative(dir));
            if (n == null) continue;
            n.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(s -> {
                if (!s.canReceive()) return;
                int send     = Math.min((int) Math.min(FE_PER_TICK_MAX, Integer.MAX_VALUE),
                                        be.feBuffer.getEnergyStored());
                int accepted = s.receiveEnergy(send, false);
                be.feBuffer.extractEnergy(accepted, false);
            });
        }
    }

    // -- Push GT EU packets to neighbours ------------------------------------

    private static void pushGTEU(Level level, BlockPos pos, BreederReactorBlockEntity be) {
        // Reset the amp meter at the start of every tick so a tick with no
        // accepting neighbours (or empty buffer) correctly reads 0.
        be.lastTickAmpsOutput = 0L;

        if (be.euBuffered <= 0) return;

        // Use the CURRENT tier's voltage/amperage (matches whatever was just
        // deposited into euBuffered this tick), not the old hard-coded
        // GT_VOLTAGE/GT_AMPERAGE. This is the core fix: previously packets
        // were always offered at ZPM voltage even when UV-tier energy (from
        // U-235) was buffered, so GT cables/hatches rejected every packet
        // and no EU ever actually reached the network.
        final long voltage  = be.currentOutputVoltage;
        final long amperage = be.currentOutputAmperage;

        final long[] ampsLeft       = {amperage};
        final long[] ampsDelivered  = {0L};
        for (Direction dir : Direction.values()) {
            if (ampsLeft[0] <= 0) break;
            BlockEntity n = level.getBlockEntity(pos.relative(dir));
            if (n == null) continue;
            n.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, dir.getOpposite())
                .ifPresent(container -> {
                    if (!container.inputsEnergy(dir.getOpposite())) return;
                    long maxAmps = Math.min(ampsLeft[0], be.euBuffered / voltage);
                    if (maxAmps <= 0) return;
                    long ampsAccepted = container.acceptEnergyFromNetwork(
                        dir.getOpposite(), voltage, maxAmps);
                    long euSent = ampsAccepted * voltage;
                    be.euBuffered      = Math.max(0, be.euBuffered - euSent);
                    ampsLeft[0]        -= ampsAccepted;
                    ampsDelivered[0]   += ampsAccepted;
                });
        }
        // Store the total amps actually delivered this tick for CC:Tweaked polling
        be.lastTickAmpsOutput = ampsDelivered[0];
    }

    // -- Fuel management ----------------------------------------------------

    public void initializeWithStarterFuel(int ingots) {
        this.fuelRemainingTicks = (long) ingots * DSConstants.BREEDER_BURN_TICKS;
        setChanged();
    }

    /** Legacy one-shot insertion (used by the crash ship placer). */
    public boolean insertFuelIngot() {
        this.fuelRemainingTicks += DSConstants.BREEDER_BURN_TICKS;
        setChanged();
        return true;
    }

    public long getFuelRemainingTicks() { return fuelRemainingTicks; }
    public boolean isActive()           { return fuelRemainingTicks > 0 && outputPercent > 0; }

    /**
     * Immediately zeroes fuelRemainingTicks and resets the burn accumulator.
     * Called by the CC:Tweaked peripheral's scram() function.
     * Queued ingots in the fuel slot are handled by the peripheral itself
     * (extracted and spawned as item entities in the world).
     */
    public void scram() {
        this.fuelRemainingTicks = 0L;
        this.burnAccumulator    = 0;
        setChanged();
        DeepSpaceMod.LOGGER.info("[BreederReactor] SCRAM executed at {}", worldPosition);
    }

    /**
     * Returns the effective (wall-clock) ticks remaining after accounting for
     * the current throttle setting.
     *
     * At 100% throttle this equals fuelRemainingTicks.
     * At 50% throttle this is 2x fuelRemainingTicks (same fuel lasts twice as long).
     * Returns 0 when outputPercent == 0 (reactor is idle).
     */
    public long getEffectiveFuelRemainingTicks() {
        if (outputPercent <= 0) return 0L;
        return fuelRemainingTicks * 100L / outputPercent;
    }

    /**
     * Returns the number of GT EU amperes actually delivered to the power network
     * during the most recently completed server tick.
     *
     * This reflects real consumption -- if no machines are drawing power, or the
     * reactor is throttled/idle, this will be 0 even though the reactor's rated
     * maximum is 16A.  The value updates every server tick (~50 ms).
     *
     * Range: 0 .. currentOutputAmperage (0 .. 16 either tier).
     */
    public long getLastTickAmpsOutput() {
        return lastTickAmpsOutput;
    }

    /**
     * Returns the voltage (EU/t) currently being offered to the GT network,
     * reflecting whichever fuel tier is active (ZPM for U-238, UV for U-235).
     */
    public long getCurrentOutputVoltage() {
        return currentOutputVoltage;
    }

    // -- Static helpers -----------------------------------------------------

	/**
	 * Returns true if the stack is a valid fuel for the breeder reactor:
	 * either uranium-238, uranium-235, or thorium.
	 */
	public static boolean isUraniumFuel(ItemStack stack) {
		if (stack.isEmpty()) return false;
		var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return URANIUM_INGOT_RL.equals(rl) || URANIUM_235_INGOT_RL.equals(rl) || THORIUM_INGOT_RL.equals(rl);
	}

	/**
	 * Returns true if the stack is specifically uranium-235.
	 */
	public static boolean isUranium235(ItemStack stack) {
		if (stack.isEmpty()) return false;
		var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return URANIUM_235_INGOT_RL.equals(rl);
	}

	/**
	 * Returns true if the stack is specifically thorium.
	 */
	public static boolean isThorium(ItemStack stack) {
		if (stack.isEmpty()) return false;
		var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
		return THORIUM_INGOT_RL.equals(rl);
	}

    // -- Capabilities -------------------------------------------------------

    @Nonnull @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemCap.cast();
        if (cap == ForgeCapabilities.ENERGY)
            return feCap.cast();
        if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER)
            return gtCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        feCap.invalidate();
        gtCap.invalidate();
    }

    // -- NBT ----------------------------------------------------------------

    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("fuel_ticks",     fuelRemainingTicks);
        tag.putInt ("fe_stored",      feBuffer.getEnergyStored());
        tag.putLong("eu_buffered",    euBuffered);
        tag.putInt ("output_percent", outputPercent);
        tag.putLong("output_voltage", currentOutputVoltage);
        tag.putLong("output_amperage", currentOutputAmperage);
        tag.put("FuelInventory",      fuelHandler.serializeNBT());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        fuelRemainingTicks = tag.getLong("fuel_ticks");
        if (tag.contains("fe_stored"))
            feBuffer.receiveEnergy(tag.getInt("fe_stored"), false);
        if (tag.contains("eu_buffered"))
            euBuffered = tag.getLong("eu_buffered");
        if (tag.contains("output_percent"))
            outputPercent = Math.max(0, Math.min(100, tag.getInt("output_percent")));
        if (tag.contains("output_voltage"))
            currentOutputVoltage = tag.getLong("output_voltage");
        if (tag.contains("output_amperage"))
            currentOutputAmperage = tag.getLong("output_amperage");
        if (tag.contains("FuelInventory")) {
            fuelHandler.deserializeNBT(tag.getCompound("FuelInventory"));
            // Legacy saves (pre-fix) won't have output_voltage/output_amperage
            // persisted; re-derive the correct tier from whatever fuel is
            // currently loaded so old worlds self-heal on next load.
            if (!tag.contains("output_voltage")) {
                if (isUranium235(fuelHandler.getStackInSlot(0))) {
                    currentOutputVoltage  = UV_VOLTAGE;
                    currentOutputAmperage = UV_AMPERAGE;
                } else {
                    currentOutputVoltage  = GT_VOLTAGE;
                    currentOutputAmperage = GT_AMPERAGE;
                }
            }
        }
        // burnAccumulator intentionally not persisted (max rounding loss: 1 tick)
    }
}
