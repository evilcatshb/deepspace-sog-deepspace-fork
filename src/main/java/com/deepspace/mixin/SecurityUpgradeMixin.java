package com.deepspace.mixin;

import com.deepspace.DeepSpaceMod;
import me.desht.modularrouters.item.IPlayerOwned;
import me.desht.modularrouters.item.upgrade.SecurityUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for SecurityUpgrade's right-click handlers to use the bare game-profile
 * name instead of the FTB-Teams-decorated display name.
 *
 * Targets two bugs:
 *  1. use() (right-click in air) → setOwner() stores the decorated name and the
 *     chat message also uses getDisplayName()
 *  2. interactLivingEntity() (right-click on another player) → addPlayer() stores
 *     the target's decorated display name as the permitted player name
 *
 * Both paths must use getGameProfile().getName() to match the fake-player identity.
 */
@Mixin(SecurityUpgrade.class)
public abstract class SecurityUpgradeMixin {

    // ─── use() : right-click in air ────────────────────────────────────────

    /**
     * Overwrites the entire use(Level, Player, InteractionHand) method.
     *
     * The original uses player.getDisplayName().getString() which returns the
     * FTB-Teams-decorated name (e.g. "[Stone] evilcats"). We must use
     * player.getGameProfile().getName() instead in both the owner tag and the
     * chat message.
     *
     * @param level   The level the player is in
     * @param player  The player using the item
     * @param hand    The hand containing the item
     * @param cir     CallbackInfoReturnable (for cancellation + return value)
     */
    @Inject(
        method = "m_7203_",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void deepspace$overwriteUse(
            Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {

        ItemStack stack = player.getItemInHand(hand);

        // Original logic: only on server side + player is sneaking
        if (!level.isClientSide && player.isShiftKeyDown()) {
            // Set the owner NBT with the correct (bare) profile name
            // We replicate IPlayerOwned.setOwner() but with getGameProfile().getName()
            CompoundTag tag = stack.getOrCreateTag();
            ListTag list = new ListTag();
            list.add(StringTag.valueOf(player.getGameProfile().getName()));  // FIX: bare name
            list.add(StringTag.valueOf(player.getUUID().toString()));
            tag.put(IPlayerOwned.NBT_OWNER, list);
            stack.setTag(tag);

            // Send chat message with the bare name (matching original behaviour but corrected)
            player.displayClientMessage(
                Component.translatable(
                    "modularrouters.itemText.security.owner",
                    player.getGameProfile().getName()  // FIX: bare name
                ),
                false
            );

            DeepSpaceMod.LOGGER.debug(
                "[SecurityUpgradeOwnerFix] use() – set owner to '{}' (UUID {})",
                player.getGameProfile().getName(),
                player.getUUID()
            );

            cir.setReturnValue(new InteractionResultHolder<>(InteractionResult.SUCCESS, stack));
            return;
        }

        // Non-sneaking or client-side: proceed normally (result PASS)
        cir.setReturnValue(InteractionResultHolder.pass(stack));
    }

    // ─── interactLivingEntity() : right-click on another player ─────────────

    /**
     * Redirects the getDisplayName() call inside interactLivingEntity() to
     * return a Component containing the bare game-profile name instead of
     * the FTB-Teams-decorated name.
     *
     * The original calls targetPlayer.getDisplayName() to get the name to store
     * as the permitted-player name. We swap in getGameProfile().getName().
     *
     * Note: We can't just redirect to return a String, because the bytecode
     * then calls .toString() on the Component. We redirect to a wrapper method
     * that returns Component.literal(gameProfileName).
     *
     * @param instance  The Player whose display name is being requested
     *                  (the targeted player, not the one holding the item)
     * @return A Component containing only the bare game-profile name
     */
    @Redirect(
        method = "m_6880_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;m_5446_()Lnet/minecraft/network/chat/Component;"
        ),
        remap = false
    )
    private Component deepspace$fixDisplayName(Player instance) {
        // Return a Component wrapping only the bare profile name
        return Component.literal(instance.getGameProfile().getName());
    }
}
