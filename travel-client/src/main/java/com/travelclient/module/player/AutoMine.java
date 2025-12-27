package com.travelclient.module.player;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import com.travelclient.module.render.BlockESP;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoMine extends Module {
    
    private BlockPos targetOre = null;
    private List<BlockPos> currentCluster = new ArrayList<>();
    private int stuckTicks = 0;
    private Vec3d lastPos = null;
    private int mineAttempts = 0;
    private Set<BlockPos> blacklist = new HashSet<>();
    private int totalMined = 0;
    private Block lockedBlockType = null;
    
    // Lock target position to prevent spinning
    private BlockPos lockedTarget = null;
    private int targetLockTicks = 0;
    
    private static final double CLUSTER_RADIUS = 8.0;
    
    public AutoMine() {
        super("AutoMine", "Locks onto ore clusters and mines them completely", Category.PLAYER);
    }
    
    @Override
    public void onEnable() {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[AutoMine] §fStarted ore scavenging!"), false);
            mc.player.sendMessage(Text.of("§7Will lock onto clusters and mine them completely"), false);
        }
        targetOre = null;
        lockedTarget = null;
        currentCluster.clear();
        stuckTicks = 0;
        lastPos = null;
        blacklist.clear();
        totalMined = 0;
        lockedBlockType = null;
        targetLockTicks = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        
        // Get BlockESP
        BlockESP esp = TravelClient.moduleManager.getModule(BlockESP.class);
        if (esp == null || !esp.isEnabled() || !esp.hasTargets()) {
            releaseKeys();
            if (mc.player.age % 60 == 0) {
                mc.player.sendMessage(Text.of("§c[AutoMine] Enable BlockESP and search for ores first! (#mine diamond)"), false);
            }
            return;
        }
        
        List<BlockPos> allOres = esp.getFoundBlocks();
        if (allOres.isEmpty()) {
            releaseKeys();
            currentCluster.clear();
            targetOre = null;
            lockedTarget = null;
            mc.player.sendMessage(Text.of("§e[AutoMine] No ores nearby. Keep exploring! Mined: §a" + totalMined), true);
            return;
        }
        
        // Check if current target is still valid
        if (targetOre != null) {
            BlockState state = mc.world.getBlockState(targetOre);
            if (state.isAir()) {
                // Successfully mined this ore!
                totalMined++;
                targetOre = null;
                lockedTarget = null;
                mineAttempts = 0;
                targetLockTicks = 0;
            } else if (!esp.getTargetBlocks().contains(state.getBlock())) {
                targetOre = null;
                lockedTarget = null;
                mineAttempts = 0;
                targetLockTicks = 0;
            }
        }
        
        // Clean up cluster
        if (!currentCluster.isEmpty()) {
            currentCluster.removeIf(pos -> {
                BlockState state = mc.world.getBlockState(pos);
                return state.isAir() || !esp.getTargetBlocks().contains(state.getBlock()) || blacklist.contains(pos);
            });
        }
        
        // Find new cluster if needed
        if (currentCluster.isEmpty()) {
            findNewCluster(allOres, esp);
        }
        
        if (currentCluster.isEmpty()) {
            releaseKeys();
            mc.player.sendMessage(Text.of("§e[AutoMine] All visible ores mined! Total: §a" + totalMined), true);
            return;
        }
        
        // Get next target - ONLY if we don't have one locked
        if (targetOre == null || lockedTarget == null) {
            targetOre = getNextFromCluster();
            lockedTarget = targetOre;
            stuckTicks = 0;
            mineAttempts = 0;
            targetLockTicks = 0;
        }
        
        if (targetOre == null) {
            releaseKeys();
            return;
        }
        
        // Keep target locked for at least 40 ticks to prevent spinning
        targetLockTicks++;
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d orePos = Vec3d.ofCenter(targetOre);
        double distance = playerPos.distanceTo(orePos);
        
        // Stuck detection
        if (lastPos != null) {
            double moved = playerPos.distanceTo(lastPos);
            if (moved < 0.03) {
                stuckTicks++;
            } else {
                stuckTicks = Math.max(0, stuckTicks - 2);
            }
        }
        lastPos = playerPos;
        
        // Handle stuck
        if (stuckTicks > 30) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
            mineObstacles();
        }
        
        if (stuckTicks > 100) {
            blacklist.add(targetOre);
            mc.player.sendMessage(Text.of("§c[AutoMine] Can't reach ore, trying next..."), false);
            targetOre = null;
            lockedTarget = null;
            stuckTicks = 0;
            targetLockTicks = 0;
            return;
        }
        
        String oreName = mc.world.getBlockState(targetOre).getBlock().getName().getString();
        int clusterRemaining = currentCluster.size();
        
        // Mining range
        if (distance <= 4.5) {
            releaseKeys();
            
            int pickSlot = findBestPickaxeForBlock(targetOre);
            if (pickSlot != -1) {
                mc.player.getInventory().selectedSlot = pickSlot;
            }
            
            lookAt(targetOre);
            mineBlock(targetOre);
            mineAttempts++;
            
            if (mineAttempts > 200) {
                blacklist.add(targetOre);
                targetOre = null;
                lockedTarget = null;
                mineAttempts = 0;
            }
            
            mc.player.sendMessage(Text.of("§a⛏ Mining: §f" + oreName + " §7[" + clusterRemaining + " in cluster] [" + totalMined + " total]"), true);
        } else {
            int pickSlot = findAnyPickaxe();
            if (pickSlot != -1) {
                mc.player.getInventory().selectedSlot = pickSlot;
            }
            
            walkTowards(targetOre);
            
            mc.player.sendMessage(Text.of("§b➤ Walking to: §f" + oreName + " §7(" + (int)distance + "m) [" + clusterRemaining + " in cluster]"), true);
        }
    }
    
    private void findNewCluster(List<BlockPos> allOres, BlockESP esp) {
        List<BlockPos> validOres = new ArrayList<>();
        for (BlockPos pos : allOres) {
            if (blacklist.contains(pos)) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;
            if (!esp.getTargetBlocks().contains(state.getBlock())) continue;
            validOres.add(pos);
        }
        
        if (validOres.isEmpty()) return;
        
        Vec3d playerPos = mc.player.getPos();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (BlockPos pos : validOres) {
            double dist = playerPos.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
        }
        
        if (nearest == null) return;
        
        lockedBlockType = mc.world.getBlockState(nearest).getBlock();
        
        currentCluster.clear();
        Vec3d clusterCenter = Vec3d.ofCenter(nearest);
        
        for (BlockPos pos : validOres) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() != lockedBlockType) continue;
            
            double dist = Vec3d.ofCenter(pos).distanceTo(clusterCenter);
            if (dist <= CLUSTER_RADIUS) {
                currentCluster.add(pos);
            }
        }
        
        // Sort ONCE when cluster is found
        currentCluster.sort((a, b) -> {
            double distA = playerPos.squaredDistanceTo(Vec3d.ofCenter(a));
            double distB = playerPos.squaredDistanceTo(Vec3d.ofCenter(b));
            return Double.compare(distA, distB);
        });
        
        if (!currentCluster.isEmpty()) {
            mc.player.sendMessage(Text.of("§a[AutoMine] Locked onto cluster of §f" + currentCluster.size() + " " + lockedBlockType.getName().getString()), false);
        }
    }
    
    private BlockPos getNextFromCluster() {
        if (currentCluster.isEmpty()) return null;
        
        // DON'T re-sort - just get the first one
        // This prevents spinning between equidistant targets
        return currentCluster.get(0);
    }
    
    private void walkTowards(BlockPos target) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetVec = Vec3d.ofCenter(target);
        
        double dx = targetVec.x - playerPos.x;
        double dy = targetVec.y - playerPos.y;
        double dz = targetVec.z - playerPos.z;
        
        // FIXED: Correct yaw calculation for Minecraft
        float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz)) + 180;
        
        // Normalize to -180 to 180
        while (targetYaw > 180) targetYaw -= 360;
        while (targetYaw < -180) targetYaw += 360;
        
        // Snap rotation faster to prevent spinning
        float currentYaw = mc.player.getYaw();
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // Faster rotation (0.5 instead of 0.2)
        float rotSpeed = 0.5f;
        // If very close to target angle, snap to it
        if (Math.abs(yawDiff) < 5) {
            mc.player.setYaw(targetYaw);
        } else {
            mc.player.setYaw(currentYaw + yawDiff * rotSpeed);
        }
        
        // Pitch
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, horizontalDist));
        targetPitch = Math.max(-45, Math.min(45, targetPitch));
        mc.player.setPitch(mc.player.getPitch() + (targetPitch - mc.player.getPitch()) * 0.3f);
        
        // Move forward
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(true);
        
        // Jump logic
        BlockPos playerBlock = mc.player.getBlockPos();
        Direction facing = mc.player.getHorizontalFacing();
        BlockPos ahead = playerBlock.offset(facing);
        BlockPos aheadUp2 = ahead.up(2);
        
        if (!mc.world.getBlockState(ahead).isAir() && mc.world.getBlockState(aheadUp2).isAir()) {
            if (mc.player.isOnGround()) mc.player.jump();
        }
        
        BlockPos belowAhead = ahead.down();
        if (mc.world.getBlockState(ahead).isAir() && mc.world.getBlockState(belowAhead).isAir()) {
            if (mc.player.isOnGround()) mc.player.jump();
        }
        
        if (dy > 2 && stuckTicks > 10) {
            if (mc.player.isOnGround()) mc.player.jump();
        }
        
        if (dy < -2 && stuckTicks > 20) {
            BlockPos below = mc.player.getBlockPos().down();
            if (!mc.world.getBlockState(below).isAir()) {
                int pickSlot = findBestPickaxeForBlock(below);
                if (pickSlot != -1) {
                    mc.player.getInventory().selectedSlot = pickSlot;
                }
                lookAt(below);
                mineBlock(below);
            }
        }
    }
    
    private void mineObstacles() {
        BlockPos playerBlock = mc.player.getBlockPos();
        Direction facing = mc.player.getHorizontalFacing();
        
        BlockPos[] toCheck = {
            playerBlock.offset(facing),
            playerBlock.offset(facing).up(),
            playerBlock.up(2).offset(facing)
        };
        
        for (BlockPos pos : toCheck) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && state.getHardness(mc.world, pos) >= 0) {
                int pickSlot = findBestPickaxeForBlock(pos);
                if (pickSlot != -1) {
                    mc.player.getInventory().selectedSlot = pickSlot;
                }
                lookAt(pos);
                mineBlock(pos);
                return;
            }
        }
    }
    
    private void lookAt(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eyes = mc.player.getEyePos();
        
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        // FIXED yaw calculation
        float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz)) + 180;
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, dist));
        
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
    
    private void mineBlock(BlockPos pos) {
        Direction bestFace = Direction.UP;
        Vec3d eyes = mc.player.getEyePos();
        double bestDist = Double.MAX_VALUE;
        
        for (Direction dir : Direction.values()) {
            Vec3d face = Vec3d.ofCenter(pos).add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5);
            double dist = eyes.distanceTo(face);
            if (dist < bestDist) {
                bestDist = dist;
                bestFace = dir;
            }
        }
        
        mc.options.attackKey.setPressed(true);
        mc.interactionManager.attackBlock(pos, bestFace);
        mc.interactionManager.updateBlockBreakingProgress(pos, bestFace);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    
    private int findBestPickaxeForBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        
        int bestSlot = -1;
        float bestSpeed = 0.0f;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof PickaxeItem) {
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        
        if (bestSlot == -1) {
            return findAnyPickaxe();
        }
        
        return bestSlot;
    }
    
    private int findAnyPickaxe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof PickaxeItem) {
                return i;
            }
        }
        return -1;
    }
    
    private void releaseKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
    }
    
    @Override
    public void onDisable() {
        releaseKeys();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§c[AutoMine] Stopped. Total mined: §a" + totalMined), false);
        }
        targetOre = null;
        lockedTarget = null;
        currentCluster.clear();
        blacklist.clear();
    }
    
    public BlockPos getCurrentTarget() { return targetOre; }
    public int getTotalMined() { return totalMined; }
    public boolean isMining() { return targetOre != null; }
    public int getClusterSize() { return currentCluster.size(); }
}
