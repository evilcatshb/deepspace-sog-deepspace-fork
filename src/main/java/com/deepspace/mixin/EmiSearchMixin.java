package com.deepspace.mixin;

import dev.emi.emi.search.EmiSearch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmiSearch.class)
public abstract class EmiSearchMixin {

    @Unique
    private static volatile boolean deepspace$searchReady;

    @Inject(method = "bake", at = @At("HEAD"), remap = false)
    private static void deepspace$beginSearchBake(CallbackInfo ci) {
        deepspace$searchReady = false;
    }

    @Inject(method = "bake", at = @At("RETURN"), remap = false)
    private static void deepspace$finishSearchBake(CallbackInfo ci) {
        deepspace$searchReady = true;
    }

    @Inject(method = "search", at = @At("HEAD"), cancellable = true, remap = false)
    private static void deepspace$guardSearch(String query, CallbackInfo ci) {
        if (!deepspace$searchReady || EmiSearch.names == null) {
            ci.cancel();
        }
    }
}
