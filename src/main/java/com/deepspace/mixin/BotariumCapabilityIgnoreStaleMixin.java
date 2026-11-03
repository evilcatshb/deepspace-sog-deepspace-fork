package com.deepspace.mixin;

import earth.terrarium.botarium.common.fluid.base.BotariumFluidItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Universal fix for Forge capability desync on ALL Botarium fluid items
 * (Ad Astra gas tanks, space suits, jet suits, etc.).
 *
 * Forge's ItemStack constructor calls initCapabilities() (which triggers
 * Botarium's attachItemCapabilities) BEFORE setTag() installs the real NBT.
 * Botarium's WrappedItemFluidContainer deserializes from the stack tag at
 * construction time, so it caches an empty fluid state. Forge then serializes
 * this empty capability into ForgeCaps NBT.
 *
 * When the ItemStack is later reconstructed from NBT, deserializeCaps would
 * restore this stale empty snapshot, overriding whatever correct BotariumData
 * the stack tag actually contains.
 *
 * This mixin cancels deserializeCaps for any ItemStack whose Item implements
 * BotariumFluidItem. The companion BotariumCapabilityRebuildMixin then
 * rebuilds a fresh capability from the current NBT when setTag() fires.
 */
@Mixin(CapabilityProvider.class)
public abstract class BotariumCapabilityIgnoreStaleMixin {
    @Inject(method = "deserializeCaps", at = @At("HEAD"), cancellable = true, remap = false)
    private void deepspace$ignoreStaleBotariumCaps(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof ItemStack stack)) {
            return;
        }

        if (stack.getItem() instanceof BotariumFluidItem<?>) {
            ci.cancel();
        }
    }
}
