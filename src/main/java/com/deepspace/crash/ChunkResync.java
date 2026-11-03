package com.deepspace.crash;

import com.deepspace.DeepSpaceMod;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.Collection;

/**
 * BUG_2: the crashed ship + guaranteed asteroids are written with silent
 * {@code setBlockState(...,false)} after worldgen. If the client already has
 * (or queued) a copy of those chunks it renders them stale — blocks exist
 * server-side but are invisible/un-collidable client-side until a relog.
 *
 * Resending a fresh {@link ClientboundLevelChunkWithLightPacket} replaces the
 * client's entire chunk copy with the post-asteroid state. Server-thread only.
 */
public final class ChunkResync {

    private ChunkResync() {}

    public static void resend(ServerLevel level, ServerPlayer player,
                              Collection<ChunkPos> chunks) {
        if (player == null || chunks == null || chunks.isEmpty()) return;
        LevelLightEngine light = level.getChunkSource().getLightEngine();
        int sent = 0;
        for (ChunkPos cp : chunks) {
            try {
                LevelChunk chunk = level.getChunk(cp.x, cp.z);
                player.connection.send(
                    new ClientboundLevelChunkWithLightPacket(chunk, light, null, null));
                sent++;
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ChunkResync] resend failed for {}: {}",
                    cp, e.toString());
            }
        }
        DeepSpaceMod.LOGGER.info("[ChunkResync] Resent {} chunk(s) to {}",
            sent, player.getName().getString());
    }
}
