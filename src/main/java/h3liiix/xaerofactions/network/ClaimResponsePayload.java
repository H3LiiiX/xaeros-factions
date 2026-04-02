package h3liiix.xaerofactions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClaimResponsePayload(boolean successful, String message) implements CustomPayload {
    public static final CustomPayload.Id<ClaimResponsePayload> ID = new CustomPayload.Id<>(Identifier.of("xaerofactions", "claim_response"));

    public static final PacketCodec<RegistryByteBuf, ClaimResponsePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, ClaimResponsePayload::successful,
            PacketCodecs.STRING, ClaimResponsePayload::message,
            ClaimResponsePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}