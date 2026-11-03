package com.deepspace.worldgen;

import com.deepspace.DeepSpaceMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Periodically regenerates bedrock ore vein operations for Deep Space asteroids.
 *
 * Design:
 *  - Every REGEN_INTERVAL_TICKS (6000 = 5 min) the Deep Space level is scanned.
 *  - Each OreVeinWorldEntry that is below MAX_OPERATIONS has REGEN_AMOUNT added.
 *  - This is entirely reflection-based so GTCEu is an optional dep at compile time.
 *
 * Rates:
 *  - MAX_OPERATIONS  = 160  (injected at placement time by GTCEuCompat.placeOreVein)
 *  - REGEN_AMOUNT    = 8    (ops per regen tick)
 *  - REGEN_INTERVAL  = 6000 ticks (~5 min)
 *  → Full regeneration from 0: 160/8 * 5 min = 100 min (~1h40m)
 *
 * Total ore yield per vein = oreYield(26–100) × MAX_OPERATIONS(160) = 4,160–16,000 items
 */
@Mod.EventBusSubscriber(modid = "deepspace")
public final class OreVeinRegenSystem {

    private OreVeinRegenSystem() {}

    // ------- tunables ---------------------------------------------------
    /** Total operations a fresh vein starts with (and regenerates back to). */
    public static final int MAX_OPERATIONS      = 160;
    /** Operations restored each regen tick. */
    private static final int REGEN_AMOUNT       = 8;
    /** Server ticks between regen passes (~5 minutes). */
    private static final int REGEN_INTERVAL_TICKS = 6000;

    private static int tickCounter = 0;

    // ------- tick handler -----------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ModList.get().isLoaded("gtceu")) return;

        if (++tickCounter < REGEN_INTERVAL_TICKS) return;
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        ServerLevel deepSpaceLevel = findDeepSpaceLevel(server);
        if (deepSpaceLevel == null) return;

        regenVeins(deepSpaceLevel);
    }

    // ------- internals --------------------------------------------------

    private static ServerLevel findDeepSpaceLevel(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> key = level.dimension();
            if ("deepspace".equals(key.location().getNamespace())
                    && "deep_space".equals(key.location().getPath())) {
                return level;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void regenVeins(ServerLevel level) {
        try {
            Class<?> savedDataClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreVeinSavedData");
            Method getOrCreate = savedDataClass.getMethod("getOrCreate", ServerLevel.class);
            Object savedData = getOrCreate.invoke(null, level);
            if (savedData == null) return;

            Field veinOresField = savedDataClass.getField("veinOres");
            HashMap<Object, Object> veinOres =
                (HashMap<Object, Object>) veinOresField.get(savedData);

            Class<?> entryClass = Class.forName(
                "com.gregtechceu.gtceu.api.data.worldgen.bedrockore.OreVeinWorldEntry");
            Method getOps  = entryClass.getMethod("getOperationsRemaining");
            Method setOps  = entryClass.getMethod("setOperationsRemaining", int.class);

            int regenCount = 0;
            for (Object entry : veinOres.values()) {
                if (entry == null) continue;
                int current = (int) getOps.invoke(entry);
                if (current < MAX_OPERATIONS) {
                    int newOps = Math.min(current + REGEN_AMOUNT, MAX_OPERATIONS);
                    setOps.invoke(entry, newOps);
                    regenCount++;
                }
            }

            if (regenCount > 0) {
                try { savedDataClass.getMethod("setDirty").invoke(savedData); }
                catch (Exception ignored) {}
                DeepSpaceMod.LOGGER.debug(
                    "[OreVeinRegen] Regenerated {} ore veins in Deep Space (+{} ops each)",
                    regenCount, REGEN_AMOUNT);
            }

        } catch (ClassNotFoundException e) {
            // GTCEu not present or wrong version – silently skip
        } catch (Exception e) {
            DeepSpaceMod.LOGGER.warn("[OreVeinRegen] Regen pass failed: {} ({})",
                e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
