package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    
    private double range = 4.0;
    private boolean players = true;
    private boolean mobs = true;
    private boolean animals = false;
    private int aps = 12; // Attacks per second
    private int tickCounter = 0;
    
    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        tickCounter++;
        int ticksPerAttack = 20 / aps;
        if (tickCounter < ticksPerAttack) return;
        tickCounter = 0;
        
        // Find closest valid target
        Entity target = null;
        double closestDist = range + 1;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (!((LivingEntity)entity).isAlive()) continue;
            
            double dist = mc.player.distanceTo(entity);
            if (dist > range) continue;
            
            // Check entity type
            boolean valid = false;
            if (players && entity instanceof PlayerEntity) valid = true;
            if (mobs && entity instanceof HostileEntity) valid = true;
            if (animals && entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && !(entity instanceof HostileEntity)) valid = true;
            
            if (valid && dist < closestDist) {
                closestDist = dist;
                target = entity;
            }
        }
        
        if (target != null) {
            // Look at target
            double dx = target.getX() - mc.player.getX();
            double dy = (target.getY() + target.getHeight()/2) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dz = target.getZ() - mc.player.getZ();
            double dist = Math.sqrt(dx*dx + dz*dz);
            
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(-dy, dist));
            
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
            
            // Attack
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
    
    public void setRange(double r) { range = Math.max(1, Math.min(6, r)); }
    public double getRange() { return range; }
    public void setPlayers(boolean b) { players = b; }
    public boolean getPlayers() { return players; }
    public void setMobs(boolean b) { mobs = b; }
    public boolean getMobs() { return mobs; }
    public void setAnimals(boolean b) { animals = b; }
    public boolean getAnimals() { return animals; }
    public void setAPS(int a) { aps = Math.max(1, Math.min(20, a)); }
    public int getAPS() { return aps; }
}
