package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    
    private int cooldown = 0;
    
    public AutoTotem() {
        super("AutoTotem", "Automatically keeps a totem in your offhand", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        // Cooldown to prevent spam
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        
        // Check offhand
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            return; // Already have totem
        }
        
        // Find totem in inventory
        int totemSlot = findTotemSlot();
        if (totemSlot == -1) {
            return; // No totems available
        }
        
        // Move totem to offhand
        // Slot 45 is offhand in player inventory
        // We need to click the totem, then click offhand
        
        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            // Pick up totem
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                totemSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            // Place in offhand (slot 45)
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                45,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            // If we picked up something from offhand, put it back in inventory
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    totemSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
            
            cooldown = 5; // Small cooldown
        }
    }
    
    private int findTotemSlot() {
        // Check hotbar first (slots 36-44 in screen handler)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return 36 + i; // Convert to screen handler slot
            }
        }
        
        // Check main inventory (slots 9-35 in screen handler)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i; // Already correct slot number
            }
        }
        
        return -1;
    }
}
