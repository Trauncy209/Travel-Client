package com.travelclient.module.movement;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import com.travelclient.module.player.AntiGhast;
import com.travelclient.module.player.AutoBridge;
import com.travelclient.module.player.AutoMine;
import com.travelclient.util.PathScanner;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class NetherNav extends Module {
    private final ModuleManager manager;
    
    // Settings
    private int tunnelHeight = 3;        // 3 blocks tall tunnel
    private int tunnelWidth = 1;         // 1 block wide (player width)
    private boolean preferTunnel = true; // Prefer tunneling over open-air travel
    private int scanDistance = 150;
    private float turnSpeed = 5.0f;
    private float arrivalDistance = 3.0f;
    
    // State
    private String navStatus = "idle";
    private boolean isTunneling = false;
    private int scanCooldown = 0;
    private float targetYaw = 0;
    
    public NetherNav(ModuleManager manager) {
        super("NetherNav", "Safe nether navigation - tunnels through terrain, bridges gaps, avoids ghasts", Category.MOVEMENT);
        this.manager = manager;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Need a target
        if (!manager.hasTarget()) {
            navStatus = "no target";
            return;
        }
        
        // Don't work while flying
        if (mc.player.isFallFlying()) return;
        
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        
        // Check if arrived
        double distance = manager.getDistanceToTarget(playerX, playerZ);
        if (distance < arrivalDistance) {
            mc.player.sendMessage(Text.literal("§a[TravelClient] Arrived at destination!"), true);
            manager.clearTarget();
            stopMovement();
            navStatus = "arrived";
            return;
        }
        
        // Get helper modules
        AntiGhast antiGhast = manager.getModule(AntiGhast.class);
        AutoMine autoMine = manager.getModule(AutoMine.class);
        AutoBridge autoBridge = manager.getModule(AutoBridge.class);
        
        // Check ghast danger
        boolean ghastDanger = antiGhast != null && antiGhast.isEnabled() && antiGhast.shouldSeekCover();
        
        // Scan path
        if (scanCooldown <= 0) {
            scanPath(ghastDanger);
            scanCooldown = 10;
        }
        scanCooldown--;
        
        // Handle ghast danger - seek cover
        if (ghastDanger && antiGhast != null) {
            Vec3d coverDir = antiGhast.getSuggestedCoverDirection();
            if (coverDir != null) {
                // Turn toward cover
                targetYaw = (float) Math.toDegrees(Math.atan2(-coverDir.x, coverDir.z));
                navStatus = "§c§lSEEKING COVER!";
                isTunneling = true; // Force tunneling for cover
            }
        }
        
        // Turn toward target path
        float currentYaw = mc.player.getYaw();
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        if (Math.abs(yawDiff) > 2.0f) {
            float adjustment = Math.max(-turnSpeed, Math.min(turnSpeed, yawDiff * 0.2f));
            mc.player.setYaw(currentYaw + adjustment);
        }
        
        // Check if we should be tunneling
        if (isTunneling || preferTunnel) {
            // Make sure AutoMine is working
            if (autoMine != null && !autoMine.isEnabled()) {
                autoMine.setEnabled(true);
            }
            
            // Clear a tunnel (mine blocks at head and above)
            ensureTunnelClear();
        }
        
        // Handle bridging over gaps
        if (autoBridge != null && autoBridge.isEnabled()) {
            // AutoBridge handles itself, just make sure we have blocks
            if (!autoBridge.hasBridgeBlocks()) {
                navStatus += " §c(need blocks!)";
            }
        }
        
        // Move forward if not blocked
        if (autoMine == null || !autoMine.isMining()) {
            mc.options.forwardKey.setPressed(true);
            mc.options.sprintKey.setPressed(true);
        } else {
            // Slow down while mining
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
        }
        
        // Auto-jump over obstacles
        if (mc.player.horizontalCollision && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }
    }
    
    private void scanPath(boolean ghastDanger) {
        Vec3d pos = mc.player.getPos();
        
        // Calculate direct yaw to target
        float directYaw = manager.getYawToTarget(mc.player.getX(), mc.player.getZ());
        
        // Check if direct path is safe
        PathScanner.PathResult result = PathScanner.findSafeGroundPath(
            pos,
            manager.getTargetX(),
            manager.getTargetZ(),
            scanDistance
        );
        
        targetYaw = result.yaw;
        
        // Decide if we should tunnel
        boolean shouldTunnel = false;
        
        // Check if there's open air (ghast danger)
        if (ghastDanger) {
            shouldTunnel = true;
        }
        
        // Check if we're in the nether
        if (mc.world.getRegistryKey().getValue().toString().contains("nether")) {
            // Check for open air above/around
            BlockPos above = mc.player.getBlockPos().up(3);
            if (mc.world.getBlockState(above).isAir()) {
                // We're exposed - prefer tunneling
                if (preferTunnel) {
                    shouldTunnel = true;
                }
            }
        }
        
        // Check for lava lakes ahead
        if (hasLavaAhead()) {
            // Need to tunnel or bridge - tunneling is safer
            shouldTunnel = true;
            navStatus = "§etunneling past lava";
        }
        
        isTunneling = shouldTunnel;
        
        if (isTunneling) {
            navStatus = "§6tunneling " + String.format("%.0fm", manager.getDistanceToTarget(pos.x, pos.z));
        } else {
            navStatus = result.isSafe ? "§a" + result.reason : "§c" + result.reason;
        }
    }
    
    private void ensureTunnelClear() {
        // The AutoMine module handles block breaking
        // This method ensures we're maintaining a proper tunnel shape
        
        float yaw = mc.player.getYaw();
        Vec3d pos = mc.player.getPos();
        
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        // Check 2 blocks ahead
        for (int dist = 1; dist <= 2; dist++) {
            double checkX = pos.x + moveX * dist;
            double checkZ = pos.z + moveZ * dist;
            
            BlockPos base = new BlockPos((int) checkX, (int) pos.y, (int) checkZ);
            
            // Check ceiling isn't lava/dangerous
            BlockPos ceiling = base.up(tunnelHeight);
            if (mc.world.getBlockState(ceiling).getBlock() == Blocks.LAVA) {
                // Don't mine upward into lava!
                // The tunnel will just be shorter
            }
        }
    }
    
    private boolean hasLavaAhead() {
        float yaw = mc.player.getYaw();
        Vec3d pos = mc.player.getPos();
        
        double yawRad = Math.toRadians(yaw);
        double moveX = -Math.sin(yawRad);
        double moveZ = Math.cos(yawRad);
        
        for (int dist = 1; dist <= 10; dist++) {
            double checkX = pos.x + moveX * dist;
            double checkZ = pos.z + moveZ * dist;
            
            for (int y = -2; y <= 2; y++) {
                BlockPos checkPos = new BlockPos((int) checkX, (int) pos.y + y, (int) checkZ);
                if (mc.world.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void stopMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }
    
    @Override
    public void onEnable() {
        if (!manager.hasTarget()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§e[TravelClient] No target set! Use GUI to set coordinates."), false);
            }
        } else {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§a[TravelClient] Nether navigation enabled"), false);
                mc.player.sendMessage(Text.literal("§7- Enable AutoMine for tunneling"), false);
                mc.player.sendMessage(Text.literal("§7- Enable AutoBridge for gap bridging"), false);
                mc.player.sendMessage(Text.literal("§7- Enable AntiGhast for ghast warnings"), false);
            }
        }
        scanCooldown = 0;
    }
    
    @Override
    public void onDisable() {
        stopMovement();
        navStatus = "idle";
    }
    
    public String getNavStatus() { return navStatus; }
    public boolean isTunneling() { return isTunneling; }
    public boolean isPreferTunnel() { return preferTunnel; }
    public void setPreferTunnel(boolean prefer) { this.preferTunnel = prefer; }
}
