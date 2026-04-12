package h3liiix.xaerofactions;

import h3liiix.xaerofactions.network.ClaimResponsePayload;
import h3liiix.xaerofactions.network.SyncClaimsPayload;
import h3liiix.xaerofactions.network.SyncPlayerPosPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class XaeroFactionsClient implements ClientModInitializer {
    public static final Map<String, SyncClaimsPayload.ClaimInfo> CACHED_CLAIMS = new ConcurrentHashMap<>();
    public static final Map<UUID, SyncPlayerPosPayload.PlayerPosInfo> TRACKED_PLAYERS = new ConcurrentHashMap<>();
    public static int claimHash = 1;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CACHED_CLAIMS.clear();
            TRACKED_PLAYERS.clear();
            claimHash++; 
            forceMapReload();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CACHED_CLAIMS.clear();
            TRACKED_PLAYERS.clear();
            claimHash++; 
            forceMapReload();
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPlayerPosPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                TRACKED_PLAYERS.clear();
                for (SyncPlayerPosPayload.PlayerPosInfo pos : payload.positions()) {
                    TRACKED_PLAYERS.put(pos.uuid(), pos);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncClaimsPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.clearExisting()) CACHED_CLAIMS.clear();

                for (SyncClaimsPayload.ClaimInfo info : payload.claims()) {
                    String key = getKey(info.level(), info.x(), info.z());
                    if (info.color() == -1) {
                        CACHED_CLAIMS.remove(key);
                    } else {
                        CACHED_CLAIMS.put(key, info);
                    }
                }
                
                claimHash++; 
                forceMapReload();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ClaimResponsePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                net.minecraft.text.Text title = net.minecraft.text.Text.literal(payload.successful() ? "Factions" : "Factions")
                        .formatted(payload.successful() ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED);
                net.minecraft.text.Text message = net.minecraft.text.Text.literal(payload.message())
                        .formatted(net.minecraft.util.Formatting.WHITE);

                context.client().getToastManager().add(
                        new net.minecraft.client.toast.SystemToast(
                                net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION, 
                                title, 
                                message
                        )
                );
            });
        });

    }

    public static String getKey(String level, int x, int z) {
        return level + "-" + x + "-" + z;
    }

    public static SyncClaimsPayload.ClaimInfo getClaim(String level, int x, int z) {
        return CACHED_CLAIMS.get(getKey(level, x, z));
    }

    public static void forceMapReload() {
        try {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
                xaero.map.WorldMapSession session = xaero.map.WorldMapSession.getCurrentSession();
                
                if (session != null && session.getMapProcessor() != null && session.getMapProcessor().getMapWorld() != null) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    
                    if (client != null && client.world != null) {
                        xaero.map.world.MapDimension mapDim = session.getMapProcessor().getMapWorld()
                                .getDimension(client.world.getRegistryKey());
                                
                        if (mapDim != null && mapDim.getHighlightHandler() != null) {
                            mapDim.getHighlightHandler().clearCachedHashes();
                        }
                    }
                }
            }
        } catch (Throwable t) {

        }
    }
}