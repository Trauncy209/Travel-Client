package com.travelclient.module.movement;

import com.travelclient.module.Module;

public class AntiVelocity extends Module {
    
    private float horizontal = 0.0f; // 0 = no knockback, 1 = full knockback
    private float vertical = 0.0f;
    
    public AntiVelocity() {
        super("AntiVelocity", "Reduces knockback from attacks and explosions", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        // Logic handled in mixin that intercepts velocity packets
    }
    
    public float getHorizontal() { return horizontal; }
    public void setHorizontal(float h) { this.horizontal = h; }
    public float getVertical() { return vertical; }
    public void setVertical(float v) { this.vertical = v; }
    
    // Called from mixin to modify incoming velocity
    public void modifyVelocity(double[] velocity) {
        if (!isEnabled()) return;
        
        velocity[0] *= horizontal; // x
        velocity[1] *= vertical;   // y
        velocity[2] *= horizontal; // z
    }
}
