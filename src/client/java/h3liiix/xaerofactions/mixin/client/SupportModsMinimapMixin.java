package h3liiix.xaerofactions.mixin.client;

import h3liiix.xaerofactions.XaeroFactionsClient;
import h3liiix.xaerofactions.client.tracker.FactionsMinimapTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.mods.SupportMods;

@Pseudo
@Mixin(SupportMods.class)
public class SupportModsMinimapMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(IXaeroMinimap modMain, CallbackInfo ci) {
        try {
            modMain.getRenderedPlayerTrackerManager().register("xaerofactions", new FactionsMinimapTracker(XaeroFactionsClient.TRACKED_PLAYERS));
        } catch (Throwable ignored) {}
    }
}