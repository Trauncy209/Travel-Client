package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class Jesus extends Module {
    
    private boolean solidMode = true; // Make water solid vs dolphin mode
    
    public Jesus() {
        super("Jesus", "Walk on water and lava", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Don't activate while sneaking (allows going underwater)
        if (mc.player.isSneaking()) return;
        
        // Don't activate while flying
        if (mc.player.isFallFlying()) return;
        
        // Check if player is above water/lava
        BlockPos below = mc.player.getBlockPos().down();
        FluidState fluidBelow = mc.world.getFluidState(below);
        
        boolean aboveWater = fluidBelow.isIn(FluidTags.WATER);
        boolean aboveLava = fluidBelow.isIn(FluidTags.LAVA);
        
        if ((aboveWater || aboveLava) && !mc.player.isTouchingWater()) {
            if (solidMode) {
                // Keep player above water
                if (mc.player.getVelocity().y < 0) {
                    mc.player.setVelocity(
                        mc.player.getVelocity().x,
                        0,
                        mc.player.getVelocity().z
                    );
                    mc.player.setOnGround(true);
                }
            }
        }
        
        // If in water, swim up
        if (mc.player.isTouchingWater() && !mc.player.isSneaking()) {
            mc.player.setVelocity(
                mc.player.getVelocity().x,
                0.11,
                mc.player.getVelocity().z
            );
        }
    }
    
    // Called from mixin to check if water should be solid
    public boolean shouldWaterBeSolid() {
        if (!isEnabled() || mc.player == null) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.isFallFlying()) return false;
        return solidMode;
    }
    
    public boolean isSolidMode() { return solidMode; }
    public void setSolidMode(boolean solid) { this.solidMode = solid; }
}
