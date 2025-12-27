package com.travelclient.module.movement;

import com.travelclient.module.Module;

public class AutoSprint extends Module {
    
    public AutoSprint() {
        super("AutoSprint", "Automatically sprints when moving forward", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Check if moving forward and can sprint
        boolean movingForward = mc.options.forwardKey.isPressed();
        boolean canSprint = !mc.player.isSneaking() && 
                           !mc.player.isUsingItem() && 
                           mc.player.getHungerManager().getFoodLevel() > 6;
        
        if (movingForward && canSprint && mc.player.isOnGround()) {
            mc.player.setSprinting(true);
        }
    }
}
