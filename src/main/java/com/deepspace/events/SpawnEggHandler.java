package com.deepspace.events;

import com.deepspace.DeepSpaceMod;
import com.deepspace.init.DSDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Fixes spawn eggs in the deepspace:deep_space dimension.
 *
 * Root cause
 * ==========
 * Ad Astra injects into Mob#checkSpawnObstruction (@At HEAD, cancellable):
 *
 *     if (PlanetApi.isPlanet(level)
 *             && !OxygenApi.hasOxygen(level)   // whole-dimension check, always false here
 *             && !entityType.is(LIVES_WITHOUT_OXYGEN))
 *         cir.setReturnValue(false);           // blocks the spawn
 *
 * deep_space.json registers the dimension as an Ad Astra planet with
 * "oxygen": false, so every mob that doesn't have the LIVES_WITHOUT_OXYGEN
 * tag fails this check. The mixin does not inspect MobSpawnType, so it fires
 * for ALL spawn paths -- including spawn eggs.
 *
 * Call chain for a spawn egg right-click:
 *   ForgeSpawnEggItem.useOn()
 *     -> EntityType.spawn(ServerLevel, ..., MobSpawnType.SPAWN_EGG)
 *       -> EntityType.create()
 *         -> EventHooks.canEntitySpawn()   // fires MobSpawnEvent.PositionCheck
 *           [DEFAULT -> vanilla checks]
 *             -> mob.checkSpawnRules()
 *             -> mob.checkSpawnObstruction()  // Ad Astra mixin returns false here
 *           -> EntityType.create() returns null
 *         -> no entity spawned, item NOT consumed
 *
 * Fix
 * ===
 * Handle MobSpawnEvent.PositionCheck for SPAWN_EGG spawns and set result
 * ALLOW. Forge then skips the vanilla check path entirely (no call to
 * checkSpawnRules or checkSpawnObstruction), bypassing the mixin.
 *
 * Note: /summon does NOT go through checkSpawnObstruction -- it uses
 * EntityType.loadEntityRecursive + addFreshEntityWithPassengers, which
 * bypasses the spawn-rules check entirely. /summon is therefore unaffected
 * by this handler; it works as long as no other system cancels the
 * EntityJoinLevelEvent for the entity.
 *
 * Natural spawning
 * ================
 * This handler does NOT affect natural spawning. Natural mob spawning in
 * deep_space is already prevented by two independent mechanisms:
 *   1. The dimension uses minecraft:the_void as its biome, which has empty
 *      spawner lists -- NaturalSpawner finds nothing to spawn.
 *   2. DeepSpaceChunkGenerator.spawnOriginalMobs() is a no-op.
 */
public final class SpawnEggHandler {

    public SpawnEggHandler() {}

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        // Only act in Deep Space.
        // event.getLevel() is ServerLevelAccessor; .getLevel() unwraps to ServerLevel.
        if (!DSDimensions.DEEP_SPACE.equals(event.getLevel().getLevel().dimension())) return;

        // Only bypass for spawn-egg use. All other spawn types are left alone.
        if (event.getSpawnType() != MobSpawnType.SPAWN_EGG) return;

        // ALLOW tells Forge to skip checkSpawnRules + checkSpawnObstruction,
        // which bypasses the Ad Astra MobMixin that would otherwise return false.
        event.setResult(Event.Result.ALLOW);

        DeepSpaceMod.LOGGER.debug(
            "[SpawnEggHandler] Allowed spawn-egg spawn of {} in Deep Space at {}",
            event.getEntity().getType().toShortString(),
            event.getEntity().blockPosition());
    }
}
