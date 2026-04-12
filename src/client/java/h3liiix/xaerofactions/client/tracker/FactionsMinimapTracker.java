package h3liiix.xaerofactions.client.tracker;

import h3liiix.xaerofactions.network.SyncPlayerPosPayload.PlayerPosInfo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xaero.hud.minimap.player.tracker.system.IRenderedPlayerTracker;
import xaero.hud.minimap.player.tracker.system.ITrackedPlayerReader;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class FactionsMinimapTracker implements IRenderedPlayerTracker<PlayerPosInfo>, ITrackedPlayerReader<PlayerPosInfo> {
    private final Map<UUID, PlayerPosInfo> map;

    public FactionsMinimapTracker(Map<UUID, PlayerPosInfo> map) {
        this.map = map;
    }

    @Override
    public ITrackedPlayerReader<PlayerPosInfo> getReader() { return this; }

    @Override
    public Iterator<PlayerPosInfo> getTrackedPlayerIterator() { return map.values().iterator(); }

    @Override
    public UUID getId(PlayerPosInfo player) { return player.uuid(); }

    @Override
    public double getX(PlayerPosInfo player) { return player.x(); }

    @Override
    public double getY(PlayerPosInfo player) { return player.y(); }

    @Override
    public double getZ(PlayerPosInfo player) { return player.z(); }

    @Override
    public RegistryKey<World> getDimension(PlayerPosInfo player) {
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(player.dimension()));
    }
}