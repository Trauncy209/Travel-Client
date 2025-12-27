package com.travelclient.module.movement;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ElytraFly extends Module {
    private final ModuleManager manager;
    
    private String flightStatus = "Idle";
    private double totalProgress = 0;
    private Vec3d lastPos = null;
    
    // Takeoff state
    private int takeoffState = 0;
    private int takeoffTicks = 0;
    
    // Scan results
    private float bestYaw = 0;
    private float bestPitch = 0;
    private int bestDist = 0;
    private int scannedCount = 0;
    
    // CRITICAL SAFETY
    private static final int EMERGENCY_DISTANCE = 15;      // HARD TURN if obstacle this close
    private static final int DANGER_DISTANCE = 40;         // Start avoiding
    private static final int SAFE_DISTANCE = 80;           // Comfortable
    private static final int SCAN_DISTANCE = 120;          // How far to look
    private static final int UNLOADED_CHUNK_PULLUP = 60;   // Pull up if unloaded chunk within this
    private static final double MIN_SAFE_ALTITUDE = 100;   // Minimum safe overworld altitude
    private static final double NETHER_SAFE_Y_MIN = 15;    // Stay above lava lakes
    private static final double NETHER_SAFE_Y_MAX = 115;   // Stay below ceiling (128 - buffer)
    
    // State
    private boolean emergencyClimb = false;
    private int emergencyTicks = 0;
    private int stuckTicks = 0;
    private Vec3d lastSafePos = null;
    private int fireworkCooldown = 0; // Prevent firework spam
    
    public ElytraFly(ModuleManager manager) {
        super("ElytraFly", "Terrain-aware autopilot with obstacle avoidance", Category.MOVEMENT);
        this.manager = manager;
    }
    
    @Override
    public void onEnable() {
        lastPos = null;
        totalProgress = 0;
        takeoffState = 0;
        takeoffTicks = 0;
        emergencyClimb = false;
        emergencyTicks = 0;
        stuckTicks = 0;
        lastSafePos = null;
        fireworkCooldown = 0;
        
        if (mc.player != null && !mc.player.isFallFlying()) {
            startTakeoff();
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Tick down firework cooldown
        if (fireworkCooldown > 0) fireworkCooldown--;
        
        // Elytra durability check
        var elytraStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (elytraStack.getItem() == Items.ELYTRA) {
            int durability = elytraStack.getMaxDamage() - elytraStack.getDamage();
            int maxDur = elytraStack.getMaxDamage();
            
            if (durability <= maxDur * 0.10 && mc.player.age % 100 == 0) {
                mc.player.sendMessage(net.minecraft.text.Text.of("§c§l[!] Elytra low! §e" + durability + "/" + maxDur), false);
            }
            
            if (durability <= maxDur * 0.05 && mc.player.isFallFlying()) {
                mc.player.sendMessage(net.minecraft.text.Text.of("§4§l[!!!] ELYTRA CRITICAL!"), false);
                emergencyLand();
                return;
            }
        }
        
        // Handle takeoff
        if (!mc.player.isFallFlying()) {
            handleTakeoff();
            return;
        }
        
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;
        
        Vec3d pos = mc.player.getPos();
        
        // Track progress
        if (lastPos != null && manager.hasTarget()) {
            double dx = pos.x - lastPos.x;
            double dz = pos.z - lastPos.z;
            float targetYaw = manager.getYawToTarget(pos.x, pos.z);
            double rad = Math.toRadians(targetYaw);
            totalProgress += dx * (-Math.sin(rad)) + dz * Math.cos(rad);
        }
        
        // Stuck detection
        if (lastPos != null) {
            double moved = pos.distanceTo(lastPos);
            if (moved < 0.5) {
                stuckTicks++;
                if (stuckTicks > 20) {
                    // We're stuck - emergency climb!
                    emergencyClimb = true;
                    emergencyTicks = 60;
                    flightStatus = "§4§lSTUCK - CLIMBING!";
                }
            } else {
                stuckTicks = 0;
                lastSafePos = pos;
            }
        }
        lastPos = pos;
        
        // Check dimension
        boolean isNether = mc.world.getRegistryKey() == World.NETHER;
        
        // CRITICAL: Check for unloaded chunks ahead
        int unloadedDist = checkUnloadedChunks(pos);
        if (unloadedDist < UNLOADED_CHUNK_PULLUP) {
            // UNLOADED CHUNK AHEAD - PULL UP NOW!
            emergencyClimb = true;
            emergencyTicks = Math.max(emergencyTicks, 40);
            flightStatus = "§4§lUNLOADED CHUNK! PULLING UP!";
            bestYaw = mc.player.getYaw();
            bestPitch = -45; // Climb hard
            applyTurn(1.0f);
            return;
        }
        
        // Emergency climb mode
        if (emergencyClimb) {
            emergencyTicks--;
            if (emergencyTicks <= 0) {
                emergencyClimb = false;
            } else {
                // Just go UP
                bestYaw = manager.hasTarget() ? manager.getYawToTarget(pos.x, pos.z) : mc.player.getYaw();
                bestPitch = -40;
                applyTurn(0.8f);
                flightStatus = "§c§lEMERGENCY CLIMB " + emergencyTicks;
                return;
            }
        }
        
        if (isNether) {
            handleNetherFlight(pos);
        } else {
            handleOverworldFlight(pos);
        }
    }
    
    /**
     * Check how far until we hit an unloaded chunk
     */
    private int checkUnloadedChunks(Vec3d pos) {
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        
        for (int d = 16; d <= SCAN_DISTANCE; d += 16) {
            int checkX = (int)(pos.x + dx * d);
            int checkZ = (int)(pos.z + dz * d);
            int cx = checkX >> 4;
            int cz = checkZ >> 4;
            
            if (!mc.world.isChunkLoaded(cx, cz)) {
                return d;
            }
        }
        return SCAN_DISTANCE + 1;
    }
    
    /**
     * OVERWORLD FLIGHT - Stay high, scan ahead, avoid everything
     */
    private void handleOverworldFlight(Vec3d pos) {
        float targetYaw = manager.hasTarget() ? manager.getYawToTarget(pos.x, pos.z) : mc.player.getYaw();
        
        // Multi-directional scan
        int forwardClear = scanDirection(pos, targetYaw, 0);
        int forwardUpClear = scanDirection(pos, targetYaw, -20);
        int forwardDownClear = scanDirection(pos, targetYaw, 20);
        int upClear = scanDirection(pos, targetYaw, -45);
        
        // Also scan slightly left and right
        int leftClear = scanDirection(pos, targetYaw - 30, 0);
        int rightClear = scanDirection(pos, targetYaw + 30, 0);
        
        scannedCount = 6;
        
        // EMERGENCY: Something very close!
        if (forwardClear < EMERGENCY_DISTANCE) {
            emergencyClimb = true;
            emergencyTicks = 40;
            flightStatus = "§4§lEMERGENCY! " + forwardClear + "m";
            bestYaw = targetYaw;
            bestPitch = -50; // HARD climb
            applyTurn(1.0f);
            useFirework(); // Boost to get out of danger
            return;
        }
        
        // DANGER: Obstacle approaching
        if (forwardClear < DANGER_DISTANCE) {
            flightStatus = "§c§lAVOIDING! " + forwardClear + "m";
            
            // Can we go up?
            if (upClear > forwardClear + 20) {
                bestYaw = targetYaw;
                bestPitch = -35;
                bestDist = upClear;
            }
            // Can we go left?
            else if (leftClear > forwardClear + 20) {
                bestYaw = targetYaw - 45;
                bestPitch = -15;
                bestDist = leftClear;
            }
            // Can we go right?
            else if (rightClear > forwardClear + 20) {
                bestYaw = targetYaw + 45;
                bestPitch = -15;
                bestDist = rightClear;
            }
            // Just go UP
            else {
                bestYaw = targetYaw;
                bestPitch = -40;
                bestDist = upClear;
            }
            
            applyTurn(0.6f);
            return;
        }
        
        // Check if we're too low (below safe altitude with terrain ahead)
        if (pos.y < MIN_SAFE_ALTITUDE && forwardClear < SAFE_DISTANCE) {
            flightStatus = "§eClimbing to safe altitude...";
            bestYaw = targetYaw;
            bestPitch = -25;
            bestDist = forwardClear;
            applyTurn(0.3f);
            return;
        }
        
        // SAFE: Clear ahead
        if (forwardClear >= SAFE_DISTANCE) {
            // Can we descend? Only if very clear below
            if (pos.y > 150 && forwardDownClear > SAFE_DISTANCE) {
                bestYaw = targetYaw;
                bestPitch = 10; // Gentle descent
                bestDist = forwardDownClear;
                flightStatus = "§aDescending - " + (int)totalProgress + "m";
            } else {
                // Level flight toward target
                bestYaw = targetYaw;
                bestPitch = 0;
                bestDist = forwardClear;
                flightStatus = "§aCruising - " + (int)totalProgress + "m";
            }
            applyTurn(0.15f);
            return;
        }
        
        // MODERATE: Some obstacle ahead but not urgent
        flightStatus = "§eNavigating - " + forwardClear + "m clear";
        
        // Prefer going up slightly
        if (forwardUpClear > forwardClear) {
            bestYaw = targetYaw;
            bestPitch = -15;
            bestDist = forwardUpClear;
        } else {
            bestYaw = targetYaw;
            bestPitch = 0;
            bestDist = forwardClear;
        }
        applyTurn(0.25f);
    }
    
    /**
     * NETHER FLIGHT - Aggressive, fast response, tight space navigation
     */
    private void handleNetherFlight(Vec3d pos) {
        float targetYaw = manager.hasTarget() ? manager.getYawToTarget(pos.x, pos.z) : mc.player.getYaw();
        
        // Nether-specific constants - MUCH more responsive
        final int NETHER_SCAN_DIST = 80;
        final int NETHER_DANGER = 25;
        final int NETHER_EMERGENCY = 10;
        
        // Safety bounds check - keep us in the flyable zone
        if (pos.y < NETHER_SAFE_Y_MIN + 5) {
            flightStatus = "§4§lTOO LOW! CLIMBING!";
            bestYaw = targetYaw;
            bestPitch = -55;
            applyTurn(1.0f);
            useFirework();
            return;
        }
        
        if (pos.y > NETHER_SAFE_Y_MAX - 5) {
            flightStatus = "§4§lNEAR CEILING! DIVING!";
            bestYaw = targetYaw;
            bestPitch = 40;
            applyTurn(1.0f);
            return;
        }
        
        // AGGRESSIVE multi-directional scan - scan 13 directions
        int fwd = scanNetherDirection(pos, targetYaw, 0, NETHER_SCAN_DIST);
        int fwdUp = scanNetherDirection(pos, targetYaw, -25, NETHER_SCAN_DIST);
        int fwdDown = scanNetherDirection(pos, targetYaw, 20, NETHER_SCAN_DIST);
        int up = scanNetherDirection(pos, targetYaw, -50, NETHER_SCAN_DIST);
        int down = scanNetherDirection(pos, targetYaw, 40, NETHER_SCAN_DIST);
        int left30 = scanNetherDirection(pos, targetYaw - 30, 0, NETHER_SCAN_DIST);
        int left60 = scanNetherDirection(pos, targetYaw - 60, 0, NETHER_SCAN_DIST);
        int left90 = scanNetherDirection(pos, targetYaw - 90, 0, NETHER_SCAN_DIST);
        int right30 = scanNetherDirection(pos, targetYaw + 30, 0, NETHER_SCAN_DIST);
        int right60 = scanNetherDirection(pos, targetYaw + 60, 0, NETHER_SCAN_DIST);
        int right90 = scanNetherDirection(pos, targetYaw + 90, 0, NETHER_SCAN_DIST);
        int leftUp = scanNetherDirection(pos, targetYaw - 45, -30, NETHER_SCAN_DIST);
        int rightUp = scanNetherDirection(pos, targetYaw + 45, -30, NETHER_SCAN_DIST);
        
        scannedCount = 13;
        
        // EMERGENCY - WALL RIGHT IN FRONT!
        if (fwd < NETHER_EMERGENCY) {
            flightStatus = "§4§lWALL! " + fwd + "m!";
            
            // Find best escape - check all options
            int[] dists = {up, leftUp, rightUp, left60, right60, left90, right90};
            float[] yaws = {targetYaw, targetYaw - 45, targetYaw + 45, targetYaw - 60, targetYaw + 60, targetYaw - 90, targetYaw + 90};
            float[] pitches = {-55, -35, -35, -15, -15, 0, 0};
            
            int bestIdx = 0;
            int bestClear = dists[0];
            for (int i = 1; i < dists.length; i++) {
                // Don't go down if too low, don't go up if too high
                if (pitches[i] < -20 && pos.y > NETHER_SAFE_Y_MAX - 20) continue;
                if (pitches[i] > 20 && pos.y < NETHER_SAFE_Y_MIN + 20) continue;
                
                if (dists[i] > bestClear) {
                    bestClear = dists[i];
                    bestIdx = i;
                }
            }
            
            bestYaw = yaws[bestIdx];
            bestPitch = pitches[bestIdx];
            bestDist = bestClear;
            
            applyTurn(1.0f); // INSTANT turn
            useFirework(); // Boost out!
            return;
        }
        
        // DANGER - obstacle approaching fast
        if (fwd < NETHER_DANGER) {
            flightStatus = "§c§lAVOIDING " + fwd + "m";
            
            // Find clearest path - prioritize forward-ish directions
            int bestClear = fwd;
            bestYaw = targetYaw;
            bestPitch = 0;
            
            // Check upward paths (if we have room)
            if (pos.y < NETHER_SAFE_Y_MAX - 25) {
                if (up > bestClear + 10) { bestClear = up; bestYaw = targetYaw; bestPitch = -45; }
                if (fwdUp > bestClear + 10) { bestClear = fwdUp; bestYaw = targetYaw; bestPitch = -25; }
                if (leftUp > bestClear + 10) { bestClear = leftUp; bestYaw = targetYaw - 45; bestPitch = -30; }
                if (rightUp > bestClear + 10) { bestClear = rightUp; bestYaw = targetYaw + 45; bestPitch = -30; }
            }
            
            // Check side paths
            if (left30 > bestClear + 10) { bestClear = left30; bestYaw = targetYaw - 30; bestPitch = -5; }
            if (right30 > bestClear + 10) { bestClear = right30; bestYaw = targetYaw + 30; bestPitch = -5; }
            if (left60 > bestClear + 15) { bestClear = left60; bestYaw = targetYaw - 60; bestPitch = -5; }
            if (right60 > bestClear + 15) { bestClear = right60; bestYaw = targetYaw + 60; bestPitch = -5; }
            
            // Check down (if we have room below)
            if (pos.y > NETHER_SAFE_Y_MIN + 30 && fwdDown > bestClear + 15) {
                bestClear = fwdDown; bestYaw = targetYaw; bestPitch = 20;
            }
            
            bestDist = bestClear;
            applyTurn(0.8f); // Fast turn
            return;
        }
        
        // MODERATE - something ahead but we have time
        if (fwd < 50) {
            flightStatus = "§eNether nav " + fwd + "m";
            
            bestYaw = targetYaw;
            bestPitch = 0;
            bestDist = fwd;
            
            // Gentle adjustments toward clearer path
            if (fwdUp > fwd + 15 && pos.y < NETHER_SAFE_Y_MAX - 20) {
                bestPitch = -20;
                bestDist = fwdUp;
            } else if (left30 > fwd + 15) {
                bestYaw = targetYaw - 20;
                bestDist = left30;
            } else if (right30 > fwd + 15) {
                bestYaw = targetYaw + 20;
                bestDist = right30;
            }
            
            applyTurn(0.5f);
            return;
        }
        
        // SAFE - open space ahead!
        flightStatus = "§aNether cruise " + (int)totalProgress + "m";
        bestYaw = targetYaw;
        bestDist = fwd;
        
        // Maintain ideal altitude (around Y=65 - middle of safe zone)
        double idealY = (NETHER_SAFE_Y_MIN + NETHER_SAFE_Y_MAX) / 2;
        if (pos.y < idealY - 10) {
            bestPitch = -12;
        } else if (pos.y > idealY + 10) {
            bestPitch = 8;
        } else {
            bestPitch = 0;
        }
        
        applyTurn(0.2f);
    }
    
    /**
     * Nether-specific scan - finer resolution for tight spaces
     */
    private int scanNetherDirection(Vec3d start, float yaw, float pitch, int maxDist) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * Math.cos(pitchRad);
        
        // Finer scan - every 2 blocks for first 30, then every 4
        for (int d = 2; d <= maxDist; d += (d < 30 ? 2 : 4)) {
            double x = start.x + dx * d;
            double y = start.y + dy * d;
            double z = start.z + dz * d;
            
            // Chunk loaded check
            int cx = (int)Math.floor(x) >> 4;
            int cz = (int)Math.floor(z) >> 4;
            if (!mc.world.isChunkLoaded(cx, cz)) {
                return d;
            }
            
            int bx = (int)Math.floor(x);
            int by = (int)Math.floor(y);
            int bz = (int)Math.floor(z);
            
            // Check 3x3x3 hitbox
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        BlockPos checkPos = new BlockPos(bx + ox, by + oy, bz + oz);
                        if (isObstacle(checkPos)) {
                            return d;
                        }
                    }
                }
            }
        }
        
        return maxDist + 1;
    }
    
    /**
     * Scan a specific direction for obstacles
     * Returns distance to first obstacle (or SCAN_DISTANCE if clear)
     */
    private int scanDirection(Vec3d start, float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz = Math.cos(yawRad) * Math.cos(pitchRad);
        
        for (int d = 5; d <= SCAN_DISTANCE; d += 3) {
            double x = start.x + dx * d;
            double y = start.y + dy * d;
            double z = start.z + dz * d;
            
            // Chunk loaded check
            int cx = (int)Math.floor(x) >> 4;
            int cz = (int)Math.floor(z) >> 4;
            if (!mc.world.isChunkLoaded(cx, cz)) {
                return d; // Treat unloaded as obstacle
            }
            
            // Check a 5x5x5 cube around the point (player hitbox + safety margin)
            int bx = (int)Math.floor(x);
            int by = (int)Math.floor(y);
            int bz = (int)Math.floor(z);
            
            for (int ox = -2; ox <= 2; ox++) {
                for (int oy = -2; oy <= 2; oy++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        BlockPos checkPos = new BlockPos(bx + ox, by + oy, bz + oz);
                        
                        if (isObstacle(checkPos)) {
                            return d;
                        }
                    }
                }
            }
        }
        
        return SCAN_DISTANCE + 1;
    }
    
    /**
     * Check if a block is an obstacle (solid, liquid, or dangerous)
     */
    private boolean isObstacle(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        
        // Air is safe
        if (state.isAir()) return false;
        
        // Any solid block is obstacle
        if (!state.getCollisionShape(mc.world, pos).isEmpty()) return true;
        
        // Liquids are obstacles (water slows, lava kills)
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) return true;
        
        // Lava specifically
        if (state.getBlock() == Blocks.LAVA) return true;
        
        // Fire
        if (state.getBlock() == Blocks.FIRE || state.getBlock() == Blocks.SOUL_FIRE) return true;
        
        return false;
    }
    
    /**
     * Apply rotation toward best direction
     */
    private void applyTurn(float speed) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        
        float yawDiff = bestYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = bestPitch - currentPitch;
        
        // Clamp pitch
        float newPitch = currentPitch + pitchDiff * speed;
        newPitch = Math.max(-60, Math.min(60, newPitch));
        
        mc.player.setYaw(currentYaw + yawDiff * speed);
        mc.player.setPitch(newPitch);
    }
    
    // ============ TAKEOFF ============
    
    private void startTakeoff() {
        takeoffState = 1;
        takeoffTicks = 0;
        flightStatus = "§eLooking for ledge...";
    }
    
    private void handleTakeoff() {
        if (mc.player == null) return;
        
        takeoffTicks++;
        
        switch (takeoffState) {
            case 0:
                return;
                
            case 1: // Find ledge
                BlockPos ledge = findNearbyLedge();
                if (ledge != null) {
                    double dx = ledge.getX() + 0.5 - mc.player.getX();
                    double dz = ledge.getZ() + 0.5 - mc.player.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    
                    if (dist < 1.0) {
                        flightStatus = "§eJumping...";
                        mc.options.forwardKey.setPressed(true);
                        mc.player.jump();
                        takeoffState = 2;
                        takeoffTicks = 0;
                    } else {
                        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        mc.player.setYaw(yaw);
                        mc.options.forwardKey.setPressed(true);
                        mc.options.sprintKey.setPressed(true);
                        flightStatus = "§eWalking to ledge... (" + (int)dist + "m)";
                    }
                } else {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.sprintKey.setPressed(false);
                    if (mc.player.isOnGround()) {
                        mc.player.jump();
                        takeoffState = 2;
                        takeoffTicks = 0;
                        flightStatus = "§eJumping (no ledge)...";
                    }
                }
                break;
                
            case 2: // Deploy elytra
                mc.options.forwardKey.setPressed(true);
                if (takeoffTicks > 3) {
                    mc.player.jump();
                    takeoffState = 3;
                    takeoffTicks = 0;
                    flightStatus = "§eDeploying...";
                }
                break;
                
            case 3: // Use firework
                mc.options.forwardKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
                
                if (mc.player.isFallFlying()) {
                    useFirework();
                    takeoffState = 0;
                    flightStatus = "§aFlying!";
                } else if (takeoffTicks > 10) {
                    takeoffState = 1;
                    takeoffTicks = 0;
                }
                break;
        }
        
        if (takeoffTicks > 100) {
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            takeoffState = 1;
            takeoffTicks = 0;
        }
    }
    
    private BlockPos findNearbyLedge() {
        if (mc.player == null || mc.world == null) return null;
        
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestLedge = null;
        int bestDrop = 0;
        
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                BlockPos check = playerPos.add(x, 0, z);
                
                if (!mc.world.getBlockState(check.down()).isSolidBlock(mc.world, check.down())) continue;
                if (!mc.world.getBlockState(check).isAir()) continue;
                if (!mc.world.getBlockState(check.up()).isAir()) continue;
                
                int drop = 0;
                for (int y = 1; y <= 20; y++) {
                    boolean hasAir = mc.world.getBlockState(check.add(1, -y, 0)).isAir() || 
                                     mc.world.getBlockState(check.add(-1, -y, 0)).isAir() ||
                                     mc.world.getBlockState(check.add(0, -y, 1)).isAir() ||
                                     mc.world.getBlockState(check.add(0, -y, -1)).isAir();
                    if (hasAir) {
                        drop = y;
                    } else {
                        break;
                    }
                }
                
                if (drop >= 5 && drop > bestDrop) {
                    bestDrop = drop;
                    bestLedge = check;
                }
            }
        }
        
        return bestLedge;
    }
    
    private void useFirework() {
        if (mc.player == null) return;
        
        // Prevent firework spam - 3 second cooldown (60 ticks)
        if (fireworkCooldown > 0) return;
        
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof FireworkRocketItem) {
                int prev = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = prev;
                fireworkCooldown = 60; // 3 second cooldown
                return;
            }
        }
    }
    
    private void emergencyLand() {
        flightStatus = "§c§lLANDING!";
        mc.player.setPitch(45);
        
        BlockPos below = mc.player.getBlockPos().down();
        for (int i = 0; i < 10; i++) {
            if (!mc.world.getBlockState(below.down(i)).isAir()) {
                if (i < 5) mc.player.setPitch(10);
                break;
            }
        }
        
        if (mc.player.isOnGround() || !mc.player.isFallFlying()) {
            mc.player.sendMessage(net.minecraft.text.Text.of("§a[Truancy] Landed! Replace elytra!"), false);
            setEnabled(false);
        }
    }
    
    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
        }
        takeoffState = 0;
    }
    
    public void autoStartFlight() {
        startTakeoff();
    }
    
    // Getters for HUD/debug
    public String getFlightStatus() { return flightStatus; }
    public double getTotalProgress() { return totalProgress; }
    public int getPathsScanned() { return scannedCount; }
    public int getBestDistance() { return bestDist; }
    public boolean isAvoiding() { return bestDist < DANGER_DISTANCE; }
    public int getObstacleDistance() { return bestDist; }
    
    // Stubs for compatibility
    public boolean isDebugMode() { return false; }
    public void setDebugMode(boolean d) {}
    public List<Vec3d> getSimulatedPath() { return new ArrayList<>(); }
    public Vec3d getPathEnd() { return null; }
    public int getPathClearDistance() { return bestDist; }
    public double getTurnSpeed() { return 50; }
    public void setTurnSpeed(double s) {}
    public double getClimbAngle() { return 35; }
    public void setClimbAngle(double a) {}
    public double getDescendAngle() { return 15; }
    public void setDescendAngle(double a) {}
    public double getScanDist() { return SCAN_DISTANCE; }
    public void setScanDist(double d) {}
    public double getDangerDist() { return DANGER_DISTANCE; }
    public void setDangerDist(double d) {}
    public double getTargetSpeed() { return 0.5; }
    public void setTargetSpeed(double s) {}
    public int getSafetyMargin() { return 5; }
    public void setSafetyMargin(int m) {}
    public boolean isWaitingForChunks() { return false; }
    public int getUnloadedChunkDistance() { return checkUnloadedChunks(mc.player.getPos()); }
    public boolean areChunksLoaded() { return checkUnloadedChunks(mc.player.getPos()) > SCAN_DISTANCE; }
    public Vec3d getCollisionPoint() { return null; }
    public PathVisualization getChosenPath() { return null; }
    public List<PathVisualization> getScannedPaths() { return new ArrayList<>(); }
    public Vec3d getPredictedPosition() { return null; }
    public double getMinSpeed() { return 0.5; }
    public void setMinSpeed(double s) {}
    public int getScanDistance() { return SCAN_DISTANCE; }
    public void setScanDistance(int d) {}
    
    public static class PathVisualization {
        public final Vec3d start, end;
        public final boolean clear, blocked;
        public PathVisualization(Vec3d s, Vec3d e, boolean c, boolean b) { start=s; end=e; clear=c; blocked=b; }
    }
}
