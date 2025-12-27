package com.travelclient.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.travelclient.module.Module;
import net.minecraft.block.Block;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockESP extends Module {
    
    private final Set<Block> targetBlocks = new HashSet<>();
    private final List<BlockPos> foundBlocks = new CopyOnWriteArrayList<>();
    private int scanTick = 0;
    private double range = 32;
    private String currentSearch = "";
    private int blockColor = 0xFF00FFFF;
    private int lastCount = -1;
    
    public BlockESP() {
        super("BlockESP", "X-ray highlights searched blocks", Category.RENDER);
    }
    
    public void setSearchBlock(String search) {
        targetBlocks.clear();
        foundBlocks.clear();
        currentSearch = search;
        lastCount = -1;
        
        String searchLower = search.toLowerCase().replace(" ", "_");
        
        // Set color based on ore type
        if (searchLower.contains("diamond")) blockColor = 0xFF00FFFF;
        else if (searchLower.contains("emerald")) blockColor = 0xFF00FF00;
        else if (searchLower.contains("gold")) blockColor = 0xFFFFD700;
        else if (searchLower.contains("iron")) blockColor = 0xFFD4AF37;
        else if (searchLower.contains("coal")) blockColor = 0xFF555555;
        else if (searchLower.contains("redstone")) blockColor = 0xFFFF0000;
        else if (searchLower.contains("lapis")) blockColor = 0xFF0000FF;
        else if (searchLower.contains("copper")) blockColor = 0xFFB87333;
        else if (searchLower.contains("ancient") || searchLower.contains("debris")) blockColor = 0xFFFF6600;
        else if (searchLower.contains("quartz")) blockColor = 0xFFFFFFFF;
        else blockColor = 0xFFFF00FF;
        
        // Find matching block types
        int typeCount = 0;
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id.getPath().toLowerCase().contains(searchLower)) {
                targetBlocks.add(block);
                typeCount++;
                if (mc.player != null) {
                    mc.player.sendMessage(Text.of("§a+ Tracking: " + id.getPath()), false);
                }
            }
        }
        
        if (typeCount == 0) {
            if (mc.player != null) mc.player.sendMessage(Text.of("§cNo block types match: " + search), false);
            return;
        }
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§aTracking " + typeCount + " block types. Scanning..."), false);
        }
        
        // Enable and force immediate scan
        if (!isEnabled()) toggle();
        scanTick = 100; // Force scan on next tick
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (targetBlocks.isEmpty()) return;
        
        scanTick++;
        
        // Scan every 10 ticks (0.5 seconds) for responsiveness
        if (scanTick >= 10) {
            scanTick = 0;
            doScan();
            
            // Show count in actionbar
            int count = foundBlocks.size();
            if (count != lastCount) {
                lastCount = count;
                mc.player.sendMessage(Text.of("§b[BlockESP] §fFound: §e" + count + " §f(" + currentSearch + ")"), true);
            }
        }
    }
    
    private void doScan() {
        if (mc.player == null || mc.world == null) return;
        if (targetBlocks.isEmpty()) return;
        
        List<BlockPos> newFound = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = (int) range;
        double rangeSq = range * range;
        
        // Scan in a sphere around player
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r; y <= r; y++) {
                    BlockPos pos = center.add(x, y, z);
                    
                    // Skip if out of range
                    double distSq = center.getSquaredDistance(pos);
                    if (distSq > rangeSq) continue;
                    
                    // Check if target block
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (targetBlocks.contains(block)) {
                        newFound.add(pos.toImmutable());
                    }
                }
            }
        }
        
        // Update found blocks
        foundBlocks.clear();
        foundBlocks.addAll(newFound);
    }
    
    public void clearTargets() {
        targetBlocks.clear();
        foundBlocks.clear();
        currentSearch = "";
        lastCount = -1;
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§c[BlockESP] Cleared all targets"), false);
        }
    }
    
    public void render(MatrixStack matrices, float delta) {
        if (mc.player == null || mc.world == null) return;
        if (!isEnabled() || foundBlocks.isEmpty()) return;
        
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        float r = ((blockColor >> 16) & 0xFF) / 255f;
        float g = ((blockColor >> 8) & 0xFF) / 255f;
        float b = (blockColor & 0xFF) / 255f;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Copy list to avoid concurrent modification
        List<BlockPos> toRender = new ArrayList<>(foundBlocks);
        
        for (BlockPos pos : toRender) {
            float x1 = (float) (pos.getX() - cam.x);
            float y1 = (float) (pos.getY() - cam.y);
            float z1 = (float) (pos.getZ() - cam.z);
            float x2 = x1 + 1;
            float y2 = y1 + 1;
            float z2 = z1 + 1;
            
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            
            // Draw box outline
            // Bottom
            buf.vertex(matrix, x1, y1, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y1, z1).color(r, g, b, 1f);
            
            // Top
            buf.vertex(matrix, x1, y2, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y2, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y2, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y2, z1).color(r, g, b, 1f);
            
            // Verticals
            buf.vertex(matrix, x1, y1, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y2, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z1).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x2, y2, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y1, z2).color(r, g, b, 1f);
            buf.vertex(matrix, x1, y2, z2).color(r, g, b, 1f);
            
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
        
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    // Getters for AutoMine integration
    public double getRange() { return range; }
    public void setRange(double r) { range = Math.max(8, Math.min(64, r)); }
    public String getCurrentSearch() { return currentSearch; }
    public int getFoundCount() { return foundBlocks.size(); }
    public List<BlockPos> getFoundBlocks() { return new ArrayList<>(foundBlocks); }
    public boolean hasTargets() { return !targetBlocks.isEmpty(); }
    public Set<Block> getTargetBlocks() { return targetBlocks; }
}
