package com.travelclient.module.render;

import com.travelclient.module.Module;

public class Fullbright extends Module {
    
    public Fullbright() {
        super("Fullbright", "See in the dark - uses lightmap override", Category.RENDER);
    }
    
    @Override
    public void onTick() {
        // Handled by LightmapMixin
    }
    
    @Override
    public void onEnable() {
        // Force lightmap update
        if (mc.gameRenderer != null) {
            mc.gameRenderer.getLightmapTextureManager().tick();
        }
    }
    
    @Override
    public void onDisable() {
        // Force lightmap update to restore normal lighting
        if (mc.gameRenderer != null) {
            mc.gameRenderer.getLightmapTextureManager().tick();
        }
    }
}
