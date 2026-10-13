package ru.friends.customjukebox.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Client -> Server: "I want to start playing this track at this jukebox".
 */
public record PlayRequestPayload(BlockPos pos, String trackId) implements CustomPayload {
    public static final CustomPayload.Id<PlayRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customjukebox", "play_request"));

    public static final PacketCodec<RegistryByteBuf, PlayRequestPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, PlayRequestPayload::pos,
            PacketCodecs.STRING, PlayRequestPayload::trackId,
            PlayRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
