package h3liiix.xaerofactions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public record SyncPlayerPosPayload(List<PlayerPosInfo> positions) implements CustomPayload {
    public static final CustomPayload.Id<SyncPlayerPosPayload> ID = new CustomPayload.Id<>(Identifier.of("xaerofactions", "sync_player_pos"));
    
    public static final PacketCodec<RegistryByteBuf, SyncPlayerPosPayload> CODEC = PacketCodec.tuple(
            PlayerPosInfo.CODEC.collect(PacketCodecs.toList()), SyncPlayerPosPayload::positions,
            SyncPlayerPosPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record PlayerPosInfo(UUID uuid, String name, double x, double y, double z, String dimension, int color) {
        public static final PacketCodec<RegistryByteBuf, PlayerPosInfo> CODEC = PacketCodec.tuple(
                net.minecraft.util.Uuids.PACKET_CODEC, PlayerPosInfo::uuid,
                PacketCodecs.STRING, PlayerPosInfo::name,
                PacketCodecs.DOUBLE, PlayerPosInfo::x,
                PacketCodecs.DOUBLE, PlayerPosInfo::y,
                PacketCodecs.DOUBLE, PlayerPosInfo::z,
                PacketCodecs.STRING, PlayerPosInfo::dimension,
                PacketCodecs.INTEGER, PlayerPosInfo::color,
                PlayerPosInfo::new
        );
    }
}