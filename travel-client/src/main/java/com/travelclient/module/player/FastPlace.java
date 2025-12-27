package com.travelclient.module.player;

import com.travelclient.module.Module;

public class FastPlace extends Module {
    
    public FastPlace() {
        super("FastPlace", "Removes delay between block placements", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        // Logic handled in mixin - sets itemUseCooldown to 0
        // This is a simple module that just signals to the mixin
    }
    
    // Called from mixin
    public boolean shouldRemoveDelay() {
        return isEnabled();
    }
}
