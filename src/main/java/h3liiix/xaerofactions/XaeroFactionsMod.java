package h3liiix.xaerofactions;

import h3liiix.xaerofactions.network.SyncClaimsPayload;
import h3liiix.xaerofactions.network.ClaimActionPayload;
import h3liiix.xaerofactions.network.ClaimResponsePayload;
import h3liiix.xaerofactions.network.SyncPlayerPosPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class XaeroFactionsMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(SyncClaimsPayload.ID, SyncClaimsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClaimResponsePayload.ID, ClaimResponsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimActionPayload.ID, ClaimActionPayload.CODEC); 
        PayloadTypeRegistry.playS2C().register(SyncPlayerPosPayload.ID, SyncPlayerPosPayload.CODEC);
    }
}