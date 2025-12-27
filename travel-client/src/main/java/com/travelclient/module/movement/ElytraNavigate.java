package com.travelclient.module.movement;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ElytraNavigate extends Module {
    private final ModuleManager manager;
    
    public ElytraNavigate(ModuleManager manager) {
        super("ElytraNavigate", "Fly to target coordinates", Category.MOVEMENT);
        this.manager = manager;
    }
    
    @Override
    public void onEnable() {
        if (!manager.hasTarget()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.of("§c[Travel] No target set! Use #goto X Z"), false);
            }
            setEnabled(false);
            return;
        }
        
        // Enable ElytraFly - this handles takeoff automatically
        ElytraFly elytraFly = manager.getModule(ElytraFly.class);
        if (elytraFly != null) {
            if (!elytraFly.isEnabled()) {
                elytraFly.setEnabled(true);
            } else {
                // Already enabled, trigger takeoff
                elytraFly.autoStartFlight();
            }
        }
        
        // Enable AutoFirework
        AutoFirework autoFirework = manager.getModule(AutoFirework.class);
        if (autoFirework != null && !autoFirework.isEnabled()) {
            autoFirework.setEnabled(true);
        }
        
        if (mc.player != null) {
            double dist = manager.getDistanceToTarget(mc.player.getX(), mc.player.getZ());
            mc.player.sendMessage(Text.of("§a[Travel] Flying to " + 
                (int)manager.getTargetX() + ", " + (int)manager.getTargetZ() + 
                " §7(" + (int)dist + " blocks)"), false);
        }
    }
    
    @Override
    public void onDisable() {
        // Disable ElytraFly
        ElytraFly elytraFly = manager.getModule(ElytraFly.class);
        if (elytraFly != null && elytraFly.isEnabled()) {
            elytraFly.setEnabled(false);
        }
        
        // Disable AutoFirework
        AutoFirework autoFirework = manager.getModule(AutoFirework.class);
        if (autoFirework != null && autoFirework.isEnabled()) {
            autoFirework.setEnabled(false);
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (!manager.hasTarget()) {
            setEnabled(false);
            return;
        }
        
        // Check if arrived
        double dist = manager.getDistanceToTarget(mc.player.getX(), mc.player.getZ());
        if (dist < 10) {
            mc.player.sendMessage(Text.of("§a[Travel] Arrived at destination!"), false);
            setEnabled(false);
            manager.clearTarget();
        }
    }
    
    public double getDistanceToTarget() {
        if (mc.player == null || !manager.hasTarget()) return -1;
        return manager.getDistanceToTarget(mc.player.getX(), mc.player.getZ());
    }
}
