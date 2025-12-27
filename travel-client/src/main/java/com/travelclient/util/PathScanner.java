package com.travelclient.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class PathScanner {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Hazardous blocks to avoid
    private static final Set<Block> HAZARD_BLOCKS = new HashSet<>(Arrays.asList(
        Blocks.LAVA,
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.CACTUS,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.MAGMA_BLOCK,
        Blocks.CAMPFIRE,
        Blocks.SOUL_CAMPFIRE,
        Blocks.WITHER_ROSE,
        Blocks.POWDER_SNOW
    ));
    
    // Blocks that are not solid ground
    private static final Set<Block> NON_SOLID = new HashSet<>(Arrays.asList(
        Blocks.AIR,
        Blocks.CAVE_AIR,
        Blocks.VOID_AIR,
        Blocks.WATER,
        Blocks.LAVA,
        Blocks.FIRE,
        Blocks.SOUL_FIRE
    ));
    
    /**
     * Scans for a safe ground path toward target coordinates
     * Returns the best direction to move (yaw adjustment)
     */
    public static PathResult findSafeGroundPath(Vec3d currentPos, double targetX, double targetZ, int scanDistance) {
        if (mc.world == null || mc.player == null) {
            return new PathResult(0, false, "No world");
        }
        
        // Direct angle to target
        double dx = targetX - currentPos.x;
        double dz = targetZ - currentPos.z;
        float directYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        
        // Scan multiple angles to find safest path
        float bestYaw = directYaw;
        int bestScore = Integer.MIN_VALUE;
        String bestReason = "direct";
        
        // Check angles from -90 to +90 degrees from direct path
        for (int angleOffset = 0; angleOffset <= 90; angleOffset += 15) {
            for (int sign : new int[]{1, -1}) {
                if (angleOffset == 0 && sign == -1) continue; // Don't check 0 twice
                
                float testYaw = directYaw + (angleOffset * sign);
                PathScore score = evaluateGroundPath(currentPos, testYaw, scanDistance);
                
                // Prefer angles closer to direct path when scores are similar
                int adjustedScore = score.score - (angleOffset / 5);
                
                if (adjustedScore > bestScore) {
                    bestScore = adjustedScore;
                    bestYaw = testYaw;
                    bestReason = score.reason;
                }
                
                // If direct path is safe, use it
                if (angleOffset == 0 && score.score > 50) {
                    return new PathResult(directYaw, true, "clear path");
                }
            }
        }
        
        // Check if we found any safe path
        boolean isSafe = bestScore > 0;
        
        return new PathResult(bestYaw, isSafe, bestReason);
    }
    
    /**
     * Evaluates a ground path in a given direction
     */
    private static PathScore evaluateGroundPath(Vec3d start, float yaw, int distance) {
        int score = 100;
        String reason = "clear";
        
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        BlockPos lastGround = new BlockPos((int) start.x, (int) start.y - 1, (int) start.z);
        
        for (int i = 1; i <= distance; i++) {
            double checkX = start.x + moveX * i;
            double checkZ = start.z + moveZ * i;
            
            // Find ground level at this position
            BlockPos groundPos = findGround(checkX, start.y, checkZ);
            
            if (groundPos == null) {
                // No ground found - void or too deep
                score -= 50;
                reason = "void/deep drop";
                continue;
            }
            
            // Check height difference (cliff detection)
            int heightDiff = Math.abs(groundPos.getY() - lastGround.getY());
            if (heightDiff > 3) {
                score -= 30;
                reason = "cliff";
            } else if (heightDiff > 1) {
                score -= 10;
            }
            
            // Check for hazards at feet and head level
            BlockPos feetPos = groundPos.up();
            BlockPos headPos = groundPos.up(2);
            
            if (isHazard(feetPos) || isHazard(headPos)) {
                score -= 40;
                reason = "hazard";
            }
            
            // Check for water (slows down)
            if (isWater(feetPos)) {
                score -= 15;
                reason = "water";
            }
            
            // Check for hostile mobs nearby
            if (hasHostileMobNear(checkX, groundPos.getY(), checkZ, 5)) {
                score -= 25;
                reason = "hostile mob";
            }
            
            // Check for blocked path (walls)
            if (!isPassable(feetPos) || !isPassable(headPos)) {
                score -= 35;
                reason = "blocked";
            }
            
            lastGround = groundPos;
        }
        
        return new PathScore(score, reason);
    }
    
    /**
     * Scans for a safe elytra flight path toward target coordinates
     */
    public static PathResult findSafeFlightPath(Vec3d currentPos, double targetX, double targetZ, int scanDistance) {
        if (mc.world == null || mc.player == null) {
            return new PathResult(0, false, "No world");
        }
        
        // Direct angle to target
        double dx = targetX - currentPos.x;
        double dz = targetZ - currentPos.z;
        float directYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        
        // Get current velocity direction
        Vec3d velocity = mc.player.getVelocity();
        
        // Scan for obstacles ahead
        float bestYaw = directYaw;
        float bestPitch = -2.0f; // Default glide pitch
        int bestScore = Integer.MIN_VALUE;
        String bestReason = "direct";
        
        // Check angles from -60 to +60 degrees from direct path
        for (int yawOffset = 0; yawOffset <= 60; yawOffset += 10) {
            for (int yawSign : new int[]{1, -1}) {
                if (yawOffset == 0 && yawSign == -1) continue;
                
                float testYaw = directYaw + (yawOffset * yawSign);
                
                // Also check different pitch angles
                for (float testPitch : new float[]{-5f, 0f, 10f, 20f, -15f}) {
                    PathScore score = evaluateFlightPath(currentPos, testYaw, testPitch, scanDistance);
                    
                    // Prefer direct path and level flight
                    int adjustedScore = score.score - (yawOffset / 3) - (int) Math.abs(testPitch);
                    
                    if (adjustedScore > bestScore) {
                        bestScore = adjustedScore;
                        bestYaw = testYaw;
                        bestPitch = testPitch;
                        bestReason = score.reason;
                    }
                }
            }
        }
        
        boolean isSafe = bestScore > 20;
        
        PathResult result = new PathResult(bestYaw, isSafe, bestReason);
        result.pitch = bestPitch;
        return result;
    }
    
    /**
     * Evaluates a flight path for obstacles
     */
    private static PathScore evaluateFlightPath(Vec3d start, float yaw, float pitch, int distance) {
        int score = 100;
        String reason = "clear";
        
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double moveX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double moveY = -Math.sin(pitchRad);
        double moveZ = Math.cos(yawRad) * Math.cos(pitchRad);
        
        for (int i = 5; i <= distance; i += 3) {
            double checkX = start.x + moveX * i;
            double checkY = start.y + moveY * i;
            double checkZ = start.z + moveZ * i;
            
            BlockPos checkPos = new BlockPos((int) checkX, (int) checkY, (int) checkZ);
            
            // Check if we're heading into terrain
            if (!isAir(checkPos)) {
                // Distance to collision affects score
                int urgency = (distance - i) / 5;
                score -= 30 + urgency * 10;
                reason = "terrain collision";
            }
            
            // Check surrounding area (we need clearance for wings)
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos nearby = checkPos.add(ox, oy, oz);
                        if (!isAir(nearby)) {
                            score -= 5;
                        }
                    }
                }
            }
            
            // Check if too low (danger of hitting ground)
            if (checkY < 10) {
                score -= 20;
                reason = "too low";
            }
            
            // Check for end crystals or other dangerous entities
            if (hasDangerousEntity(checkX, checkY, checkZ, 10)) {
                score -= 30;
                reason = "dangerous entity";
            }
        }
        
        return new PathScore(score, reason);
    }
    
    /**
     * Finds the ground level at a position
     */
    private static BlockPos findGround(double x, double startY, double z) {
        int searchY = (int) startY;
        
        // Search down for ground
        for (int y = searchY + 3; y > searchY - 20; y--) {
            BlockPos pos = new BlockPos((int) x, y, (int) z);
            BlockPos above = pos.up();
            
            if (isSolid(pos) && isPassable(above)) {
                return pos;
            }
        }
        
        return null;
    }
    
    private static boolean isHazard(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return HAZARD_BLOCKS.contains(block);
    }
    
    private static boolean isWater(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.WATER;
    }
    
    private static boolean isSolid(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }
    
    private static boolean isPassable(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return !state.isSolidBlock(mc.world, pos);
    }
    
    private static boolean isAir(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir();
    }
    
    private static boolean hasHostileMobNear(double x, double y, double z, double radius) {
        Box searchBox = new Box(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<HostileEntity> hostiles = mc.world.getEntitiesByClass(HostileEntity.class, searchBox, e -> true);
        return !hostiles.isEmpty();
    }
    
    private static boolean hasDangerousEntity(double x, double y, double z, double radius) {
        Box searchBox = new Box(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            // Check for end crystals, withers, etc
            String name = entity.getType().toString().toLowerCase();
            if (name.contains("crystal") || name.contains("wither") || name.contains("tnt")) {
                return true;
            }
        }
        return false;
    }
    
    public static class PathResult {
        public float yaw;
        public float pitch = 0;
        public boolean isSafe;
        public String reason;
        
        public PathResult(float yaw, boolean isSafe, String reason) {
            this.yaw = yaw;
            this.isSafe = isSafe;
            this.reason = reason;
        }
    }
    
    private static class PathScore {
        int score;
        String reason;
        
        PathScore(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }
}
