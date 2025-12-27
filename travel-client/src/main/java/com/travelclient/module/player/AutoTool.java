package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    
    private boolean switchBack = true;
    private int previousSlot = -1;
    private boolean wasMining = false;
    
    public AutoTool() {
        super("AutoTool", "Automatically switches to the best tool for the block you're mining", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        boolean mining = mc.options.attackKey.isPressed();
        
        if (mining && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
            BlockState state = mc.world.getBlockState(hit.getBlockPos());
            
            if (!state.isAir()) {
                int bestSlot = findBestTool(state);
                
                if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
                    if (!wasMining) {
                        previousSlot = mc.player.getInventory().selectedSlot;
                    }
                    mc.player.getInventory().selectedSlot = bestSlot;
                }
                wasMining = true;
            }
        } else {
            // Not mining anymore
            if (wasMining && switchBack && previousSlot != -1) {
                mc.player.getInventory().selectedSlot = previousSlot;
                previousSlot = -1;
            }
            wasMining = false;
        }
    }
    
    private int findBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            
            // Check if this tool is effective
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        
        return bestSlot;
    }
    
    @Override
    public void onDisable() {
        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
            previousSlot = -1;
        }
        wasMining = false;
    }
    
    public boolean isSwitchBack() { return switchBack; }
    public void setSwitchBack(boolean switchBack) { this.switchBack = switchBack; }
}
