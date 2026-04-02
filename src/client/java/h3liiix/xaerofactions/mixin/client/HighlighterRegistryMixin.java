package h3liiix.xaerofactions.mixin.client;

import h3liiix.xaerofactions.client.CachedFactionsChunkHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.highlight.AbstractHighlighter;
import xaero.map.highlight.HighlighterRegistry;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(HighlighterRegistry.class)
public class HighlighterRegistryMixin {
    
    @Unique
    private static final CachedFactionsChunkHighlighter FACTIONS_HIGHLIGHTER = new CachedFactionsChunkHighlighter();

    @Shadow
    private List<AbstractHighlighter> highlighters;

    @Inject(method = "getHighlighters", at = @At("RETURN"), cancellable = true, remap = false)
    private void addFactionsHighlighter(CallbackInfoReturnable<List<AbstractHighlighter>> cir) {
        if (highlighters.contains(FACTIONS_HIGHLIGHTER)) return;
        List<AbstractHighlighter> newList = new ArrayList<>(highlighters);
        newList.add(FACTIONS_HIGHLIGHTER);
        this.highlighters = newList;
        cir.setReturnValue(newList);
    }
}