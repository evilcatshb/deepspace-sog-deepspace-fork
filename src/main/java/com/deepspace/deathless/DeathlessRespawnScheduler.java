package com.deepspace.deathless;

import com.deepspace.DeepSpaceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Per-server-tick scheduler that walks the {@link DeathlessWorldData} queue
 * and reconstructs each entry whose respawn tick has arrived.
 *
 * Reconstruction:
 *   1. Look up the captured EntityType + dimension; skip (and re-queue) if
 *      either is currently unavailable.
 *   2. Load the entity from the captured NBT (this restores equipment, AI
 *      memories, custom data, etc.) and assign a FRESH UUID (we do not reuse
 *      the original UUID, to avoid colliding with anything that might still
 *      reference it).
 *   3. Move it to the recorded position/rotation, restore full health, and
 *      re-apply the {@code deathless} tag so the cycle continues.
 *   4. Add to the level. On success, drop the queue entry.
 */
public final class DeathlessRespawnScheduler {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        DeathlessWorldData data;
        try {
            data = DeathlessWorldData.getOrCreate(server);
        } catch (IllegalStateException ex) {
            // Overworld not yet loaded — nothing to do.
            return;
        }
        if (data.size() == 0) return;

        // Use the overworld's persistent game time as the master clock — it's
        // saved with the level and survives server restarts, so countdowns
        // resume correctly after reload.
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        long now = overworld.getGameTime();
        for (DeathlessEntry entry : data.getAll()) {
            if (now < entry.respawnTick) {
                continue;
            }
            ServerLevel target = resolveLevel(server, entry.dimensionId);
            if (target == null) {
                // Dimension currently unavailable — keep the entry, try again later.
                continue;
            }

            EntityType<?> type = lookupType(entry.entityTypeId);
            if (type == null) {
                DeepSpaceMod.LOGGER.warn("[Deathless] Entity type '{}' no longer registered — dropping queued respawn for {}",
                    entry.entityTypeId, entry.originalUuid);
                data.remove(entry.originalUuid);
                continue;
            }

            try {
                respawnOne(target, type, entry);
                data.remove(entry.originalUuid);
            } catch (Exception ex) {
                DeepSpaceMod.LOGGER.error("[Deathless] Failed to respawn {} ({}) — keeping entry for retry",
                    entry.originalUuid, entry.entityTypeId, ex);
                // Leave the entry in place — we'll retry next tick. Errors are
                // typically transient (chunk not loaded yet, etc.).
            }
        }
    }

    private static void respawnOne(ServerLevel level, EntityType<?> type, DeathlessEntry entry) {
        // Work on a copy so we can strip identity fields and inject "id" without
        // mutating the queue's NBT (which we may need to retry next tick).
        CompoundTag nbt = entry.entityNbt.copy();
        // Drop any captured UUID so the new entity gets a fresh one — no collision
        // risk with anything that might still hold a stale reference.
        nbt.remove("UUID");
        // EntityType.loadEntityRecursive needs the "id" field; saveWithoutId omits it.
        nbt.putString("id", entry.entityTypeId);

        // Prefer the recursive loader — this handles passengers and any per-type
        // load hooks correctly. Fall back to bare create+load if it returns null.
        Entity raw = EntityType.loadEntityRecursive(nbt, level, e -> e);
        if (raw == null) {
            raw = type.create(level);
            if (raw == null) {
                throw new IllegalStateException("EntityType.create returned null for " + entry.entityTypeId);
            }
            raw.load(nbt);
        }

        // Position/rotation override the loaded values to guarantee placement.
        raw.moveTo(entry.x, entry.y, entry.z, entry.yaw, entry.pitch);
        raw.setOldPosAndRot();

        // Restore full health and re-apply the deathless tag.
        if (raw instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }
        raw.addTag(DeathlessDeathHandler.DEATHLESS_TAG);

        // We use addFreshEntity (not finalizeSpawn) — the loaded NBT already
        // contains everything; this is a SCHEDULED respawn, not a natural spawn.
        if (!level.addFreshEntity(raw)) {
            throw new IllegalStateException("addFreshEntity rejected respawn for " + entry.entityTypeId);
        }

        DeepSpaceMod.LOGGER.info("[Deathless] Respawned {} ({}) at [{},{},{}] in {}",
            entry.originalUuid, entry.entityTypeId,
            (int) entry.x, (int) entry.y, (int) entry.z, entry.dimensionId);
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation rl;
        try {
            rl = new ResourceLocation(dimensionId);
        } catch (Exception ex) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, rl);
        return server.getLevel(key);
    }

    private static EntityType<?> lookupType(String entityTypeId) {
        ResourceLocation rl;
        try {
            rl = new ResourceLocation(entityTypeId);
        } catch (Exception ex) {
            return null;
        }
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(rl);
    }
}
