package com.deepspace.deathless;

import com.deepspace.DeepSpaceMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

/**
 * Captures any non-player {@link LivingEntity} carrying the scoreboard tag
 * {@code deathless} when it dies, queues a respawn 30–60 seconds later, and
 * suppresses its drops/AI bookkeeping so vanilla death proceeds cleanly with
 * nothing left behind.
 *
 * This handler does NOT cancel the death event — vanilla still removes the
 * entity. {@link DeathlessRespawnScheduler} reconstructs a fresh copy from
 * the captured NBT when its tick comes due.
 */
public final class DeathlessDeathHandler {

    public static final String DEATHLESS_TAG = "deathless";

    /** 30 seconds @ 20 tps = 600 ticks. */
    private static final int MIN_DELAY_TICKS = 600;
    /** 60 seconds @ 20 tps = 1200 ticks (inclusive). */
    private static final int MAX_DELAY_TICKS = 1200;
    /** Faction traders respawn exactly 60 seconds after death. */
    private static final int TRADER_DELAY_TICKS = 1200;

    private final Random random = new Random();

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Server-side only.
        if (!(entity.level() instanceof ServerLevel level)) return;
        // Players are intentionally excluded from the deathless entity system - never queue them here.
        if (entity instanceof Player) return;
        // Must carry the deathless scoreboard tag. Faction traders are a
        // special deathless case: they MUST be queued and MUST retain normal
        // loot-table drops.
        if (!entity.getTags().contains(DEATHLESS_TAG)) return;
        boolean factionTrader = entity.getTags().contains("trader");

        MinecraftServer server = level.getServer();
        if (server == null) return;

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null) {
            DeepSpaceMod.LOGGER.warn("[Deathless] Entity {} has no registered type id — cannot queue respawn",
                entity);
            return;
        }

        // Capture full NBT BEFORE we strip drops / clear inventory below, so the
        // respawned copy keeps everything (equipment, custom data, AI memories).
        CompoundTag nbt = new CompoundTag();
        entity.saveWithoutId(nbt);

        long delay = factionTrader
            ? TRADER_DELAY_TICKS
            : MIN_DELAY_TICKS + random.nextInt(MAX_DELAY_TICKS - MIN_DELAY_TICKS + 1);
        // Anchor on the overworld's persistent game time so countdowns survive
        // server restarts (overworld.getGameTime() is saved with the level).
        ServerLevel overworld = server.overworld();
        long now = (overworld != null) ? overworld.getGameTime() : level.getGameTime();
        long respawnTick = now + delay;

        DeathlessEntry entry = new DeathlessEntry(
            entity.getUUID(),
            typeId.toString(),
            nbt,
            entity.getX(), entity.getY(), entity.getZ(),
            entity.getYRot(), entity.getXRot(),
            level.dimension().location().toString(),
            respawnTick
        );

        DeathlessWorldData data = DeathlessWorldData.getOrCreate(server);
        boolean queued = data.addIfAbsent(entry);
        if (!queued) {
            // A previous death event for the same UUID already queued this one.
            DeepSpaceMod.LOGGER.debug("[Deathless] Duplicate death for {} ({}) — skipping",
                entity.getUUID(), typeId);
            return;
        }

        // Non-trader deathless entities keep the existing no-drop behavior.
        // Faction traders deliberately retain their normal drops.
        if (!factionTrader && entity instanceof Mob mob) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
        }

        // Clear villager-style internal inventory so it isn't dropped.
        if (!factionTrader && entity instanceof InventoryCarrier carrier) {
            carrier.getInventory().clearContent();
        }

        // Sever last-attacker link so vanilla doesn't credit kill or process aggro.
        entity.setLastHurtByMob(null);

        DeepSpaceMod.LOGGER.info("[Deathless] Queued respawn for {} ({}) at [{},{},{}] in {} (tick {}, delay {})",
            entity.getUUID(), typeId,
            (int) entity.getX(), (int) entity.getY(), (int) entity.getZ(),
            entry.dimensionId, respawnTick, delay);
    }

    /**
     * Cancels the loot-table-based item drops (rotten flesh, bones, etc.) for
     * any deathless entity. Equipment drops are already suppressed in
     * {@link #onLivingDeath} via {@code setDropChance}, but the main loot list
     * is populated separately and must be cleared here.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (!entity.getTags().contains(DEATHLESS_TAG)) return;
        // Faction traders must still emit their configured loot-table drops.
        if (entity.getTags().contains("trader")) return;
        event.getDrops().clear();
        event.setCanceled(true);
    }

    /** Suppress XP drops too — the entity is coming back, killing it shouldn't reward the player. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void onXpDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (!entity.getTags().contains(DEATHLESS_TAG)) return;
        // Traders retain their normal XP behavior as well as loot.
        if (entity.getTags().contains("trader")) return;
        event.setDroppedExperience(0);
        event.setCanceled(true);
    }
}
