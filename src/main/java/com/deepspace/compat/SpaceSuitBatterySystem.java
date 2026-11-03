package com.deepspace.compat;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import earth.terrarium.adastra.api.events.AdAstraEvents;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.tags.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Space Suit EU Battery Life-Support System
 *
 * Survival in vacuum requires BOTH simultaneously:
 *   1. Oxygen in the suit tank         (Ad Astra, unchanged)
 *   2. EU in a GTCEu inventory battery (this system, additive requirement)
 *
 *   NO OXYGEN  ->  SUFFOCATE  (Ad Astra default)
 *   NO EU      ->  SUFFOCATE  (this system, additive condition)
 *
 * "In vacuum" = dimension has no natural O2 AND position has no distributor/room O2.
 * Inside a distributor bubble or sealed room: EU drain is skipped, no EU check.
 * Creative mode: fully exempt.
 *
 * --- Part A: EU DRAIN (PlayerTickEvent, server side) -------------------------
 *   Drains 25 EU/t whenever: wearing full suit + position in vacuum.
 *   Only drains from the first charged battery per tick.
 *   25 EU/t * 72,000 ticks (1 h) = 1,800,000 EU.
 *
 * --- Part B: EU-DEPLETION SUFFOCATION (EntityOxygenEvent) -------------------
 *   Called by Ad Astra every tick. If position is vacuum + wearing suit + no EU:
 *   returns false -> player suffocates even if the suit tank still has oxygen.
 *   The position check (not entity check) distinguishes vacuum from distributor.
 */
public final class SpaceSuitBatterySystem {

    private static final long SPACE_SUIT_EU_PER_TICK = 16L;
    private static final long JET_SUIT_EU_PER_TICK = 128L;
    private static final int  ANY_TIER    = Integer.MAX_VALUE;

    private static final boolean GTCEU_LOADED   = ModList.get().isLoaded("gtceu");
    private static final boolean ADASTRA_LOADED = ModList.get().isLoaded("ad_astra");

    public SpaceSuitBatterySystem() {}

    // ── Registration ──────────────────────────────────────────────────────────

    public static void register() {
        if (!GTCEU_LOADED || !ADASTRA_LOADED) return;
        AdAstraEvents.EntityOxygenEvent.register(SpaceSuitBatterySystem::onEntityOxygenCheck);
    }

    // ── Part A: EU drain on player tick ───────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;
        if (!wearingFullSuit(player)) return;
        if (!isPositionInVacuum(player)) return;

        long euPerTick = isWearingFullJetSuit(player)
            ? JET_SUIT_EU_PER_TICK
            : SPACE_SUIT_EU_PER_TICK;

        if (hasAnyBatteryCharge(player)) {
            // Has EU: drain the configured amount and let Ad Astra handle oxygen normally
            drainFirstBattery(player, euPerTick);
        } else {
            // No EU: force suffocation directly.
            // SpaceSuitItem.inventoryTick() calls setAirSupply(maxAirSupply) whenever
            // the suit tank has O2, which prevents vanilla air-depletion damage even
            // if EntityOxygenEvent returns false.  We bypass this by directly dealing
            // drown damage every tick (2.0f = 1 heart per tick, same as Ad Astra rate).
            player.hurt(player.damageSources().drown(), 2.0f);
        }
    }

    // ── Part B: EntityOxygenEvent -- suppress suit O2 consumption when no EU ──

    /**
     * When no EU in vacuum: return false so SpaceSuitItem.inventoryTick does NOT
     * consume oxygen from the suit tank (the suit only drains its tank when
     * hasOxygen returns false). Actual damage when EU is absent is handled
     * directly in onPlayerTick above.
     */
    private static boolean onEntityOxygenCheck(Entity entity, boolean hasOxygen) {
        if (entity.level().isClientSide()) return hasOxygen;
        if (!(entity instanceof Player player)) return hasOxygen;
        if (player.isCreative()) return hasOxygen;
        if (!wearingFullSuit(player)) return hasOxygen;
        if (!isPositionInVacuum(player)) return hasOxygen;

        // In vacuum with suit: require EU. No EU -> suppress oxygen (and suit tank drain).
        if (!hasAnyBatteryCharge(player)) return false;

        return hasOxygen;
    }

    // ── Vacuum position check ─────────────────────────────────────────────────

    /**
     * True when: dimension has no natural O2 AND block position has no
     * distributor/room O2. Uses the Level+BlockPos overload which does NOT
     * fire EntityOxygenEvent, avoiding recursion.
     */
    private static boolean isPositionInVacuum(Player player) {
        Level level = player.level();
        if (OxygenApi.API.hasOxygen(level)) return false;
        BlockPos pos = player.blockPosition();
        return !OxygenApi.API.hasOxygen(level, pos);
    }

    // ── Battery helpers ───────────────────────────────────────────────────────

    private static boolean drainFirstBattery(Player player, long euPerTick) {
        List<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            IElectricItem e = GTCapabilityHelper.getElectricItem(stack);
            if (e == null || e.getCharge() <= 0L) continue;
            long drained = e.discharge(euPerTick, ANY_TIER, true, false, false);
            if (drained > 0L) {
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyBatteryCharge(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            IElectricItem e = GTCapabilityHelper.getElectricItem(stack);
            if (e != null && e.getCharge() > 0L) return true;
        }
        return false;
    }

    // ── Suit check ────────────────────────────────────────────────────────────

    private static boolean wearingFullSuit(Player player) {
        return hasFullArmorSet(player, ModItemTags.SPACE_SUITS)
            || hasFullArmorSet(player, ModItemTags.NETHERITE_SPACE_SUITS);
    }

    private static boolean isWearingFullJetSuit(Player player) {
        return hasFullArmorSet(player, ModItemTags.JET_SUITS);
    }

    private static boolean hasFullArmorSet(Player player, TagKey<Item> tag) {
        for (ItemStack s : player.getArmorSlots()) {
            if (!s.is(tag)) return false;
        }
        return true;
    }
}
