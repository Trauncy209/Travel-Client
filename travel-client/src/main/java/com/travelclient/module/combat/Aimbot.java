package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Aimbot extends Module {
    
    private double range = 4.5;
    private double aimSpeed = 0.5;
    private boolean players = true;
    private boolean mobs = true;
    private boolean visibleOnly = true;
    
    public Aimbot() {
        super("Aimbot", "Automatically aims at entities", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        Entity target = findTarget();
        if (target == null) return;
        
        // Calculate angles
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
        
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, dist));
        
        // Smooth aim
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = targetPitch - currentPitch;
        
        mc.player.setYaw(currentYaw + (float)(yawDiff * aimSpeed));
        mc.player.setPitch(currentPitch + (float)(pitchDiff * aimSpeed));
    }
    
    private Entity findTarget() {
        Entity closest = null;
        double closestDist = range + 1;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (!((LivingEntity) entity).isAlive()) continue;
            
            boolean valid = false;
            if (players && entity instanceof PlayerEntity) valid = true;
            if (mobs && entity instanceof HostileEntity) valid = true;
            
            if (!valid) continue;
            
            double dist = mc.player.distanceTo(entity);
            if (dist > range) continue;
            
            if (visibleOnly && !mc.player.canSee(entity)) continue;
            
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        
        return closest;
    }
    
    public void setRange(double r) { range = r; }
    public double getRange() { return range; }
    public void setAimSpeed(double s) { aimSpeed = s; }
    public double getAimSpeed() { return aimSpeed; }
    public void setPlayers(boolean p) { players = p; }
    public boolean getPlayers() { return players; }
    public void setMobs(boolean m) { mobs = m; }
    public boolean getMobs() { return mobs; }
}
