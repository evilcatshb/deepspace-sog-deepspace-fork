package com.deepspace.mixin;

import com.deepspace.compat.mixin.StructureTemplatePoolRawTemplatesAccess;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

/** Makes the raw template list appendable without replacing the structure pool. */
@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolRawTemplatesMixin implements StructureTemplatePoolRawTemplatesAccess {
    // Forge's production runtime uses the SRG field name. Explicitly disable remapping because
    // the project's generated refmap omitted this private field mapping.
    @Shadow(remap = false) @Final @Mutable
    private List<Pair<StructurePoolElement, Integer>> f_210559_;

    @Override
    public void deepspace$makeRawTemplatesMutable() {
        if (!(f_210559_ instanceof ArrayList<?>)) {
            f_210559_ = new ArrayList<>(f_210559_);
        }
    }
}

