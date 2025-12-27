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

public class GScaffold extends Module {
    
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
    private int stage = 0;
    private BlockPos targetPos = null;
    private int blockSlot = -1;
    
    public GScaffold() {
        super("GScaffold", "Staircase up/down - look up/down while walking", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        switch (stage) {
            case 0: // Idle
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }
                
                blockSlot = findBlock();
                if (blockSlot == -1) return;
                
                float pitch = mc.player.getPitch();
                
                if (pitch < -25) {
                    targetPos = findStairUpPos();
                } else if (pitch > 25) {
                    targetPos = findStairDownPos();
                } else {
                    return;
                }
                
                if (targetPos == null) return;
                
                savedSlot = mc.player.getInventory().selectedSlot;
                if (savedSlot != blockSlot) {
                    mc.player.getInventory().selectedSlot = blockSlot;
                }
                stage = 1;
                break;
                
            case 1: // Place
                if (targetPos != null && mc.world.getBlockState(targetPos).isAir()) {
                    doPlace(targetPos);
                }
                stage = 2;
                break;
                
            case 2: // Wait
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
    
    private BlockPos findStairUpPos() {
        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        for (int dist = 1; dist <= 3; dist++) {
            double checkX = pos.x + moveX * dist;
            double checkZ = pos.z + moveZ * dist;
            
            BlockPos stepPos = new BlockPos(
                (int) Math.floor(checkX),
                (int) Math.floor(pos.y),
                (int) Math.floor(checkZ)
            );
            
            if (needsBlock(stepPos) && canPlace(stepPos)) {
                return stepPos;
            }
            
            BlockPos higherPos = stepPos.up();
            if (needsBlock(higherPos) && canPlace(higherPos)) {
                return higherPos;
            }
        }
        
        return null;
    }
    
    private BlockPos findStairDownPos() {
        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        for (int dist = 1; dist <= 3; dist++) {
            double checkX = pos.x + moveX * dist;
            double checkZ = pos.z + moveZ * dist;
            
            BlockPos stepDown = new BlockPos(
                (int) Math.floor(checkX),
                (int) Math.floor(pos.y) - 1,
                (int) Math.floor(checkZ)
            );
            
            if (needsBlock(stepDown) && canPlace(stepDown)) {
                return stepDown;
            }
            
            BlockPos lowerPos = stepDown.down();
            if (needsBlock(lowerPos) && canPlace(lowerPos)) {
                return lowerPos;
            }
        }
        
        return null;
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
