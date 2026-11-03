package com.deepspace.mixin;

import java.util.function.Supplier;

import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses Forge's protected CapabilityProvider.gatherCapabilities method. */
@Mixin(CapabilityProvider.class)
public interface CapabilityProviderGatherInvoker {
    @Invoker("gatherCapabilities")
    void deepspace$gatherCapabilities(Supplier<ICapabilityProvider> parent);

    @Invoker("invalidateCaps")
    void deepspace$invalidateCaps();

    @Invoker("reviveCaps")
    void deepspace$reviveCaps();
}
