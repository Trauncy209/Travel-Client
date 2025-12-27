package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.util.math.BlockPos;

public class SafeWalk extends Module {
    
    public SafeWalk() {
        super("SafeWalk", "Prevents walking off edges - sneak behavior without sneaking", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        // Logic handled in mixin
    }
    
    public boolean shouldPreventFall() {
        if (mc.player == null || mc.world == null) return false;
        if (!isEnabled()) return false;
        if (mc.player.isSneaking()) return false;  // Already sneaking
        if (!mc.player.isOnGround()) return false;
        if (mc.player.isFallFlying()) return false;
        
        // Check if there's a drop ahead
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        
        // Get movement direction
        float yaw = mc.player.getYaw();
        double moveX = -Math.sin(Math.toRadians(yaw)) * 0.5;
        double moveZ = Math.cos(Math.toRadians(yaw)) * 0.5;
        
        // Check block ahead and below
        BlockPos ahead = new BlockPos(
            (int) Math.floor(x + moveX),
            (int) Math.floor(y - 1),
            (int) Math.floor(z + moveZ)
        );
        
        return mc.world.getBlockState(ahead).isAir();
    }
}
