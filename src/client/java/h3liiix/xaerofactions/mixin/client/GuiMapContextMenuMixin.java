package h3liiix.xaerofactions.mixin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import h3liiix.xaerofactions.network.ClaimActionPayload;

import java.util.ArrayList;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapContextMenuMixin implements IRightClickableElement {

    @Shadow private int rightClickX;
    @Shadow private int rightClickZ;
    @Shadow private MapTileSelection mapTileSelection;
    @Shadow private MapProcessor mapProcessor;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void addFactionsOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ArrayList<RightClickOption> options = cir.getReturnValue();

        boolean hasSelection = this.mapTileSelection != null;

        int minX = hasSelection ? this.mapTileSelection.getLeft() : (this.rightClickX >> 4);
        int maxX = hasSelection ? this.mapTileSelection.getRight() : (this.rightClickX >> 4);
        int minZ = hasSelection ? this.mapTileSelection.getTop() : (this.rightClickZ >> 4);
        int maxZ = hasSelection ? this.mapTileSelection.getBottom() : (this.rightClickZ >> 4);

        String dimension = this.mapProcessor.getMapWorld().getFutureDimensionId().getValue().toString();

        options.add(new RightClickOption(hasSelection ? "Claim Selected Chunks" : "Claim Chunk", options.size(), this) {
            @Override
            public void onAction(Screen screen) {
                ClientPlayNetworking.send(new ClaimActionPayload(true, minX, minZ, maxX, maxZ, dimension));
            }
        });

        options.add(new RightClickOption(hasSelection ? "Unclaim Selected Chunks" : "Unclaim Chunk", options.size(), this) {
            @Override
            public void onAction(Screen screen) {
                ClientPlayNetworking.send(new ClaimActionPayload(false, minX, minZ, maxX, maxZ, dimension));
            }
        });
    }
}