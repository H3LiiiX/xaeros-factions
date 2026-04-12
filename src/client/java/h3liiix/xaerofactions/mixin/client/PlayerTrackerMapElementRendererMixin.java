package h3liiix.xaerofactions.mixin.client;

import h3liiix.xaerofactions.XaeroFactionsClient;
import h3liiix.xaerofactions.network.SyncPlayerPosPayload;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.map.radar.tracker.PlayerTrackerMapElementRenderer;

@Pseudo
@Mixin(PlayerTrackerMapElementRenderer.class)
public class PlayerTrackerMapElementRendererMixin {

    @Inject(method = "renderElement", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void preRenderElement(PlayerTrackerMapElement<?> e, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, MapElementGraphics guiGraphics, VertexConsumerProvider.Immediate vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, CallbackInfoReturnable<Boolean> cir) {
        SyncPlayerPosPayload.PlayerPosInfo trackedInfo = XaeroFactionsClient.TRACKED_PLAYERS.get(e.getPlayerId());
        
        if (trackedInfo != null) {
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