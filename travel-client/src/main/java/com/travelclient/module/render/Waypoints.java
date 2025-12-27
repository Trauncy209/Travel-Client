package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class Waypoints extends Module {
    
    private final List<Waypoint> waypoints = new ArrayList<>();
    private boolean showBeam = true;
    private boolean showTracer = true;
    private boolean showDistance = true;
    
    public Waypoints() {
        super("Waypoints", "Save and display custom waypoint locations", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void render(MatrixStack matrices, float tickDelta) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        if (waypoints.isEmpty()) return;
        
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        
        for (Waypoint wp : waypoints) {
            // Check dimension
            String currentDim = mc.world.getRegistryKey().getValue().toString();
            if (!wp.dimension.equals(currentDim)) continue;
            
            if (showBeam) {
                renderBeam(matrices, wp);
            }
            
            if (showTracer) {
                renderTracer(matrices, camPos, new Vec3d(wp.x, wp.y, wp.z), wp.color);
            }
        }
        
        matrices.pop();
    }
    
    private void renderBeam(MatrixStack matrices, Waypoint wp) {
        float r = ((wp.color >> 16) & 0xFF) / 255.0f;
        float g = ((wp.color >> 8) & 0xFF) / 255.0f;
        float b = (wp.color & 0xFF) / 255.0f;
        float a = 0.5f;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Draw a vertical beam
        float x = (float) wp.x + 0.5f;
        float z = (float) wp.z + 0.5f;
        
        buffer.vertex(matrix, x, 0, z).color(r, g, b, a);
        buffer.vertex(matrix, x, 256, z).color(r, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderTracer(MatrixStack matrices, Vec3d from, Vec3d to, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z).color(r, g, b, 0.6f);
        buffer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z).color(r, g, b, 0.6f);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    public void addWaypoint(String name, double x, double y, double z, String dimension, int color) {
        waypoints.add(new Waypoint(name, x, y, z, dimension, color));
    }
    
    public void addWaypointAtPlayer(String name, int color) {
        if (mc.player == null || mc.world == null) return;
        String dim = mc.world.getRegistryKey().getValue().toString();
        waypoints.add(new Waypoint(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim, color));
    }
    
    public void removeWaypoint(String name) {
        waypoints.removeIf(wp -> wp.name.equals(name));
    }
    
    public void clearWaypoints() {
        waypoints.clear();
    }
    
    public List<Waypoint> getWaypoints() {
        return waypoints;
    }
    
    public static class Waypoint {
        public final String name;
        public final double x, y, z;
        public final String dimension;
        public final int color;
        
        public Waypoint(String name, double x, double y, double z, String dimension, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
        }
    }
}
