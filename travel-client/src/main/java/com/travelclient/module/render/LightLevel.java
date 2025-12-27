package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

public class LightLevel extends Module {
    
    private int radius = 8;
    private int safeLevel = 8; // Mobs can't spawn at 8+
    
    public LightLevel() {
        super("LightLevel", "Shows light levels on blocks for spawn-proofing", Category.RENDER);
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
        
        BlockPos playerPos = mc.player.getBlockPos();
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    BlockState below = mc.world.getBlockState(pos.down());
                    
                    // Only show on air blocks with solid below
                    if (!state.isAir()) continue;
                    if (!below.isSolidBlock(mc.world, pos.down())) continue;
                    
                    int light = mc.world.getLightLevel(LightType.BLOCK, pos);
                    
                    // Color based on light level
                    float r, g, b;
                    if (light < safeLevel) {
                        // Red - mobs can spawn
                        r = 1.0f; g = 0.0f; b = 0.0f;
                    } else {
                        // Green - safe
                        r = 0.0f; g = 1.0f; b = 0.0f;
                    }
                    
                    // Draw X marker on the block
                    float bx = pos.getX() + 0.5f;
                    float by = pos.getY() + 0.01f;
                    float bz = pos.getZ() + 0.5f;
                    float size = 0.3f;
                    
                    buffer.vertex(matrix, bx - size, by, bz - size).color(r, g, b, 0.8f);
                    buffer.vertex(matrix, bx + size, by, bz + size).color(r, g, b, 0.8f);
                    buffer.vertex(matrix, bx - size, by, bz + size).color(r, g, b, 0.8f);
                    buffer.vertex(matrix, bx + size, by, bz - size).color(r, g, b, 0.8f);
                }
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        matrices.pop();
    }
    
    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }
}
