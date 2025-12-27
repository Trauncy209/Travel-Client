package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.util.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

public class ItemESP extends Module {
    
    private double range = 50;
    private boolean tracers = true;
    private boolean box = true;
    private boolean nametags = true;
    
    // Valuable items to highlight specially
    private static final Set<String> VALUABLE_ITEMS = new HashSet<>();
    static {
        VALUABLE_ITEMS.add("diamond");
        VALUABLE_ITEMS.add("emerald");
        VALUABLE_ITEMS.add("netherite");
        VALUABLE_ITEMS.add("enchanted");
        VALUABLE_ITEMS.add("totem");
        VALUABLE_ITEMS.add("elytra");
        VALUABLE_ITEMS.add("shulker");
        VALUABLE_ITEMS.add("beacon");
        VALUABLE_ITEMS.add("nether_star");
        VALUABLE_ITEMS.add("trident");
        VALUABLE_ITEMS.add("ancient_debris");
        VALUABLE_ITEMS.add("golden_apple");
    }
    
    public ItemESP() {
        super("ItemESP", "Highlights items with nametags", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void onRender(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        RenderUtils.setup();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (mc.player.distanceTo(entity) > range) continue;
            
            ItemStack stack = itemEntity.getStack();
            String itemName = stack.getName().getString();
            String registryName = stack.getItem().toString().toLowerCase();
            
            // Check if valuable
            boolean isValuable = false;
            for (String valuable : VALUABLE_ITEMS) {
                if (registryName.contains(valuable) || itemName.toLowerCase().contains(valuable)) {
                    isValuable = true;
                    break;
                }
            }
            
            // Check for enchantments
            if (stack.hasEnchantments()) {
                isValuable = true;
            }
            
            // Color based on value
            float r, g, b;
            if (isValuable) {
                r = 1.0f; g = 0.8f; b = 0.0f; // Gold for valuable
            } else {
                r = 0.0f; g = 1.0f; b = 1.0f; // Cyan for normal
            }
            
            if (box) {
                RenderUtils.drawEntityBox(matrices, entity, r, g, b, 1.0f);
            }
            
            if (tracers) {
                RenderUtils.drawTracer(matrices, entity, r, g, b, 0.5f);
            }
        }
        
        RenderUtils.cleanup();
    }
    
    /**
     * Render item nametags - called from HUD mixin
     */
    public void renderNametags(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled() || !nametags) return;
        
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > range) continue;
            
            ItemStack stack = itemEntity.getStack();
            String itemName = stack.getName().getString();
            int count = stack.getCount();
            String registryName = stack.getItem().toString().toLowerCase();
            
            // Check if valuable
            boolean isValuable = false;
            for (String valuable : VALUABLE_ITEMS) {
                if (registryName.contains(valuable) || itemName.toLowerCase().contains(valuable)) {
                    isValuable = true;
                    break;
                }
            }
            if (stack.hasEnchantments()) isValuable = true;
            
            // Build display text
            String displayText;
            if (count > 1) {
                displayText = itemName + " x" + count;
            } else {
                displayText = itemName;
            }
            
            // Add distance
            displayText += " §7[" + (int)dist + "m]";
            
            // Color prefix
            String color = isValuable ? "§6" : "§b";
            displayText = color + displayText;
            
            // Calculate screen position
            Vec3d entityPos = entity.getPos().add(0, entity.getHeight() + 0.5, 0);
            Vec3d delta = entityPos.subtract(camPos);
            
            // Simple world-to-screen (approximate)
            double x = entityPos.x - camPos.x;
            double y = entityPos.y - camPos.y;
            double z = entityPos.z - camPos.z;
            
            // Render text at entity position (world space)
            matrices.push();
            matrices.translate(entityPos.x - camPos.x, entityPos.y - camPos.y, entityPos.z - camPos.z);
            matrices.multiply(camera.getRotation());
            float scale = (float) Math.max(0.02, dist * 0.01);
            matrices.scale(-scale, -scale, scale);
            
            TextRenderer textRenderer = mc.textRenderer;
            int width = textRenderer.getWidth(displayText);
            
            // Background
            int bgColor = isValuable ? 0x80442200 : 0x80000000;
            mc.textRenderer.draw(displayText, -width / 2f, 0, 0xFFFFFF, false, 
                matrices.peek().getPositionMatrix(), 
                mc.getBufferBuilders().getEntityVertexConsumers(), 
                TextRenderer.TextLayerType.NORMAL, bgColor, 15728880);
            
            matrices.pop();
        }
    }
    
    public void setRange(double r) { range = r; }
    public double getRange() { return range; }
    public void setTracers(boolean t) { tracers = t; }
    public boolean hasTracers() { return tracers; }
    public void setBox(boolean b) { box = b; }
    public boolean hasBox() { return box; }
    public void setNametags(boolean n) { nametags = n; }
    public boolean hasNametags() { return nametags; }
}
