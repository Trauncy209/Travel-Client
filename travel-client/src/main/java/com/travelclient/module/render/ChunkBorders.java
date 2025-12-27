package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import org.joml.Matrix4f;

public class ChunkBorders extends Module {
    
    private int color = 0xFFFF0000; // Red
    private int renderDistance = 2; // Chunks around player
    
    public ChunkBorders() {
        super("ChunkBorders", "Shows chunk boundaries", Category.RENDER);
    }
    
    @Override
    public void onTick() {}
    
    public void render(MatrixStack matrices, float tickDelta) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        double camX = mc.gameRenderer.getCamera().getPos().x;
        double camY = mc.gameRenderer.getCamera().getPos().y;
        double camZ = mc.gameRenderer.getCamera().getPos().z;
        
        matrices.push();
        matrices.translate(-camX, -camY, -camZ);
        
        ChunkPos playerChunk = mc.player.getChunkPos();
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.6f;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Draw chunk borders
        for (int cx = -renderDistance; cx <= renderDistance; cx++) {
            for (int cz = -renderDistance; cz <= renderDistance; cz++) {
                int chunkX = (playerChunk.x + cx) * 16;
                int chunkZ = (playerChunk.z + cz) * 16;
                
                // Draw vertical lines at chunk corners
                for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y += 16) {
                    // Corner lines
                    buffer.vertex(matrix, chunkX, y, chunkZ).color(r, g, b, a);
                    buffer.vertex(matrix, chunkX, y + 16, chunkZ).color(r, g, b, a);
                    
                    buffer.vertex(matrix, chunkX + 16, y, chunkZ).color(r, g, b, a);
                    buffer.vertex(matrix, chunkX + 16, y + 16, chunkZ).color(r, g, b, a);
                    
                    buffer.vertex(matrix, chunkX, y, chunkZ + 16).color(r, g, b, a);
                    buffer.vertex(matrix, chunkX, y + 16, chunkZ + 16).color(r, g, b, a);
                    
                    buffer.vertex(matrix, chunkX + 16, y, chunkZ + 16).color(r, g, b, a);
                    buffer.vertex(matrix, chunkX + 16, y + 16, chunkZ + 16).color(r, g, b, a);
                }
                
                // Horizontal lines at player Y level
                float py = (float) mc.player.getY();
                
                // Bottom edges
                buffer.vertex(matrix, chunkX, py, chunkZ).color(r, g, b, a);
                buffer.vertex(matrix, chunkX + 16, py, chunkZ).color(r, g, b, a);
                
                buffer.vertex(matrix, chunkX, py, chunkZ).color(r, g, b, a);
                buffer.vertex(matrix, chunkX, py, chunkZ + 16).color(r, g, b, a);
                
                buffer.vertex(matrix, chunkX + 16, py, chunkZ).color(r, g, b, a);
                buffer.vertex(matrix, chunkX + 16, py, chunkZ + 16).color(r, g, b, a);
                
                buffer.vertex(matrix, chunkX, py, chunkZ + 16).color(r, g, b, a);
                buffer.vertex(matrix, chunkX + 16, py, chunkZ + 16).color(r, g, b, a);
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        matrices.pop();
    }
    
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
