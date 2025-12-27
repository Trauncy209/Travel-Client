package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.module.ModuleManager;
import com.travelclient.module.movement.ElytraFly;
import com.travelclient.module.movement.ElytraNavigate;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class HudModule extends Module {
    
    private final ModuleManager manager;
    
    public HudModule(ModuleManager manager) {
        super("HUD", "On-screen info display", Category.RENDER);
        this.manager = manager;
    }
    
    @Override
    public void onTick() {}
    
    public void render(DrawContext context) {
        if (mc.player == null || !isEnabled()) return;
        
        List<String> lines = new ArrayList<>();
        
        // Coordinates
        lines.add(String.format("§fXYZ: §7%.1f / %.1f / %.1f",
            mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        
        // Speed
        double speed = mc.player.getVelocity().horizontalLength() * 20;
        lines.add(String.format("§fSpeed: §7%.1f b/s", speed));
        
        // FPS
        lines.add("§fFPS: §7" + mc.getCurrentFps());
        
        // Target
        if (manager.hasTarget()) {
            double dist = manager.getDistanceToTarget(mc.player.getX(), mc.player.getZ());
            lines.add(String.format("§fTarget: §a%.0f, %.0f §7(%.0fm)", 
                manager.getTargetX(), manager.getTargetZ(), dist));
        }
        
        // Elytra status
        ElytraFly elytraFly = manager.getModule(ElytraFly.class);
        if (elytraFly != null && elytraFly.isEnabled()) {
            lines.add("§fFlight: " + elytraFly.getFlightStatus());
        }
        
        // Navigation status
        ElytraNavigate nav = manager.getModule(ElytraNavigate.class);
        if (nav != null && nav.isEnabled()) {
            double dist = nav.getDistanceToTarget();
            if (dist > 0) {
                lines.add(String.format("§fNav: §a%.0fm to target", dist));
            }
        }
        
        lines.add("");
        
        // Active modules (compact)
        StringBuilder active = new StringBuilder("§6Enabled: ");
        int count = 0;
        for (Module m : manager.getModules()) {
            if (m.isEnabled() && m != this) {
                if (count > 0) active.append("§7, ");
                active.append("§a").append(m.getName());
                count++;
            }
        }
        if (count > 0) lines.add(active.toString());
        
        // Render top-left HUD
        int y = 5;
        for (String line : lines) {
            context.drawTextWithShadow(mc.textRenderer, line, 5, y, 0xFFFFFF);
            y += 10;
        }
        
        // === BOTTOM OF SCREEN - NewerChunks Status ===
        NewerChunks newerChunks = manager.getModule(NewerChunks.class);
        if (newerChunks != null && newerChunks.isEnabled()) {
            String chunkStatus = newerChunks.getCurrentChunkStatus();
            int signs = newerChunks.getCurrentChunkSigns();
            
            int screenHeight = mc.getWindow().getScaledHeight();
            int screenWidth = mc.getWindow().getScaledWidth();
            
            // Draw chunk status at bottom center
            String statusLine = "§fChunk: " + chunkStatus;
            if (signs > 0) {
                statusLine += " §7(" + signs + " signs)";
            }
            
            int textWidth = mc.textRenderer.getWidth(statusLine.replaceAll("§.", ""));
            int x = (screenWidth - textWidth) / 2;
            int bottomY = screenHeight - 25;
            
            // Draw background box
            context.fill(x - 5, bottomY - 3, x + textWidth + 5, bottomY + 12, 0x80000000);
            
            // Draw text
            context.drawTextWithShadow(mc.textRenderer, statusLine, x, bottomY, 0xFFFFFF);
            
            // Draw color legend below
            String legend = "§cRed=Recent §eYellow=Old §aGreen=Light §7Gray=Empty";
            int legendWidth = mc.textRenderer.getWidth(legend.replaceAll("§.", ""));
            int legendX = (screenWidth - legendWidth) / 2;
            context.drawTextWithShadow(mc.textRenderer, legend, legendX, bottomY + 12, 0xAAAAAA);
        }
    }
}
