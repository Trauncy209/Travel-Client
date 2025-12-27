package com.travelclient.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class RenderUtils {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public static void setup() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }
    
    public static void cleanup() {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    public static void drawBox(MatrixStack matrices, Box box, float r, float g, float b, float a) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        
        double x1 = box.minX - cam.x;
        double y1 = box.minY - cam.y;
        double z1 = box.minZ - cam.z;
        double x2 = box.maxX - cam.x;
        double y2 = box.maxY - cam.y;
        double z2 = box.maxZ - cam.z;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Bottom
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
        
        // Top
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
        
        // Verticals
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
        
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
        buffer.vertex(matrix, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    public static void drawLine(MatrixStack matrices, Vec3d start, Vec3d end, float r, float g, float b, float a) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, (float)(start.x - cam.x), (float)(start.y - cam.y), (float)(start.z - cam.z)).color(r, g, b, a);
        buffer.vertex(matrix, (float)(end.x - cam.x), (float)(end.y - cam.y), (float)(end.z - cam.z)).color(r, g, b, a);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    public static void drawLine(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        drawLine(matrices, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), r, g, b, a);
    }
    
    public static void drawTracer(MatrixStack matrices, Entity entity, float r, float g, float b, float a) {
        if (mc.player == null) return;
        
        Vec3d start = new Vec3d(0, 0, 1)
            .rotateX((float) -Math.toRadians(mc.player.getPitch()))
            .rotateY((float) -Math.toRadians(mc.player.getYaw()))
            .add(mc.gameRenderer.getCamera().getPos());
        
        Vec3d end = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        drawLine(matrices, start, end, r, g, b, a);
    }
    
    public static void drawEntityBox(MatrixStack matrices, Entity entity, float r, float g, float b, float a) {
        Box box = entity.getBoundingBox();
        drawBox(matrices, box, r, g, b, a);
    }
}
