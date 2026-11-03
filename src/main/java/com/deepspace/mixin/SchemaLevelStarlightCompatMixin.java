package com.deepspace.mixin;

import brachy.modularui.drawable.schema.SchemaLevel;
import ca.spottedleaf.starlight.common.world.ExtendedWorld;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Provides Starlight's ExtendedWorld methods to ModularUI's SchemaLevel.
 *
 * GTCEu 8.0.0 embeds ModularUI 3.3.1-SNAPSHOT. SchemaLevel is a custom fake
 * Level used by GTCEu's EMI/JEI multiblock previews and owns a DummyChunkSource
 * which supplies DummyChunk (a LevelChunk). Starlight expects custom Levels
 * implementing ExtendedWorld, so delegate to SchemaLevel's existing chunk
 * source rather than bypassing ModularUI's schema-world implementation.
 */
@Mixin(SchemaLevel.class)
public abstract class SchemaLevelStarlightCompatMixin implements ExtendedWorld {

    @Override
    public LevelChunk getChunkAtImmediately(final int chunkX, final int chunkZ) {
        return ((SchemaLevel) (Object) this).getChunkSource().getChunk(chunkX, chunkZ, false);
    }

    @Override
    public ChunkAccess getAnyChunkImmediately(final int chunkX, final int chunkZ) {
        return ((SchemaLevel) (Object) this).getChunkSource()
                .getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }
}
