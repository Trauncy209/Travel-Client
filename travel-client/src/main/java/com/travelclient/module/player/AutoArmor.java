package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

public class AutoArmor extends Module {
    
    private int cooldown = 0;
    
    // Armor tier values (higher = better)
    private static final Map<Item, Integer> ARMOR_VALUES = new HashMap<>();
    
    static {
        // Helmets
        ARMOR_VALUES.put(Items.NETHERITE_HELMET, 100);
        ARMOR_VALUES.put(Items.DIAMOND_HELMET, 80);
        ARMOR_VALUES.put(Items.IRON_HELMET, 60);
        ARMOR_VALUES.put(Items.GOLDEN_HELMET, 40);
        ARMOR_VALUES.put(Items.CHAINMAIL_HELMET, 50);
        ARMOR_VALUES.put(Items.LEATHER_HELMET, 20);
        ARMOR_VALUES.put(Items.TURTLE_HELMET, 70);
        
        // Chestplates (skip elytra handling - let ElytraReplace deal with that)
        ARMOR_VALUES.put(Items.NETHERITE_CHESTPLATE, 100);
        ARMOR_VALUES.put(Items.DIAMOND_CHESTPLATE, 80);
        ARMOR_VALUES.put(Items.IRON_CHESTPLATE, 60);
        ARMOR_VALUES.put(Items.GOLDEN_CHESTPLATE, 40);
        ARMOR_VALUES.put(Items.CHAINMAIL_CHESTPLATE, 50);
        ARMOR_VALUES.put(Items.LEATHER_CHESTPLATE, 20);
        
        // Leggings
        ARMOR_VALUES.put(Items.NETHERITE_LEGGINGS, 100);
        ARMOR_VALUES.put(Items.DIAMOND_LEGGINGS, 80);
        ARMOR_VALUES.put(Items.IRON_LEGGINGS, 60);
        ARMOR_VALUES.put(Items.GOLDEN_LEGGINGS, 40);
        ARMOR_VALUES.put(Items.CHAINMAIL_LEGGINGS, 50);
        ARMOR_VALUES.put(Items.LEATHER_LEGGINGS, 20);
        
        // Boots
        ARMOR_VALUES.put(Items.NETHERITE_BOOTS, 100);
        ARMOR_VALUES.put(Items.DIAMOND_BOOTS, 80);
        ARMOR_VALUES.put(Items.IRON_BOOTS, 60);
        ARMOR_VALUES.put(Items.GOLDEN_BOOTS, 40);
        ARMOR_VALUES.put(Items.CHAINMAIL_BOOTS, 50);
        ARMOR_VALUES.put(Items.LEATHER_BOOTS, 20);
    }
    
    public AutoArmor() {
        super("AutoArmor", "Automatically equips best armor from inventory", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        
        // Check each armor slot
        checkAndEquip(EquipmentSlot.HEAD, 5);    // Helmet slot
        checkAndEquip(EquipmentSlot.LEGS, 7);    // Leggings slot
        checkAndEquip(EquipmentSlot.FEET, 8);    // Boots slot
        // Skip chest - let elytra handling do that
    }
    
    private void checkAndEquip(EquipmentSlot slot, int screenSlot) {
        ItemStack current = mc.player.getEquippedStack(slot);
        int currentValue = getArmorValue(current);
        
        // Find better armor in inventory
        int bestSlot = -1;
        int bestValue = currentValue;
        
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            
            if (isArmorForSlot(stack.getItem(), slot)) {
                int value = getArmorValue(stack);
                if (value > bestValue) {
                    bestValue = value;
                    bestSlot = i;
                }
            }
        }
        
        if (bestSlot != -1) {
            // Swap armor
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                bestSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            // Put old armor back
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    bestSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
            
            cooldown = 5;
        }
    }
    
    private int getArmorValue(ItemStack stack) {
        return ARMOR_VALUES.getOrDefault(stack.getItem(), 0);
    }
    
    private boolean isArmorForSlot(Item item, EquipmentSlot slot) {
        if (!(item instanceof ArmorItem armor)) return false;
        return armor.getSlotType() == slot;
    }
}
