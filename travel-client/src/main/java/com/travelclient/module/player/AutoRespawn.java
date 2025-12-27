package com.travelclient.module.player;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    
    private int delay = 20; // 1 second delay
    private int timer = 0;
    private boolean wasOnDeathScreen = false;
    
    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawns after death", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        boolean onDeathScreen = mc.currentScreen instanceof DeathScreen;
        
        if (onDeathScreen) {
            if (!wasOnDeathScreen) {
                // Just died - log coords
                TravelClient.LOGGER.info("DEATH at coords: {}, {}, {}", 
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
                timer = delay;
                wasOnDeathScreen = true;
            }
            
            if (timer > 0) {
                timer--;
            } else {
                // Respawn
                mc.player.requestRespawn();
                mc.setScreen(null);
                wasOnDeathScreen = false;
            }
        } else {
            wasOnDeathScreen = false;
        }
    }
    
    public int getDelay() { return delay; }
    public void setDelay(int ticks) { this.delay = ticks; }
}
