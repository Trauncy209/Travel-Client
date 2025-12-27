package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.util.Hand;

public class AntiAFK extends Module {
    
    private int timer = 0;
    private int interval = 600; // Every 30 seconds (600 ticks) - more frequent
    private boolean swing = true;
    private boolean rotate = true;
    private boolean jump = true;
    private boolean sneak = true;
    private int actionIndex = 0;
    
    public AntiAFK() {
        super("AntiAFK", "Prevents AFK kicks with various actions", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        timer++;
        
        if (timer >= interval) {
            timer = 0;
            
            // Cycle through different actions to look more natural
            switch (actionIndex % 4) {
                case 0:
                    if (swing) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                    break;
                case 1:
                    if (jump && mc.player.isOnGround()) {
                        mc.player.jump();
                    }
                    break;
                case 2:
                    if (rotate) {
                        // Small random rotation
                        float randomYaw = (float) (Math.random() * 10 - 5);
                        mc.player.setYaw(mc.player.getYaw() + randomYaw);
                    }
                    break;
                case 3:
                    if (sneak) {
                        // Quick sneak toggle
                        mc.player.setSneaking(true);
                    }
                    break;
            }
            
            actionIndex++;
        }
        
        // Unsneak after a few ticks
        if (timer == 5 && sneak) {
            mc.player.setSneaking(false);
        }
    }
    
    @Override
    public void onEnable() {
        timer = 0;
        actionIndex = 0;
    }
    
    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.setSneaking(false);
        }
    }
    
    public int getInterval() { return interval; }
    public void setInterval(int ticks) { this.interval = Math.max(100, ticks); }
}
