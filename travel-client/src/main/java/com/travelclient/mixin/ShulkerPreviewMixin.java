package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.render.ShulkerPreview;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class ShulkerPreviewMixin {
    
    @Shadow
    protected Slot focusedSlot;
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Check if ShulkerPreview is enabled
        if (TravelClient.moduleManager == null) return;
        ShulkerPreview preview = TravelClient.moduleManager.getModule(ShulkerPreview.class);
        if (preview == null || !preview.isEnabled()) return;
        
        // Check if Alt is held
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                         GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        
        if (!altHeld) return;
        
        // Check if hovering over a slot
        if (focusedSlot == null || !focusedSlot.hasStack()) return;
        
        ItemStack hoveredStack = focusedSlot.getStack();
        
        // Check if it's a shulker box
        if (!ShulkerPreview.isShulkerBox(hoveredStack)) return;
        
        // Render the preview
        ShulkerPreview.renderPreview(context, hoveredStack, mouseX, mouseY);
    }
}
