package com.travelclient.module.combat;

import com.travelclient.module.Module;

public class Reach extends Module {
    
    private double reachDistance = 4.5; // Default is 3.0
    
    public Reach() {
        super("Reach", "Extended attack range", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        // Reach is applied through a mixin
    }
    
    public double getReachDistance() { return reachDistance; }
    public void setReachDistance(double d) { reachDistance = Math.max(3.0, Math.min(6.0, d)); }
}
