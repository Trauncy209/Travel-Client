package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.util.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerESP extends Module {
    
    private double range = 200;
    private boolean tracers = true;
    private boolean box = true;
    
    public PlayerESP() {
        super("PlayerESP", "Highlights players", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void onRender(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        RenderUtils.setup();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range) continue;
            
            // Red color for players
            float r = 1.0f, g = 0.2f, b = 0.2f, a = 1.0f;
            
            // Friends could be green (not implemented yet)
            
            if (box) {
                RenderUtils.drawEntityBox(matrices, player, r, g, b, a);
            }
            
            if (tracers) {
                RenderUtils.drawTracer(matrices, player, r, g, b, 0.7f);
            }
        }
        
        RenderUtils.cleanup();
    }
    
    public void setRange(double r) { range = r; }
    public double getRange() { return range; }
    public void setTracers(boolean t) { tracers = t; }
    public boolean hasTracers() { return tracers; }
    public void setBox(boolean b) { box = b; }
    public boolean hasBox() { return box; }
}
