package com.deepspace.events;

import com.deepspace.DSConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hotfix for Modular Routers SecurityUpgrade owner name corruption.
 *
 * PROBLEM
 * -------
 * When a modularrouters:security_upgrade is crafted, MiscEventHandler.onItemCrafted()
 * calls IPlayerOwned.setOwner(stack, player), which stores the owner name via:
 *
 *   player.getDisplayName().getString()   // Decorated by FTB Ranks: "[Stone] evilcats"
 *
 * Instead of:
 *
 *   player.getGameProfile().getName()     // Bare username: "evilcats"
 *
 * FTB Teams/Chunks generates the fake-player name from the bare profile name,
 * so the router doesn't recognize the fake player as the owner and denies access.
 *
 * SOLUTION
 * --------
 * Listen to PlayerEvent.ItemCraftedEvent at LOW priority (after Modular Routers
 * has already corrupted the owner tag), check if the crafted item is a security
 * upgrade with a decorated owner name, and fix it to use the bare profile name.
 *
 * This is simpler and more reliable than trying to Mixin into an interface default
 * method, and it works for both crafting table and AE2 crafting terminal.
 */
@Mod.EventBusSubscriber(modid = DSConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SecurityUpgradeOwnerFixHandler {

    /** Modular Routers security upgrade item ID. */
    private static final String SECURITY_UPGRADE_ID = "modularrouters:security_upgrade";

    /** NBT tag key where the owner name/UUID is stored. */
    private static final String OWNER_TAG = "Owner";

    /**
     * Intercepts ItemCraftedEvent and fixes the owner name if it's a security upgrade
     * with a decorated (rank-prefixed) owner name.
     *
     * Runs at LOW priority so Modular Routers' handler fires first, then we fix it.
     *
     * @param event The ItemCraftedEvent fired by Forge after crafting completes.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack craftedStack = event.getCrafting();

        // Quick exit: empty stack or not a security upgrade
        if (craftedStack.isEmpty()) return;
        if (!isSecurityUpgrade(craftedStack)) return;

        // Quick exit: stack has no NBT
        if (!craftedStack.hasTag()) return;

        // Attempt to fix the owner tag if it's corrupted
        fixOwnerTagIfCorrupted(craftedStack, event.getEntity());
    }

    /**
     * Returns true if the item is a modularrouters:security_upgrade.
     */
    private static boolean isSecurityUpgrade(ItemStack stack) {
        var item = stack.getItem();
        var registryKey = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        return registryKey != null && registryKey.toString().equals(SECURITY_UPGRADE_ID);
    }

    /**
     * If the Owner tag contains a decorated name (e.g., "[Stone] evilcats"), replaces
     * it with the bare game-profile name. Leaves the UUID unchanged.
     *
     * The Owner tag is a ListTag with two StringTag entries:
     *   [0] = owner name (string)
     *   [1] = owner UUID (string)
     *
     * @param stack The security upgrade ItemStack.
     * @param player The player who crafted it (for UUID validation).
     */
    private static void fixOwnerTagIfCorrupted(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Owner tag must be a ListTag of type TAG_STRING (type ID 8)
        if (!tag.contains(OWNER_TAG, Tag.TAG_LIST)) return;

        ListTag ownerList = tag.getList(OWNER_TAG, Tag.TAG_STRING);
        if (ownerList.size() < 2) return;  // Must have at least name + UUID

        String storedName = ownerList.getString(0);
        String storedUuid = ownerList.getString(1);
        String correctName = player.getGameProfile().getName();
        String correctUuid = player.getGameProfile().getId().toString();

        // Check if the stored name is decorated (contains rank prefix like "[Stone]")
        // A simple heuristic: if it doesn't match the bare profile name and the
        // UUID matches, it's likely corrupted by display name decoration.
        if (!storedName.equals(correctName) && storedUuid.equals(correctUuid)) {
            // Rewrite the owner name to the bare profile name
            ownerList.set(0, StringTag.valueOf(correctName));
            tag.put(OWNER_TAG, ownerList);
            stack.setTag(tag);

            // Log the fix for debugging
            com.deepspace.DeepSpaceMod.LOGGER.info(
                "[SecurityUpgradeOwnerFix] Corrected owner name for {} from '{}' to '{}'",
                SECURITY_UPGRADE_ID,
                storedName,
                correctName
            );
        }
    }
}
