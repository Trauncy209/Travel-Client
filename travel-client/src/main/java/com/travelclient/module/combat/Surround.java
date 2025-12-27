package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Surround extends Module {
    
    // Positions around feet
    private static final BlockPos[] SURROUND = {
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1)
    };
    
    private static final BlockPos[] FLOOR = {
        new BlockPos(0, -1, 0)
    };
    
    private int savedSlot = -1;
    private int stage = 0; // 0=idle, 1=switched, 2=placed, 3=waiting
    private BlockPos targetPos = null;
    private int blockSlot = -1;
    private int placeIndex = 0;
    private boolean centered = false;
    
    public Surround() {
        super("Surround", "Places obsidian around feet", Category.COMBAT);
    }
    
    @Override
    public void onEnable() {
        centered = false;
        placeIndex = 0;
        stage = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // Center player once at start
        if (!centered && mc.player.isOnGround()) {
            BlockPos pos = mc.player.getBlockPos();
            double centerX = pos.getX() + 0.5;
            double centerZ = pos.getZ() + 0.5;
            mc.player.setPosition(centerX, mc.player.getY(), centerZ);
            centered = true;
        }
        
        // State machine
        switch (stage) {
            case 0: // Find next block to place
                blockSlot = findObsidian();
                if (blockSlot == -1) return;
                
                BlockPos feetPos = mc.player.getBlockPos();
                targetPos = null;
                
                // Check floor first
                for (BlockPos offset : FLOOR) {
                    BlockPos check = feetPos.add(offset);
                    if (mc.world.getBlockState(check).isAir() && canPlace(check)) {
                        targetPos = check;
                        break;
                    }
                }
                
                // Then surround
                if (targetPos == null) {
                    for (BlockPos offset : SURROUND) {
                        BlockPos check = feetPos.add(offset);
                        if (mc.world.getBlockState(check).isAir() && canPlace(check)) {
                            targetPos = check;
                            break;
                        }
                    }
                }
                
                if (targetPos == null) return; // All done
                
                // Save and switch
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
                stage = 0;
                break;
        }
    }
    
    private boolean canPlace(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            if (!mc.world.getBlockState(adj).isAir()) {
                return true;
            }
        }
        return false;
    }
    
    private void doPlace(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            if (!mc.world.getBlockState(adj).isAir()) {
                Direction placeDir = dir.getOpposite();
                Vec3d hitVec = Vec3d.ofCenter(adj).add(Vec3d.of(placeDir.getVector()).multiply(0.5));
                BlockHitResult hit = new BlockHitResult(hitVec, placeDir, adj, false);
                
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                break;
            }
        }
    }
    
    private int findObsidian() {
        // Obsidian first
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.OBSIDIAN) return i;
            }
        }
        // Crying obsidian
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.CRYING_OBSIDIAN) return i;
            }
        }
        // Ender chest
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.ENDER_CHEST) return i;
            }
        }
        return -1;
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
