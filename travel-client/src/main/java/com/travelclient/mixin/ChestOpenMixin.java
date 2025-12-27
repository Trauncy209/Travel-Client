package com.travelclient.mixin;

import com.travelclient.module.render.ChestESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ChestOpenMixin {
    
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Check if opening a container screen
        if (screen instanceof GenericContainerScreen || screen instanceof ShulkerBoxScreen) {
            // Get the block we're looking at
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
                BlockPos pos = blockHit.getBlockPos();
                
                // Mark as opened by us
                ChestESP.markAsOpened(pos);
            }
        }
    }
}
