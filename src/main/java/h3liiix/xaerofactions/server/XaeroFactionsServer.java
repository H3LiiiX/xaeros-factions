package h3liiix.xaerofactions.server;

import h3liiix.xaerofactions.network.ClaimActionPayload;
import h3liiix.xaerofactions.network.SyncClaimsPayload;
import h3liiix.xaerofactions.network.SyncPlayerPosPayload;
import h3liiix.xaerofactions.config.XaeroFactionsConfig;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
    
    private static boolean isBulkClaiming = false;

    @Override
    public void onInitializeServer() {
        XaeroFactionsConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendAllClaimsToPlayer(handler.getPlayer());
        });

        RelationshipEvents.NEW_DECLARATION.register((relationship) -> resyncAllPlayers());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;

            for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
                User viewerUser = User.get(viewer.getUuid());
                Faction viewerFaction = viewerUser != null ? viewerUser.getFaction() : null;

                List<SyncPlayerPosPayload.PlayerPosInfo> trackedPlayers = new ArrayList<>();

                for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                    if (viewer == target) continue;

                    User targetUser = User.get(target.getUuid());
                    Faction targetFaction = targetUser != null ? targetUser.getFaction() : null;

                    boolean sameFaction = false;
                    Relationship.Status status = Relationship.Status.NEUTRAL;

                    if (viewerFaction != null && targetFaction != null) {
                        if (viewerFaction.getID().equals(targetFaction.getID())) {
                            sameFaction = true;
                        } else {
                            Relationship rel = viewerFaction.getRelationship(targetFaction.getID());
                            if (rel != null) status = rel.status;
                        }
                    }

                    boolean shouldTrack = false;
                    if (sameFaction && XaeroFactionsConfig.INSTANCE.trackSameFaction) shouldTrack = true;
                    else if (!sameFaction && status == Relationship.Status.ALLY && XaeroFactionsConfig.INSTANCE.trackAlly) shouldTrack = true;
                    else if (!sameFaction && status == Relationship.Status.ENEMY && XaeroFactionsConfig.INSTANCE.trackEnemy) shouldTrack = true;
                    else if (!sameFaction && status == Relationship.Status.NEUTRAL && XaeroFactionsConfig.INSTANCE.trackNeutral) shouldTrack = true;

                    if (shouldTrack) {
                        int color = (targetFaction != null && targetFaction.getColor() != null && targetFaction.getColor().getColorValue() != null)
                                ? targetFaction.getColor().getColorValue() : 0xFFFFFF;

                        trackedPlayers.add(new SyncPlayerPosPayload.PlayerPosInfo(
                                target.getUuid(),
                                target.getName().getString(),
                                target.getX(), target.getY(), target.getZ(),
                                target.getEntityWorld().getRegistryKey().getValue().toString(),
                                color
                        ));
                    }
                }
                
                ServerPlayNetworking.send(viewer, new SyncPlayerPosPayload(trackedPlayers));
            }
        });

        ClaimEvents.ADD.register(claim -> {
            if (currentServer == null || isBulkClaiming) return;
            Faction targetFaction = claim.getFaction();
            
            for (ServerPlayerEntity player : currentServer.getPlayerManager().getPlayerList()) {
                User user = User.get(player.getUuid());
                Faction playerFaction = user != null ? user.getFaction() : null;
                
                if (isVisibleTo(playerFaction, targetFaction)) {
                    ServerPlayNetworking.send(player, new SyncClaimsPayload(List.of(toClaimInfo(claim)), false));
                }
            }
        });

        ClaimEvents.REMOVE.register((x, z, level, faction) -> {
            if (currentServer == null || isBulkClaiming) return;
            SyncClaimsPayload.ClaimInfo removedInfo = new SyncClaimsPayload.ClaimInfo(x, z, level, "", -1);
            SyncClaimsPayload payload = new SyncClaimsPayload(List.of(removedInfo), false);
            
            for (ServerPlayerEntity player : currentServer.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }
        });

        FactionEvents.MODIFY.register(faction -> {
            if (faction == null || currentServer == null) return;
            
            for (ServerPlayerEntity player : currentServer.getPlayerManager().getPlayerList()) {
                User user = User.get(player.getUuid());
                Faction playerFaction = user != null ? user.getFaction() : null;
                
                if (isVisibleTo(playerFaction, faction)) {
                    List<SyncClaimsPayload.ClaimInfo> updatedClaims = new ArrayList<>();
                    for (Claim claim : faction.getClaims()) {
                        updatedClaims.add(toClaimInfo(claim));
                    }
                    if (!updatedClaims.isEmpty()) {
                        ServerPlayNetworking.send(player, new SyncClaimsPayload(updatedClaims, false));
                    }
                }
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
                            boolean isEndDisabled = false;
                            try {
                                java.lang.reflect.Field field = FactionsMod.CONFIG.getClass().getField("DISABLE_END_ISLAND_CLAIMS");
                                isEndDisabled = field.getBoolean(FactionsMod.CONFIG);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                isEndDisabled = false; 
                            }

                            if (isEndDisabled && payload.dimension().equals("minecraft:the_end") && x >= -11 && x <= 11 && z >= -11 && z <= 11) {
                                sendMapNotification(player, false, "You cannot claim the main End island!");
                                return;
                            }

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

                    List<SyncClaimsPayload.ClaimInfo> successfulClaims = new ArrayList<>();
                    
                    isBulkClaiming = true;
                    try {
                        for (int x = minX; x <= maxX; x++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                if (Claim.get(x, z, payload.dimension()) == null) {
                                    faction.addClaim(x, z, payload.dimension());
                                    Claim added = Claim.get(x, z, payload.dimension());
                                    if (added != null) {
                                        successfulClaims.add(toClaimInfo(added));
                                    }
                                }
                            }
                        }
                    } finally {
                        isBulkClaiming = false;
                    }

                    if (!successfulClaims.isEmpty()) {
                        for (ServerPlayerEntity p : currentServer.getPlayerManager().getPlayerList()) {
                            User u = User.get(p.getUuid());
                            Faction pFaction = u != null ? u.getFaction() : null;
                            if (isVisibleTo(pFaction, faction)) {
                                ServerPlayNetworking.send(p, new SyncClaimsPayload(successfulClaims, false));
                            }
                        }
                    }
                    sendMapNotification(player, true, "Successfully claimed " + chunksToClaim + " chunks!");

                } else {
                    int successCount = 0;
                    List<SyncClaimsPayload.ClaimInfo> removedClaims = new ArrayList<>();
                    
                    isBulkClaiming = true;
                    try {
                        for (int x = minX; x <= maxX; x++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                Claim existing = Claim.get(x, z, payload.dimension());
                                if (existing != null && (user.bypass || existing.getFaction().getID().equals(faction.getID()))) {
                                    existing.remove();
                                    removedClaims.add(new SyncClaimsPayload.ClaimInfo(x, z, payload.dimension(), "", -1));
                                    successCount++;
                                }
                            }
                        }
                    } finally {
                        isBulkClaiming = false;
                    }
                    
                    if (successCount > 0) {
                        SyncClaimsPayload removalPayload = new SyncClaimsPayload(removedClaims, false);
                        for (ServerPlayerEntity p : currentServer.getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(p, removalPayload);
                        }
                        sendMapNotification(player, true, "Successfully unclaimed " + successCount + " chunks!");
                    } else {
                        sendMapNotification(player, false, "No chunks owned by your faction were found in that selection.");
                    }
                }
            });
        });

    }

    private void resyncAllPlayers() {
        if (currentServer != null) {
            for (ServerPlayerEntity player : currentServer.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, new SyncClaimsPayload(new ArrayList<>(), true));
                sendAllClaimsToPlayer(player);
            }
        }
    }

    private void sendAllClaimsToPlayer(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        Faction playerFaction = user != null ? user.getFaction() : null;

        List<SyncClaimsPayload.ClaimInfo> allClaims = new ArrayList<>();
        
        java.util.Collection<Faction> factionsList;
        
        try {
            java.lang.reflect.Method hiddenMethod = Faction.class.getMethod("allIncludeHidden");
            factionsList = (java.util.Collection<Faction>) hiddenMethod.invoke(null);
        } catch (Exception e) {
            factionsList = Faction.all();
        }

        for (Faction faction : factionsList) {
            if (!isVisibleTo(playerFaction, faction)) continue;

            for (Claim claim : faction.getClaims()) {
                allClaims.add(toClaimInfo(claim));
            }
        }
        
        ServerPlayNetworking.send(player, new SyncClaimsPayload(allClaims, true));
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

    private boolean isVisibleTo(Faction viewer, Faction target) {
        if (target == null) return false;
        if (viewer != null && viewer.getID().equals(target.getID())) return true;

        Relationship.Status status = Relationship.Status.NEUTRAL;
        if (viewer != null) {
            Relationship rel = target.getRelationship(viewer.getID());
            if (rel != null) status = rel.status;
        }

        if (status == Relationship.Status.ALLY && XaeroFactionsConfig.INSTANCE.hideAllyClaims) return false;
        if (status == Relationship.Status.ENEMY && XaeroFactionsConfig.INSTANCE.hideEnemyClaims) return false;
        if (status == Relationship.Status.NEUTRAL && XaeroFactionsConfig.INSTANCE.hideNeutralClaims) return false;

        return true;
    }

    private static void sendMapNotification(ServerPlayerEntity player, boolean success, String message) {
        ServerPlayNetworking.send(player, new h3liiix.xaerofactions.network.ClaimResponsePayload(success, message));
        player.sendMessage(net.minecraft.text.Text.literal(message).formatted(success ? Formatting.GREEN : Formatting.RED), false);
    }

}