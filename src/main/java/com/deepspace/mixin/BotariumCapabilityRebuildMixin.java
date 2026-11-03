package com.deepspace.mixin;

import earth.terrarium.botarium.common.fluid.base.BotariumFluidItem;
import earth.terrarium.botarium.common.fluid.base.ItemFluidContainer;
import earth.terrarium.botarium.forge.fluid.ForgeItemFluidContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nuclear fix for the Botarium / Forge capability desync.
 *
 * Root cause: Forge initialises capabilities BEFORE the authoritative NBT
 * tag is installed on the ItemStack.  Botarium's
 * {@code WrappedItemFluidContainer} caches the (empty) fluid state at
 * construction time, and from that point on every Forge-cap-based query
 * returns stale data.
 *
 * This mixin intercepts {@code getCapability()} on {@link CapabilityProvider}
 * (the superclass of ItemStack).  When the call originates from an ItemStack
 * whose item is a {@link BotariumFluidItem} and the requested capability is
 * {@code FLUID_HANDLER_ITEM}, we short-circuit the normal (potentially stale)
 * capability dispatch and return a <b>freshly constructed</b>
 * {@code ForgeItemFluidContainer} deserialised from the stack's
 * <em>current</em> NBT tag.
 *
 * This guarantees that the returned handler always reflects the real
 * {@code BotariumData} regardless of how or when the ItemStack was
 * created, transferred, serialised, or synchronised.
 */
@Mixin(CapabilityProvider.class)
public abstract class BotariumCapabilityRebuildMixin {

    @SuppressWarnings("unchecked")
    @Inject(
        method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private <T> void deepspace$freshBotariumFluidCap(
            Capability<T> cap,
            @Nullable Direction side,
            CallbackInfoReturnable<LazyOptional<T>> cir) {

        if (cap != ForgeCapabilities.FLUID_HANDLER_ITEM) return;

        Object self = this;
        if (!(self instanceof ItemStack stack)) return;
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof BotariumFluidItem<?> fluidItem)) return;

        // Build a fresh handler from the stack's current NBT — always
        // authoritative, never stale.
        ItemFluidContainer container = fluidItem.getFluidContainer(stack);
        ForgeItemFluidContainer handler = new ForgeItemFluidContainer(container, stack);

        cir.setReturnValue((LazyOptional<T>) LazyOptional.of(() -> handler));
    }
}
