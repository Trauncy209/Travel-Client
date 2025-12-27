package com.travelclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCredit(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TitleScreen screen = (TitleScreen) (Object) this;
        
        String credit = "Created by Truancy209";
        int textWidth = mc.textRenderer.getWidth(credit);
        
        // Position: top right with some padding
        int x = screen.width - textWidth - 10;
        int y = 10;
        
        // Bright red color
        int color = 0xFFFF5555;
        
        // Draw with shadow for visibility
        context.drawTextWithShadow(mc.textRenderer, credit, x, y, color);
        
        // Also draw the mod name below it
        String modName = "§6Travel Client §7v1.0.0";
        int modNameWidth = mc.textRenderer.getWidth(modName);
        context.drawTextWithShadow(mc.textRenderer, modName, screen.width - modNameWidth - 10, y + 12, 0xFFFFFFFF);
    }
}
