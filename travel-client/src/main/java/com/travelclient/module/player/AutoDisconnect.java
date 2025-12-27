package com.travelclient.module.player;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

public class AutoDisconnect extends Module {
    
    private float healthThreshold = 4.0f;  // Disconnect at 2 hearts
    private boolean onTotemPop = true;     // Disconnect after using totem
    private boolean disconnected = false;
    private int totemPopCooldown = 0;
    private float lastHealth = 20.0f;
    
    public AutoDisconnect() {
        super("AutoDisconnect", "Disconnects when health is critical - essential for AFK safety!", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        float health = mc.player.getHealth();
        
        // Detect totem pop (health was very low, now back up)
        if (onTotemPop && lastHealth <= 1.0f && health > lastHealth && totemPopCooldown == 0) {
            TravelClient.LOGGER.warn("TOTEM POPPED! Disconnecting for safety...");
            disconnect("§c[TravelClient] Totem popped! Disconnected for safety.");
            totemPopCooldown = 40; // 2 second cooldown
        }
        
        lastHealth = health;
        
        if (totemPopCooldown > 0) {
            totemPopCooldown--;
        }
        
        // Check health threshold
        if (health <= healthThreshold && health > 0 && !disconnected) {
            TravelClient.LOGGER.warn("Health critical ({})! Disconnecting...", health);
            disconnect("§c[TravelClient] Health critical! Disconnected at " + String.format("%.1f", health) + " HP");
        }
    }
    
    private void disconnect(String reason) {
        if (mc.player == null || disconnected) return;
        
        disconnected = true;
        
        // Log coords before disconnect
        TravelClient.LOGGER.info("Disconnect coords: {}, {}, {}", 
            mc.player.getX(), mc.player.getY(), mc.player.getZ());
        
        // Save coords to config
        TravelClient.configManager.save();
        
        // Disconnect
        if (mc.world != null) {
            mc.world.disconnect();
        }
        
        mc.disconnect(new DisconnectedScreen(
            new TitleScreen(),
            Text.literal("Disconnected"),
            Text.literal(reason)
        ));
    }
    
    @Override
    public void onEnable() {
        disconnected = false;
        if (mc.player != null) {
            lastHealth = mc.player.getHealth();
        }
    }
    
    @Override
    public void onDisable() {
        disconnected = false;
    }
    
    public float getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(float threshold) { this.healthThreshold = threshold; }
    public boolean isOnTotemPop() { return onTotemPop; }
    public void setOnTotemPop(boolean onPop) { this.onTotemPop = onPop; }
}
