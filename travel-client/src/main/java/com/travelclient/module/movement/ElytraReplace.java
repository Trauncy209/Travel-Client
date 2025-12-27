package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class ElytraReplace extends Module {
    
    private int durabilityThreshold = 10; // Replace when durability below this
    private int cooldown = 0;
    
    public ElytraReplace() {
        super("ElytraReplace", "Automatically replaces elytra when durability is low", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        
        // Check current elytra
        ItemStack chestSlot = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        
        if (chestSlot.getItem() != Items.ELYTRA) return;
        
        // Check durability
        int durability = chestSlot.getMaxDamage() - chestSlot.getDamage();
        
        if (durability <= durabilityThreshold) {
            // Find replacement elytra in inventory
            int newElytraSlot = findElytraSlot();
            
            if (newElytraSlot != -1) {
                // Swap elytra
                // Chest slot in player screen handler is slot 6
                
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    newElytraSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
                
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    6, // Chest armor slot
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
                
                // Put old elytra in inventory
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    newElytraSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
                
                mc.player.sendMessage(Text.literal("§a[TravelClient] Replaced low durability elytra!"), true);
                cooldown = 20; // 1 second cooldown
            } else {
                mc.player.sendMessage(Text.literal("§c[TravelClient] Elytra durability low but no replacement found!"), true);
                cooldown = 200; // 10 second cooldown for warning
            }
        }
    }
    
    private int findElytraSlot() {
        // Search inventory for elytra with good durability
        // Slots 9-44 in screen handler (main inventory + hotbar)
        
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            
            if (stack.getItem() == Items.ELYTRA) {
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > durabilityThreshold) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    public int getDurabilityThreshold() { return durabilityThreshold; }
    public void setDurabilityThreshold(int threshold) { this.durabilityThreshold = threshold; }
}
