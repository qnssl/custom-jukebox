package ru.friends.customjukebox.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server -> Client: "everyone, stop whatever is playing at this jukebox".
 */
public record StopSyncPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<StopSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customjukebox", "stop_sync"));

    public static final PacketCodec<RegistryByteBuf, StopSyncPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, StopSyncPayload::pos,
            StopSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
