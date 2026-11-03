package com.deepspace.blocks;

import com.deepspace.init.DSMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * Container menu for the Small Breeder Reactor.
 *
 * Layout:
 *   Slot 0    : uranium ingot input (mapped to BreederReactorBlockEntity.fuelHandler slot 0)
 *   Slots 1-36: player inventory + hotbar
 *
 * ContainerData (3 shorts, synced server→client every tick):
 *   [0] fuelRemainingTicks & 0xFFFF          (bits  0-15)
 *   [1] (fuelRemainingTicks >> 16) & 0xFFFF  (bits 16-31)
 *   [2] (fuelRemainingTicks >> 32) & 0xFFFF  (bits 32-47)
 *
 * Hopper / pipe / ME bus support:
 *   The block entity exposes IItemHandler (fuelHandler) on all sides via
 *   ForgeCapabilities.ITEM_HANDLER, so external automation can insert
 *   uranium ingots directly without opening the GUI.
 */
public class BreederReactorMenu extends AbstractContainerMenu {

    // GUI slot position (in texture-space coords relative to leftPos/topPos)
    public static final int FUEL_SLOT_X = 79;
    public static final int FUEL_SLOT_Y = 35;

    private final BreederReactorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // -- Server-side constructor (called via MenuProvider.createMenu) ---------

    public BreederReactorMenu(int windowId, Inventory playerInv,
                               BreederReactorBlockEntity be, ContainerData data) {
        super(DSMenuTypes.BREEDER_REACTOR_MENU.get(), windowId);
        this.blockEntity = be;
        this.level = playerInv.player.level();
        this.data = data;

        checkContainerDataCount(data, 4);

        // Slot 0: uranium ingot input
        addSlot(new UraniumSlot(be.getFuelHandler(), 0, FUEL_SLOT_X, FUEL_SLOT_Y));

        // Slots 1-27: player inventory (rows 0-2)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // Slots 28-36: hotbar
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    // -- Network / client constructor (via IForgeMenuType) --------------------

    public static BreederReactorMenu networkConstructor(int windowId, Inventory playerInv,
                                                         FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInv.player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BreederReactorBlockEntity reactor) {
            return new BreederReactorMenu(windowId, playerInv, reactor, reactor.getContainerData());
        }
        // Fallback: create a dummy menu backed by an empty container so the client
        // doesn't crash even if the block entity hasn't loaded yet.
        return new BreederReactorMenu(windowId, playerInv,
            new BreederReactorBlockEntity(pos, level.getBlockState(pos)),
            new SimpleContainerData(4));
    }

    // -- Fuel remaining (reconstructed from 3 synced shorts) ------------------

    public long getFuelRemainingTicks() {
        return (long)(data.get(0) & 0xFFFF)
             | ((long)(data.get(1) & 0xFFFF) << 16)
             | ((long)(data.get(2) & 0xFFFF) << 32);
    }

    /** Returns the current output throttle (0-100) as synced from the server. */
    public int getOutputPercent() {
        return Math.max(1, data.get(3)); // guard against 0 to avoid divide-by-zero in screen
    }

    /**
     * Returns the effective (wall-clock) ticks remaining, accounting for the
     * current throttle.  At 50% this is 2x fuelRemainingTicks, etc.
     */
    public long getEffectiveFuelRemainingTicks() {
        int pct = getOutputPercent();
        if (pct <= 0) return 0L;
        return getFuelRemainingTicks() * 100L / pct;
    }

    public boolean isActive() {
        return getFuelRemainingTicks() > 0;
    }

    // -- Shift-click handling -------------------------------------------------

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return returnStack;

        ItemStack stackInSlot = slot.getItem();
        returnStack = stackInSlot.copy();

        if (index == 0) {
            // Fuel slot -> player inventory
            if (!moveItemStackTo(stackInSlot, 1, 37, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stackInSlot, returnStack);
        } else {
            // Player inventory -> fuel slot (uranium ingots only)
            if (BreederReactorBlockEntity.isUraniumFuel(stackInSlot)) {
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
            } else if (index < 28) {
                // Inventory -> hotbar
                if (!moveItemStackTo(stackInSlot, 28, 37, false)) return ItemStack.EMPTY;
            } else {
                // Hotbar -> inventory
                if (!moveItemStackTo(stackInSlot, 1, 28, false)) return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stackInSlot.getCount() == returnStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stackInSlot);

        return returnStack;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return blockEntity.stillValid(player);
    }

    // -- Inner slot class: only accepts uranium ingots ------------------------

    private static class UraniumSlot extends SlotItemHandler {
        public UraniumSlot(IItemHandler handler, int index, int xPosition, int yPosition) {
            super(handler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return BreederReactorBlockEntity.isUraniumFuel(stack);
        }
    }
}
