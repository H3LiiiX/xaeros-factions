package h3liiix.xaerofactions.server;
import h3liiix.xaerofactions.network.ClaimActionPayload;
import h3liiix.xaerofactions.network.SyncClaimsPayload;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.ChunkPos;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;

public class XaeroFactionsServer implements DedicatedServerModInitializer {
    private static MinecraftServer currentServer;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendAllClaimsToPlayer(handler.getPlayer());
        });

        ClaimEvents.ADD.register(claim -> {
            SyncClaimsPayload payload = new SyncClaimsPayload(List.of(toClaimInfo(claim)), false);
            broadcastToAll(payload);
        });

        ClaimEvents.REMOVE.register((x, z, level, faction) -> {
            SyncClaimsPayload.ClaimInfo removedInfo = new SyncClaimsPayload.ClaimInfo(x, z, level, "", -1);
            SyncClaimsPayload payload = new SyncClaimsPayload(List.of(removedInfo), false);
            broadcastToAll(payload);
        });

        FactionEvents.MODIFY.register(faction -> {
            if (faction == null) return;
            
            List<SyncClaimsPayload.ClaimInfo> updatedClaims = new ArrayList<>();
            for (Claim claim : faction.getClaims()) {
                updatedClaims.add(toClaimInfo(claim));
            }
            
            if (!updatedClaims.isEmpty()) {
                SyncClaimsPayload payload = new SyncClaimsPayload(updatedClaims, false);
                broadcastToAll(payload);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimActionPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                User user = User.get(player.getUuid());
                if (user == null) return;
                Faction faction = user.getFaction();
                
                if (faction == null) {
                    sendMapNotification(player, false, "You must be in a faction to do this!");
                    return;
                }

                if (user.rank.ordinal() > User.Rank.COMMANDER.ordinal() && !user.bypass) {
                    sendMapNotification(player, false, "You must be a Commander or higher to edit claims!");
                    return;
                }

                int minX = Math.min(payload.minX(), payload.maxX());
                int maxX = Math.max(payload.minX(), payload.maxX());
                int minZ = Math.min(payload.minZ(), payload.maxZ());
                int maxZ = Math.max(payload.minZ(), payload.maxZ());

                if (payload.isAdd()) {
                    String playerDim = player.getEntityWorld().getRegistryKey().getValue().toString();
                    int playerChunkX = player.getBlockX() >> 4;
                    int playerChunkZ = player.getBlockZ() >> 4;

                    boolean insideSelection = (playerChunkX >= minX && playerChunkX <= maxX && playerChunkZ >= minZ && playerChunkZ <= maxZ);

                    if (!user.bypass) {
                        if (!playerDim.equals(payload.dimension())) {
                            sendMapNotification(player, false, "You must be in the same dimension to claim this area!");
                            return;
                        }
                    }

                    boolean isAttachedToContiguousBase = false;

                    if (!insideSelection) {
                        java.util.Set<net.minecraft.util.math.ChunkPos> factionClaims = new java.util.HashSet<>();
                        for (Claim c : faction.getClaims()) {
                            if (c.level.equals(payload.dimension())) {
                                factionClaims.add(new net.minecraft.util.math.ChunkPos(c.x, c.z));
                            }
                        }

                        net.minecraft.util.math.ChunkPos startNode = new net.minecraft.util.math.ChunkPos(playerChunkX, playerChunkZ);
                        
                        if (factionClaims.contains(startNode)) {
                            java.util.Set<net.minecraft.util.math.ChunkPos> contiguousBase = new java.util.HashSet<>();
                            java.util.Queue<net.minecraft.util.math.ChunkPos> queue = new java.util.LinkedList<>();
                            
                            queue.add(startNode);
                            contiguousBase.add(startNode);
                            
                            int[][] dirs = {{0,1}, {1,0}, {0,-1}, {-1,0}};
                            
                            while (!queue.isEmpty()) {
                                net.minecraft.util.math.ChunkPos curr = queue.poll();
                                for (int[] d : dirs) {
                                    net.minecraft.util.math.ChunkPos neighbor = new net.minecraft.util.math.ChunkPos(curr.x + d[0], curr.z + d[1]);
                                    if (factionClaims.contains(neighbor) && !contiguousBase.contains(neighbor)) {
                                        contiguousBase.add(neighbor);
                                        queue.add(neighbor);
                                    }
                                }
                            }

                            for (int x = minX; x <= maxX; x++) {
                                for (int z = minZ; z <= maxZ; z++) {
                                    net.minecraft.util.math.ChunkPos pos = new net.minecraft.util.math.ChunkPos(x, z);
                                    if (contiguousBase.contains(pos)) {
                                        isAttachedToContiguousBase = true;
                                        break;
                                    }
                                    for (int[] d : dirs) {
                                        if (contiguousBase.contains(new net.minecraft.util.math.ChunkPos(x + d[0], z + d[1]))) {
                                            isAttachedToContiguousBase = true;
                                            break;
                                        }
                                    }
                                }
                                if (isAttachedToContiguousBase) break;
                            }
                        }
                    }

                    if (!user.bypass && !insideSelection && !isAttachedToContiguousBase) {
                        sendMapNotification(player, false, "You must be standing inside the claim!");
                        return;
                    }

                    int chunksToClaim = 0;
                    boolean conflict = false;

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Claim existing = Claim.get(x, z, payload.dimension());
                            if (existing == null) {
                                chunksToClaim++;
                            } else if (!existing.getFaction().getID().equals(faction.getID())) {
                                conflict = true;
                            }
                        }
                    }

                    if (conflict && !user.bypass) {
                        sendMapNotification(player, false, "Part of this selection is already claimed by another faction!");
                        return;
                    }

                    if (chunksToClaim == 0) {
                        sendMapNotification(player, false, "All selected chunks are already owned by your faction.");
                        return;
                    }

                    int requiredPower = (faction.getClaims().size() + chunksToClaim) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
                    int maxPower = faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE + faction.getAdminPower();

                    if (maxPower < requiredPower && !user.bypass) {
                        sendMapNotification(player, false, "Not enough power! You need " + requiredPower + " power to claim this area.");
                        return;
                    }

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (Claim.get(x, z, payload.dimension()) == null) {
                                faction.addClaim(x, z, payload.dimension());
                            }
                        }
                    }
                    sendMapNotification(player, true, "Successfully claimed " + chunksToClaim + " chunks!");

                } else {
                    int successCount = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Claim existing = Claim.get(x, z, payload.dimension());
                            if (existing != null && (user.bypass || existing.getFaction().getID().equals(faction.getID()))) {
                                existing.remove();
                                successCount++;
                            }
                        }
                    }
                    
                    if (successCount > 0) {
                        sendMapNotification(player, true, "Successfully unclaimed " + successCount + " chunks!");
                    } else {
                        sendMapNotification(player, false, "No chunks owned by your faction were found in that selection.");
                    }
                }
            });
        });

    }

    private void sendAllClaimsToPlayer(ServerPlayerEntity player) {
        List<SyncClaimsPayload.ClaimInfo> allClaims = new ArrayList<>();
        for (Faction faction : Faction.all()) {
            for (Claim claim : faction.getClaims()) {
                allClaims.add(toClaimInfo(claim));
            }
        }
        ServerPlayNetworking.send(player, new SyncClaimsPayload(allClaims, true));
    }

    private void broadcastToAll(SyncClaimsPayload payload) {
        if (currentServer != null) {
            for (ServerPlayerEntity player : currentServer.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private SyncClaimsPayload.ClaimInfo toClaimInfo(Claim claim) {
        Faction faction = claim.getFaction();
        int color = 0xFFFFFF; 
        String name = "Unknown";
        if (faction != null) {
            name = faction.getName();
            Formatting formatting = faction.getColor();
            if (formatting != null && formatting.getColorValue() != null) {
                color = formatting.getColorValue();
            }
        }
        return new SyncClaimsPayload.ClaimInfo(claim.x, claim.z, claim.level, name, color);
    }

    private static void sendMapNotification(ServerPlayerEntity player, boolean success, String message) {
        ServerPlayNetworking.send(player, new h3liiix.xaerofactions.network.ClaimResponsePayload(success, message));
        player.sendMessage(net.minecraft.text.Text.literal(message).formatted(success ? Formatting.GREEN : Formatting.RED), false);
    }
}