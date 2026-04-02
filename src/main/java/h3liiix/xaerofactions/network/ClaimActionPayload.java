package h3liiix.xaerofactions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClaimActionPayload(boolean isAdd, int minX, int minZ, int maxX, int maxZ, String dimension) implements CustomPayload {
    public static final CustomPayload.Id<ClaimActionPayload> ID = new CustomPayload.Id<>(Identifier.of("xaerofactions", "claim_action"));

    public static final PacketCodec<RegistryByteBuf, ClaimActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, ClaimActionPayload::isAdd,
            PacketCodecs.INTEGER, ClaimActionPayload::minX,
            PacketCodecs.INTEGER, ClaimActionPayload::minZ,
            PacketCodecs.INTEGER, ClaimActionPayload::maxX,
            PacketCodecs.INTEGER, ClaimActionPayload::maxZ,
            PacketCodecs.STRING, ClaimActionPayload::dimension,
            ClaimActionPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}