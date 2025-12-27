package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.movement.ElytraFly;
import com.travelclient.module.render.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRenderMixin {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(matrix4f);
        
        // === RENDER ALL ESP MODULES ===
        
        PlayerESP playerESP = TravelClient.moduleManager.getModule(PlayerESP.class);
        if (playerESP != null && playerESP.isEnabled()) {
            playerESP.onRender(matrices);
        }
        
        MobESP mobESP = TravelClient.moduleManager.getModule(MobESP.class);
        if (mobESP != null && mobESP.isEnabled()) {
            mobESP.onRender(matrices);
        }
        
        ChestESP chestESP = TravelClient.moduleManager.getModule(ChestESP.class);
        if (chestESP != null && chestESP.isEnabled()) {
            chestESP.onRender(matrices);
        }
        
        ItemESP itemESP = TravelClient.moduleManager.getModule(ItemESP.class);
        if (itemESP != null && itemESP.isEnabled()) {
            itemESP.onRender(matrices);
        }
        
        BlockESP blockESP = TravelClient.moduleManager.getModule(BlockESP.class);
        if (blockESP != null && blockESP.isEnabled()) {
            blockESP.render(matrices, tickCounter.getTickDelta(true));
        }
        
        // NewerChunks overlay
        NewerChunks newerChunks = TravelClient.moduleManager.getModule(NewerChunks.class);
        if (newerChunks != null && newerChunks.isEnabled()) {
            newerChunks.onRender(matrices);
        }
        
        // === ELYTRA DEBUG RENDERER ===
        
        ElytraDebugRenderer debugRenderer = TravelClient.moduleManager.getModule(ElytraDebugRenderer.class);
        if (debugRenderer == null || !debugRenderer.isEnabled()) return;
        
        ElytraFly elytraFly = TravelClient.moduleManager.getModule(ElytraFly.class);
        if (elytraFly == null || !elytraFly.isEnabled()) return;
        
        Vec3d camPos = camera.getPos();
        
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        java.util.List<Vec3d> path = elytraFly.getSimulatedPath();
        Vec3d collision = elytraFly.getCollisionPoint();
        
        if (path.size() >= 2) {
            for (int i = 0; i < path.size() - 1; i++) {
                Vec3d start = path.get(i);
                Vec3d end = path.get(i + 1);
                drawVeryBoldLine(matrices, start, end, 0.2f, 1.0f, 0.2f, 1.0f);
            }
        }
        
        if (collision != null) {
            drawBigX(matrices, collision, 1.0f, 0.0f, 0.0f, 1.0f, 2.0f);
        }
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d vel = mc.player.getVelocity();
        if (vel.length() > 0.05) {
            Vec3d velEnd = playerPos.add(vel.normalize().multiply(8));
            drawVeryBoldLine(matrices, playerPos, velEnd, 0.0f, 1.0f, 1.0f, 0.9f);
        }
        
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    private void drawVeryBoldLine(MatrixStack matrices, Vec3d start, Vec3d end, float r, float g, float b, float a) {
        float[] offsets = {-0.15f, 0f, 0.15f};
        for (float ox : offsets) {
            for (float oy : offsets) {
                drawLine(matrices, start.add(ox, oy, 0), end.add(ox, oy, 0), r, g, b, a * 0.8f);
            }
        }
    }
    
    private void drawBigX(MatrixStack matrices, Vec3d pos, float r, float g, float b, float a, float size) {
        drawVeryBoldLine(matrices, pos.add(-size, -size, -size), pos.add(size, size, size), r, g, b, a);
        drawVeryBoldLine(matrices, pos.add(size, -size, -size), pos.add(-size, size, size), r, g, b, a);
    }
    
    private void drawLine(MatrixStack matrices, Vec3d start, Vec3d end, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).color(r, g, b, a);
        buffer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
