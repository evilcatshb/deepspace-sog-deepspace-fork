package com.deepspace.mixin;

import com.deepspace.compat.mixin.StructureTemplatePoolRawTemplatesAccess;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the vanilla village animal pool's raw list mutable before Genetic Animals appends entries. */
@Mixin(targets = "mokiyoki.enhancedanimals.GeneticAnimals", remap = false)
public abstract class GeneticAnimalsVillagePoolMixin {
    private static final ResourceLocation VILLAGE_ANIMALS_POOL = new ResourceLocation("minecraft", "village/common/animals");

    @Inject(method = "addAnimalsToVillages", at = @At("HEAD"), remap = false)
    private void deepspace$makeVillageAnimalPoolMutable(ServerAboutToStartEvent event, CallbackInfo ci) {
        StructureTemplatePool pool = event.getServer().registryAccess()
                .registryOrThrow(Registries.TEMPLATE_POOL)
                .get(VILLAGE_ANIMALS_POOL);
        if (pool != null) {
            ((StructureTemplatePoolRawTemplatesAccess) (Object) pool).deepspace$makeRawTemplatesMutable();
        }
    }
}

