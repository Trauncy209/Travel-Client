package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.player.NBTViewer;
import com.travelclient.module.render.HudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (TravelClient.moduleManager != null) {
            HudModule hud = TravelClient.moduleManager.getModule(HudModule.class);
            if (hud != null && hud.isEnabled()) {
                hud.render(context);
            }
            
            // NBT Viewer
            NBTViewer nbtViewer = TravelClient.moduleManager.getModule(NBTViewer.class);
            if (nbtViewer != null && nbtViewer.isEnabled()) {
                nbtViewer.render(context);
            }
        }
    }
}
