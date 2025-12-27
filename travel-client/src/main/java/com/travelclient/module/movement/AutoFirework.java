package com.travelclient.module.movement;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import com.travelclient.module.player.AutoEat;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class AutoFirework extends Module {
    private final ModuleManager manager;
    
    private double minSpeed = 0.8;  // Increased from 0.5
    private int cooldown = 0;
    private int minCooldown = 12;   // Reduced from 15
    
    public AutoFirework(ModuleManager manager) {
        super("AutoFirework", "Auto-uses fireworks (pauses for eating)", Category.MOVEMENT);
        this.manager = manager;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // Must be flying elytra
        if (!mc.player.isFallFlying()) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;
        
        // *** PRIORITY CHECK ***
        // If player is eating or needs to eat, DO NOT interrupt!
        if (AutoEat.IS_EATING) {
            cooldown = 20;
            return;
        }
        
        // If health is low and AutoEat needs to eat, let it
        if (AutoEat.NEEDS_TO_EAT && mc.player.getHealth() < 10) {
            cooldown = 10;
            return;
        }
        
        // If player is using any item (eating, drinking), don't interrupt
        if (mc.player.isUsingItem()) {
            cooldown = 10;
            return;
        }
        
        // Cooldown
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        
        // Check if need boost
        double speed = mc.player.getVelocity().horizontalLength();
        double vertSpeed = mc.player.getVelocity().y;
        
        // More aggressive in nether
        boolean isNether = mc.world.getRegistryKey() == World.NETHER;
        double speedThreshold = isNether ? minSpeed * 1.2 : minSpeed;
        
        boolean needsBoost = speed < speedThreshold || (speed < 1.2 && vertSpeed < -0.4);
        
        // Extra boost when climbing
        if (mc.player.getPitch() < -20 && speed < 1.5) {
            needsBoost = true;
        }
        
        if (!needsBoost) return;
        
        // Find firework
        int fireworkSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof FireworkRocketItem) {
                fireworkSlot = i;
                break;
            }
        }
        
        if (fireworkSlot == -1) return;
        
        // Use firework
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = fireworkSlot;
        
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        
        mc.player.getInventory().selectedSlot = prevSlot;
        
        // Shorter cooldown in nether for more responsive flight
        cooldown = isNether ? Math.max(8, minCooldown - 4) : minCooldown;
    }
    
    public void setMinSpeed(double speed) { minSpeed = Math.max(0.1, Math.min(2.0, speed)); }
    public double getMinSpeed() { return minSpeed; }
    public void setMinCooldown(int cd) { minCooldown = Math.max(5, cd); }
    public int getMinCooldown() { return minCooldown; }
}
