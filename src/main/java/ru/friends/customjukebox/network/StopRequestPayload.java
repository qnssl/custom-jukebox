package ru.friends.customjukebox.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Client -> Server: "stop whatever is playing at this jukebox".
 */
public record StopRequestPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<StopRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customjukebox", "stop_request"));

    public static final PacketCodec<RegistryByteBuf, StopRequestPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, StopRequestPayload::pos,
            StopRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
