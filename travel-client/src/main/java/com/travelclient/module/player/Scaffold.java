package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public class Scaffold extends Module {
    
    private static final Set<Block> INVALID = new HashSet<>();
    static {
        INVALID.add(Blocks.AIR);
        INVALID.add(Blocks.CAVE_AIR);
        INVALID.add(Blocks.VOID_AIR);
        INVALID.add(Blocks.WATER);
        INVALID.add(Blocks.LAVA);
    }
    
    private int cooldown = 0;
    private int savedSlot = -1;
    private int stage = 0; // 0=idle, 1=switched, 2=placed, 3=waiting
    private BlockPos targetPos = null;
    private int blockSlot = -1;
    
    public Scaffold() {
        super("Scaffold", "Auto-places blocks under you while walking", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // State machine for proper timing
        switch (stage) {
            case 0: // Idle - look for blocks to place
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }
                
                blockSlot = findBlock();
                if (blockSlot == -1) return;
                
                // Find position that needs a block
                targetPos = findPlacePos();
                if (targetPos == null) return;
                
                // Save current slot and switch
                savedSlot = mc.player.getInventory().selectedSlot;
                if (savedSlot != blockSlot) {
                    mc.player.getInventory().selectedSlot = blockSlot;
                }
                stage = 1;
                break;
                
            case 1: // Switched - wait one tick then place
                if (targetPos != null && !mc.world.getBlockState(targetPos).isAir()) {
                    // Already filled, abort
                    stage = 3;
                    return;
                }
                
                // Place the block
                if (targetPos != null) {
                    doPlace(targetPos);
                }
                stage = 2;
                break;
                
            case 2: // Placed - wait one tick
                stage = 3;
                break;
                
            case 3: // Switch back
                if (savedSlot != -1 && savedSlot != mc.player.getInventory().selectedSlot) {
                    mc.player.getInventory().selectedSlot = savedSlot;
                }
                savedSlot = -1;
                targetPos = null;
                cooldown = 1;
                stage = 0;
                break;
        }
    }
    
    private BlockPos findPlacePos() {
        // Check directly below
        BlockPos below = mc.player.getBlockPos().down();
        if (needsBlock(below) && canPlace(below)) {
            return below;
        }
        
        // Check ahead in movement direction
        Vec3d vel = mc.player.getVelocity();
        if (vel.horizontalLength() > 0.03) {
            float yaw = mc.player.getYaw();
            double yawRad = Math.toRadians(yaw);
            double moveX = -Math.sin(yawRad);
            double moveZ = Math.cos(yawRad);
            
            for (int dist = 1; dist <= 2; dist++) {
                double checkX = mc.player.getX() + moveX * dist;
                double checkZ = mc.player.getZ() + moveZ * dist;
                
                BlockPos ahead = new BlockPos(
                    (int) Math.floor(checkX),
                    (int) Math.floor(mc.player.getY()) - 1,
                    (int) Math.floor(checkZ)
                );
                
                if (needsBlock(ahead) && canPlace(ahead)) {
                    return ahead;
                }
            }
        }
        
        return null;
    }
    
    private int findBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().getDefaultState().isSolidBlock(mc.world, BlockPos.ORIGIN)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private boolean needsBlock(BlockPos pos) {
        return INVALID.contains(mc.world.getBlockState(pos).getBlock());
    }
    
    private boolean canPlace(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            if (!INVALID.contains(mc.world.getBlockState(adj).getBlock())) {
                return true;
            }
        }
        return false;
    }
    
    private void doPlace(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            if (!INVALID.contains(mc.world.getBlockState(adj).getBlock())) {
                Direction placeDir = dir.getOpposite();
                Vec3d hitVec = Vec3d.ofCenter(adj).add(Vec3d.of(placeDir.getVector()).multiply(0.5));
                BlockHitResult hit = new BlockHitResult(hitVec, placeDir, adj, false);
                
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                break;
            }
        }
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
