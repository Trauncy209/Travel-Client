package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AirPlace extends Module {
    
    private int cooldown = 0;
    private int savedSlot = -1;
    private int stage = 0; // 0=idle, 1=switched, 2=placed, 3=waiting
    private BlockPos targetPos = null;
    private int blockSlot = -1;
    
    public AirPlace() {
        super("AirPlace", "Place blocks in mid-air while holding right click", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // State machine for proper timing
        switch (stage) {
            case 0: // Idle - check if should place
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }
                
                // Only place if right click held
                if (!mc.options.useKey.isPressed()) return;
                
                // Find block slot
                blockSlot = findBlockSlot();
                if (blockSlot == -1) return;
                
                // Find target position
                targetPos = findAirTarget();
                if (targetPos == null) return;
                
                // Save and switch
                savedSlot = mc.player.getInventory().selectedSlot;
                if (savedSlot != blockSlot) {
                    mc.player.getInventory().selectedSlot = blockSlot;
                }
                stage = 1;
                break;
                
            case 1: // Switched - place next tick
                if (targetPos != null && mc.world.getBlockState(targetPos).isAir()) {
                    doPlace(targetPos);
                }
                stage = 2;
                break;
                
            case 2: // Wait a tick
                stage = 3;
                break;
                
            case 3: // Switch back
                if (savedSlot != -1 && savedSlot != mc.player.getInventory().selectedSlot) {
                    mc.player.getInventory().selectedSlot = savedSlot;
                }
                savedSlot = -1;
                targetPos = null;
                cooldown = 3;
                stage = 0;
                break;
        }
    }
    
    private BlockPos findAirTarget() {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        
        // Look for air block in line of sight
        for (double d = 2; d <= 5; d += 0.5) {
            Vec3d checkVec = eyePos.add(lookVec.multiply(d));
            BlockPos checkPos = new BlockPos(
                (int) Math.floor(checkVec.x),
                (int) Math.floor(checkVec.y),
                (int) Math.floor(checkVec.z)
            );
            
            if (mc.world.getBlockState(checkPos).isAir()) {
                return checkPos;
            }
        }
        
        return null;
    }
    
    private int findBlockSlot() {
        // Check if already holding block
        ItemStack held = mc.player.getMainHandStack();
        if (held.getItem() instanceof BlockItem) {
            return mc.player.getInventory().selectedSlot;
        }
        
        // Find solid block in hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().getDefaultState().isSolidBlock(mc.world, BlockPos.ORIGIN)) {
                    return i;
                }
            }
        }
        
        // Any block
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        
        return -1;
    }
    
    private void doPlace(BlockPos pos) {
        // Try to find adjacent block to place against
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            if (!mc.world.getBlockState(adj).isAir()) {
                Direction placeDir = dir.getOpposite();
                Vec3d hitVec = Vec3d.ofCenter(adj).add(Vec3d.of(placeDir.getVector()).multiply(0.5));
                BlockHitResult hit = new BlockHitResult(hitVec, placeDir, adj, false);
                
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
        
        // No adjacent block - try placing against the block below target
        BlockPos below = pos.down();
        Vec3d hitVec = Vec3d.ofCenter(below).add(0, 0.5, 0);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, below, false);
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    
    @Override
    public void onDisable() {
        if (savedSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = savedSlot;
        }
        savedSlot = -1;
        stage = 0;
        targetPos = null;
    }
}
