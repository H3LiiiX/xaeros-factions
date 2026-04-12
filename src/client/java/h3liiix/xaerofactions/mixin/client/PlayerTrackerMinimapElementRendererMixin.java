package h3liiix.xaerofactions.mixin.client;

import h3liiix.xaerofactions.XaeroFactionsClient;
import h3liiix.xaerofactions.network.SyncPlayerPosPayload;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer;

@Pseudo
@Mixin(PlayerTrackerMinimapElementRenderer.class)
public class PlayerTrackerMinimapElementRendererMixin {

    @Inject(method = "renderElement", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void preRenderElement(PlayerTrackerMinimapElement<?> e, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, MinimapElementGraphics guiGraphics, VertexConsumerProvider.Immediate vanillaBufferSource, CallbackInfoReturnable<Boolean> cir) {
        SyncPlayerPosPayload.PlayerPosInfo trackedInfo = XaeroFactionsClient.TRACKED_PLAYERS.get(e.getPlayerId());

        if (trackedInfo != null) {
            if (renderInfo.location == MinimapElementRenderLocation.IN_WORLD) {
                cir.setReturnValue(false);
                return;
            }

            if (net.minecraft.client.MinecraftClient.getInstance().world != null) {
                String currentDim = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
                if (!trackedInfo.dimension().equals(currentDim)) {
                    cir.setReturnValue(false);
                    return;
                }

                if (net.minecraft.client.MinecraftClient.getInstance().world.getPlayerByUuid(trackedInfo.uuid()) != null) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}