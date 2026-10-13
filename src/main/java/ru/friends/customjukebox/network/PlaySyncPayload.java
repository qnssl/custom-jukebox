package ru.friends.customjukebox.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server -> Client: "everyone, play this track at this jukebox; it
 * (should have) started at startMillis (System.currentTimeMillis() on the
 * server)". Clients use startMillis to compute how much has already
 * elapsed for late joiners.
 */
public record PlaySyncPayload(BlockPos pos, String trackId, long startMillis) implements CustomPayload {
    public static final CustomPayload.Id<PlaySyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("customjukebox", "play_sync"));

    public static final PacketCodec<RegistryByteBuf, PlaySyncPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, PlaySyncPayload::pos,
            PacketCodecs.STRING, PlaySyncPayload::trackId,
            PacketCodecs.VAR_LONG, PlaySyncPayload::startMillis,
            PlaySyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
