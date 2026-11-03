package com.deepspace.network;

import com.deepspace.DeepSpaceMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server packet sent when the player clicks WARP in the Ship Scanner GUI.
 *
 * <p>The server receives the target coordinates + dimension and executes
 * {@code /wd plan <dim> <x> <y> <z>} through the WarpDrive command dispatcher
 * using the player's own command source stack, so all permission checks still apply.
 * The GUI is already closed client-side before this packet is sent.
 */
public record WarpRequestPacket(String dimension, int x, int y, int z) {

    public static void encode(WarpRequestPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.dimension, 256);
        buf.writeInt(pkt.x);
        buf.writeInt(pkt.y);
        buf.writeInt(pkt.z);
    }

    public static WarpRequestPacket decode(FriendlyByteBuf buf) {
        return new WarpRequestPacket(
            buf.readUtf(256),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        );
    }

    public static void handle(WarpRequestPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Run "/wd plan <dim> <x> <y> <z>" as the player.
            // performPrefixedCommand prepends "/" automatically.
            String cmd = String.format("wd plan %s %d %d %d",
                pkt.dimension(), pkt.x(), pkt.y(), pkt.z());
            try {
                player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(), cmd);
            } catch (Exception e) {
                DeepSpaceMod.LOGGER.warn("[ShipScanner] Warp command failed for {}: {}",
                    player.getName().getString(), e.getMessage());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
