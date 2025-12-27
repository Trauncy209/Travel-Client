package com.travelclient.module.movement;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import com.travelclient.util.PathScanner;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class GroundNavigate extends Module {
    private final ModuleManager manager;
    
    // Settings
    private float turnSpeed = 5.0f;
    private float arrivalDistance = 3.0f;
    private boolean sprint = true;
    private boolean jump = true;
    private int scanDistance = 150;
    private int scanCooldown = 0;
    private float targetYaw = 0;
    private String pathStatus = "idle";
    
    public GroundNavigate(ModuleManager manager) {
        super("GroundNav", "Smart pathfinding - scans 150 blocks ahead for safest route", Category.MOVEMENT);
        this.manager = manager;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Need a target
        if (!manager.hasTarget()) {
            return;
        }
        
        // Don't interfere with elytra flight
        if (mc.player.isFallFlying()) return;
        
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        
        // Check if arrived
        double distance = manager.getDistanceToTarget(playerX, playerZ);
        if (distance < arrivalDistance) {
            mc.player.sendMessage(Text.literal("§a[TravelClient] Arrived at destination!"), true);
            manager.clearTarget();
            stopMovement();
            return;
        }
        
        // Scan for safe path periodically (not every tick for performance)
        if (scanCooldown <= 0) {
            Vec3d currentPos = mc.player.getPos();
            PathScanner.PathResult path = PathScanner.findSafeGroundPath(
                currentPos, 
                manager.getTargetX(), 
                manager.getTargetZ(), 
                scanDistance
            );
            
            targetYaw = path.yaw;
            pathStatus = path.isSafe ? "§a" + path.reason : "§c" + path.reason;
            
            // If path is dangerous, maybe slow down or stop
            if (!path.isSafe) {
                // Still try to navigate, but be more careful
                scanCooldown = 5; // Scan more frequently when unsafe
            } else {
                scanCooldown = 10; // Normal scan rate
            }
        }
        scanCooldown--;
        
        // Turn toward calculated safe path
        float currentYaw = mc.player.getYaw();
        float yawDiff = targetYaw - currentYaw;
        
        // Normalize yaw difference
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Smooth turn toward target
        if (Math.abs(yawDiff) > 2.0f) {
            float adjustment = Math.max(-turnSpeed, Math.min(turnSpeed, yawDiff * 0.2f));
            mc.player.setYaw(currentYaw + adjustment);
        }
        
        // Check immediate surroundings before moving
        boolean immediateHazard = checkImmediateHazard();
        
        if (immediateHazard) {
            // Stop and reassess
            mc.options.forwardKey.setPressed(false);
            scanCooldown = 0; // Force rescan
            return;
        }
        
        // Move forward
        mc.options.forwardKey.setPressed(true);
        
        // Sprint if enabled and safe
        if (sprint && !immediateHazard) {
            mc.options.sprintKey.setPressed(true);
        } else {
            mc.options.sprintKey.setPressed(false);
        }
        
        // Smart jumping - only when actually blocked
        if (jump && mc.player.horizontalCollision && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }
    
    private boolean checkImmediateHazard() {
        if (mc.player == null || mc.world == null) return false;
        
        // Check blocks directly in front (2 blocks ahead)
        double yawRad = Math.toRadians(mc.player.getYaw());
        double frontX = mc.player.getX() - Math.sin(yawRad) * 2;
        double frontZ = mc.player.getZ() + Math.cos(yawRad) * 2;
        
        Vec3d frontPos = new Vec3d(frontX, mc.player.getY(), frontZ);
        
        // Quick hazard check
        PathScanner.PathResult immediateCheck = PathScanner.findSafeGroundPath(
            mc.player.getPos(),
            frontX,
            frontZ,
            5
        );
        
        return !immediateCheck.isSafe && immediateCheck.reason.contains("hazard");
    }
    
    private void stopMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }
    
    @Override
    public void onDisable() {
        stopMovement();
        pathStatus = "idle";
    }
    
    @Override
    public void onEnable() {
        if (!manager.hasTarget()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§e[TravelClient] No target set! Use GUI to set coordinates."), false);
            }
        } else {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§a[TravelClient] Smart pathfinding enabled - scanning " + scanDistance + " blocks ahead"), false);
            }
        }
        scanCooldown = 0;
    }
    
    public String getPathStatus() { return pathStatus; }
    public boolean isSprinting() { return sprint; }
    public void setSprint(boolean sprint) { this.sprint = sprint; }
    public boolean isJumping() { return jump; }
    public void setJump(boolean jump) { this.jump = jump; }
    public int getScanDistance() { return scanDistance; }
    public void setScanDistance(int distance) { this.scanDistance = distance; }
}
