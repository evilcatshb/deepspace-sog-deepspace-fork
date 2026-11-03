package com.deepspace.compat.cctweaked;

import com.deepspace.blocks.BreederReactorBlockEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * CC:Tweaked peripheral for the Breeder Reactor block.
 * Peripheral type: {@code "breeder_reactor"}
 *
 * <h3>Lua API</h3>
 * <pre>
 *   local reactor = peripheral.find("breeder_reactor")
 *
 *   -- Throttle (0-100 inclusive, default 100)
 *   reactor.setOutputPercent(50)   -- run at 50%, ingots last 2x longer
 *   local pct = reactor.getOutputPercent()  -- returns 50
 *
 *   -- SCRAM: eject all queued fuel and stop the reactor
 *   reactor.scram()
 * </pre>
 *
 * <h3>setOutputPercent behaviour</h3>
 * <ul>
 *   <li>100% -- full 2,097,152 EU/t; 1 uranium ingot per 24 h</li>
 *   <li> 50% -- 1,048,576 EU/t;      1 uranium ingot per 48 h</li>
 *   <li> 25% -- 524,288 EU/t;        1 uranium ingot per 96 h</li>
 *   <li>  0% -- idle; no energy output, no fuel burn</li>
 * </ul>
 *
 * <h3>scram() behaviour</h3>
 * Removes all queued uranium ingots from the reactor's input slot, spawns
 * them as item entities next to the reactor's main block, and sets
 * fuelRemainingTicks to zero (stops the currently-burning ingot immediately).
 * The "burned" portion of the current ingot is lost -- this is intentional
 * (a reactor SCRAM is an emergency shutdown, not a graceful shutdown).
 */
public class BreederReactorPeripheral implements IPeripheral {

    private final BreederReactorBlockEntity reactor;

    public BreederReactorPeripheral(BreederReactorBlockEntity reactor) {
        this.reactor = reactor;
    }

    @Nonnull
    @Override
    public String getType() {
        return "breeder_reactor";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof BreederReactorPeripheral p
            && p.reactor.getBlockPos().equals(reactor.getBlockPos());
    }

    // ── Lua API ───────────────────────────────────────────────────────────────

    /**
     * Sets the reactor output throttle.
     *
     * @param percent integer 0-100 inclusive.
     * @throws LuaException if the value is not in range [0, 100].
     */
    @LuaFunction(mainThread = true)
    public final void setOutputPercent(int percent) throws LuaException {
        if (percent < 0 || percent > 100) {
            throw new LuaException("setOutputPercent: value must be 0-100, got " + percent);
        }
        reactor.setOutputPercent(percent);
    }

    /**
     * Returns the current output throttle (0-100 inclusive).
     *
     * @return integer 0-100.
     */
    @LuaFunction(mainThread = true)
    public final int getOutputPercent() {
        return reactor.getOutputPercent();
    }

    /**
     * Returns the number of GT EU amperes actually delivered to the power network
     * during the most recently completed server tick.
     *
     * <p>This reflects real consumption, not rated capacity.  If no machines are
     * drawing power the value will be 0 even if the reactor is fuelled and running.
     * The value is updated every server tick (~50 ms), so polling once per second
     * from Lua is more than sufficient.
     *
     * <pre>
     *   local amps = reactor.getOutputAmps()
     *   -- amps is an integer in the range 0..16 for a ZPM breeder reactor
     *   print("Currently delivering " .. amps .. "A")
     * </pre>
     *
     * @return integer amps delivered last tick (0-16 for ZPM tier).
     */
    @LuaFunction(mainThread = true)
    public final long getOutputAmps() {
        return reactor.getLastTickAmpsOutput();
    }

    /**
     * Emergency SCRAM: ejects all queued uranium ingots and stops the reactor.
     *
     * <p>Any uranium ingots present in the fuel input slot are removed and
     * spawned as item entities floating next to the reactor block.  The
     * currently-burning ingot's remaining energy is discarded (fuelRemainingTicks
     * is zeroed).
     *
     * <p>After a SCRAM the reactor is inert -- it will restart automatically
     * once new fuel is inserted (subject to the current outputPercent setting).
     *
     * @return the number of uranium ingots ejected.
     */
    @LuaFunction(mainThread = true)
    public final int scram() {
        Level level = reactor.getLevel();
        if (level == null || level.isClientSide()) return 0;

        BlockPos pos = reactor.getBlockPos();
        ItemStackHandler fuelHandler = reactor.getFuelHandler();

        // Count and eject queued ingots from slot 0
        ItemStack queued = fuelHandler.getStackInSlot(0);
        int ejected = 0;

        if (!queued.isEmpty()) {
            ejected = queued.getCount();
            // Extract all items from the slot
            ItemStack toSpawn = fuelHandler.extractItem(0, ejected, false);

            // Spawn the item entity slightly above and offset from the block centre
            double spawnX = pos.getX() + 0.5 + 0.6; // offset +X so it doesn't clip inside
            double spawnY = pos.getY() + 0.8;
            double spawnZ = pos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, toSpawn);
            // Give it a gentle toss so it doesn't clip through the floor
            itemEntity.setDeltaMovement(0.1, 0.2, 0.0);
            itemEntity.setPickUpDelay(10); // 0.5 s pickup delay
            level.addFreshEntity(itemEntity);
        }

        // Zero out the burning fuel -- the partially-burned ingot energy is lost
        // (same semantics as a real reactor SCRAM: the reaction is stopped hard)
        // We do this via the block entity's internal field via saveAdditional round-trip
        // by calling the package-accessible tick bypass. We instead just expose a method.
        reactor.scram();

        return ejected;
    }
}
