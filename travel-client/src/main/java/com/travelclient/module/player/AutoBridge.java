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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoBridge extends Module {
    
    private static final List<Block> BRIDGE_BLOCKS = Arrays.asList(
        Blocks.COBBLESTONE, Blocks.COBBLED_DEEPSLATE, Blocks.NETHERRACK,
        Blocks.BLACKSTONE, Blocks.BASALT, Blocks.STONE, Blocks.DIRT
    );
    
    private static final Set<Block> INVALID = new HashSet<>(Arrays.asList(
        Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER, Blocks.LAVA
    ));
    
    private int cooldown = 0;
    private int savedSlot = -1;
    private int stage = 0;
    private BlockPos targetPos = null;
    private int blockSlot = -1;
    
    public AutoBridge() {
        super("AutoBridge", "Bridges gaps while walking", Category.PLAYER);
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
                
                targetPos = findBridgePos();
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
        // Preferred blocks first
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi) {
                if (BRIDGE_BLOCKS.contains(bi.getBlock())) return i;
            }
        }
        // Any solid block
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
    
    private BlockPos findBridgePos() {
        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        for (int dist = 1; dist <= 4; dist++) {
            double checkX = pos.x + moveX * dist;
            double checkZ = pos.z + moveZ * dist;
            
            BlockPos groundPos = new BlockPos(
                (int) Math.floor(checkX),
                (int) Math.floor(pos.y) - 1,
                (int) Math.floor(checkZ)
            );
            
            if (needsBlock(groundPos) && canPlace(groundPos)) {
                return groundPos;
            }
        }
        
        // Check below if falling
        if (mc.player.getVelocity().y < -0.1 && !mc.player.isOnGround()) {
            BlockPos below = mc.player.getBlockPos().down();
            if (needsBlock(below) && canPlace(below)) {
                return below;
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
    
    public boolean hasBridgeBlocks() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (BRIDGE_BLOCKS.contains(block) || block.getDefaultState().isSolidBlock(mc.world, BlockPos.ORIGIN)) {
                    return true;
                }
            }
        }
        return false;
    }
}
