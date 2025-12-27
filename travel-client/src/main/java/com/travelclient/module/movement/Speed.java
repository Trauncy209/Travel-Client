package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Speed extends Module {
    
    private double speedMultiplier = 1.5;
    private String mode = "Strafe"; // Strafe, Vanilla, Jump
    
    public Speed() {
        super("Speed", "Move faster", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Only when moving
        if (!mc.player.input.pressingForward && !mc.player.input.pressingBack &&
            !mc.player.input.pressingLeft && !mc.player.input.pressingRight) return;
        
        if (mc.player.isOnGround()) {
            // Apply speed
            double motionX = mc.player.getVelocity().x * speedMultiplier;
            double motionZ = mc.player.getVelocity().z * speedMultiplier;
            mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
            
            // Bunny hop for extra speed
            if (mode.equals("Jump") || mode.equals("Strafe")) {
                mc.player.jump();
            }
        }
    }
    
    public void setSpeed(double speed) { speedMultiplier = speed; }
    public double getSpeed() { return speedMultiplier; }
    public void setMode(String m) { mode = m; }
    public String getMode() { return mode; }
}
