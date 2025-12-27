package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    
    private double flySpeed = 1.0;
    private String mode = "Vanilla"; // Vanilla, Creative
    
    public Fly() {
        super("Fly", "Fly around", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Stop falling
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, 0, velocity.z);
        
        // Movement
        double speed = flySpeed * 0.5;
        
        if (mc.player.input.jumping) {
            mc.player.setVelocity(mc.player.getVelocity().add(0, speed, 0));
        }
        if (mc.player.input.sneaking) {
            mc.player.setVelocity(mc.player.getVelocity().add(0, -speed, 0));
        }
        
        // Horizontal movement
        float yaw = mc.player.getYaw();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        
        if (forward != 0 || strafe != 0) {
            double rad = Math.toRadians(yaw);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);
            
            double motionX = (forward * -sin + strafe * cos) * speed;
            double motionZ = (forward * cos + strafe * sin) * speed;
            
            mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
        }
    }
    
    public void setFlySpeed(double speed) { flySpeed = speed; }
    public double getFlySpeed() { return flySpeed; }
}
