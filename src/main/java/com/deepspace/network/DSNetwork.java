package com.deepspace.network;

import com.deepspace.DeepSpaceMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central network channel for DeepSpace GUI packets.
 *
 * Packets registered here:
 *   0 → {@link WarpRequestPacket}  (C→S)  player clicked WARP in Ship Scanner GUI
 */
public final class DSNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DeepSpaceMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(
            nextId++,
            WarpRequestPacket.class,
            WarpRequestPacket::encode,
            WarpRequestPacket::decode,
            WarpRequestPacket::handle
        );
    }

    private DSNetwork() {}
}
