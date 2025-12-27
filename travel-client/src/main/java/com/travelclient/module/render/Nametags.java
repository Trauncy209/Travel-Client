package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Nametags extends Module {
    
    private boolean showHealth = true;
    private boolean showDistance = true;
    private boolean showArmor = true;
    private float scale = 1.5f;
    
    public Nametags() {
        super("Nametags", "Enhanced nametags showing health and distance through walls", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void render(DrawContext context, float tickDelta) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        Camera camera = mc.gameRenderer.getCamera();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            // Get interpolated position
            double x = player.prevX + (player.getX() - player.prevX) * tickDelta;
            double y = player.prevY + (player.getY() - player.prevY) * tickDelta;
            double z = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;
            
            // Check if on screen (basic frustum check)
            Vec3d playerPos = new Vec3d(x, y + player.getHeight() + 0.5, z);
            Vec3d camPos = camera.getPos();
            
            double distance = camPos.distanceTo(playerPos);
            if (distance > 200) continue; // Don't render too far
            
            // Build nametag text
            StringBuilder text = new StringBuilder();
            text.append(player.getName().getString());
            
            if (showHealth) {
                float health = player.getHealth();
                String healthColor = health > 15 ? "§a" : health > 10 ? "§e" : health > 5 ? "§6" : "§c";
                text.append(" ").append(healthColor).append(String.format("%.0f", health)).append("§r❤");
            }
            
            if (showDistance) {
                text.append(" §7[").append(String.format("%.0f", distance)).append("m]");
            }
            
            // This is simplified - full implementation would use world-to-screen projection
            // For now, we'll use the HUD mixin to render this info
        }
    }
    
    // Called from HUD to render nametag info in corner
    public String getPlayerInfo(PlayerEntity player) {
        if (!isEnabled()) return null;
        
        double distance = mc.player.distanceTo(player);
        float health = player.getHealth();
        
        String healthColor = health > 15 ? "§a" : health > 10 ? "§e" : health > 5 ? "§6" : "§c";
        
        StringBuilder info = new StringBuilder();
        info.append(player.getName().getString());
        
        if (showHealth) {
            info.append(" ").append(healthColor).append(String.format("%.0f", health)).append("❤§r");
        }
        
        if (showDistance) {
            info.append(" §7").append(String.format("%.0fm", distance));
        }
        
        return info.toString();
    }
    
    public boolean isShowHealth() { return showHealth; }
    public void setShowHealth(boolean show) { this.showHealth = show; }
    public boolean isShowDistance() { return showDistance; }
    public void setShowDistance(boolean show) { this.showDistance = show; }
}
