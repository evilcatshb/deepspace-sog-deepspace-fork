package com.deepspace.items;

import com.deepspace.DSConstants;
import com.deepspace.structures.RegisteredStructure;
import com.deepspace.structures.StructureRadiusMapImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abnormality Scanner — a handheld radar device that locates the 3 nearest
 * randomly-generated NBT structures when right-clicked.
 *
 * For each found structure, the item:
 *   1. Sends "Abnormality Found @ [X Y Z]" to the player's chat.
 *   2. Executes "/wd plan deepspace:deep_space X Y Z" as the player.
 *
 * The scanner does NOT reveal what type of structure was detected — only its coordinates.
 */
public class AbnormalityScannerItem extends Item {

    /** Maximum search radius in blocks. */
    private static final int SCAN_RADIUS = 50000;

    /** Maximum number of results to return. */
    private static final int MAX_RESULTS = 3;

    /** Cooldown in ticks (3 seconds) to prevent spam. */
    private static final int COOLDOWN_TICKS = 60;

    public AbnormalityScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Server-side only
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel serverLevel = serverPlayer.serverLevel();

        // Apply cooldown to prevent spam
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // Find all structures within scan radius
        BlockPos playerPos = serverPlayer.blockPosition();
        List<RegisteredStructure> allStructures = StructureRadiusMapImpl.INSTANCE.getStructuresInRadius(
                serverLevel, serverLevel.getSeed(), playerPos, SCAN_RADIUS
        );

        if (allStructures.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("No abnormalities detected within scan range.")
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))
            );
            return InteractionResultHolder.success(stack);
        }

        // Sort by distance and take top 3
        List<RegisteredStructure> nearest = allStructures.stream()
                .sorted(Comparator.comparingDouble(s -> {
                    double dx = s.location.x - playerPos.getX();
                    double dy = s.location.y - playerPos.getY();
                    double dz = s.location.z - playerPos.getZ();
                    return dx * dx + dy * dy + dz * dz;
                }))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());

        // Output results to chat — coordinates are clickable and run /wd plan when clicked
        for (RegisteredStructure structure : nearest) {
            int x = structure.location.x;
            int y = structure.location.y;
            int z = structure.location.z;

            String wdCommand = "/wd plan deepspace:deep_space " + x + " " + y + " " + z;

            // "[X Y Z]" — clickable, runs /wd plan deepspace:deep_space X Y Z on click
            Component coords = Component.literal("[" + x + " " + y + " " + z + "]")
                    .withStyle(Style.EMPTY
                            .withColor(TextColor.fromRgb(0x00FF7F))          // bright spring green
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND, wdCommand))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to plan route: " + wdCommand))));

            // "Abnormality Found @ " — plain green prefix
            Component message = Component.literal("Abnormality Found @ ")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55)))
                    .append(coords);

            serverPlayer.sendSystemMessage(message);
        }

        return InteractionResultHolder.success(stack);
    }
}
