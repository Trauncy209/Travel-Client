package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoPot extends Module {
    
    private double healthThreshold = 10.0;
    private int cooldown = 0;
    
    public AutoPot() {
        super("AutoPot", "Auto-uses health potions", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        
        if (mc.player.getHealth() > healthThreshold) return;
        
        // Find health potion in hotbar
        int potSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isHealthPotion(stack)) {
                potSlot = i;
                break;
            }
        }
        
        if (potSlot == -1) return;
        
        // Use potion
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = potSlot;
        
        // Look down and throw
        float prevPitch = mc.player.getPitch();
        mc.player.setPitch(90);
        
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        
        mc.player.setPitch(prevPitch);
        mc.player.getInventory().selectedSlot = prevSlot;
        
        cooldown = 20; // 1 second cooldown
    }
    
    private boolean isHealthPotion(ItemStack stack) {
        if (stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) {
            return false;
        }
        
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        
        for (var effect : contents.getEffects()) {
            if (effect.getEffectType().value() == StatusEffects.INSTANT_HEALTH.value() ||
                effect.getEffectType().value() == StatusEffects.REGENERATION.value()) {
                return true;
            }
        }
        return false;
    }
    
    public void setHealthThreshold(double h) { healthThreshold = h; }
    public double getHealthThreshold() { return healthThreshold; }
}
