package h3liiix.xaerofactions.mixin.client;

import h3liiix.xaerofactions.XaeroFactionsClient;
import h3liiix.xaerofactions.client.tracker.FactionsWorldMapTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.mods.SupportMods;

@Pseudo
@Mixin(SupportMods.class)
public class SupportModsWorldMapMixin {
    @Inject(method = "load", at = @At("TAIL"), remap = false)
    private void onLoad(CallbackInfo ci) {
        try {
            WorldMap.playerTrackerSystemManager.register("xaerofactions", new FactionsWorldMapTracker(XaeroFactionsClient.TRACKED_PLAYERS));
        } catch (Throwable ignored) {}
    }
}