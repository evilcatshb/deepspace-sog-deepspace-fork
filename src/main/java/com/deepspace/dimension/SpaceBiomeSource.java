package com.deepspace.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Single-biome source for Deep Space -- always returns minecraft:the_void.
 *
 * ROOT CAUSE FIX (chunk save NPE / "Failed to save chunk"):
 *   The previous implementation used Holder.direct(buildVoidBiome()) -- a biome
 *   built inline that exists in NO registry.  When ChunkSerializer tries to
 *   encode chunk section biome data it calls Codec.encode on each Holder.
 *   Holder.direct() produces a DataResult whose Lifecycle field is null,
 *   causing "Cannot invoke Lifecycle.add because this.lifecycle is null" and
 *   preventing ALL chunks in the Deep Space dimension from ever saving.
 *   Because chunks could not save, asteroid generation results were discarded
 *   every session, making the dimension appear empty.
 *
 *   Fix: take the biome Holder directly from the codec registry context via
 *   RegistryOps so it is a proper registry-backed Holder with a valid Lifecycle.
 *   The codec stores the biome ResourceLocation and resolves it through the
 *   registry at deserialization time -- exactly how vanilla single-biome sources
 *   work (e.g. TheEndBiomeSource).
 *
 *   collectPossibleBiomes() returns Stream.of(biome) -- required by
 *   ChunkSerializer to build the biome palette.  This does NOT cause vanilla
 *   feature/structure decoration because our chunk generator overrides
 *   buildSurface, applyCarvers, spawnOriginalMobs, and applyBiomeDecoration to
 *   all be no-ops or asteroid-only.
 */
public class SpaceBiomeSource extends BiomeSource {

    public static final ResourceKey<Biome> VOID_BIOME_KEY =
        ResourceKey.create(Registries.BIOME, new ResourceLocation("minecraft", "the_void"));

    public static final Codec<SpaceBiomeSource> CODEC =
        RecordCodecBuilder.create(inst -> inst.group(
            Biome.CODEC.fieldOf("biome")
                .forGetter(s -> s.biome)
        ).apply(inst, SpaceBiomeSource::new));

    private final Holder<Biome> biome;

    public SpaceBiomeSource(Holder<Biome> biome) {
        this.biome = biome;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() { return CODEC; }

    /**
     * Must return the biome so ChunkSerializer can build the biome palette.
     * Without this, saving any chunk in this dimension produces a NPE on the
     * null Lifecycle inside a Holder.direct() DataResult.
     */
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(biome);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return biome;
    }
}
