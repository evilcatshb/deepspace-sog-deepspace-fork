package com.deepspace.dimension;

import com.deepspace.DSConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the chunk generator codec and biome source codec with Forge's
 * codec registries so that the dimension JSON can reference them by name.
 *
 * dimension/deep_space.json -> "type": "deepspace:asteroid_generator"
 *                           -> "biome_source": { "type": "deepspace:space_biome_source" }
 */
public final class DSCodecs {

    private DSCodecs() {}

    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>>
        CHUNK_GENERATORS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.CHUNK_GENERATOR,
            DSConstants.MODID);

    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraft.world.level.biome.BiomeSource>>
        BIOME_SOURCES = DeferredRegister.create(
            net.minecraft.core.registries.Registries.BIOME_SOURCE,
            DSConstants.MODID);

    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator>>
        ASTEROID_GENERATOR = CHUNK_GENERATORS.register("asteroid_generator",
            () -> DeepSpaceChunkGenerator.CODEC);

    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraft.world.level.biome.BiomeSource>>
        SPACE_BIOME_SOURCE = BIOME_SOURCES.register("space_biome_source",
            () -> SpaceBiomeSource.CODEC);

    public static void register(IEventBus modBus) {
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
    }
}