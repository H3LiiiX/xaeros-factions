package h3liiix.xaerofactions.client;

import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.map.highlight.ChunkHighlighter;
import h3liiix.xaerofactions.network.SyncClaimsPayload.ClaimInfo;
import h3liiix.xaerofactions.XaeroFactionsClient;

import java.util.List;

public class CachedFactionsChunkHighlighter extends ChunkHighlighter {

    public CachedFactionsChunkHighlighter() {
        super(true);
    }

    private ClaimInfo getClaim(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        return XaeroFactionsClient.getClaim(dimension.getValue().toString(), chunkX, chunkZ);
    }

    @Override
    public boolean chunkIsHighlit(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        return getClaim(dimension, chunkX, chunkZ) != null;
    }

    private int toXaeroColor(int rgb, int alpha) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        return (b << 24) | (g << 16) | (r << 8) | alpha;
    }

    @Override
    protected int[] getColors(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        ClaimInfo claim = getClaim(dimension, chunkX, chunkZ);
        if (claim == null) return null;

        int fillColor = toXaeroColor(claim.color(), 0x40);
        int borderColor = toXaeroColor(claim.color(), 0xCC);

        int[] colors = new int[5];
        colors[0] = fillColor;
        colors[1] = isSameFaction(claim, getClaim(dimension, chunkX, chunkZ - 1)) ? fillColor : borderColor; // Top
        colors[2] = isSameFaction(claim, getClaim(dimension, chunkX + 1, chunkZ)) ? fillColor : borderColor; // Right
        colors[3] = isSameFaction(claim, getClaim(dimension, chunkX, chunkZ + 1)) ? fillColor : borderColor; // Bottom
        colors[4] = isSameFaction(claim, getClaim(dimension, chunkX - 1, chunkZ)) ? fillColor : borderColor; // Left

        return colors;
    }

    private boolean isSameFaction(ClaimInfo original, ClaimInfo adjacent) {
        return adjacent != null && adjacent.factionName().equals(original.factionName());
    }

    @Override
    public Text getChunkHighlightSubtleTooltip(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        ClaimInfo claim = getClaim(dimension, chunkX, chunkZ);
        if (claim != null) {
            return Text.literal(claim.factionName()).withColor(claim.color());
        }
        return null;
    }

    @Override
    public Text getChunkHighlightBluntTooltip(RegistryKey<World> dimension, int chunkX, int chunkZ) {
        return getChunkHighlightSubtleTooltip(dimension, chunkX, chunkZ); 
    }

    @Override
    public boolean regionHasHighlights(RegistryKey<World> dimension, int regionX, int regionZ) {
        return true; 
    }

    @Override
    public int calculateRegionHash(RegistryKey<World> dimension, int regionX, int regionZ) {
        return XaeroFactionsClient.claimHash;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Text> list, RegistryKey<World> dimension, int blockX, int blockZ, int width) {
    }
}