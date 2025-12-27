package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

public class LowHealthWarning extends Module {
    
    private float healthThreshold = 10.0f; // 5 hearts
    private boolean flashScreen = true;
    private boolean playSound = true;
    private int flashTimer = 0;
    private int soundCooldown = 0;
    private boolean wasLow = false;
    
    public LowHealthWarning() {
        super("HealthWarn", "Screen flash and sound when health is low", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        float health = mc.player.getHealth();
        boolean isLow = health <= healthThreshold && health > 0;
        
        if (isLow) {
            flashTimer = 10; // Flash for half second
            
            // Play sound only when first dropping below threshold
            if (!wasLow && playSound && soundCooldown == 0) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 0.5f);
                soundCooldown = 40; // 2 second cooldown
            }
        }
        
        wasLow = isLow;
        
        if (soundCooldown > 0) soundCooldown--;
        if (flashTimer > 0) flashTimer--;
    }
    
    public void renderWarning(DrawContext context) {
        if (!isEnabled() || flashTimer <= 0) return;
        
        if (flashScreen) {
            // Red overlay
            int alpha = (int) ((flashTimer / 10.0f) * 80); // Fade out
            int color = (alpha << 24) | 0xFF0000; // ARGB red
            context.fill(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), color);
        }
    }
    
    public boolean shouldRenderWarning() {
        return isEnabled() && flashTimer > 0 && flashScreen;
    }
    
    public float getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(float threshold) { this.healthThreshold = threshold; }
    public boolean isFlashScreen() { return flashScreen; }
    public void setFlashScreen(boolean flash) { this.flashScreen = flash; }
    public boolean isPlaySound() { return playSound; }
    public void setPlaySound(boolean sound) { this.playSound = sound; }
}
