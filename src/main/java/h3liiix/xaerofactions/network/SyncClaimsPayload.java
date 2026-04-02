package h3liiix.xaerofactions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record SyncClaimsPayload(List<ClaimInfo> claims, boolean clearExisting) implements CustomPayload {
    public static final CustomPayload.Id<SyncClaimsPayload> ID = new CustomPayload.Id<>(Identifier.of("xaerofactions", "sync_claims"));

    public static final PacketCodec<RegistryByteBuf, SyncClaimsPayload> CODEC = PacketCodec.tuple(
            ClaimInfo.CODEC.collect(PacketCodecs.toList()), SyncClaimsPayload::claims,
            PacketCodecs.BOOLEAN, SyncClaimsPayload::clearExisting,
            SyncClaimsPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public record ClaimInfo(int x, int z, String level, String factionName, int color) {
        public static final PacketCodec<RegistryByteBuf, ClaimInfo> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, ClaimInfo::x,
                PacketCodecs.INTEGER, ClaimInfo::z,
                PacketCodecs.STRING, ClaimInfo::level,
                PacketCodecs.STRING, ClaimInfo::factionName,
                PacketCodecs.INTEGER, ClaimInfo::color,
                ClaimInfo::new
        );
    }
}