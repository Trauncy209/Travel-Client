package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class Freecam extends Module {
    
    private Vec3d savedPos;
    private float savedYaw, savedPitch;
    private float speed = 2.0f;
    
    // Fake position for freecam
    private double freecamX, freecamY, freecamZ;
    private float freecamYaw, freecamPitch;
    private boolean active = false;
    
    public Freecam() {
        super("Freecam", "Fly camera freely while player stays still", Category.MOVEMENT);
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        // Save original position
        savedPos = mc.player.getPos();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
        
        // Initialize freecam at player position
        freecamX = mc.player.getX();
        freecamY = mc.player.getY();
        freecamZ = mc.player.getZ();
        freecamYaw = savedYaw;
        freecamPitch = savedPitch;
        
        active = true;
        
        mc.player.sendMessage(Text.of("§a[Freecam] Enabled - WASD to move, Space/Shift for up/down"), false);
        mc.player.sendMessage(Text.of("§7Your real body stays at: " + (int)savedPos.x + ", " + (int)savedPos.y + ", " + (int)savedPos.z), false);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || !active) return;
        
        // Keep player at saved position (prevent server movement)
        // This makes it look like you're standing still to server
        mc.player.setVelocity(0, 0, 0);
        
        // Get camera rotation from player input
        freecamYaw = mc.player.getYaw();
        freecamPitch = mc.player.getPitch();
        
        // Calculate movement direction
        double yawRad = Math.toRadians(freecamYaw);
        
        // Forward/back direction
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        
        // Strafe direction  
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        
        // Movement input
        double mx = 0, my = 0, mz = 0;
        
        if (mc.options.forwardKey.isPressed()) {
            mx += fx * speed;
            mz += fz * speed;
        }
        if (mc.options.backKey.isPressed()) {
            mx -= fx * speed;
            mz -= fz * speed;
        }
        if (mc.options.leftKey.isPressed()) {
            mx -= rx * speed;
            mz -= rz * speed;
        }
        if (mc.options.rightKey.isPressed()) {
            mx += rx * speed;
            mz += rz * speed;
        }
        if (mc.options.jumpKey.isPressed()) {
            my += speed;
        }
        if (mc.options.sneakKey.isPressed()) {
            my -= speed;
        }
        
        // Apply sprint multiplier
        if (mc.options.sprintKey.isPressed()) {
            mx *= 2;
            my *= 2;
            mz *= 2;
        }
        
        // Update freecam position
        freecamX += mx * 0.1;
        freecamY += my * 0.1;
        freecamZ += mz * 0.1;
        
        // Move player to freecam position (client-side only appearance)
        mc.player.setPosition(freecamX, freecamY, freecamZ);
        mc.player.prevX = freecamX;
        mc.player.prevY = freecamY;
        mc.player.prevZ = freecamZ;
        
        // Disable gravity and collision
        mc.player.noClip = true;
        mc.player.setOnGround(false);
        mc.player.getAbilities().flying = true;
    }
    
    @Override
    public void onDisable() {
        if (mc.player == null) return;
        
        active = false;
        
        // Return to saved position
        if (savedPos != null) {
            mc.player.setPosition(savedPos.x, savedPos.y, savedPos.z);
            mc.player.prevX = savedPos.x;
            mc.player.prevY = savedPos.y;
            mc.player.prevZ = savedPos.z;
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
        }
        
        // Re-enable collision and gravity
        mc.player.noClip = false;
        mc.player.getAbilities().flying = false;
        mc.player.setVelocity(Vec3d.ZERO);
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§c[Freecam] Disabled - Returned to original position"), false);
        }
    }
    
    public float getSpeed() { return speed; }
    public void setSpeed(float s) { speed = Math.max(0.5f, Math.min(10.0f, s)); }
    
    public boolean isFreecamActive() { return active; }
    public Vec3d getFreecamPos() { return new Vec3d(freecamX, freecamY, freecamZ); }
    public Vec3d getSavedPos() { return savedPos; }
}
