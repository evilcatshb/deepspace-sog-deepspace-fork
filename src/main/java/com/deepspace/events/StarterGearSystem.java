package com.deepspace.events;

import com.deepspace.DeepSpaceMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Starter gear -- equips armour and adds oxygen tank.
 *
 * -- Slot assignments ------------------------------------------------------
 *   EquipmentSlot.HEAD   <- ad_astra:space_helmet  (vanilla armour slot)
 *   EquipmentSlot.CHEST  <- ad_astra:space_suit    (vanilla armour slot)
 *   EquipmentSlot.LEGS   <- ad_astra:space_pants   (vanilla armour slot)
 *   EquipmentSlot.FEET   <- ad_astra:space_boots   (vanilla armour slot)
 *   Inventory/hotbar     <- ad_astra:zip_gun filled 3000L oxygen (immediately usable)
 *   Inventory            <- ad_astra:large_gas_tank filled 3K/3K oxygen
 *                          (NOT equipped -- held in hotbar/bag)
 *
 * -- Dedup guard -----------------------------------------------------------
 *   schedule() sets "ds_gear_queued" flag and defers to next tick.
 *   A second schedule() call while the flag is set is silently dropped.
 *   The flag is cleared after applyAll() runs so future respawns work.
 *
 * -- Idempotent ------------------------------------------------------------
 *   Every check (slot empty? tank in inventory?) is idempotent.  Calling
 *   schedule() twice in a row is safe -- the second pass finds all slots
 *   occupied and exits without touching the inventory.
 */
public final class StarterGearSystem {

    // -- Item IDs -- confirmed from jar bytecode --------------------------------
    private static final String SPACE_HELMET     = "ad_astra:space_helmet";
    private static final String SPACE_SUIT       = "ad_astra:space_suit";
    private static final String SPACE_PANTS      = "ad_astra:space_pants";
    private static final String SPACE_BOOTS      = "ad_astra:space_boots";
    private static final String LARGE_GAS_TANK   = "ad_astra:large_gas_tank";
    private static final String ZIP_GUN          = "ad_astra:zip_gun";
    private static final String OXYGEN_FLUID_ID  = "ad_astra:oxygen";

    private static final long TANK_CAPACITY_MB = 3_000_000L; // actual tank max capacity (unchanged)
    private static final long TANK_STARTER_FILL_MB = 3_000L;  // amount to give player on spawn (3K mB)
    // Angel Ring has no energy charge needed

    /** Persistent data key -- prevents concurrent double-queuing. */
    private static final String GEAR_QUEUED_KEY = "ds_gear_queued";

    private StarterGearSystem() {}

    // -- Public API ------------------------------------------------------------

    /**
     * Queue one gear-apply for the next server tick.
     * Duplicate calls before the tick fires are dropped (dedup flag).
     */
    public static void schedule(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(GEAR_QUEUED_KEY)) {
            DeepSpaceMod.LOGGER.debug("[StarterGear] Already queued for {} -- duplicate dropped",
                player.getName().getString());
            return;
        }
        player.getPersistentData().putBoolean(GEAR_QUEUED_KEY, true);

        player.getServer().execute(() -> {
            player.getPersistentData().remove(GEAR_QUEUED_KEY);
            applyAll(player);
        });
    }

    // -- Apply all gear (runs on next tick, inventory fully settled) -----------

    private static void applyAll(ServerPlayer player) {
        equipSpaceHelmet(player);
        equipSpaceSuit(player);
        equipSpacePants(player);
        equipSpaceBoots(player);
        addZipGun(player);
        addGasTank(player);
        addLargeLithiumBattery(player);
    }

    // -- Armour slots (vanilla EquipmentSlot) ----------------------------------

    private static void equipSpaceHelmet(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
        ItemStack item = makeItem(SPACE_HELMET);
        if (item.isEmpty()) return;
        player.setItemSlot(EquipmentSlot.HEAD, item);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Space helmet -> HEAD for {}",
            player.getName().getString());
    }

    private static void equipSpaceSuit(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return;
        ItemStack item = makeItem(SPACE_SUIT);
        if (item.isEmpty()) return;

        // Fill the space suit with 100% oxygen using the Botarium NBT format:
        // {BotariumData: {StoredFluids: [{Fluid: "ad_astra:oxygen", Amount: 1000L}]}, Damage: 0}
        CompoundTag fluidEntry = new CompoundTag();
        fluidEntry.putString("Fluid", OXYGEN_FLUID_ID);
        fluidEntry.putLong("Amount", 1000L); // 1000L = 100% full oxygen in space suit

        net.minecraft.nbt.ListTag storedFluids = new net.minecraft.nbt.ListTag();
        storedFluids.add(fluidEntry);

        CompoundTag botariumData = new CompoundTag();
        botariumData.put("StoredFluids", storedFluids);

        item.getOrCreateTag().put("BotariumData", botariumData);

        player.setItemSlot(EquipmentSlot.CHEST, item);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Space suit (100% O2) -> CHEST for {}",
            player.getName().getString());
    }

    private static void equipSpacePants(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) return;
        ItemStack item = makeItem(SPACE_PANTS);
        if (item.isEmpty()) return;
        player.setItemSlot(EquipmentSlot.LEGS, item);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Space pants -> LEGS for {}",
            player.getName().getString());
    }

    private static void equipSpaceBoots(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.FEET).isEmpty()) return;
        ItemStack item = makeItem(SPACE_BOOTS);
        if (item.isEmpty()) return;
        player.setItemSlot(EquipmentSlot.FEET, item);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Space boots -> FEET for {}",
            player.getName().getString());
    }

    // -- Zip Gun -> hotbar/inventory (3000L oxygen, immediately usable) ----------

    private static void addZipGun(ServerPlayer player) {
        // Don't give if the player already has a zip_gun with oxygen in inventory
        for (ItemStack s : player.getInventory().items) {
            if (isFilledZipGun(s)) return;
        }

        ItemStack zipGun = makeItem(ZIP_GUN);
        if (zipGun.isEmpty()) return;

        CompoundTag fluidEntry = new CompoundTag();
        fluidEntry.putString("Fluid", OXYGEN_FLUID_ID);
        fluidEntry.putLong("Amount", TANK_STARTER_FILL_MB); // 3000L

        net.minecraft.nbt.ListTag storedFluids = new net.minecraft.nbt.ListTag();
        storedFluids.add(fluidEntry);

        CompoundTag botariumData = new CompoundTag();
        botariumData.put("StoredFluids", storedFluids);

        zipGun.getOrCreateTag().put("BotariumData", botariumData);

        player.getInventory().add(zipGun);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Zip Gun (3000L O2) -> inventory for {}",
            player.getName().getString());
    }

    private static boolean isFilledZipGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!matchesId(stack, ZIP_GUN)) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BotariumData")) return false;
        CompoundTag bd = tag.getCompound("BotariumData");
        if (!bd.contains("StoredFluids")) return false;
        net.minecraft.nbt.ListTag sf = bd.getList("StoredFluids", 10);
        return !sf.isEmpty() && sf.getCompound(0).getLong("Amount") > 0;
    }

    // -- Gas tank -> inventory (explicitly NOT an armour slot) -----------------

    private static void addGasTank(ServerPlayer player) {
        // Don't give if a filled tank is already anywhere in inventory
        for (ItemStack s : player.getInventory().items) {
            if (isFilledGasTank(s)) return;
        }

        ItemStack tank = makeItem(LARGE_GAS_TANK);
        if (tank.isEmpty()) return;

        // Botarium 2.3.4 WrappedItemFluidContainer NBT structure
        // confirmed from botarium-forge-1.20.1-2.3.4.jar bytecode:
        //
        //   item.tag {
        //     BotariumData: {                  <- CompoundTag (SimpleFluidContainer root)
        //       StoredFluids: [                <- ListTag of TAG_COMPOUND (type 10)
        //         { Fluid: "ad_astra:oxygen",  <- String (ResourceLocation)
        //           Amount: 3000000L }         <- Long
        //       ]
        //     }
        //   }
        //
        // Key names confirmed: ForgeFluidHolder.fromCompound reads "Fluid" (String)
        // and "Amount" (Long). SimpleFluidContainer.deserialize reads "BotariumData"
        // CompoundTag then "StoredFluids" ListTag.
        CompoundTag fluidEntry = new CompoundTag();
        fluidEntry.putString("Fluid", OXYGEN_FLUID_ID);
        fluidEntry.putLong("Amount", TANK_STARTER_FILL_MB); // 3,000 mB (3K) — not full capacity

        net.minecraft.nbt.ListTag storedFluids = new net.minecraft.nbt.ListTag();
        storedFluids.add(fluidEntry);

        CompoundTag botariumData = new CompoundTag();
        botariumData.put("StoredFluids", storedFluids);

        tank.getOrCreateTag().put("BotariumData", botariumData);

        player.getInventory().add(tank);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Gas tank (3K O2) -> inventory for {}",
            player.getName().getString());
    }

    // -- Helpers ---------------------------------------------------------------

    // -- Large Lithium Battery -> inventory (full 1,800,000 EU charge) ---------

    /**
     * Gives a fully-charged gtceu:hv_lithium_battery to the player on each
     * spawn/respawn, provided they do not already have one with charge remaining
     * in their inventory.
     *
     * A battery is considered "present and charged" if the item ID matches AND
     * the GTCEu "Charge" NBT tag is greater than 0.  This means an empty battery
     * already in the inventory will NOT prevent a fresh one being given.
     *
     * The charge is stored directly as NBT key "Charge" (Long) matching GTCEu's
     * ElectricItem NBT structure (confirmed from ElectricItem.class bytecode).
     */
    private static void addLargeLithiumBattery(ServerPlayer player) {
        // Skip if the player already has a charged large lithium battery
        for (ItemStack s : player.getInventory().items) {
            if (isChargedLargeLithiumBattery(s)) return;
        }

        ItemStack battery = makeItem("gtceu:hv_lithium_battery");
        if (battery.isEmpty()) return;

        // GTCEu ElectricItem stores charge under NBT key "Charge" (Long).
        // 1,800,000 EU = full charge for a Large Lithium Battery.
        battery.getOrCreateTag().putLong("Charge", 1_800_000L);

        player.getInventory().add(battery);
        DeepSpaceMod.LOGGER.debug("[StarterGear] Large Lithium Battery (1,800,000 EU) -> inventory for {}",
            player.getName().getString());
    }

    private static boolean isChargedLargeLithiumBattery(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !rl.toString().equals("gtceu:hv_lithium_battery")) return false;
        // Charged = has Charge NBT key and value > 0
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("Charge") && tag.getLong("Charge") > 0L;
    }


    private static boolean isFilledGasTank(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!matchesId(stack, LARGE_GAS_TANK)) return false;
        CompoundTag tag = stack.getTag();
        // Check using the correct Botarium NBT structure:
        // BotariumData -> StoredFluids (list) -> [0].Amount
        if (tag == null || !tag.contains("BotariumData")) return false;
        net.minecraft.nbt.CompoundTag bd = tag.getCompound("BotariumData");
        if (!bd.contains("StoredFluids")) return false;
        net.minecraft.nbt.ListTag sf = bd.getList("StoredFluids", 10);
        return !sf.isEmpty() && sf.getCompound(0).getLong("Amount") > 0;
    }

    private static boolean matchesId(ItemStack stack, String id) {
        var rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl != null && rl.toString().equals(id);
    }

    private static ItemStack makeItem(String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) {
            DeepSpaceMod.LOGGER.warn("[StarterGear] Item not found: {}", id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }
}
