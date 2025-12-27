package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AntiGhast extends Module {
    
    private int detectionRange = 100;
    private int warningCooldown = 0;
    private List<GhastEntity> nearbyGhasts = new ArrayList<>();
    private List<FireballEntity> incomingFireballs = new ArrayList<>();
    private boolean inDanger = false;
    private String dangerStatus = "clear";
    private Vec3d suggestedCover = null;
    
    public AntiGhast() {
        super("AntiGhast", "Detects ghasts and fireballs, suggests cover positions", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Scan for ghasts
        scanForGhasts();
        
        // Scan for incoming fireballs
        scanForFireballs();
        
        // Determine danger level
        assessDanger();
        
        // Warning cooldown
        if (warningCooldown > 0) warningCooldown--;
    }
    
    private void scanForGhasts() {
        nearbyGhasts.clear();
        
        Box searchBox = mc.player.getBoundingBox().expand(detectionRange);
        
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (entity instanceof GhastEntity ghast) {
                nearbyGhasts.add(ghast);
            }
        }
    }
    
    private void scanForFireballs() {
        incomingFireballs.clear();
        
        Box searchBox = mc.player.getBoundingBox().expand(50);
        
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (entity instanceof FireballEntity fireball) {
                // Check if it's heading toward us
                Vec3d fireballVel = fireball.getVelocity();
                Vec3d toPlayer = mc.player.getPos().subtract(fireball.getPos());
                
                // Dot product - positive means heading toward us
                double dot = fireballVel.normalize().dotProduct(toPlayer.normalize());
                
                if (dot > 0.5) { // Roughly heading our direction
                    incomingFireballs.add(fireball);
                }
            }
        }
    }
    
    private void assessDanger() {
        boolean wasInDanger = inDanger;
        inDanger = false;
        suggestedCover = null;
        
        // Check for incoming fireballs (most urgent)
        if (!incomingFireballs.isEmpty()) {
            inDanger = true;
            dangerStatus = "§c§lFIREBALL INCOMING!";
            
            // Find nearest fireball
            FireballEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;
            
            for (FireballEntity fireball : incomingFireballs) {
                double dist = mc.player.distanceTo(fireball);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = fireball;
                }
            }
            
            if (nearest != null && nearestDist < 20) {
                // Suggest perpendicular movement to dodge
                Vec3d fireballDir = nearest.getVelocity().normalize();
                // Perpendicular vector (rotate 90 degrees)
                suggestedCover = new Vec3d(-fireballDir.z, 0, fireballDir.x);
                
                if (warningCooldown == 0) {
                    mc.player.sendMessage(Text.literal("§c§l[!] FIREBALL! Move sideways!"), true);
                    warningCooldown = 40;
                }
            }
            return;
        }
        
        // Check for ghasts with line of sight
        for (GhastEntity ghast : nearbyGhasts) {
            if (hasLineOfSight(ghast)) {
                inDanger = true;
                double dist = mc.player.distanceTo(ghast);
                dangerStatus = String.format("§eGhast spotted! %.0fm", dist);
                
                // Suggest getting behind cover
                suggestedCover = findCoverDirection(ghast);
                
                if (warningCooldown == 0 && dist < 50) {
                    mc.player.sendMessage(Text.literal("§e[!] Ghast has line of sight! Find cover!"), true);
                    warningCooldown = 100;
                }
                return;
            }
        }
        
        // Check for ghasts nearby but no line of sight
        if (!nearbyGhasts.isEmpty()) {
            double nearestDist = Double.MAX_VALUE;
            for (GhastEntity ghast : nearbyGhasts) {
                double dist = mc.player.distanceTo(ghast);
                if (dist < nearestDist) nearestDist = dist;
            }
            dangerStatus = String.format("§a%d ghast(s) nearby, in cover", nearbyGhasts.size());
        } else {
            dangerStatus = "§aclear";
        }
    }
    
    private boolean hasLineOfSight(GhastEntity ghast) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d ghastPos = ghast.getEyePos();
        
        // Raycast between ghast and player
        Vec3d direction = playerPos.subtract(ghastPos).normalize();
        double distance = playerPos.distanceTo(ghastPos);
        
        // Check for blocks in the way
        for (double d = 0; d < distance; d += 1.0) {
            Vec3d checkPos = ghastPos.add(direction.multiply(d));
            
            if (!mc.world.getBlockState(
                new net.minecraft.util.math.BlockPos(
                    (int) checkPos.x, 
                    (int) checkPos.y, 
                    (int) checkPos.z
                )
            ).isAir()) {
                // Block in the way - no line of sight
                return false;
            }
        }
        
        return true;
    }
    
    private Vec3d findCoverDirection(GhastEntity ghast) {
        Vec3d ghastDir = ghast.getPos().subtract(mc.player.getPos()).normalize();
        
        // Opposite direction from ghast (retreat to cover)
        return ghastDir.multiply(-1);
    }
    
    /**
     * Called by navigation modules to check if we should seek cover
     */
    public boolean shouldSeekCover() {
        return inDanger;
    }
    
    /**
     * Get suggested direction to move for safety
     */
    public Vec3d getSuggestedCoverDirection() {
        return suggestedCover;
    }
    
    /**
     * Check if we're safe from ghasts (in cover or none nearby)
     */
    public boolean isSafe() {
        return !inDanger;
    }
    
    public List<GhastEntity> getNearbyGhasts() {
        return nearbyGhasts;
    }
    
    public List<FireballEntity> getIncomingFireballs() {
        return incomingFireballs;
    }
    
    public String getDangerStatus() {
        return dangerStatus;
    }
    
    public int getDetectionRange() { return detectionRange; }
    public void setDetectionRange(int range) { this.detectionRange = range; }
}
