package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import java.util.Arrays;
import java.util.List;

public class AutoEat extends Module {
    
    private static final List<Item> GAPS = Arrays.asList(
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.GOLDEN_APPLE
    );
    
    private static final List<Item> FOODS = Arrays.asList(
        Items.COOKED_BEEF,
        Items.COOKED_PORKCHOP,
        Items.COOKED_MUTTON,
        Items.COOKED_CHICKEN,
        Items.GOLDEN_CARROT,
        Items.COOKED_SALMON,
        Items.BREAD,
        Items.BAKED_POTATO
    );
    
    private float healthThreshold = 10.0f; // Eat gap below 5 hearts
    private float hungerThreshold = 14.0f;
    
    private boolean eating = false;
    private int savedSlot = -1;
    private int eatTicks = 0;
    
    // Static flag for other modules to check
    public static boolean IS_EATING = false;
    public static boolean NEEDS_TO_EAT = false;
    
    public AutoEat() {
        super("AutoEat", "Auto-eats food when low (priority over fireworks)", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        float health = mc.player.getHealth();
        int hunger = mc.player.getHungerManager().getFoodLevel();
        
        // Update static flags
        NEEDS_TO_EAT = (health < healthThreshold) || (hunger < hungerThreshold && !mc.player.isFallFlying());
        IS_EATING = eating;
        
        // If eating, maintain it
        if (eating) {
            eatTicks++;
            
            // Keep right click held
            mc.options.useKey.setPressed(true);
            
            // Check if done
            if (!mc.player.isUsingItem() && eatTicks > 5) {
                // Done eating
                mc.options.useKey.setPressed(false);
                
                if (savedSlot != -1) {
                    mc.player.getInventory().selectedSlot = savedSlot;
                    savedSlot = -1;
                }
                
                eating = false;
                IS_EATING = false;
                eatTicks = 0;
            }
            
            // Timeout after 3 seconds
            if (eatTicks > 60) {
                mc.options.useKey.setPressed(false);
                eating = false;
                IS_EATING = false;
                eatTicks = 0;
            }
            
            return;
        }
        
        // Check if need to eat gap (PRIORITY - even while flying)
        if (health < healthThreshold) {
            int gapSlot = findFood(GAPS);
            if (gapSlot != -1) {
                startEating(gapSlot);
                return;
            }
            // No gaps, try regular food
            int foodSlot = findFood(FOODS);
            if (foodSlot != -1) {
                startEating(foodSlot);
                return;
            }
        }
        
        // Check if hungry (only when not flying to avoid interruption)
        if (hunger < hungerThreshold && !mc.player.isFallFlying()) {
            int foodSlot = findFood(FOODS);
            if (foodSlot != -1) {
                startEating(foodSlot);
            }
        }
    }
    
    private int findFood(List<Item> foods) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (foods.contains(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }
    
    private void startEating(int slot) {
        // Save current slot
        if (savedSlot == -1) {
            savedSlot = mc.player.getInventory().selectedSlot;
        }
        
        // Switch to food
        mc.player.getInventory().selectedSlot = slot;
        
        // Start using
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.options.useKey.setPressed(true);
        
        eating = true;
        IS_EATING = true;
        eatTicks = 0;
    }
    
    @Override
    public void onDisable() {
        mc.options.useKey.setPressed(false);
        
        if (savedSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = savedSlot;
            savedSlot = -1;
        }
        
        eating = false;
        IS_EATING = false;
        NEEDS_TO_EAT = false;
        eatTicks = 0;
    }
    
    public boolean isEating() { return eating; }
    public float getHealthThreshold() { return healthThreshold; }
    public void setHealthThreshold(float t) { healthThreshold = Math.max(2, Math.min(18, t)); }
    public float getHungerThreshold() { return hungerThreshold; }
    public void setHungerThreshold(float t) { hungerThreshold = Math.max(6, Math.min(18, t)); }
}
