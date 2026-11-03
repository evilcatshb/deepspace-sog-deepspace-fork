package com.deepspace.mixin;

import ca.spottedleaf.starlight.common.world.ExtendedWorld;
import guideme.scene.level.GuidebookLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Provides the Starlight ExtendedWorld methods required by GuideME's custom
 * GuidebookLevel on Forge 1.20.1.
 *
 * Starlight calls these methods while rendering blocks in a custom Level.
 * GuideME owns the chunk source, so delegate directly to it.
 */
@Mixin(GuidebookLevel.class)
public abstract class GuidebookLevelStarlightCompatMixin implements ExtendedWorld {

    @Override
    public LevelChunk getChunkAtImmediately(final int chunkX, final int chunkZ) {
        return ((GuidebookLevel) (Object) this).getChunkSource().getChunk(chunkX, chunkZ, false);
    }

    @Override
    public ChunkAccess getAnyChunkImmediately(final int chunkX, final int chunkZ) {
        return ((GuidebookLevel) (Object) this).getChunkSource()
                .getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }
}
