package com.travelclient.module.render;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import com.travelclient.module.movement.ElytraFly;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class ElytraDebugRenderer extends Module {
    
    public ElytraDebugRenderer() {
        super("ElytraDebug", "Visualize elytra pathfinding - toggle with #elytra debug", Category.RENDER);
    }
    
    @Override
    public void onTick() {
        // Sync with ElytraFly debug mode
        ElytraFly elytraFly = TravelClient.moduleManager.getModule(ElytraFly.class);
        if (elytraFly != null) {
            elytraFly.setDebugMode(this.isEnabled());
        }
    }
    
    public void render(MatrixStack matrices, Camera camera) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        
        ElytraFly elytraFly = TravelClient.moduleManager.getModule(ElytraFly.class);
        if (elytraFly == null || !elytraFly.isEnabled()) return;
        
        Vec3d camPos = camera.getPos();
        
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        
        // Get the tessellator and buffer
        Tessellator tessellator = Tessellator.getInstance();
        
        // Draw all scanned paths in green (clear) or yellow (blocked early)
        for (ElytraFly.PathVisualization path : elytraFly.getScannedPaths()) {
            if (path.clear) {
                drawLine(matrices, path.start, path.end, 0.0f, 1.0f, 0.0f, 0.3f); // Green, transparent
            } else if (path.blocked) {
                drawLine(matrices, path.start, path.end, 1.0f, 0.5f, 0.0f, 0.5f); // Orange
            } else {
                drawLine(matrices, path.start, path.end, 1.0f, 1.0f, 0.0f, 0.2f); // Yellow, very transparent
            }
        }
        
        // Draw chosen path in bright red (thick)
        ElytraFly.PathVisualization chosen = elytraFly.getChosenPath();
        if (chosen != null) {
            // Draw multiple lines for thickness
            for (float offset = -0.3f; offset <= 0.3f; offset += 0.1f) {
                Vec3d start = chosen.start.add(offset, offset, offset);
                Vec3d end = chosen.end.add(offset, offset, offset);
                drawLine(matrices, start, end, 1.0f, 0.0f, 0.0f, 1.0f); // Bright red, solid
            }
        }
        
        // Draw predicted position box
        Vec3d predicted = elytraFly.getPredictedPosition();
        if (predicted != null) {
            drawBox(matrices, predicted, 1.0f, 0.0f, 0.0f, 0.5f); // Red box
        }
        
        // Draw forward direction indicator
        Vec3d playerPos = mc.player.getPos();
        double forwardYawRad = Math.toRadians(mc.player.getYaw());
        Vec3d forwardEnd = new Vec3d(
            playerPos.x - Math.sin(forwardYawRad) * 20,
            playerPos.y,
            playerPos.z + Math.cos(forwardYawRad) * 20
        );
        drawLine(matrices, playerPos, forwardEnd, 0.0f, 0.5f, 1.0f, 1.0f); // Blue - current direction
        
        matrices.pop();
    }
    
    private void drawLine(MatrixStack matrices, Vec3d start, Vec3d end, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z)
              .color(r, g, b, a);
        buffer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z)
              .color(r, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void drawBox(MatrixStack matrices, Vec3d center, float r, float g, float b, float a) {
        float size = 0.5f;
        
        Vec3d[] corners = new Vec3d[] {
            center.add(-size, -size, -size),
            center.add(size, -size, -size),
            center.add(size, -size, size),
            center.add(-size, -size, size),
            center.add(-size, size, -size),
            center.add(size, size, -size),
            center.add(size, size, size),
            center.add(-size, size, size)
        };
        
        // Bottom
        drawLine(matrices, corners[0], corners[1], r, g, b, a);
        drawLine(matrices, corners[1], corners[2], r, g, b, a);
        drawLine(matrices, corners[2], corners[3], r, g, b, a);
        drawLine(matrices, corners[3], corners[0], r, g, b, a);
        
        // Top
        drawLine(matrices, corners[4], corners[5], r, g, b, a);
        drawLine(matrices, corners[5], corners[6], r, g, b, a);
        drawLine(matrices, corners[6], corners[7], r, g, b, a);
        drawLine(matrices, corners[7], corners[4], r, g, b, a);
        
        // Verticals
        drawLine(matrices, corners[0], corners[4], r, g, b, a);
        drawLine(matrices, corners[1], corners[5], r, g, b, a);
        drawLine(matrices, corners[2], corners[6], r, g, b, a);
        drawLine(matrices, corners[3], corners[7], r, g, b, a);
    }
}
