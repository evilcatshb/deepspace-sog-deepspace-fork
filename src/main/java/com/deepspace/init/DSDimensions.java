package com.deepspace.init;

import com.deepspace.DSConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Dimension resource keys for Deep Space.
 *
 * The actual dimension registration is data-driven:
 *   data/deepspace/dimension_type/deep_space.json
 *   data/deepspace/dimension/deep_space.json
 *
 * These keys are used at runtime to reference the registered dimension.
 */
public final class DSDimensions {

    private DSDimensions() {}

    /** The dimension key for deepspace:deep_space. */
    public static final ResourceKey<Level> DEEP_SPACE =
        ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(DSConstants.MODID, "deep_space"));

    /** The dimension type key for deepspace:deep_space. */
    public static final ResourceKey<DimensionType> DEEP_SPACE_TYPE =
        ResourceKey.create(Registries.DIMENSION_TYPE,
            new ResourceLocation(DSConstants.MODID, "deep_space"));

    /** The level stem key (for dimension JSON). */
    public static final ResourceKey<LevelStem> DEEP_SPACE_STEM =
        ResourceKey.create(Registries.LEVEL_STEM,
            new ResourceLocation(DSConstants.MODID, "deep_space"));
}
