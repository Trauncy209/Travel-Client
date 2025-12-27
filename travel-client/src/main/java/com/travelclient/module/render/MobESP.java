package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.util.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;

public class MobESP extends Module {
    
    private double range = 100;
    private boolean tracers = true;
    private boolean box = true;
    private boolean hostile = true;
    private boolean passive = true;
    
    public MobESP() {
        super("MobESP", "Highlights mobs", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void onRender(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        RenderUtils.setup();
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (mc.player.distanceTo(entity) > range) continue;
            
            float r, g, b;
            
            if (entity instanceof HostileEntity) {
                if (!hostile) continue;
                r = 1.0f; g = 0.5f; b = 0.0f; // Orange for hostile
            } else if (entity instanceof AnimalEntity) {
                if (!passive) continue;
                r = 0.0f; g = 1.0f; b = 0.5f; // Green for passive
            } else if (entity instanceof VillagerEntity) {
                if (!passive) continue;
                r = 0.5f; g = 0.5f; b = 1.0f; // Blue for villagers
            } else {
                continue;
            }
            
            if (box) {
                RenderUtils.drawEntityBox(matrices, entity, r, g, b, 1.0f);
            }
            
            if (tracers) {
                RenderUtils.drawTracer(matrices, entity, r, g, b, 0.5f);
            }
        }
        
        RenderUtils.cleanup();
    }
    
    public void setRange(double r) { range = r; }
    public void setTracers(boolean t) { tracers = t; }
    public void setBox(boolean b) { box = b; }
}
