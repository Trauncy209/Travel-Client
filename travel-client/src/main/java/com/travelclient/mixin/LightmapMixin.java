package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.render.Fullbright;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/NativeImage;setColor(III)V"), index = 2)
    private int modifyLightmapColor(int color) {
        Fullbright fullbright = TravelClient.moduleManager.getModule(Fullbright.class);
        if (fullbright != null && fullbright.isEnabled()) {
            // Return full white (max brightness)
            return 0xFFFFFFFF;
        }
        return color;
    }
}
