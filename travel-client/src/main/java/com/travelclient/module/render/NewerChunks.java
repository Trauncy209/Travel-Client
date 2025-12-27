package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.util.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;

public class NewerChunks extends Module {
    
    // Track chunk activity - ChunkPos -> timestamp of last player activity
    private static final Map<ChunkPos, Long> chunkActivity = new HashMap<>();
    
    // Track chunks with player-made structures
    private static final Map<ChunkPos, Integer> chunkPlayerSigns = new HashMap<>();
    
    private int scanCooldown = 0;
    private ChunkPos lastChunk = null;
    
    // Time thresholds (in milliseconds)
    private static final long VERY_RECENT = 5 * 60 * 1000;      // 5 minutes
    private static final long RECENT = 30 * 60 * 1000;          // 30 minutes  
    private static final long SOMEWHAT_RECENT = 2 * 60 * 60 * 1000; // 2 hours
    private static final long OLD = 24 * 60 * 60 * 1000;        // 24 hours
    
    public NewerChunks() {
        super("NewerChunks", "Tracks player activity in chunks", Category.RENDER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        ChunkPos currentChunk = new ChunkPos(mc.player.getBlockPos());
        
        // Mark current chunk as having recent activity (us)
        chunkActivity.put(currentChunk, System.currentTimeMillis());
        
        // Track other players
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            ChunkPos playerChunk = new ChunkPos(player.getBlockPos());
            chunkActivity.put(playerChunk, System.currentTimeMillis());
        }
        
        // Scan surrounding chunks for player signs periodically
        if (scanCooldown > 0) {
            scanCooldown--;
        } else {
            scanCooldown = 100; // Every 5 seconds
            scanForPlayerSigns();
        }
        
        // Alert when entering chunk with recent activity
        if (lastChunk == null || !lastChunk.equals(currentChunk)) {
            lastChunk = currentChunk;
            
            Long lastActivity = chunkActivity.get(currentChunk);
            Integer playerSigns = chunkPlayerSigns.get(currentChunk);
            
            if (lastActivity != null || (playerSigns != null && playerSigns > 0)) {
                String status = getChunkStatus(currentChunk);
                if (!status.equals("§7Empty")) {
                    // Don't spam for chunks we just visited
                    if (lastActivity == null || System.currentTimeMillis() - lastActivity > 10000) {
                        mc.player.sendMessage(Text.of("§e[NewerChunks] §fEntered chunk: " + status), false);
                    }
                }
            }
        }
    }
    
    /**
     * Scan nearby chunks for signs of player activity
     */
    private void scanForPlayerSigns() {
        BlockPos playerPos = mc.player.getBlockPos();
        ChunkPos playerChunk = new ChunkPos(playerPos);
        
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                ChunkPos checkChunk = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                
                if (chunkPlayerSigns.containsKey(checkChunk)) continue; // Already scanned
                
                WorldChunk chunk = mc.world.getChunk(checkChunk.x, checkChunk.z);
                if (chunk == null) continue;
                
                int signs = countPlayerSigns(chunk);
                chunkPlayerSigns.put(checkChunk, signs);
                
                // If very high activity, also mark as "recent"
                if (signs >= 20 && !chunkActivity.containsKey(checkChunk)) {
                    // Estimate based on block freshness (obsidian/player blocks = recent base)
                    chunkActivity.put(checkChunk, System.currentTimeMillis() - SOMEWHAT_RECENT);
                }
            }
        }
    }
    
    /**
     * Count signs of player activity in a chunk
     */
    private int countPlayerSigns(WorldChunk chunk) {
        int signs = 0;
        
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        
        // Sample blocks in the chunk
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y += 4) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    
                    // Torches
                    if (block == Blocks.TORCH || block == Blocks.WALL_TORCH ||
                        block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH ||
                        block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN) {
                        signs += 3;
                    }
                    
                    // Player-placed blocks
                    if (block == Blocks.COBBLESTONE || block == Blocks.COBBLED_DEEPSLATE) {
                        if (y < 60) signs += 1; // Underground cobble = mining
                    }
                    
                    // Building materials in weird places
                    if (block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS ||
                        block == Blocks.BIRCH_PLANKS || block == Blocks.DARK_OAK_PLANKS) {
                        signs += 2;
                    }
                    
                    // Chests/containers
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST ||
                        block == Blocks.BARREL || block == Blocks.ENDER_CHEST) {
                        signs += 5;
                    }
                    
                    // Shulker boxes
                    if (block.toString().contains("shulker")) {
                        signs += 10;
                    }
                    
                    // Obsidian (bases/portals)
                    if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
                        signs += 3;
                    }
                    
                    // Beds
                    if (block.toString().contains("_bed")) {
                        signs += 5;
                    }
                    
                    // Furnaces/crafting
                    if (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE ||
                        block == Blocks.CRAFTING_TABLE || block == Blocks.ANVIL) {
                        signs += 4;
                    }
                    
                    // Rails
                    if (block == Blocks.RAIL || block == Blocks.POWERED_RAIL ||
                        block == Blocks.DETECTOR_RAIL || block == Blocks.ACTIVATOR_RAIL) {
                        signs += 2;
                    }
                    
                    // Farms
                    if (block == Blocks.FARMLAND || block == Blocks.WHEAT ||
                        block == Blocks.CARROTS || block == Blocks.POTATOES) {
                        signs += 3;
                    }
                }
            }
        }
        
        return signs;
    }
    
    /**
     * Get status string for a chunk
     */
    public String getChunkStatus(ChunkPos chunk) {
        Long lastActivity = chunkActivity.get(chunk);
        Integer playerSigns = chunkPlayerSigns.getOrDefault(chunk, 0);
        
        long now = System.currentTimeMillis();
        
        if (lastActivity != null) {
            long age = now - lastActivity;
            
            if (age < VERY_RECENT) {
                return "§c§lVERY RECENT §7(<5min)";
            } else if (age < RECENT) {
                return "§c§lRECENT §7(<30min)";
            } else if (age < SOMEWHAT_RECENT) {
                return "§ePlayer activity §7(<2hr)";
            } else if (age < OLD) {
                return "§eOld activity §7(<24hr)";
            }
        }
        
        if (playerSigns >= 30) {
            return "§c§lHEAVY player base";
        } else if (playerSigns >= 15) {
            return "§eMedium activity";
        } else if (playerSigns >= 5) {
            return "§aLight activity";
        }
        
        return "§7Empty";
    }
    
    /**
     * Get color for chunk based on activity
     */
    public int getChunkColor(ChunkPos chunk) {
        Long lastActivity = chunkActivity.get(chunk);
        Integer playerSigns = chunkPlayerSigns.getOrDefault(chunk, 0);
        
        long now = System.currentTimeMillis();
        
        if (lastActivity != null) {
            long age = now - lastActivity;
            
            if (age < VERY_RECENT) {
                return 0xFFFF0000; // Red
            } else if (age < RECENT) {
                return 0xFFFF6600; // Orange
            } else if (age < SOMEWHAT_RECENT) {
                return 0xFFFFFF00; // Yellow
            } else if (age < OLD) {
                return 0xFF00FF00; // Green
            }
        }
        
        if (playerSigns >= 30) {
            return 0xFFFF0000; // Red
        } else if (playerSigns >= 15) {
            return 0xFFFFFF00; // Yellow
        } else if (playerSigns >= 5) {
            return 0xFF00FF00; // Green
        }
        
        return 0x00000000; // Transparent
    }
    
    public void onRender(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        RenderUtils.setup();
        
        ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        
        // Render at bedrock level
        double renderY = mc.world.getBottomY(); // -64 in modern MC
        
        // Render chunk borders with colors
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                ChunkPos chunk = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                int color = getChunkColor(chunk);
                
                if (color == 0) continue; // Skip empty chunks
                
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >> 8) & 0xFF) / 255f;
                float b = (color & 0xFF) / 255f;
                
                int startX = chunk.getStartX();
                int startZ = chunk.getStartZ();
                
                // Draw flat chunk plane at bedrock level
                Box chunkBox = new Box(startX, renderY, startZ, startX + 16, renderY + 0.5, startZ + 16);
                RenderUtils.drawBox(matrices, chunkBox, r, g, b, 0.4f);
                
                // Draw chunk border lines (more visible)
                RenderUtils.drawLine(matrices, startX, renderY + 0.6, startZ, startX + 16, renderY + 0.6, startZ, r, g, b, 1.0f);
                RenderUtils.drawLine(matrices, startX + 16, renderY + 0.6, startZ, startX + 16, renderY + 0.6, startZ + 16, r, g, b, 1.0f);
                RenderUtils.drawLine(matrices, startX + 16, renderY + 0.6, startZ + 16, startX, renderY + 0.6, startZ + 16, r, g, b, 1.0f);
                RenderUtils.drawLine(matrices, startX, renderY + 0.6, startZ + 16, startX, renderY + 0.6, startZ, r, g, b, 1.0f);
            }
        }
        
        // Draw player position marker - a bright cyan dot showing exactly where you are
        float dotSize = 0.5f;
        Box playerDot = new Box(
            playerX - dotSize, renderY + 0.5, playerZ - dotSize,
            playerX + dotSize, renderY + 2.0, playerZ + dotSize
        );
        RenderUtils.drawBox(matrices, playerDot, 0.0f, 1.0f, 1.0f, 1.0f); // Cyan, full opacity
        
        // Draw a vertical beam from dot up to player
        RenderUtils.drawLine(matrices, playerX, renderY + 2, playerZ, playerX, mc.player.getY(), playerZ, 0.0f, 1.0f, 1.0f, 0.3f);
        
        RenderUtils.cleanup();
    }
    
    /**
     * Get current chunk status for HUD display
     */
    public String getCurrentChunkStatus() {
        if (mc.player == null) return "";
        ChunkPos current = new ChunkPos(mc.player.getBlockPos());
        return getChunkStatus(current);
    }
    
    /**
     * Get player signs count for current chunk
     */
    public int getCurrentChunkSigns() {
        if (mc.player == null) return 0;
        ChunkPos current = new ChunkPos(mc.player.getBlockPos());
        return chunkPlayerSigns.getOrDefault(current, 0);
    }
    
    /**
     * Clear all tracking data
     */
    public void clearData() {
        chunkActivity.clear();
        chunkPlayerSigns.clear();
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[NewerChunks] Data cleared!"), false);
        }
    }
    
    @Override
    public void onDisable() {
        // Keep data even when disabled
    }
}